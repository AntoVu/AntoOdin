package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.features.impl.noamm.RelicSpawnTimer
import com.anto.antoodin.utils.Skit
import com.anto.antoodin.utils.rightClick
import com.odtheking.odin.clickgui.settings.impl.ActionSetting
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.MapSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.EntityEvent
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.MessageEvent
import com.odtheking.odin.events.ScreenEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onSend
import com.odtheking.odin.features.Module
import com.odtheking.odin.features.ModuleManager
import com.odtheking.odin.features.impl.boss.MelodyMessage
import com.odtheking.odin.utils.clickSlot
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.itemId
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.skyblock.dungeon.DungeonClass
import com.odtheking.odin.utils.skyblock.dungeon.DungeonPlayer
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.world.entity.EntityTypes
import kotlin.random.Random

/**
 * Left click with a Spirit Leap / Infinileap: cancels the click, opens the leap menu with a simulated right click,
 * then clicks the configured teammate's head after a humanized delay. Works with any leap menu style, since it
 * clicks the real chest slot (heads in 11-15), like Odin's own leapTo.
 *
 * Targets are per own class and per phase (edited in [FastLeapScreen]). The phase follows F7/M7 boss chat lines.
 * Each During S# target is used once (any leap counts, even a manual one), then that section's next target (EE2, EE3, Core, Recore) takes over.
 * A blank During S# goes straight to that next target.
 */
object FastLeap : Module(
    name = "Fast Leap",
    description = "Left click a leap to leap straight to the teammate set for your class and the current phase. /ao fastleap to configure.",
    category = Skit.ANTO
) {
    private val delay by NumberSetting("Delay", 150, 0..1000, 10, desc = "Delay after the leap menu opens before clicking.", unit = "ms")
    private val randomDelay by NumberSetting("Random Delay", 75, 0..500, 5, desc = "Random extra delay on top.", unit = "ms")
    private val bloodLeap by NumberSetting("Blood Leap", 3.0, 1.0..5.0, 0.5, desc = "How long Door Opener stays active after the blood door opens.", unit = "s")
    private val sectionCarry by NumberSetting("Section Carry Over", 1.5, 1.0..5.0, 0.5, desc = "How long EE2, EE3 and Core stay active after a terminal section completes.", unit = "s")
    private val goldorDeath by NumberSetting("Goldor Death", 3.0, 1.0..5.0, 0.5, desc = "How long after the core opens Recore turns into Goldor Dead.", unit = "s")
    private val skipDoneMelody by BooleanSetting("Skip Done Melody", true, desc = "@melody targets: once anyone has finished melody (3/3) this section, leap to the next phase's target instead (EE2, EE3, Core or Recore), the same way a used During S# leap does. Resets every section.")
    private val melodyThreshold by NumberSetting("Melody Threshold", 3, 1..3, 1, desc = "@melody targets: leap to the next phase's target instead once the player furthest into melody has this many clicks. 1 = only while everyone is at 0/3, 2 = up to 1/3, 3 = always leap to them, however far along they are (finishing is up to Skip Done Melody). The target is still whoever opened melody first; only the threshold looks at the furthest player.")
    // ponytail: temporary testing aid, remove once the phases are tuned
    private val dryRun by BooleanSetting("Dry Run", false, desc = "Doesn't leap: says who it would have leaped to and how long it waited after the menu opened, then closes the menu.")
    private val openEditor by ActionSetting("Open Editor", desc = "Opens the per-class leap target editor.") {
        mc.gui.setScreen(FastLeapScreen(mc.gui.screen()))
    }
    private val targets by MapSetting("Leap Targets", mutableMapOf<String, String>())

    enum class Phase(val label: String) {
        DOOR_OPENER("Door Opener"), MAXOR_CRYSTALS("Maxor Crystals"), SECOND_LASER("Second Laser"),
        BEFORE_LIGHTNING("Before Lightning"), AFTER_LIGHTNING("After Lightning"), FIRST_CRUSH("After First Crush"), STORM_DEAD("Storm Dead"),
        DURING_S1("During S1"), EE2("EE2"), DURING_S2("During S2"), EE3("EE3"), DURING_S3("During S3"),
        CORE("Core"), DURING_S4("During S4"), RECORE("Recore"), GOLDOR_DEAD("Goldor Dead"), MIDWAY_NECRON("Midway Necron"),
        PICKUP_RELIC("Pickup Relic"), FIRST_DRAGON("First Dragon")
    }

    val classes = listOf(DungeonClass.ARCHER, DungeonClass.BERSERK, DungeonClass.MAGE, DungeonClass.HEALER, DungeonClass.TANK)
    // Common party finder targets, one entry per phase in Phase order ("-" = blank)
    private val defaults: Map<String, String> = mapOf(
        DungeonClass.ARCHER to "on T B - - M H - M - H - M H M H H - -",
        DungeonClass.BERSERK to "- H - T - - - @melody/T M - H - M - M H H - -",
        DungeonClass.MAGE to "on - B A H T H - - - - - - - - H H B H",
        DungeonClass.HEALER to "on - B A - B - - M - - - M - M - - B -",
        DungeonClass.TANK to "- - B - - - H - M B H A M H M H H A H",
    ).flatMap { (clazz, row) ->
        row.split(' ').also { check(it.size == Phase.entries.size) }
            .mapIndexed { i, v -> "${clazz.name}:${Phase.entries[i].name}" to v.takeIf { it != "-" }.orEmpty() }
    }.toMap()
    private val classLetters = mapOf('A' to DungeonClass.ARCHER, 'B' to DungeonClass.BERSERK, 'M' to DungeonClass.MAGE, 'H' to DungeonClass.HEALER, 'T' to DungeonClass.TANK)

    fun get(clazz: DungeonClass, phase: Phase): String = "${clazz.name}:${phase.name}".let { targets[it] ?: defaults[it] ?: "" }
    fun set(clazz: DungeonClass, phase: Phase, value: String) { targets["${clazz.name}:${phase.name}"] = value.trim() }
    fun save() = ModuleManager.saveConfigurations()

    private val sectionDoneRegex = Regex("^.+ (?:completed a device|activated a terminal|activated a lever)! \\((\\d+)/\\1\\)$")
    private val leapedRegex = Regex("^You have teleported to \\w{1,16}!$")
    private val cooldownRegex = Regex("^This ability is on cooldown for [\\d.]+s\\.$")
    private const val LIGHTNING_SECONDS = 1.5
    private const val MENU_TIMEOUT_MS = 2000L
    private const val MELODY_CLOSE_GRACE_MS = 1500L

    // Boss phase from chat; windowed phases roll over to the next one when [phaseUntil] passes
    private var bossPhase: Phase? = null
    private var phaseUntil = 0L
    private var lasers = 0
    private var section = 0
    private var bloodOpenedAt = 0L

    /** Resolved leap: [name] is null when nobody in the party matches [value]. */
    private class Pick(val phase: Phase, val value: String, val name: String?)

    private var pending: Pick? = null
    private var pendingItem: String? = null // leap item the pending click used; its cooldown message cancels it
    private var timeoutAt = 0L
    private var menuOpenedAt = 0L
    private var rolledDelay = 0L

    private val usedDuring = HashSet<Phase>()
    private val duringNext = mapOf(Phase.DURING_S1 to Phase.EE2, Phase.DURING_S2 to Phase.EE3, Phase.DURING_S3 to Phase.CORE, Phase.DURING_S4 to Phase.RECORE)

    // Melody this section: players in the order they opened it, and the furthest each got (0-3, 3 = done).
    // Filled from Odin's melody websocket and from party chat, reset every section.
    private val melodyOrder = LinkedHashSet<String>()
    private val melodyClicks = HashMap<String, Int>()
    private val melodyLastSeen = HashMap<String, Long>() // last time Odin listed them in melody
    private val terminalDoneRegex = Regex("^(\\w{1,16}) activated a terminal! \\(\\d+/\\d+\\)$")
    private val melodyChatRegex = Regex("^Party > (?:\\[[^\\]]+] )?(\\w{1,16}): .*\\b(?:melody|meowlody)\\b.*$", RegexOption.IGNORE_CASE)
    private val melodyChatClicksRegex = Regex("\\b([0-3])/3\\b|\\b(33|66|67|100)%")

    /** Set with /ao fastleap phase|next to test phases anywhere; cleared on world change. */
    private var forcedPhase: Phase? = null

    private val rollover = mapOf(
        Phase.BEFORE_LIGHTNING to Phase.AFTER_LIGHTNING, Phase.EE2 to Phase.DURING_S2, Phase.EE3 to Phase.DURING_S3,
        Phase.CORE to Phase.DURING_S4, Phase.RECORE to Phase.GOLDOR_DEAD
    )

    /** [seconds] = how long until the phase rolls over to its [rollover] entry; null = until something else replaces it. */
    private fun enter(phase: Phase, seconds: Double? = null) {
        bossPhase = phase
        phaseUntil = seconds?.let { System.currentTimeMillis() + (it * 1000).toLong() } ?: Long.MAX_VALUE
    }

    private fun before(phase: Phase) = (bossPhase?.ordinal ?: -1) < phase.ordinal

    private fun currentPhase(): Phase? {
        forcedPhase?.let { return it }
        if (!DungeonUtils.inDungeons) return null
        if (!DungeonUtils.inBoss) {
            val doorOpenerActive = bloodOpenedAt == 0L || System.currentTimeMillis() < bloodOpenedAt + (bloodLeap * 1000).toLong()
            return if (doorOpenerActive) Phase.DOOR_OPENER else null
        }
        if (System.currentTimeMillis() >= phaseUntil) bossPhase?.let { rollover[it] }?.let { enter(it) }
        return bossPhase
    }

    init {
        on<MessageEvent.Chat> {
            when (message) {
                "The BLOOD DOOR has been opened!" -> bloodOpenedAt = System.currentTimeMillis()
                "[BOSS] Maxor: WELL! WELL! WELL! LOOK WHO'S HERE!" -> enter(Phase.MAXOR_CRYSTALS)
                "[BOSS] Maxor: THAT BEAM! IT HURTS! IT HURTS!!", "[BOSS] Maxor: YOU TRICKED ME!" -> if (++lasers == 2) enter(Phase.SECOND_LASER)
                "[BOSS] Storm: Don't boast about beating this simple minded Wither." -> if (before(Phase.BEFORE_LIGHTNING)) enter(Phase.BEFORE_LIGHTNING)
                // Said while the lightning charges; the strike is over about LIGHTNING_SECONDS later
                "[BOSS] Storm: I'd be happy to show you what that's like!" ->
                    if (before(Phase.AFTER_LIGHTNING) && phaseUntil == Long.MAX_VALUE) enter(Phase.BEFORE_LIGHTNING, LIGHTNING_SECONDS)
                "[BOSS] Storm: Oof", "[BOSS] Storm: Ouch, that hurt!" -> if (before(Phase.FIRST_CRUSH)) enter(Phase.FIRST_CRUSH)
                "[BOSS] Storm: I should have known that I stood no chance." -> enter(Phase.STORM_DEAD)
                "[BOSS] Goldor: Who dares trespass into my domain?" -> { section = 1; resetMelody(); enter(Phase.DURING_S1) }
                // Goldor's real death has no marker, so Recore just lasts a set time
                "The Core entrance is opening!" -> enter(Phase.RECORE, goldorDeath)
                "[BOSS] Necron: That's a very impressive trick. I guess I'll have to handle this myself." -> enter(Phase.MIDWAY_NECRON)
                else -> {
                    // Hypixel's own leap message, so leaps from the menu by hand or from other mods use up During S# too
                    if (leapedRegex.matches(message)) currentPhase()?.takeIf { it in duringNext }?.let { usedDuring.add(it) }
                    // On cooldown the menu never opens. Only counts while that same leap is held: Spirit Leap and
                    // Infinileap have separate cooldowns, and other items (Ice Spray) send the same message.
                    if (menuOpenedAt == 0L && cooldownRegex.matches(message) && heldItemId() == pendingItem) pending = null
                    melodyChatRegex.find(message)?.let { match ->
                        val clicks = melodyChatClicksRegex.find(message)?.groupValues?.let { g ->
                            g[1].toIntOrNull() ?: mapOf("33" to 1, "66" to 2, "67" to 2, "100" to 3)[g[2]]
                        }
                        noteMelody(match.groupValues[1], clicks ?: 0)
                    }
                    terminalDoneRegex.find(message)?.groupValues?.get(1)?.let { melodyMaybeDone(it) }
                    if (section !in 1..3 || !sectionDoneRegex.matches(message)) return@on
                    enter(listOf(Phase.EE2, Phase.EE3, Phase.CORE)[section - 1], sectionCarry)
                    section++
                    resetMelody()
                }
            }
        }

        // Stays on First Dragon for the rest of the run. Dragons only exist after Necron.
        on<EntityEvent.Add> {
            if (entity.type == EntityTypes.ENDER_DRAGON && DungeonUtils.inBoss && !before(Phase.MIDWAY_NECRON) && before(Phase.FIRST_DRAGON)) enter(Phase.FIRST_DRAGON)
        }

        on<TickEvent.End> {
            if (before(Phase.PICKUP_RELIC) && RelicSpawnTimer.relicsSpawned) enter(Phase.PICKUP_RELIC)
            if (section > 0) pollOdinMelody()
            if (pending == null) return@on
            if (menuOpenedAt != 0L) {
                if (!leapMenuOpen()) pending = null // closed by hand before the delay ran out
            } else if (System.currentTimeMillis() > timeoutAt) {
                pending = null
                closeSoon()
            }
        }

        // A click we didn't send (ours clears pending first): the player leaped by hand, so don't click a second head
        onSend<ServerboundContainerClickPacket> { pending = null }

        // Checked every frame so the random delay isn't rounded to whole ticks
        on<ScreenEvent.Render> { pending?.let { tryLeap(it) } }

        on<LevelEvent.Load> {
            bossPhase = null
            phaseUntil = 0L
            lasers = 0
            section = 0
            bloodOpenedAt = 0L
            usedDuring.clear()
            resetMelody()
            forcedPhase = null
            pending = null
        }
    }

    private fun heldItemId() = mc.player?.mainHandItem?.itemId
    private fun holdingLeap() = heldItemId().let { it == "SPIRIT_LEAP" || it == "INFINITE_SPIRIT_LEAP" }

    /** Called from MinecraftMixin at startAttack HEAD; true cancels the attack. Blank target = left click passes through. */
    fun onLeftClick(): Boolean {
        if (!enabled || mc.gui.screen() != null || !holdingLeap()) return false
        if (pending != null) return true // still waiting on the menu; extra clicks do nothing
        val pick = resolve() ?: return false
        if (pick.name == null && !dryRun) {
            modMessage("§c${describe(pick)} cannot be found.")
            return true
        }
        pending = pick
        pendingItem = heldItemId()
        timeoutAt = System.currentTimeMillis() + MENU_TIMEOUT_MS
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
        // A blank During S# leaps to that section's next target (EE2, EE3, Core, Recore)
        if (phase in duringNext && get(self.clazz, phase).isBlank()) phase = duringNext.getValue(phase)
        var value = get(self.clazz, phase).takeIf { it.isNotBlank() } ?: return null
        if (isMelody(value) && phase in duringNext && melodyPassed()) {
            phase = duringNext.getValue(phase)
            value = get(self.clazz, phase).takeIf { it.isNotBlank() } ?: return null
        }
        val others = DungeonUtils.leapTeammates.filter { it.clazz != DungeonClass.EMPTY && it.name != self.name }
        fun named(name: String?) = others.firstOrNull { it.name.equals(name, true) }?.name
        fun target(v: String) = if (v.length == 1) {
            // Duplicate classes: first alive one in leap menu order (type a name to pick a specific player)
            val clazz = classLetters[v.uppercase()[0]]
            others.filter { it.clazz == clazz }.let { list -> list.firstOrNull { !it.isDead } ?: list.firstOrNull() }?.name
        } else named(v)

        val name = when {
            phase == Phase.DOOR_OPENER -> named(DungeonUtils.doorOpener)
            // First player who opened melody this section, else the optional fallback after the slash (@melody/T)
            isMelody(value) -> melodyOrder.firstNotNullOfOrNull { named(it) } ?: melodyFallback(value)?.let { target(it) }
            else -> target(value)
        }
        return Pick(phase, value, name)
    }

    /** When [phase] is active, for the editor's hover tooltips. Keep in step with the chat triggers in init. */
    fun trigger(phase: Phase): String {
        val carry = "for Section Carry Over (${sectionCarry}s)"
        val once = "Used once per section (any leap counts), then the next row's target takes over. Blank: leaps to the next row's target. @melody: whoever opened melody first."
        return when (phase) {
            Phase.DOOR_OPENER -> "During clear, until Blood Leap (${bloodLeap}s) after \"The BLOOD DOOR has been opened!\". Leaps to whoever opened the last wither door. On/off only."
            Phase.MAXOR_CRYSTALS -> "From \"[BOSS] Maxor: WELL! WELL! WELL! LOOK WHO'S HERE!\" until Maxor's second laser."
            Phase.SECOND_LASER -> "From Maxor's second laser hit: the second \"[BOSS] Maxor: THAT BEAM! IT HURTS! IT HURTS!!\" or \"[BOSS] Maxor: YOU TRICKED ME!\"."
            Phase.BEFORE_LIGHTNING -> "From \"[BOSS] Storm: Don't boast about beating this simple minded Wither.\" until the lightning strike is over."
            Phase.AFTER_LIGHTNING -> "${LIGHTNING_SECONDS}s after \"[BOSS] Storm: I'd be happy to show you what that's like!\" (when the lightning strike is over)."
            Phase.FIRST_CRUSH -> "From Storm's first crush: \"[BOSS] Storm: Oof\" or \"[BOSS] Storm: Ouch, that hurt!\"."
            Phase.STORM_DEAD -> "From \"[BOSS] Storm: I should have known that I stood no chance.\""
            Phase.DURING_S1 -> "From \"[BOSS] Goldor: Who dares trespass into my domain?\" until section 1 completes. $once"
            Phase.EE2 -> "When section 1 completes (the \"(n/n)\" terminal, device or lever line), $carry, then During S2."
            Phase.DURING_S2 -> "After EE2 ends, until section 2 completes. $once"
            Phase.EE3 -> "When section 2 completes, $carry, then During S3."
            Phase.DURING_S3 -> "After EE3 ends, until section 3 completes. $once"
            Phase.CORE -> "When section 3 completes, $carry, then During S4."
            Phase.DURING_S4 -> "After Core ends, until \"The Core entrance is opening!\". $once"
            Phase.RECORE -> "From \"The Core entrance is opening!\" for Goldor Death (${goldorDeath}s)."
            Phase.GOLDOR_DEAD -> "Goldor Death (${goldorDeath}s) after the core opens (Goldor's death has no chat line), until Necron's line."
            Phase.MIDWAY_NECRON -> "From \"[BOSS] Necron: That's a very impressive trick. I guess I'll have to handle this myself.\""
            Phase.PICKUP_RELIC -> "When the M7 relics spawn after Necron dies. Needs Relics > Relic Spawn Timer on."
            Phase.FIRST_DRAGON -> "When the first M7 dragon spawns, for the rest of the run."
        }
    }

    private fun isMelody(value: String) = value.startsWith("@m", true)
    private fun melodyFallback(value: String) = value.substringAfter('/', "").trim().takeIf { it.isNotEmpty() }

    /** Melody is far enough along (see Melody Threshold / Skip Done Melody) that @melody moves on to the next phase. */
    private fun melodyPassed(): Boolean {
        val furthest = melodyClicks.values.maxOrNull() ?: return false
        return (skipDoneMelody && furthest >= 3) || (melodyThreshold < 3 && furthest >= melodyThreshold)
    }

    private fun noteMelody(name: String, clicks: Int) {
        if (name.equals(mc.user.name, true)) return
        melodyOrder.add(name)
        melodyClicks.merge(name, clicks, ::maxOf)
    }

    private fun resetMelody() {
        melodyOrder.clear()
        melodyClicks.clear()
        melodyLastSeen.clear()
    }

    /**
     * [name] activated a terminal. Odin never reports 3/3, so this is how melody counts as done: the player was in
     * melody on Odin's websocket just now (it drops them on close, around when this line arrives), or, if Odin
     * never listed them, they said melody in party chat this section (their next terminal is taken to be melody).
     */
    private fun melodyMaybeDone(name: String) {
        if (name !in melodyOrder) return
        val seen = melodyLastSeen[name]
        if (seen == null || System.currentTimeMillis() - seen < MELODY_CLOSE_GRACE_MS) melodyClicks[name] = 3
    }

    @Suppress("UNCHECKED_CAST")
    private val odinMelodies by lazy {
        runCatching { MelodyMessage::class.java.getDeclaredField("melodies").apply { isAccessible = true }.get(MelodyMessage) as Map<String, Any> }.getOrNull()
    }

    /**
     * Odin's melody websocket (needs Odin's Melody Message with Melody Progress) keeps players in melody in a private map.
     * `clay` is the row of the terracotta button: row 2 = 1/3 and row 3 = 2/3 (Odin's own 33%/67%). Done comes from [melodyMaybeDone].
     */
    private fun pollOdinMelody() {
        val map = odinMelodies ?: return
        val now = System.currentTimeMillis()
        for ((name, data) in map) {
            melodyLastSeen[name] = now
            val clay = runCatching { data.javaClass.getDeclaredField("clay").apply { isAccessible = true }.get(data) as Int? }.getOrNull()
            noteMelody(name, ((clay ?: 1) - 1).coerceIn(0, 3))
        }
    }

    private fun describe(pick: Pick): String = when {
        pick.phase == Phase.DOOR_OPENER -> "Door opener"
        isMelody(pick.value) -> "Melody player" + (melodyFallback(pick.value)?.let { " and ${describeTarget(it)}" } ?: "")
        else -> describeTarget(pick.value)
    }

    private fun describeTarget(v: String) =
        if (v.length == 1) classLetters[v.uppercase()[0]]?.name?.lowercase()?.replaceFirstChar(Char::uppercase) ?: "Class '$v'" else v

    private fun tryLeap(pick: Pick) {
        if (!leapMenuOpen()) return
        val chest = mc.gui.screen() as? AbstractContainerScreen<*> ?: return
        val heads = chest.menu.slots.subList(11, 16)
        if (heads.all { it.item.isEmpty }) return
        val now = System.currentTimeMillis()
        if (menuOpenedAt == 0L) {
            menuOpenedAt = now
            rolledDelay = (delay + Random.nextInt(randomDelay + 1)).toLong()
        }
        if (now - menuOpenedAt < rolledDelay) return
        pending = null

        val waited = "waited ${now - menuOpenedAt} ms, rolled $rolledDelay ms"
        if (dryRun) {
            if (pick.phase in duringNext) usedDuring.add(pick.phase) // no real leap, so no chat line to count it
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

    private fun leapMenuOpen() = mc.gui.screen()?.title?.string.let { it == "Spirit Leap" || it == "Teleport to Player" }

    private fun closeSoon() = schedule(2) { if (leapMenuOpen()) mc.player?.closeContainer() }

    /** Testing: null goes back to automatic. Also forgets which During S# leaps were used. */
    fun forcePhase(name: String?) {
        forcedPhase = if (name == null) null else Phase.entries.firstOrNull { it.name.equals(name, true) }
            ?: return modMessage("§cUnknown phase $name.")
        usedDuring.clear()
        modMessage("Fast Leap phase: ${forcedPhase?.label ?: "automatic"}")
    }

    fun nextPhase() = forcePhase(Phase.entries.getOrNull((forcedPhase?.ordinal ?: -1) + 1)?.name)

    fun forceMelody(name: String, clicks: Int?) {
        noteMelody(name, (clicks ?: 0).coerceIn(0, 3))
        modMessage("Fast Leap melody: $name at ${melodyClicks[name] ?: 0}/3 (order: ${melodyOrder.joinToString()})")
    }
}
