package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.utils.Skit
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.odtheking.odin.clickgui.settings.RenderableSetting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.MessageEvent
import com.odtheking.odin.events.RenderExtractEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.features.impl.dungeon.map.tile.DungeonRoom
import com.odtheking.odin.features.impl.dungeon.map.tile.MapCheckmark
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Color.Companion.withAlpha
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.render.BoxStyle
import com.odtheking.odin.utils.render.drawStyledBox
import com.odtheking.odin.utils.render.drawText
import com.odtheking.odin.utils.skyblock.dungeon.DungeonClass
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

object BloodRushWaypoints : Module(
    name = "Blood Rush Waypoints",
    description = "Master mode blood rush: shows your class's mob boxes for each room, by the door you came in through.",
    category = Skit.ANTO
) {
    private val role by SelectorSetting("Role", Role.AUTO, desc = "Auto picks from your class: Archer or Mage. The Healer runs doors and gets none. Override to see a set role, or both.")
    private val boxStyle by SelectorSetting("Box Style", BoxStyle.FILLED_OUTLINE, desc = "How the boxes are drawn.")
    private val depthCheck by BooleanSetting("Depth Check", false, desc = "Hide boxes behind walls.")
    private val showOrder by BooleanSetting("Show Kill Order", true, desc = "Numbers your boxes in the order they're killed.")
    private val textScale by NumberSetting("Text Scale", 2f, 0.5..5.0, 0.25f, desc = "Size of the kill order numbers.").withDependency { showOrder }
    private val showStacks by BooleanSetting("Show Stack", true, desc = "Shows the boxes your role helps with once its own are done.")
    private val archerColor by ColorSetting("Archer Color", Color(255, 85, 85, 0.4f), true, desc = "Archer's boxes.")
    private val mageColor by ColorSetting("Mage Color", Color(26, 255, 26, 0.4f), true, desc = "Mage's boxes.")
    private val stackColor by ColorSetting("Stack Color", Color(255, 170, 0, 0.3f), true, desc = "Boxes in your role's stack.").withDependency { showStacks }
    private val allClasses by BooleanSetting("Show on All Classes", false, desc = "General clear guide: after the blood door opens, shows Archer and Mage boxes together, plus any box neither kills. Other classes see it the whole run.")
    private val generalColor by ColorSetting("General Color", Color(85, 255, 255, 0.4f), true, desc = "Every box in the general clear guide.").withDependency { allClasses }
    private val unknownDoorBoxes by BooleanSetting("Unknown Door Boxes", true, desc = "If the door you came in through can't be told and it changes the plan, shows every box in the room in gray.")
    private val chatSummary by BooleanSetting("Chat Summary", true, desc = "Sends the door you came in through and your box count when you enter a room.")

    enum class Role { AUTO, ARCHER, MAGE, BOTH }

    // Plans are kept by how many players kill: 3 in the party, the Healer on doors, so 2.
    private const val KILLING = "2"
    private const val ARCHER = 0 // role numbers on the site: 1 Archer, 2 Mage
    private const val MAGE = 1
    // How far (in blocks) a door can be from where you came in and still count as the one you used
    private const val DOOR_RANGE = 12
    private const val BUNDLED = "/assets/antoodin/brwaypoints.json"

    /** A box in room coordinates (Odin's: clay at 0, 0, turned to face north), corners inclusive. */
    private class Box(val min: BlockPos, val max: BlockPos)
    private data class Plan(val roles: List<List<Int>>, val stacks: List<List<Int>>)
    private class BrData(val boxes: Map<String, Map<Int, Box>>, val plans: Map<String, Map<String, Plan>>, val mini: Set<String>)

    /** Where the player first walked into a map tile, and from which tile. */
    private class TileEntry(val from: Int, val x: Double, val z: Double, val tick: Long)

    /** A room worked out once its rotation is known: the door used, its plan, and every box in world space. */
    private class Resolved(val door: String?, val doorName: String?, val plan: Plan?, val mini: Boolean, val boxes: Map<Int, AABB>)

    // Built into the mod; nothing is downloaded
    private val data: BrData? = runCatching { loadBundled() }.onFailure { it.printStackTrace() }.getOrNull()

    private val tileEntries = HashMap<Int, TileEntry>()
    private val resolved = HashMap<DungeonRoom, Resolved>()
    // Rooms whose checkmark went white or green: hidden for the rest of the run, by name (unique per run)
    private val cleared = HashSet<String>()
    private var bloodOpened = false
    private var lastTile = -1
    private var ticks = 0L

    init {
        on<LevelEvent.Load> {
            tileEntries.clear()
            resolved.clear()
            cleared.clear()
            bloodOpened = false
            lastTile = -1
        }

        on<MessageEvent.Chat> {
            if (message == "The BLOOD DOOR has been opened!") bloodOpened = true
        }

        on<TickEvent.End> {
            ticks++
            if (!active()) return@on
            val player = mc.player ?: return@on
            val tx = (player.blockX + 201) shr 5
            val tz = (player.blockZ + 201) shr 5
            if (tx !in 0..5 || tz !in 0..5) return@on
            val tile = tx + tz * 6
            if (tile != lastTile && tile !in tileEntries) tileEntries[tile] = TileEntry(lastTile, player.x, player.z, ticks)
            lastTile = tile

            val room = DungeonUtils.currentRoom?.takeUnless { isCleared(it) } ?: return@on
            if (room in resolved) return@on
            val result = resolve(room) ?: return@on
            resolved[room] = result
            if (chatSummary) summarize(room, result)
        }

        on<RenderExtractEvent> {
            if (!active()) return@on
            val room = DungeonUtils.currentRoom?.takeUnless { isCleared(it) } ?: return@on
            val result = resolved[room] ?: return@on

            for (w in waypoints(result)) {
                drawStyledBox(w.aabb, w.color, boxStyle, depthCheck)
                if (showOrder && w.label != null) drawText(w.label, Vec3(w.aabb.center.x, w.aabb.maxY + 0.5, w.aabb.center.z), textScale, depthCheck)
            }
        }
    }

    private fun isCleared(room: DungeonRoom): Boolean {
        val name = room.data?.name ?: return false
        if (name in cleared) return true
        if (room.checkmark != MapCheckmark.WHITE && room.checkmark != MapCheckmark.GREEN) return false
        cleared += name
        return true
    }

    private fun active() = DungeonUtils.inClear && DungeonUtils.floor?.isMM == true

    // ---------------------------------------------------------------- which roles to show

    private fun shownRoles(): List<Int> = when (role) {
        Role.ARCHER -> listOf(ARCHER)
        Role.MAGE -> listOf(MAGE)
        Role.BOTH -> listOf(ARCHER, MAGE)
        Role.AUTO -> when (DungeonUtils.currentDungeonPlayer.clazz) {
            DungeonClass.ARCHER -> listOf(ARCHER)
            DungeonClass.MAGE -> listOf(MAGE)
            else -> emptyList()
        }
    }

    /** The general clear guide: Archer and Mage together, after blood opens or for other classes. */
    private fun general() = allClasses && (bloodOpened || shownRoles().isEmpty())
    private fun roles() = if (general()) listOf(ARCHER, MAGE) else shownRoles()

    private fun roleColor(r: Int) = if (r == ARCHER) archerColor else mageColor
    private fun roleName(r: Int) = if (r == ARCHER) "Archer" else "Mage"
    private fun roleCode(r: Int) = if (r == ARCHER) DungeonClass.ARCHER.colorCode else DungeonClass.MAGE.colorCode

    private class Waypoint(val aabb: AABB, val color: Color, val label: String?)

    private fun waypoints(result: Resolved): List<Waypoint> {
        val general = general()
        val roles = if (general) listOf(ARCHER, MAGE) else shownRoles()
        if (roles.isEmpty()) return emptyList()
        val both = roles.size > 1

        // Miniboss rooms: every role kills every box, from any door
        if (result.mini) {
            val color = if (general) generalColor else roleColor(roles.first())
            return result.boxes.values.mapIndexed { i, aabb -> Waypoint(aabb, color, "${i + 1}") }
        }

        // The general guide shows every box anyway, so the door doesn't matter
        if (general && result.plan == null) return result.boxes.values.map { Waypoint(it, generalColor, null) }
        val plan = result.plan
            ?: return if (unknownDoorBoxes) result.boxes.values.map { Waypoint(it, Colors.MINECRAFT_GRAY.withAlpha(0.3f), "?") } else emptyList()

        // One waypoint per box: a box both roles kill gets the first role's color (or the general one) and both labels
        val owned = LinkedHashMap<Int, Pair<Color, MutableList<String>>>()
        for (r in roles) {
            plan.roles.getOrNull(r)?.forEachIndexed { i, id ->
                if (id !in result.boxes) return@forEachIndexed
                owned.getOrPut(id) { (if (general) generalColor else roleColor(r)) to ArrayList() }.second += "§${roleCode(r)}${if (both) roleName(r).first() else ""}${i + 1}"
            }
        }
        val out = owned.mapTo(ArrayList()) { (id, v) -> Waypoint(result.boxes.getValue(id), v.first, v.second.joinToString(" ")) }
        // A full clear needs every mob, so the general guide also shows boxes neither role kills in the rush
        if (general) result.boxes.forEach { (id, aabb) -> if (id !in owned) out += Waypoint(aabb, generalColor, null) }
        // A role's stack is the other role's boxes it helps with, so it's only shown for a single role
        if (showStacks && !both) {
            plan.stacks.getOrNull(roles.first())?.forEach { id ->
                if (id in owned) return@forEach
                val aabb = result.boxes[id] ?: return@forEach
                out += Waypoint(aabb, stackColor, "§6Stack")
            }
        }
        return out
    }

    // ---------------------------------------------------------------- working out the room

    private fun resolve(room: DungeonRoom): Resolved? {
        val data = data ?: return null
        val name = room.data?.name ?: return null
        if (room.clayPos == null || room.rotation == null) return null

        val boxes = data.boxes[name].orEmpty().entries.sortedBy { it.key }.associate { (id, box) -> id to worldBox(room, box) }
        if (name in data.mini) return Resolved(null, null, null, true, boxes)

        val plans = data.plans[name].orEmpty()
        if (plans.isEmpty()) return Resolved(null, null, null, false, boxes)

        val door = entryPoint(room)?.let { (x, z) ->
            val rel = room.getRelativeCoords(BlockPos(floor(x).toInt(), 0, floor(z).toInt()))
            plans.keys.map { it to doorPos(it) }
                .minByOrNull { (_, p) -> p?.let { abs(it.first - rel.x) + abs(it.second - rel.z) } ?: Int.MAX_VALUE }
                ?.takeIf { (_, p) -> p != null && abs(p.first - rel.x) + abs(p.second - rel.z) <= DOOR_RANGE }
                ?.first
        }
        // Without a door the plan is still known if every door has the same one
        val plan = door?.let(plans::get) ?: plans.values.distinct().singleOrNull()
        return Resolved(door, door?.let { compass(room, it) }, plan, false, boxes)
    }

    /**
     * Where the player came into the room: the door between the two tiles if they walked in from the
     * tile next to it, else where they stood (a leap or pearl). The first time any of its tiles was
     * entered from outside the room.
     */
    private fun entryPoint(room: DungeonRoom): Pair<Double, Double>? {
        val roomTiles = room.tiles.map { it.x + it.z * 6 }.toSet()
        val (tile, entry) = roomTiles.mapNotNull { t -> tileEntries[t]?.takeIf { it.from !in roomTiles }?.let { t to it } }
            .minByOrNull { it.second.tick } ?: return null
        val (fx, fz) = entry.from % 6 to entry.from / 6
        val (tx, tz) = tile % 6 to tile / 6
        return if (entry.from >= 0 && abs(fx - tx) + abs(fz - tz) == 1)
            (tileCenter(fx) + tileCenter(tx)) / 2.0 to (tileCenter(fz) + tileCenter(tz)) / 2.0
        else entry.x to entry.z
    }

    private fun tileCenter(t: Int) = t * 32 - 185

    private fun doorPos(key: String): Pair<Int, Int>? {
        val parts = key.split(',')
        return (parts.getOrNull(0)?.toIntOrNull() ?: return null) to (parts.getOrNull(1)?.toIntOrNull() ?: return null)
    }

    private fun worldBox(room: DungeonRoom, box: Box): AABB {
        val a = room.getRealCoords(box.min)
        val b = room.getRealCoords(box.max)
        return AABB(
            min(a.x, b.x).toDouble(), min(a.y, b.y).toDouble(), min(a.z, b.z).toDouble(),
            max(a.x, b.x) + 1.0, max(a.y, b.y) + 1.0, max(a.z, b.z) + 1.0
        )
    }

    /** Which side of its tile a door is on, in the world: North, East, South or West. */
    private fun compass(room: DungeonRoom, key: String): String? {
        val (x, z) = doorPos(key) ?: return null
        val door = room.getRealCoords(BlockPos(x, 0, z))
        val center = room.tiles.map { tileCenter(it.x) to tileCenter(it.z) }
            .minByOrNull { (cx, cz) -> abs(cx - door.x) + abs(cz - door.z) } ?: return null
        val dx = door.x - center.first
        val dz = door.z - center.second
        return if (abs(dx) > abs(dz)) (if (dx > 0) "East" else "West") else (if (dz > 0) "South" else "North")
    }

    private fun summarize(room: DungeonRoom, result: Resolved) {
        val roles = roles()
        if (roles.isEmpty() || result.boxes.isEmpty()) return
        val name = room.data?.name ?: return
        val from = result.doorName?.let { "§7from the §f$it §7door" } ?: "§7(door unknown)"
        val detail = when {
            result.mini -> "§7miniboss room, kill all §f${result.boxes.size}"
            result.plan == null -> "§cplan depends on the door, showing every box"
            else -> roles.joinToString(" §8| ") { r ->
                val own = result.plan.roles.getOrNull(r)?.size ?: 0
                val stack = result.plan.stacks.getOrNull(r)?.size ?: 0
                "§${roleCode(r)}${roleName(r)} §f$own" + (if (stack > 0 && roles.size == 1) " §6+$stack stack" else "")
            }
        }
        modMessage("§cBR §8» §f$name $from§7: $detail")
    }

    // ---------------------------------------------------------------- data

    private fun loadBundled(): BrData? {
        val text = BloodRushWaypoints::class.java.getResourceAsStream(BUNDLED)?.bufferedReader()?.use { it.readText() } ?: return null
        val root = JsonParser.parseString(text).asJsonObject
        return parse(root.getAsJsonObject("boxes"), root.getAsJsonObject("m7"), root.getAsJsonObject("mini"))
    }

    /**
     * boxes: room -> [[x1, y1, z1, x2, y2, z2, id]]. m7: room -> door "x,z" -> number killing -> plan.
     * mini: room -> true for miniboss rooms. The BR Roles site's own formats.
     */
    private fun parse(boxes: JsonObject?, m7: JsonObject?, mini: JsonObject?): BrData {
        val boxMap = boxes?.entrySet()?.associate { (room, list) ->
            room to list.asJsonArray.mapNotNull { e ->
                val a = e.asJsonArray
                if (a.size() < 7) return@mapNotNull null
                val v = IntArray(7) { a[it].asInt }
                v[6] to Box(
                    BlockPos(min(v[0], v[3]), min(v[1], v[4]), min(v[2], v[5])),
                    BlockPos(max(v[0], v[3]), max(v[1], v[4]), max(v[2], v[5]))
                )
            }.toMap()
        }.orEmpty()

        val plans = m7?.entrySet()?.associate { (room, doors) ->
            room to doors.asJsonObject.entrySet().mapNotNull { (door, counts) ->
                val p = counts.asJsonObject.getAsJsonObject(KILLING) ?: return@mapNotNull null
                door to Plan(groups(p.get("roles")), groups(p.get("stacks")))
            }.toMap()
        }.orEmpty()

        val miniRooms = mini?.entrySet()?.filter { runCatching { it.value.asBoolean }.getOrDefault(false) }?.map { it.key }?.toSet().orEmpty()
        return BrData(boxMap, plans, miniRooms)
    }

    private fun groups(e: JsonElement?): List<List<Int>> =
        e?.takeIf { it.isJsonArray }?.asJsonArray?.map { g -> g.asJsonArray.map { it.asInt } } ?: emptyList()
}
