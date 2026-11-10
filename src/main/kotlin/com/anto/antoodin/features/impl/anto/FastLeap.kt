package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.utils.Skit
import com.anto.antoodin.utils.rightClick
import com.odtheking.odin.clickgui.settings.impl.ActionSetting
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.MapSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.MessageEvent
import com.odtheking.odin.events.ScreenEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.features.ModuleManager
import com.odtheking.odin.features.impl.boss.MelodyMessage
import com.odtheking.odin.features.impl.boss.WitherDragonState
import com.odtheking.odin.features.impl.boss.WitherDragonsEnum
import com.odtheking.odin.utils.clickSlot
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.itemId
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.skyblock.dungeon.DungeonClass
import com.odtheking.odin.utils.skyblock.dungeon.DungeonPlayer
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import kotlin.random.Random

/**
 * Left click with a Spirit Leap / Infinileap: cancels the click, opens the leap menu with a simulated right click,
 * then clicks the configured teammate's head after a humanized delay. Works with any leap menu style, since it
 * clicks the real chest slot (heads in 11-15), like Odin's own leapTo.
 *
 * Targets are per own class and per phase (edited in [FastLeapScreen]). The phase follows F7/M7 boss chat lines.
 * Each During S# target is used once, then that section's next target (EE2, EE3, Core, Recore) takes over.
 */
object FastLeap : Module(
    name = "Fast Leap",
    description = "Left click a leap to leap straight to the teammate set for your class and the current phase. /ao fastleap to configure.",
    category = Skit.ANTO
) {
    private val delay by NumberSetting("Delay", 150, 0..1000, 10, desc = "Delay after the leap menu opens before clicking.", unit = "ms")
    private val randomDelay by NumberSetting("Random Delay", 75, 0..500, 5, desc = "Random extra delay on top.", unit = "ms")
    private val window by NumberSetting("Phase Window", 3.0, 1.0..10.0, 0.5, desc = "How long short phases (door opener after blood, Maxor Dead, EE2, EE3, Core, First Dragon Dead) stay active.", unit = "s")
    // ponytail: temporary testing aid, remove once the phases are tuned
    private val dryRun by BooleanSetting("Dry Run", false, desc = "Doesn't leap: says who it would have leaped to and how long it waited after the menu opened, then closes the menu.")
    private val openEditor by ActionSetting("Open Editor", desc = "Opens the per-class leap target editor.") {
        mc.gui.setScreen(FastLeapScreen(mc.gui.screen()))
    }
    private val targets by MapSetting("Targets", mutableMapOf<String, String>())

    enum class Phase(val label: String) {
        DOOR_OPENER("Door Opener"), MAXOR_CRYSTALS("Maxor Crystals"), MAXOR_DEAD("Maxor Dead"),
        BEFORE_LIGHTNING("Before Lightning"), AFTER_LIGHTNING("After Lightning"), STORM_DEAD("Storm Dead"),
        DURING_S1("During S1"), EE2("EE2"), DURING_S2("During S2"), EE3("EE3"), DURING_S3("During S3"),
        CORE("Core"), DURING_S4("During S4"), RECORE("Recore"), GOLDOR_DEAD("Goldor Dead"), NECRON_DEAD("Necron Dead"),
        PICKUP_RELIC("Pickup Relic"), FIRST_DRAGON_DEAD("First Dragon Dead")
    }

    val classes = listOf(DungeonClass.ARCHER, DungeonClass.BERSERK, DungeonClass.MAGE, DungeonClass.HEALER, DungeonClass.TANK)
    private val defaults = mapOf("ARCHER:MAXOR_CRYSTALS" to "T", "BERSERK:MAXOR_CRYSTALS" to "H") +
        classes.associate { "${it.name}:DOOR_OPENER" to "on" }
    private val classLetters = mapOf('A' to DungeonClass.ARCHER, 'B' to DungeonClass.BERSERK, 'M' to DungeonClass.MAGE, 'H' to DungeonClass.HEALER, 'T' to DungeonClass.TANK)

    fun get(clazz: DungeonClass, phase: Phase): String = "${clazz.name}:${phase.name}".let { targets[it] ?: defaults[it] ?: "" }
    fun set(clazz: DungeonClass, phase: Phase, value: String) { targets["${clazz.name}:${phase.name}"] = value.trim() }
    fun save() = ModuleManager.saveConfigurations()

    private val sectionDoneRegex = Regex("^.{1,16} (?:activated|completed) a (?:terminal|lever|device)! \\((\\d)/(\\d)\\)$")

    // Boss phase from chat; windowed phases roll over to the next one when [phaseUntil] passes
    private var bossPhase: Phase? = null
    private var phaseUntil = 0L
    private var finished = false
    private var section = 0
    private var bloodOpenedAt = 0L
    private val dragonsSeenAlive = HashSet<WitherDragonsEnum>()

    /** Resolved leap: [name] is null when nobody in the party matches [value]. */
    private class Pick(val phase: Phase, val value: String, val name: String?)

    private var pending: Pick? = null
    private var timeoutAt = 0L
    private var menuOpenedAt = 0L
    private var rolledDelay = 0L

    private val usedDuring = HashSet<Phase>()
    private val duringNext = mapOf(Phase.DURING_S1 to Phase.EE2, Phase.DURING_S2 to Phase.EE3, Phase.DURING_S3 to Phase.CORE, Phase.DURING_S4 to Phase.RECORE)

    // Melody holder from party chat, reset each section; Odin's melody websocket is checked first
    private var melodyFromChat: String? = null
    private val melodyChatRegex = Regex("^Party > (?:\\[[^\\]]+] )?(\\w{1,16}): .*\\b(?:melody|meowlody)\\b.*$", RegexOption.IGNORE_CASE)

    /** Set with /ao fastleap phase|next to test phases anywhere; cleared on world change. */
    private var forcedPhase: Phase? = null

    private val rollover = mapOf(
        Phase.MAXOR_DEAD to Phase.BEFORE_LIGHTNING, Phase.EE2 to Phase.DURING_S2, Phase.EE3 to Phase.DURING_S3,
        Phase.CORE to Phase.DURING_S4, Phase.FIRST_DRAGON_DEAD to null
    )

    private fun enter(phase: Phase) {
        if (finished) return
        bossPhase = phase
        phaseUntil = if (phase in rollover) System.currentTimeMillis() + (window * 1000).toLong() else Long.MAX_VALUE
    }

    private fun currentPhase(): Phase? {
        forcedPhase?.let { return it }
        if (!DungeonUtils.inDungeons || finished) return null
        if (!DungeonUtils.inBoss) {
            val doorOpenerActive = bloodOpenedAt == 0L || System.currentTimeMillis() < bloodOpenedAt + (window * 1000).toLong()
            return if (doorOpenerActive) Phase.DOOR_OPENER else null
        }
        if (System.currentTimeMillis() >= phaseUntil) bossPhase?.let { old ->
            val next = rollover[old]
            if (next == null) { finished = true; bossPhase = null } else enter(next)
        }
        return bossPhase
    }

    init {
        on<MessageEvent.Chat> {
            when {
                message == "The BLOOD DOOR has been opened!" -> bloodOpenedAt = System.currentTimeMillis()
                message == "[BOSS] Maxor: WELL! WELL! WELL! LOOK WHO'S HERE!" -> enter(Phase.MAXOR_CRYSTALS)
                message == "[BOSS] Storm: Pathetic Maxor, just like expected." -> enter(Phase.MAXOR_DEAD)
                message == "[BOSS] Storm: ENERGY HEED MY CALL!" || message == "[BOSS] Storm: THUNDER LET ME BE YOUR CATALYST!" ->
                    if (bossPhase == Phase.MAXOR_DEAD || bossPhase == Phase.BEFORE_LIGHTNING) enter(Phase.AFTER_LIGHTNING)
                message == "[BOSS] Storm: I should have known that I stood no chance." -> enter(Phase.STORM_DEAD)
                message == "[BOSS] Goldor: Who dares trespass into my domain?" -> { section = 1; melodyFromChat = null; enter(Phase.DURING_S1) }
                message == "The Core entrance is opening!" -> enter(Phase.RECORE)
                message == "[BOSS] Necron: You went further than any human before, congratulations." -> enter(Phase.GOLDOR_DEAD)
                message == "[BOSS] Necron: All this, for nothing..." -> enter(Phase.NECRON_DEAD)
                else -> {
                    melodyChatRegex.find(message)?.let { melodyFromChat = it.groupValues[1] }
                    val (done, total) = sectionDoneRegex.find(message)?.destructured ?: return@on
                    if (done != total || section !in 1..3) return@on
                    enter(listOf(Phase.EE2, Phase.EE3, Phase.CORE)[section - 1])
                    section++
                    melodyFromChat = null
                }
            }
        }

        on<TickEvent.End> {
            val player = mc.player ?: return@on
            when (bossPhase) {
                Phase.NECRON_DEAD -> if ((0..8).any { player.inventory.getItem(it).itemId.endsWith("_KING_RELIC") }) enter(Phase.PICKUP_RELIC)
                else -> {}
            }
            // Odin's dragon states (needs Odin's Wither Dragons on); the first one that was alive and is now dead ends it
            if (bossPhase == Phase.NECRON_DEAD || bossPhase == Phase.PICKUP_RELIC) WitherDragonsEnum.entries.forEach {
                if (it.state == WitherDragonState.ALIVE) dragonsSeenAlive.add(it)
                else if (it.state == WitherDragonState.DEAD && it in dragonsSeenAlive && bossPhase != Phase.FIRST_DRAGON_DEAD) enter(Phase.FIRST_DRAGON_DEAD)
            }
            if (pending != null && System.currentTimeMillis() > timeoutAt) pending = null
        }

        // Checked every frame so the random delay isn't rounded to whole ticks
        on<ScreenEvent.Render> { pending?.let { tryLeap(it) } }

        on<LevelEvent.Load> {
            bossPhase = null
            phaseUntil = 0L
            finished = false
            section = 0
            bloodOpenedAt = 0L
            dragonsSeenAlive.clear()
            usedDuring.clear()
            melodyFromChat = null
            forcedPhase = null
            pending = null
        }
    }

    private fun holdingLeap() = mc.player?.mainHandItem?.itemId.let { it == "SPIRIT_LEAP" || it == "INFINITE_SPIRIT_LEAP" }

    /** Called from MinecraftMixin at startAttack HEAD; true cancels the attack. Blank target = left click passes through. */
    fun onLeftClick(): Boolean {
        if (!enabled || pending != null || mc.gui.screen() != null || !holdingLeap()) return false
        val pick = resolve() ?: return false
        if (pick.name == null && !dryRun) {
            modMessage("§c${describe(pick)} cannot be found.")
            return true
        }
        pending = pick
        timeoutAt = System.currentTimeMillis() + 3000
        menuOpenedAt = 0L
        rightClick()
        return true
    }

    /** Stops held left click from mining while a leap is in hand. */
    fun blocksAttack(): Boolean = enabled && DungeonUtils.inDungeons && holdingLeap()

    private fun resolve(): Pick? {
        var phase = currentPhase() ?: return null
        if (phase in usedDuring) phase = duringNext.getValue(phase)
        val self = DungeonUtils.currentDungeonPlayer
        val value = get(self.clazz, phase).takeIf { it.isNotBlank() } ?: return null
        val others = DungeonUtils.leapTeammates.filter { it.clazz != DungeonClass.EMPTY && it.name != self.name }
        fun named(name: String?) = others.firstOrNull { it.name.equals(name, true) }?.name

        val name = when {
            phase == Phase.DOOR_OPENER -> named(DungeonUtils.doorOpener)
            value.startsWith("@m", true) -> named(melodyPlayer())
            value.length == 1 -> {
                // Duplicate classes: first alive one in leap menu order (type a name to pick a specific player)
                val clazz = classLetters[value.uppercase()[0]]
                others.filter { it.clazz == clazz }.let { list -> list.firstOrNull { !it.isDead } ?: list.firstOrNull() }?.name
            }
            else -> named(value)
        }
        return Pick(phase, value, name)
    }

    @Suppress("UNCHECKED_CAST")
    private fun melodyPlayer(): String? {
        // Odin keeps who is in melody (websocket, needs Odin's Melody Message with Melody Progress) in a private map
        val fromOdin = runCatching {
            val field = MelodyMessage::class.java.getDeclaredField("melodies").apply { isAccessible = true }
            (field.get(MelodyMessage) as Map<String, *>).keys.firstOrNull { it != mc.user.name }
        }.getOrNull()
        return fromOdin ?: melodyFromChat
    }

    private fun describe(pick: Pick): String = when {
        pick.phase == Phase.DOOR_OPENER -> "Door opener"
        pick.value.startsWith("@m", true) -> "Melody player"
        pick.value.length == 1 -> classLetters[pick.value.uppercase()[0]]?.name?.lowercase()?.replaceFirstChar(Char::uppercase) ?: "Class '${pick.value}'"
        else -> pick.value
    }

    private fun tryLeap(pick: Pick) {
        val chest = mc.gui.screen() as? AbstractContainerScreen<*> ?: return
        if (chest.title.string != "Spirit Leap" && chest.title.string != "Teleport to Player") return
        val heads = chest.menu.slots.subList(11, 16)
        if (heads.all { it.item.isEmpty }) return
        val now = System.currentTimeMillis()
        if (menuOpenedAt == 0L) {
            menuOpenedAt = now
            rolledDelay = (delay + Random.nextInt(randomDelay + 1)).toLong()
        }
        if (now - menuOpenedAt < rolledDelay) return
        pending = null
        if (pick.phase in duringNext) usedDuring.add(pick.phase)

        val waited = "waited ${now - menuOpenedAt} ms, rolled $rolledDelay ms"
        if (dryRun) {
            modMessage("§eWould have leaped to ${pick.name ?: describe(pick)} §7(${pick.phase.label}: ${pick.value}, $waited)")
            return closeSoon()
        }
        val slot = heads.firstOrNull { it.item.hoverName.string.substringAfter(' ').equals(pick.name, true) }
        if (slot == null || DungeonUtils.leapTeammates.find { it.name == pick.name }?.isDead == true) {
            modMessage("§c${pick.name} cannot be found${if (slot != null) " (dead)" else ""}.")
            return closeSoon()
        }
        mc.player?.clickSlot(slot.index)
        modMessage("Fast leaping to ${pick.name} §7($waited)")
    }

    private fun closeSoon() = schedule(2) {
        val title = mc.gui.screen()?.title?.string
        if (title == "Spirit Leap" || title == "Teleport to Player") mc.player?.closeContainer()
    }

    /** Testing: null goes back to automatic. Also forgets which During S# leaps were used. */
    fun forcePhase(name: String?) {
        forcedPhase = if (name == null) null else Phase.entries.firstOrNull { it.name.equals(name, true) }
            ?: return modMessage("§cUnknown phase $name.")
        usedDuring.clear()
        modMessage("Fast Leap phase: ${forcedPhase?.label ?: "automatic"}")
    }

    fun nextPhase() = forcePhase(Phase.entries.getOrNull((forcedPhase?.ordinal ?: -1) + 1)?.name)

    fun forceMelody(name: String) {
        melodyFromChat = name
        modMessage("Fast Leap melody player: $name")
    }
}
