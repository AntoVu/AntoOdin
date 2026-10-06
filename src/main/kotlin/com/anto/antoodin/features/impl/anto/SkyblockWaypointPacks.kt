// Adapted from Odin's DungeonWaypoints pack storage (Copyright (c) 2025, odtheking, BSD 3-Clause)
package com.anto.antoodin.features.impl.anto

import com.google.gson.reflect.TypeToken
import com.odtheking.odin.OdinMod
import com.odtheking.odin.config.DungeonWaypointConfig
import com.odtheking.odin.config.WaypointPackState
import com.odtheking.odin.config.normalized
import com.odtheking.odin.features.ModuleManager
import com.odtheking.odin.features.impl.dungeon.dungeonwaypoints.DungeonWaypoints.DungeonWaypoint
import com.odtheking.odin.utils.modMessage
import net.minecraft.util.FileUtil
import java.io.File
import java.util.TreeMap

typealias WaypointMap = MutableMap<String, MutableList<DungeonWaypoint>>

/**
 * One JSON file per pack in `config/odin/addons/antoodin-waypoints`, each mapping an island key
 * ([SkyblockWaypoints.currentKey]) to waypoints at absolute coordinates. Every pack is kept in memory,
 * and every change is written straight away. Files are small, so this all runs on the main thread.
 */
object SkyblockWaypointPacks {
    private val folder = File(OdinMod.configFile, "addons/antoodin-waypoints")
    private val packType = object : TypeToken<WaypointMap>() {}.type
    private var cache: TreeMap<String, WaypointMap>? = null

    /** Pack name -> island key -> waypoints, sorted by pack name. */
    val packs: MutableMap<String, WaypointMap> get() = cache ?: reload()

    fun reload(): TreeMap<String, WaypointMap> {
        folder.mkdirs()
        val loaded = TreeMap<String, WaypointMap>()
        folder.listFiles { f -> f.extension == "json" }?.forEach { file -> read(file)?.let { loaded[file.nameWithoutExtension] = it } }
        cache = loaded
        if (loaded.isEmpty()) {
            loaded["default"] = mutableMapOf()
            save("default")
        }
        applyState(SkyblockWaypoints.selectedPackIds, SkyblockWaypoints.editPackId)
        return loaded
    }

    // An unreadable pack is left out (not replaced with an empty one) so saving never overwrites it
    private fun read(file: File): WaypointMap? = try {
        DungeonWaypointConfig.gson.fromJson<WaypointMap>(file.readText(), packType) ?: mutableMapOf()
    } catch (e: Exception) {
        OdinMod.logger.error("Failed to read waypoint pack ${file.name}", e)
        modMessage("§cSkipped unreadable waypoint pack '${file.nameWithoutExtension}'.")
        null
    }

    fun save(pack: String = SkyblockWaypoints.editPackId) {
        val waypoints = packs[pack] ?: return
        runCatching { file(pack).writeText(DungeonWaypointConfig.gson.toJson(waypoints.filterValues { it.isNotEmpty() })) }
            .onFailure { OdinMod.logger.error("Failed to save waypoint pack $pack", it); modMessage("§cFailed to save waypoint pack '$pack'!") }
    }

    /** Waypoints from every enabled pack on [key]. */
    fun visible(key: String): List<DungeonWaypoint> =
        SkyblockWaypoints.selectedPackIds.flatMap { packs[it]?.get(key).orEmpty() }

    /** The edit pack's waypoints on [key], the only ones that can be changed. */
    fun editable(key: String): MutableList<DungeonWaypoint> =
        packs.getOrPut(SkyblockWaypoints.editPackId) { mutableMapOf() }.getOrPut(key) { mutableListOf() }

    /** Imports an export string into the edit pack. Islands in it replace the pack's waypoints there, other islands are kept. */
    fun import(text: String) {
        val imported = DungeonWaypointConfig.decodeWaypoints(text.trim()) ?: return modMessage("§cFailed to decode waypoints. §fIs the data valid?")
        val pack = SkyblockWaypoints.editPackId
        packs.getOrPut(pack) { mutableMapOf() }.putAll(imported)
        save(pack)
        modMessage("§aImported ${imported.size} island(s) into pack '$pack'.${if (!SkyblockWaypoints.enabled) " §7(Make sure to enable Skyblock Waypoints)" else ""}")
    }

    fun create(name: String, waypoints: WaypointMap = mutableMapOf()): Boolean {
        if (!isValidName(name)) return fail("Invalid pack name!")
        if (name in packs || file(name).exists()) return fail("Pack '$name' already exists!")
        packs[name] = waypoints
        save(name)
        applyState(SkyblockWaypoints.selectedPackIds + name, name)
        return true
    }

    fun delete(name: String): Boolean {
        if (packs.size <= 1) return fail("Cannot delete the only pack!")
        if (!file(name).delete()) return fail("Failed to delete pack '$name'!")
        packs.remove(name)
        applyState(SkyblockWaypoints.selectedPackIds - name, SkyblockWaypoints.editPackId)
        return true
    }

    fun rename(old: String, new: String): Boolean {
        if (!isValidName(new)) return fail("Invalid pack name!")
        if (new in packs || file(new).exists()) return fail("Pack '$new' already exists!")
        if (!file(old).renameTo(file(new))) return fail("Failed to rename pack '$old'!")
        packs[new] = packs.remove(old) ?: mutableMapOf()
        applyState(SkyblockWaypoints.selectedPackIds.map { if (it == old) new else it }, SkyblockWaypoints.editPackId.takeIf { it != old } ?: new)
        return true
    }

    fun toggle(name: String) {
        val selected = SkyblockWaypoints.selectedPackIds
        val edit = SkyblockWaypoints.editPackId
        when {
            name !in selected -> applyState(selected + name, edit)
            selected.size > 1 -> applyState(selected - name, if (edit == name) selected.first { it != name } else edit)
        }
    }

    fun setEdit(name: String) = applyState(SkyblockWaypoints.selectedPackIds + name, name)

    private fun applyState(selected: List<String>, edit: String) {
        val state = WaypointPackState(selected.toMutableList(), edit).normalized(packs.keys.toList())
        if (state.selectedPackIds == SkyblockWaypoints.selectedPackIds && state.editPackId == SkyblockWaypoints.editPackId) return
        SkyblockWaypoints.selectedPackIds = state.selectedPackIds
        SkyblockWaypoints.editPackId = state.editPackId
        ModuleManager.saveConfigurations()
    }

    private fun file(name: String) = File(folder, "$name.json")

    private fun isValidName(name: String) =
        name.isNotBlank() && FileUtil.sanitizeName(name) == name && FileUtil.isPathPartPortable(name)

    private fun fail(message: String): Boolean {
        modMessage("§c$message")
        return false
    }
}
