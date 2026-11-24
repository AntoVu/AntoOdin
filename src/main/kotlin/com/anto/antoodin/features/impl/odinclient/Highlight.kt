// Taken from OdinFabric (Copyright (c) 2023-2025, odtheking, BSD 3-Clause)
// Implementation based on skies-starred OdinClient (Copyright (c) 2026, Starred, BSD 3-Clause)
// Starred mob categories: feature idea from Devonian (GPL-3.0). Written from scratch for AntoOdin, no Devonian code is used.
package com.anto.antoodin.features.impl.odinclient

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.RenderableSetting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.DropdownSetting
import com.odtheking.odin.clickgui.settings.impl.MapSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.EntityEvent
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.RenderExtractEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.features.ModuleManager
import com.odtheking.odin.features.impl.dungeon.map.DungeonScan
import com.odtheking.odin.features.impl.dungeon.map.tile.DungeonRoom
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.render.BoxStyle
import com.odtheking.odin.utils.render.drawStyledBox
import com.odtheking.odin.utils.render.drawTracer
import com.odtheking.odin.utils.renderBoundingBox
import com.odtheking.odin.utils.renderPos
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.skyblock.dungeon.M7Phases
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.ambient.Bat
import net.minecraft.world.entity.boss.wither.WitherBoss
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player

// Replaces Odin's Highlight (dropped in ModuleManagerMixin): starred mobs, hidden names and teammate glow live here
object Highlight : Module(
    name = "Highlight (C)",
    description = "Starred mobs, withers, bats and /ao highlight mobs, as boxes or glow.",
    category = Skit.ODINCLIENT
) {
    // Names match BoxStyle's, so configs saved with BoxStyle still load
    @Suppress("unused")
    enum class Style(val box: BoxStyle?) { FILLED(BoxStyle.FILLED), OUTLINE(BoxStyle.OUTLINE), FILLED_OUTLINE(BoxStyle.FILLED_OUTLINE), GLOW(null) }

    private val depthCheck by BooleanSetting("Depth Check", false, desc = "Disable to enable ESP. Glow always shows through walls.")
    private val color by ColorSetting("Highlight color", Colors.WHITE, true, desc = "Default color for mobs added without one.")
    private val renderStyle by SelectorSetting("Render Style", Style.OUTLINE, desc = "Box style, or Minecraft's glow outline.")

    private val starDropdown by DropdownSetting("Starred Mobs", desc = "Starred dungeon mobs, colored by type.")
    private val highlightStar by BooleanSetting("Highlight Starred Mobs", true, desc = "Highlights starred dungeon mobs.").withDependency { starDropdown }
    private val starredTracer by BooleanSetting("Starred Tracer", false, desc = "Draws a tracer to starred mobs.").withDependency { starDropdown && highlightStar }
    private val tracerRoomOnly by BooleanSetting("Current Room Only", true, desc = "Only traces starred mobs in the room you are in.").withDependency { starDropdown && highlightStar && starredTracer }

    // Each type gets a toggle and a color under the Starred Mobs dropdown
    class StarType(name: String, default: Color, desc: String) {
        private val toggle = registerSetting(BooleanSetting(name, true, desc = desc).withDependency { starDropdown && highlightStar })
        private val picker = registerSetting(ColorSetting("$name Color", default, false, desc = "Color for $name.").withDependency { starDropdown && highlightStar && toggle.value })
        val on get() = toggle.value
        val color: Color get() = picker.value
    }

    private val normal = StarType("Starred", Color(0, 255, 255), "Starred mobs without a type below.")
    private val chunky = StarType("Chunky", Color(255, 0, 128), "Withermancers, Commanders, Lords and Super Archers.")
    private val fels = StarType("Fels", Color(0, 255, 128), "Starred Fels.")
    private val miniboss = StarType("Miniboss", Color(235, 1, 165), "Adventurers, Angry Archaeologist, King Midas and Diamond Guy.")
    private val shadowAssassin = StarType("Shadow Assassin", Color(255, 0, 0), "Shadow Assassins, visible or not.")
    private val skeletonMaster = StarType("Skeleton Master", Color(255, 128, 0), "Starred Skeleton Masters.")
    private val starredMinisOnly by BooleanSetting("Starred Minibosses Only", true, desc = "Only highlights minibosses (Shadow Assassins included) that are starred.").withDependency { starDropdown && highlightStar }
    private val alwaysShadowAssassin by BooleanSetting("Always Show SA", true, desc = "Highlights every Shadow Assassin, even with Starred Minibosses Only. Unstarred ones get no tracer.").withDependency { starDropdown && highlightStar && starredMinisOnly }

    // starred = matched by its ✯ name tag; fake-player minis are found by name with no star check
    private class Tracked(val type: StarType, val starred: Boolean)

    private val hideNonNames by BooleanSetting("Hide non-starred names", true, desc = "Hides name tags of mobs that are not starred.")
    private val teammateClassGlow by BooleanSetting("Teammate Class Glow", true, desc = "Colors dungeon teammates' glow by their class.")
    private val highlightWither by BooleanSetting("Highlight Withers", true, desc = "Highlights Necron, Goldor, Storm and Maxor.")
    private val witherColor by ColorSetting("Wither ESP Color", Color(255, 0, 0, 1f), true, desc = "The color of the wither highlight.").withDependency { highlightWither }
    private val witherTracer by BooleanSetting("Wither Tracer", true, desc = "Draws a tracer to the wither boss in P3.").withDependency { highlightWither }
    private val highlightBats by BooleanSetting("Highlight Bats", true, desc = "Highlights bats in dungeons.")
    private val batColor by ColorSetting("Bat color", Color(0, 255, 255, 1f), true, desc = "The color of the bat highlight.").withDependency { highlightBats }
    private val customTracer by BooleanSetting("Custom tracer", false, desc = "Draws a tracer to the mobs added manually")

    private val dungeonMobSpawns = listOf(
        "Lurker", "Dreadlord", "Souleater", "Zombie", "Skeleton", "Skeletor", "Sniper", "Super Archer", "Spider", "Fels",
        "Withermancer", "Lost Adventurer", "Angry Archaeologist", "Frozen Adventurer", "Shadow Assassin", "King Midas"
    )
    private val starredRegex = Regex("^.*✯ .*\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?[kM]?❤$")
    private val colorRegex = Regex("^(.*?)(?:\\s+#?([0-9a-fA-F]{6}))?$")

    // Lowercase name part -> color
    private val highlightMap by MapSetting("highlightMap", mutableMapOf<String, Color>())

    private val starredIds = hashMapOf<Int, Tracked>()
    private val checkedIds = hashSetOf<Int>()
    private val hiddenNameIds = hashSetOf<Int>()
    private val customIds = hashMapOf<Int, Color>()
    private val witherIds = hashSetOf<Int>()
    private val spiritSceptreIds = hashSetOf<Int>()

    init {
        // Main thread entity data, which also fires when a name tag changes
        on<EntityEvent.SetData> {
            if (!entity.isAlive) return@on
            val d = DungeonUtils.inDungeons
            val b = DungeonUtils.inBoss

            when {
                d && DungeonUtils.getF7Phase() != M7Phases.P5 && highlightWither && entity is WitherBoss && !entity.isInvisible && (entity as WitherBoss).invulnerableTicks != 800 ->
                    witherIds.add(entity.id)

                // Spirit Sceptre bats spawn on the player, so skip those
                d && !b && highlightBats && entity is Bat && !entity.isPassenger && !entity.isInvisible -> {
                    val player = mc.player ?: return@on
                    if (player.distanceTo(entity) < 1.0) spiritSceptreIds.add(entity.id)
                }

                // Shadow Assassins and minibosses are fake players without a starred name tag
                d && !b && highlightStar && entity is Player && entity != mc.player -> {
                    val name = (entity as Player).gameProfile.name
                    val type = when {
                        "Shadow Assassin" in name -> shadowAssassin
                        name == "Lost Adventurer" || name == "Diamond Guy" || name == "King Midas" -> miniboss
                        else -> return@on
                    }
                    // Keep a starred match from the name tag
                    starredIds.putIfAbsent(entity.id, Tracked(type, false))
                }

                !b && entity is ArmorStand -> {
                    val name = entity.customName?.string?.noControlCodes ?: return@on
                    val stand = entity as ArmorStand

                    if (d && (highlightStar || hideNonNames) && dungeonMobSpawns.any(name::contains)) {
                        val starred = starredRegex.matches(name)
                        if (!starred && stand.isInvisible) hiddenNameIds.add(stand.id) else hiddenNameIds.remove(stand.id)
                        if (highlightStar && starred && checkedIds.add(stand.id))
                            stand.starredMob(name)?.let { starredIds[it.id] = Tracked(starType(name), true) }
                    }

                    if (highlightMap.isNotEmpty()) {
                        val nameLower = name.lowercase()
                        val match = highlightMap.entries.firstOrNull { nameLower.contains(it.key) } ?: return@on
                        stand.mobBelow()?.let { customIds[it.id] = match.value }
                    }
                }
            }
        }

        on<EntityEvent.Remove> {
            starredIds.remove(entity.id)
            checkedIds.remove(entity.id)
            hiddenNameIds.remove(entity.id)
        }

        on<RenderExtractEvent> {
            val world = mc.level ?: return@on
            val box = renderStyle.box
            val tracerWither = witherTracer && DungeonUtils.getF7Phase() == M7Phases.P3
            val room = if (tracerRoomOnly) DungeonUtils.currentRoom else null

            if (highlightStar && !DungeonUtils.inBoss) starredIds.forEach { (id, tracked) ->
                if (!tracked.shown) return@forEach
                val type = tracked.type
                val entity = world.getEntity(id) ?: return@forEach
                if (!entity.isAlive) return@forEach
                if (box != null) drawStyledBox(entity.renderBoundingBox, type.color, box, depthCheck)
                if (starredTracer && tracked.starred && (room == null || roomAt(entity) === room)) drawTracer(entity.renderPos, type.color, depthCheck)
            }

            witherIds.forEach { id ->
                val entity = world.getEntity(id) as? WitherBoss ?: return@forEach
                if (!entity.isAlive) return@forEach
                if (box != null) drawStyledBox(entity.renderBoundingBox, witherColor, box, depthCheck)
                // OdinClient also required invulnerableTicks != 0, which seems to stay 0 on the live boss, so the tracer never drew
                if (tracerWither) drawTracer(entity.renderPos, witherColor, depthCheck)
            }

            if (box != null && highlightBats && DungeonUtils.inDungeons && !DungeonUtils.inBoss) {
                world.entitiesForRendering()
                    .filterIsInstance<Bat>()
                    .filter { isHighlightedBat(it) }
                    .forEach { drawStyledBox(it.renderBoundingBox, batColor, box, depthCheck) }
            }

            customIds.forEach { (id, color) ->
                val entity = world.getEntity(id) ?: return@forEach
                if (!entity.isAlive) return@forEach
                if (box != null) drawStyledBox(entity.renderBoundingBox, color, box, depthCheck)
                if (customTracer) drawTracer(entity.renderPos, color, depthCheck)
            }
        }

        on<LevelEvent.Load> {
            starredIds.clear()
            checkedIds.clear()
            hiddenNameIds.clear()
            customIds.clear()
            witherIds.clear()
            spiritSceptreIds.clear()
        }
    }

    // Glow color for EntityRendererMixin, called per entity per frame: lookups only
    @JvmStatic
    fun glowColor(entity: Entity): Int? {
        if (!enabled || renderStyle != Style.GLOW) return null
        val id = entity.id
        starredIds[id]?.let { return if (highlightStar && it.shown && !DungeonUtils.inBoss) it.type.color.rgba else null }
        if (id in witherIds) return witherColor.rgba
        customIds[id]?.let { return it.rgba }
        if (highlightBats && entity is Bat && DungeonUtils.inDungeons && !DungeonUtils.inBoss && isHighlightedBat(entity)) return batColor.rgba
        return null
    }

    @JvmStatic
    fun teammateColor(entity: Entity): Int? {
        if (!enabled || !teammateClassGlow || entity !is Player || !DungeonUtils.inDungeons) return null
        return DungeonUtils.dungeonTeammates.find { it.name == entity.name.string }?.clazz?.color?.rgba
    }

    @JvmStatic
    fun shouldHideEntity(entity: Entity): Boolean = enabled && hideNonNames && entity.id in hiddenNameIds

    private val Tracked.shown: Boolean get() = type.on && when {
        starred || !starredMinisOnly -> true
        else -> type === shadowAssassin && alwaysShadowAssassin
    }

    // Odin's dungeon grid: 6x6 tiles of 32 blocks starting at -201
    private fun roomAt(entity: Entity): DungeonRoom? {
        val tileX = (entity.blockX + 201) shr 5
        val tileZ = (entity.blockZ + 201) shr 5
        if (tileX !in 0..5 || tileZ !in 0..5) return null
        return DungeonScan.tiles[tileX + tileZ * 6].room
    }

    private fun isHighlightedBat(bat: Bat) = !bat.isPassenger && !bat.isInvisible && bat.isAlive && bat.id !in spiritSceptreIds

    private fun starType(name: String): StarType = when {
        "Shadow Assassin" in name -> shadowAssassin
        "Fels" in name -> fels
        "Skeleton Master" in name -> skeletonMaster
        "Withermancer" in name || "Lord" in name || "Zombie Commander" in name || "Super Archer" in name -> chunky
        "Adventurer" in name || "Angry Archaeologist" in name || "King Midas" in name -> miniboss
        else -> normal
    }

    // Withermancers spawn two skulls between the mob and its name tag
    private fun ArmorStand.starredMob(name: String): Entity? =
        if ("Withermancer" in name) mc.level?.getEntity(id - 3)?.takeIf { isValidEntity(it, true) } ?: mobBelow(true)
        else mobBelow(true)

    // The mob a name tag stand belongs to: overlapping it, or spawned right before it
    private fun ArmorStand.mobBelow(allowInvisible: Boolean = false): Entity? =
        mc.level?.getEntities(this, boundingBox.inflate(0.0, 1.0, 0.0)) { isValidEntity(it, allowInvisible) }?.firstOrNull()
            ?: mc.level?.getEntity(id - 1)?.takeIf { isValidEntity(it, allowInvisible) }

    private fun isValidEntity(entity: Entity, allowInvisible: Boolean = false): Boolean =
        when (entity) {
            is ArmorStand -> false
            is WitherBoss -> false
            is Player -> entity.uuid.version() == 2 && entity != mc.player
            else -> allowInvisible || !entity.isInvisible
        }

    fun add(arg: String) {
        val (key, color) = arg.parse() ?: return modMessage("Invalid format. Use: /ao highlight add <mob name> [#RRGGBB]")
        if (highlightMap.containsKey(key)) return modMessage("\"$key\" is already highlighted.")

        highlightMap[key] = color ?: this.color
        ModuleManager.saveConfigurations()
        modMessage("Added \"$key\" to highlight list${color?.let { " with color #%02X%02X%02X".format(it.red, it.green, it.blue) } ?: ""}.")
    }

    fun remove(name: String) {
        val key = name.trim().lowercase()
        if (highlightMap.remove(key) == null) return modMessage("\"$key\" isn't highlighted.")
        ModuleManager.saveConfigurations()
        modMessage("Removed \"$key\" from highlight list.")
    }

    fun clear() {
        if (highlightMap.isEmpty()) return modMessage("Highlight list is already empty.")
        highlightMap.clear()
        ModuleManager.saveConfigurations()
        modMessage("Highlight list cleared.")
    }

    fun list() {
        if (highlightMap.isEmpty()) return modMessage("Highlight list is empty.")
        modMessage("Highlight list:\n" + highlightMap.entries.joinToString("\n") { (name, color) -> "$name - #%02X%02X%02X".format(color.red, color.green, color.blue) })
    }

    fun names(): Set<String> = highlightMap.keys

    private fun String.parse(): Pair<String, Color?>? {
        val match = colorRegex.matchEntire(trim()) ?: return null
        val name = match.groupValues[1].trim().takeIf { it.isNotEmpty() } ?: return null
        val hex = match.groupValues[2]
        return name.lowercase() to if (hex.isNotEmpty()) Color(hex + "ff") else null
    }
}
