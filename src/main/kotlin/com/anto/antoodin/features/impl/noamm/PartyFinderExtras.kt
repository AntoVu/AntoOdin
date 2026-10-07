// Ported from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.noamm

import com.anto.antoodin.mixin.accessors.AbstractContainerScreenAccessor
import com.anto.antoodin.utils.Skit
import com.odtheking.odin.OdinMod
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.DropdownSetting
import com.odtheking.odin.clickgui.settings.RenderableSetting.Companion.withDependency
import com.odtheking.odin.events.SetSlotEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.calculateDungeonLevel
import com.odtheking.odin.utils.loreString
import com.odtheking.odin.utils.network.hypixelapi.HypixelData
import com.odtheking.odin.utils.network.hypixelapi.RequestUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.util.concurrent.ConcurrentHashMap

/**
 * Player stats come from Odin's own API proxy (RequestUtils), not Noamm's API.
 *
 * @see com.anto.antoodin.mixin.mixins.AbstractContainerScreenMixin
 */
object PartyFinderExtras : Module(
    name = "Party Finder Extras",
    description = "Shows level requirements, missing classes and player stats in the Party Finder.",
    category = Skit.NOAMM
) {
    private val showLevelReq by BooleanSetting("Level Requirement", true, desc = "Shows the required dungeon level on each party's head.")
    private val showMissingOverlay by BooleanSetting("Missing Classes On Head", true, desc = "Shows the classes a party is missing on its head.")
    private val showMissingTooltip by BooleanSetting("Missing Classes In Tooltip", true, desc = "Lists the missing classes at the bottom of the tooltip.")
    private val statsDropdown by DropdownSetting("Tooltip Stats", desc = "Which stats to show in the tooltip.")
    private val showCataLevel by BooleanSetting("Catacombs Level", true, desc = "Shows each member's Catacombs level.").withDependency { statsDropdown }
    private val showSecrets by BooleanSetting("Secrets", true, desc = "Shows each member's total secrets and secrets per run.").withDependency { statsDropdown }
    private val showMagicalPower by BooleanSetting("Magical Power", true, desc = "Shows each member's magical power (needs their inventory API on).").withDependency { statsDropdown }
    private val showPb by BooleanSetting("S+ PB", true, desc = "Shows each member's S+ PB for the party's floor.").withDependency { statsDropdown }

    private val headSlots = (10..16) + (19..25) + (28..34)
    private val classes = listOf("Archer" to "§4", "Tank" to "§a", "Berserk" to "§6", "Healer" to "§5", "Mage" to "§b")

    private val memberRegex = Regex("^\\s*(\\w{1,16}):?\\s+(Archer|Tank|Berserk|Healer|Mage)\\s*\\((\\d+)\\)\\s*$")
    private val levelRequiredRegex = Regex("Dungeon Level Required:\\s*(\\d+)")
    private val floorRegex = Regex("Floor:\\s*Floor\\s+(\\w+)")
    private val selectedClassRegex = Regex("Currently Selected: (\\w+)")
    private val romanFloors = mapOf("I" to 1, "II" to 2, "III" to 3, "IV" to 4, "V" to 5, "VI" to 6, "VII" to 7)

    private var selectedClass: String? = null

    // name -> (fetched at, result). Fetched one at a time, because Odin's own profile cache isn't thread safe
    private const val CACHE_MS = 60 * 60 * 1000L
    private val profiles = ConcurrentHashMap<String, Pair<Long, Result<HypixelData.PlayerInfo>>>()
    private val pending = ConcurrentHashMap.newKeySet<String>()
    private val fetchLock = Semaphore(1)

    init {
        // Your selected class shows in the Catacombs Gate menu
        on<SetSlotEvent> {
            if (slotIndex != 45 || (mc.gui.screen() as? AbstractContainerScreen<*>)?.title?.string != "Catacombs Gate") return@on
            itemStack.loreString.firstNotNullOfOrNull { selectedClassRegex.find(it) }?.let { selectedClass = it.groupValues[1] }
        }

        ItemTooltipCallback.EVENT.register { stack, _, _, lines ->
            if (enabled) editTooltip(stack, lines)
        }
    }

    // The Party Finder "Party Finder" title is shared with its settings menu, which has combat level in slot 50's lore
    private fun partyFinderScreen(): AbstractContainerScreen<*>? {
        val screen = mc.gui.screen() as? AbstractContainerScreen<*> ?: return null
        if (screen.title.string != "Party Finder") return null
        val star = screen.menu.slots.getOrNull(50)?.item ?: return null
        if (!star.`is`(Items.NETHER_STAR) || star.loreString.getOrNull(5)?.contains("Combat Level:") == true) return null
        return screen
    }

    private fun partyHead(slot: Slot): ItemStack? =
        slot.item.takeIf { slot.index in headSlots && it.`is`(Items.PLAYER_HEAD) }

    @JvmStatic
    fun drawSlot(graphics: GuiGraphicsExtractor, slot: Slot) {
        if (!enabled || (!showLevelReq && !showMissingOverlay)) return
        val head = partyHead(slot) ?: return
        if (partyFinderScreen() == null) return
        val lore = head.loreString
        val font = mc.font

        if (showLevelReq) lore.firstNotNullOfOrNull { levelRequiredRegex.find(it) }?.let {
            val text = "§c${it.groupValues[1]}"
            drawScaled(graphics, text, slot.x + 16 - font.width(text) * 0.6f, slot.y.toFloat(), 0.6f)
        }

        if (showMissingOverlay) {
            val present = lore.mapNotNull { memberRegex.matchEntire(it)?.groupValues?.get(2) }
            val missing = classes.filter { it.first !in present }.map { (name, color) -> "$color§l${name.first()}" }
            missing.chunked(2).take(2).forEachIndexed { i, pair ->
                drawScaled(graphics, pair.joinToString(""), slot.x.toFloat(), slot.y + 10f - i * 6f, 0.65f)
            }
        }
    }

    private fun drawScaled(graphics: GuiGraphicsExtractor, text: String, x: Float, y: Float, scale: Float) {
        graphics.pose().pushMatrix()
        graphics.pose().translate(x, y)
        graphics.pose().scale(scale, scale)
        graphics.text(mc.font, text, 0, 0, -1)
        graphics.pose().popMatrix()
    }

    private fun editTooltip(stack: ItemStack, lines: MutableList<Component>) {
        val screen = partyFinderScreen() ?: return
        val hovered = (screen as AbstractContainerScreenAccessor).hoveredSlot ?: return
        if (hovered.item !== stack || partyHead(hovered) == null) return

        var floor = 0
        var master = false
        val missing = classes.map { it.first }.toMutableList()

        for (i in lines.indices) {
            val line = lines[i].string
            if ("Dungeon: Master Mode" in line) master = true
            floorRegex.find(line)?.groupValues?.get(1)?.let { floor = it.toIntOrNull() ?: romanFloors[it] ?: 0 }

            val (name, className, level) = memberRegex.matchEntire(line)?.destructured ?: continue
            missing.remove(className)
            val stats = stats(name, floor, master)
            lines[i] = Component.literal(" §b$name: §e$className ${levelColor(level.toInt())}$level $stats")
        }

        if (showMissingTooltip) {
            val text = missing.joinToString("§7, ") { name ->
                val color = classes.first { it.first == name }.second
                if (name == selectedClass) "§7$name" else "$color$name"
            }
            lines.add(Component.literal("§cMissing: $text"))
        }
    }

    private fun stats(name: String, floor: Int, master: Boolean): String {
        if (!showCataLevel && !showSecrets && !showMagicalPower && !showPb) return ""
        val key = name.lowercase()
        val cached = profiles[key]?.takeIf { System.currentTimeMillis() - it.first < CACHE_MS }?.second
        if (cached == null) {
            fetch(key)
            return "§7(Loading...)"
        }
        val member = cached.getOrNull()?.memberData ?: return "§c(Failed)"
        val dungeons = member.dungeons

        return buildString {
            if (showCataLevel) append("§b(§6${calculateDungeonLevel(dungeons.dungeonTypes.catacombs.experience).toInt()}§b) ")
            if (showSecrets) append("§8[§a${dungeons.secrets}§8/§b${"%.2f".format(dungeons.avrSecrets)}§8] ")
            if (showMagicalPower) append("§8[§d${if (member.inventoryApi) member.magicalPower else "API off"}§8] ")
            if (showPb) {
                val type = if (master) dungeons.dungeonTypes.mastermode else dungeons.dungeonTypes.catacombs
                append("§8[§9${type.fastestTimeSPlus["$floor"]?.let(::formatTime) ?: "N/A"}§8] ")
            }
        }
    }

    private fun fetch(key: String) {
        if (!pending.add(key)) return
        OdinMod.scope.launch {
            fetchLock.withPermit {
                profiles[key] = System.currentTimeMillis() to RequestUtils.getProfile(key)
            }
            pending.remove(key)
        }
    }

    private fun formatTime(ms: Double): String {
        val seconds = (ms / 1000).toLong()
        return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
    }

    private fun levelColor(level: Int) = when {
        level >= 50 -> "§c§l"
        level >= 45 -> "§c"
        level >= 40 -> "§6"
        level >= 35 -> "§d"
        level >= 30 -> "§9"
        level >= 25 -> "§b"
        level >= 20 -> "§2"
        level >= 15 -> "§a"
        level >= 10 -> "§e"
        level >= 5 -> "§f"
        else -> "§7"
    }
}
