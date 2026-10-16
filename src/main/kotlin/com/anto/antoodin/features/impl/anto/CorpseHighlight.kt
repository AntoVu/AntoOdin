// Feature idea from NoFrills (GPL-3.0). Written from scratch for AntoOdin, no NoFrills code is used.
package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.EntityEvent
import com.odtheking.odin.events.EntityInteractEvent
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.RenderExtractEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.itemId
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.render.BoxStyle
import com.odtheking.odin.utils.render.drawStyledBox
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.decoration.ArmorStand

object CorpseHighlight : Module(
    name = "Corpse Highlight",
    description = "Highlights corpses in Glacite Mineshafts by type.",
    category = Skit.ANTO
) {
    private val hideOpened by BooleanSetting("Hide Opened", true, desc = "Removes the highlight from corpses you've opened.")
    private val renderStyle by SelectorSetting("Render Style", BoxStyle.OUTLINE, desc = "Style of the box.")
    private val depthCheck by BooleanSetting("Depth Check", true, desc = "Disable to see corpses through walls.")
    private val lapisColor by ColorSetting("Lapis Color", Color(85, 85, 255), true, desc = "Lapis corpses.")
    private val tungstenColor by ColorSetting("Tungsten Color", Color(170, 170, 170), true, desc = "Tungsten corpses.")
    private val umberColor by ColorSetting("Umber Color", Color(255, 170, 0), true, desc = "Umber corpses.")
    private val vanguardColor by ColorSetting("Vanguard Color", Color(255, 85, 255), true, desc = "Vanguard corpses.")

    // A corpse is an armor stand wearing its type's helmet; key is the item needed to open it
    private enum class Corpse(val helmet: String, val key: String?, val color: () -> Color) {
        Lapis("Lapis Armor Helmet", null, { lapisColor }),
        Tungsten("Mineral Helmet", "TUNGSTEN_KEY", { tungstenColor }),
        Umber("Yog Helmet", "UMBER_KEY", { umberColor }),
        Vanguard("Vanguard Helmet", "SKELETON_KEY", { vanguardColor })
    }

    private val corpses = HashMap<ArmorStand, Corpse>()
    private val opened = HashSet<ArmorStand>()

    init {
        // Helmets arrive by equipment packet, on the main thread
        on<EntityEvent.SetItemSlot> { if (slot == EquipmentSlot.HEAD) update(entity) }

        on<EntityEvent.Remove> { corpses.remove(entity) }

        on<LevelEvent.Load> {
            corpses.clear()
            opened.clear()
        }

        // Opening needs the key in your inventory, otherwise the click does nothing
        on<EntityInteractEvent> {
            val corpse = corpses[entity] ?: return@on
            if (corpse.key == null || hasItem(corpse.key)) opened.add(entity as ArmorStand)
        }

        on<RenderExtractEvent> {
            if (corpses.isEmpty() || !LocationUtils.isCurrentArea(Island.Mineshaft)) return@on
            corpses.forEach { (stand, corpse) ->
                if (!stand.isAlive || (hideOpened && stand in opened)) return@forEach
                val box = stand.getDimensions(Pose.STANDING).makeBoundingBox(stand.position()).inflate(0.25, 0.0, 0.25)
                drawStyledBox(box, corpse.color(), renderStyle, depthCheck)
            }
        }
    }

    override fun onEnable() {
        super.onEnable()
        // Equipment only reaches us while enabled, so pick up corpses that already exist
        mc.level?.entitiesForRendering()?.forEach(::update)
    }

    override fun onDisable() {
        super.onDisable()
        corpses.clear()
    }

    private fun update(entity: Entity) {
        if (entity !is ArmorStand || entity.isInvisible) return
        val helmet = entity.getItemBySlot(EquipmentSlot.HEAD).hoverName.string.noControlCodes
        Corpse.entries.firstOrNull { it.helmet == helmet }?.let { corpses[entity] = it } ?: corpses.remove(entity)
    }

    private fun hasItem(id: String): Boolean {
        val inventory = mc.player?.inventory ?: return false
        return (0..35).any { inventory.getItem(it).itemId == id }
    }
}
