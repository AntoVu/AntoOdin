package com.anto.antoodin.features

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mojang.blaze3d.platform.InputConstants
import com.odtheking.odin.OdinMod
import com.odtheking.odin.events.InputEvent
import com.odtheking.odin.events.core.EventPriority
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.ModuleManager
import com.odtheking.odin.features.impl.dungeon.dungeonwaypoints.DungeonWaypoints
import com.odtheking.odin.features.impl.dungeon.dungeonwaypoints.getEditableWaypoints
import com.odtheking.odin.features.impl.dungeon.dungeonwaypoints.reachPosition
import com.odtheking.odin.features.impl.dungeon.dungeonwaypoints.saveWaypoints
import com.odtheking.odin.features.impl.dungeon.dungeonwaypoints.syncRoomToActive
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import kotlinx.coroutines.launch
import java.io.File

/** Saved sets of Odin's "Next Waypoint Settings" for Dungeon Waypoints, plus an optional title. */
object DwpPresets {
    data class Preset(
        val name: String, val type: String, val color: String, val filled: Boolean, val depth: Boolean,
        val useBlockSize: Boolean, val sizeX: Double, val sizeY: Double, val sizeZ: Double, val text: String?,
    )

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val listType = object : TypeToken<MutableList<Preset>>() {}.type
    private val file = File(OdinMod.configFile, "addons/antoodin-dwp-presets.json")

    private val presets: MutableList<Preset> =
        runCatching { gson.fromJson<MutableList<Preset>>(file.readText(), listType) }.getOrNull() ?: mutableListOf()

    // Title for waypoints placed while the active preset has one. Not saved: a restart clears it
    private var activeText: String? = null

    init {
        // After Odin's own handler (default priority) has added the waypoint, give it the preset's title
        on<InputEvent>(EventPriority.LOW) {
            val text = activeText ?: return@on
            if (key.type != InputConstants.Type.MOUSE || key.value != InputConstants.MOUSE_BUTTON_RIGHT) return@on
            if (!DungeonWaypoints.enabled || !DungeonWaypoints.allowEdits || OdinMod.mc.gui.screen() != null) return@on
            val room = DungeonUtils.currentRoom ?: return@on
            val pos = room.getRelativeCoords(reachPosition ?: return@on)
            val list = DungeonWaypoints.getEditableWaypoints(room)
            val index = list.indexOfFirst { it.blockPos == pos && it.title == null }.takeIf { it >= 0 } ?: return@on
            list[index] = list[index].copy(title = text)
            DungeonWaypoints.syncRoomToActive(room)
            OdinMod.scope.launch { DungeonWaypoints.saveWaypoints() }
        }
    }

    fun names(): List<String> = presets.map { it.name }

    // A 1-based number, else a name (case ignored)
    private fun find(arg: String): Preset? =
        arg.toIntOrNull()?.let { presets.getOrNull(it - 1) } ?: presets.firstOrNull { it.name.equals(arg, true) }

    fun list() {
        if (presets.isEmpty()) return modMessage("No dungeon waypoint presets. Save one with §e/ao dwp preset add <name> [text]§r.")
        modMessage("Dungeon waypoint presets:")
        presets.forEachIndexed { i, p ->
            val text = p.text?.let { " §7\"$it§7\"" }.orEmpty()
            modMessage("§8${i + 1}. §${if (p.filled) "a" else "7"}${p.name} §7${p.type.lowercase()} #${p.color}$text", prefix = "")
        }
    }

    fun apply(arg: String) {
        val p = find(arg) ?: return modMessage("§cNo preset \"$arg\".")
        with(DungeonWaypoints) {
            waypointType = DungeonWaypoints.WaypointType.entries.firstOrNull { it.name == p.type } ?: DungeonWaypoints.WaypointType.NONE
            color = Color(p.color)
            filled = p.filled
            depthCheck = p.depth
            useBlockSize = p.useBlockSize
            sizeX = p.sizeX
            sizeY = p.sizeY
            sizeZ = p.sizeZ
        }
        activeText = p.text
        ModuleManager.saveConfigurations()
        modMessage("Using preset §e${p.name}§r.")
    }

    // Saves Odin's current next-waypoint settings; an existing name is overwritten in place
    fun add(name: String, text: String?) {
        val p = with(DungeonWaypoints) {
            Preset(name, waypointType.name, color.hex(), filled, depthCheck, useBlockSize, sizeX, sizeY, sizeZ, text?.takeIf { it.isNotBlank() })
        }
        val i = presets.indexOfFirst { it.name.equals(name, true) }
        if (i >= 0) presets[i] = p else presets += p
        save()
        modMessage("${if (i >= 0) "Updated" else "Saved"} preset §e$name§r from the current Dungeon Waypoints settings.")
    }

    fun delete(arg: String) {
        val p = find(arg) ?: return modMessage("§cNo preset \"$arg\".")
        presets.remove(p)
        save()
        modMessage("Deleted preset §e${p.name}§r.")
    }

    private fun save() = runCatching {
        file.parentFile.mkdirs()
        file.writeText(gson.toJson(presets, listType))
    }.onFailure { modMessage("§cCouldn't save presets: ${it.message}") }
}
