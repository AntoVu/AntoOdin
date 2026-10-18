// Feature idea from Devonian (GPL-3.0). Written from scratch for AntoOdin, no Devonian code is used.
// Hidden door check based on skies-starred OdinClient (Copyright (c) 2026, Starred, BSD 3-Clause)
package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.utils.AwtText
import com.anto.antoodin.utils.Skit
import com.anto.antoodin.utils.SystemFonts
import com.odtheking.odin.clickgui.settings.RenderableSetting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.DropdownSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.clickgui.settings.impl.SelectorSetting
import com.odtheking.odin.clickgui.settings.impl.StringSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.features.impl.dungeon.map.DungeonScan
import com.odtheking.odin.features.impl.dungeon.map.buildExampleDoors
import com.odtheking.odin.features.impl.dungeon.map.buildExampleRooms
import com.odtheking.odin.features.impl.dungeon.map.tile.*
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Color.Companion.darker
import com.odtheking.odin.utils.IVec2
import com.odtheking.odin.utils.itemId
import com.odtheking.odin.utils.renderX
import com.odtheking.odin.utils.renderZ
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.PlayerFaceExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.core.BlockPos
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.PlayerSkin
import net.minecraft.world.item.DyeColor
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.chunk.LevelChunk
import kotlin.math.ceil
import kotlin.math.max

/**
 * Devonian-style dungeon map drawn from Odin's map data ([DungeonScan]). Rooms and doors are drawn in a space where
 * one tile is 100 units, so the fractional room/door sizes stay exact with integer fills.
 */
object CleanMap : Module(
    name = "Dungeon Map (A)",
    description = "A clean dungeon map in Devonian's style, using Odin's map data.",
    category = Skit.ANTO
) {
    private val styleDropdown by DropdownSetting("Style", desc = "Map layout.")
    private val showHidden by BooleanSetting("Show Hidden Rooms", true, desc = "Shows every room loaded in the world, even ones you haven't found.").withDependency { styleDropdown }
    private val darken by NumberSetting("Hidden Darken", 0.37, 0.0..1.0, 0.01, desc = "Brightness of rooms you haven't found.").withDependency { styleDropdown && showHidden }
    private val padding by NumberSetting("Padding", 0.36, 0.0..2.0, 0.01, desc = "Space around the rooms, in rooms.").withDependency { styleDropdown }
    private val border by NumberSetting("Border", 1.96, 0.0..20.0, 0.01, desc = "Border thickness.").withDependency { styleDropdown }
    private val roomSize by NumberSetting("Room Size", 0.81, 0.0..1.0, 0.01, desc = "Room size as a share of a tile. The rest is the gap.").withDependency { styleDropdown }
    private val doorSize by NumberSetting("Door Size", 0.27, 0.0..1.0, 0.01, desc = "Door width as a share of a tile.").withDependency { styleDropdown }
    private val backgroundColor by ColorSetting("Background", Color("00000059"), true, desc = "Background color.").withDependency { styleDropdown }
    private val borderColor by ColorSetting("Border Color", Color("000000FF"), true, desc = "Border color.").withDependency { styleDropdown }

    private val textDropdown by DropdownSetting("Text", desc = "Room names and checkmarks.")
    var font by StringSetting("Font", "ComicSansMS", 64, desc = "Installed font for map text, without spaces (/ao mapfont suggests them). Minecraft for the vanilla font.", placeholder = "Minecraft").withDependency { textDropdown }
    private val textSize by NumberSetting("Text Size", 1.13, 0.0..2.0, 0.01, desc = "Room name size.").withDependency { textDropdown }
    private val textShadow by SelectorSetting("Text Shadow", AwtText.Shadow.DROP, desc = "Shadow behind the map text.").withDependency { textDropdown }
    private val colorNames by BooleanSetting("Color Room Names", true, desc = "Colors names by checkmark: white, green or red.").withDependency { textDropdown }
    private val normalNames by BooleanSetting("Normal Room Names", true, desc = "Shows names of normal rooms too, not just special ones.").withDependency { textDropdown }
    private val checkmarks by BooleanSetting("Checkmarks", true, desc = "Shows checkmarks under room names.").withDependency { textDropdown }
    private val iconSize by NumberSetting("Checkmark Size", 0.57, 0.0..2.0, 0.01, desc = "Checkmark size.").withDependency { textDropdown && checkmarks }

    private val markerDropdown by DropdownSetting("Markers", desc = "Players on the map.")
    private val heads by BooleanSetting("Player Heads", true, desc = "Draws teammates as their heads.").withDependency { markerDropdown }
    private val headScale by NumberSetting("Head Size", 2.5, 0.0..10.0, 0.01, desc = "Head size.").withDependency { markerDropdown && heads }
    private val selfMarker by BooleanSetting("Marker For Self", true, desc = "Draws you as an arrow instead of a head.").withDependency { markerDropdown }
    private val markerScale by NumberSetting("Marker Size", 1.96, 0.0..10.0, 0.01, desc = "Arrow size.").withDependency { markerDropdown }
    private val names by BooleanSetting("Player Names", true, desc = "Shows teammate names above their markers.").withDependency { markerDropdown }
    private val namesLeap by BooleanSetting("Names Need Leap", true, desc = "Only shows names while holding a Spirit Leap.").withDependency { markerDropdown && names }
    private val nameScale by NumberSetting("Name Size", 3.31, 0.0..10.0, 0.01, desc = "Name size.").withDependency { markerDropdown && names }

    private val colorDropdown by DropdownSetting("Colors", desc = "Room and door colors.")
    private val entranceColor by ColorSetting("Entrance", Color("007B00FF"), desc = "Entrance room.").withDependency { colorDropdown }
    private val normalColor by ColorSetting("Normal", Color("72431BFF"), desc = "Normal room.").withDependency { colorDropdown }
    private val minibossColor by ColorSetting("Miniboss", Color("FED40FFF"), desc = "Miniboss (yellow) room.").withDependency { colorDropdown }
    private val rareColor by ColorSetting("Rare", Color("F6C268FF"), desc = "Rare room.").withDependency { colorDropdown }
    private val fairyColor by ColorSetting("Fairy", Color("DD0FFBFF"), desc = "Fairy room.").withDependency { colorDropdown }
    private val bloodColor by ColorSetting("Blood", Color("FF0000FF"), desc = "Blood room.").withDependency { colorDropdown }
    private val puzzleColor by ColorSetting("Puzzle", Color("880888FF"), desc = "Puzzle room.").withDependency { colorDropdown }
    private val trapColor by ColorSetting("Trap", Color("D57E32FF"), desc = "Trap room.").withDependency { colorDropdown }
    private val unknownColor by ColorSetting("Unknown", Color("404040FF"), desc = "Room seen through a door but not on the map yet.").withDependency { colorDropdown }
    private val witherDoorColor by ColorSetting("Wither Door", Color("FD8E0FFF"), desc = "Wither door.").withDependency { colorDropdown }
    private val bloodDoorColor by ColorSetting("Blood Door", Color("FF0000FF"), desc = "Blood door.").withDependency { colorDropdown }
    private val entranceDoorColor by ColorSetting("Entrance Door", Color("007B00FF"), desc = "Doors out of the entrance. Other doors take the color of the special room they lead to.").withDependency { colorDropdown }

    private var awtFont: Pair<String, java.awt.Font?> = "" to null
    private var ticks = 0
    private val frame = Identifier.withDefaultNamespace("textures/map/decorations/frame.png")
    private val cross = Identifier.fromNamespaceAndPath("odin", "map/cross.png")
    private val green = Identifier.fromNamespaceAndPath("odin", "map/green_check.png")
    private val white = Identifier.fromNamespaceAndPath("odin", "map/white_check.png")
    private val question = Identifier.fromNamespaceAndPath("odin", "map/question.png")

    private val exampleRooms by lazy { buildExampleRooms() }
    private val exampleDoors by lazy { buildExampleDoors() }

    private const val SIZE = 100
    private const val TILE = 100

    private val hud by HUD(name, "The dungeon map.", false) { example ->
        AwtText.sweep()
        if (!example && (!DungeonUtils.inDungeons || DungeonUtils.inBoss)) return@HUD 0 to 0
        drawMap(this, example)
        SIZE to SIZE
    }

    /** Teammate marker half size in map units, for clicking them (Leap Menu Extras' Map Leap). */
    val markerRadius get() = max(2f * headScale.toFloat(), 3f)

    /**
     * Draws the map in a [SIZE] x [SIZE] box at the current pose. Returns each living teammate's marker
     * position (not yours) in that box, by name.
     */
    fun drawMap(graphics: GuiGraphicsExtractor, example: Boolean): Map<String, Pair<Float, Float>> = graphics.run {
        val (w, h) = if (example) 6 to 6 else gridSize()
        val cell = SIZE / (max(w, h) + padding.toFloat() * 2)
        val ox = (SIZE - w * cell) / 2
        val oy = (SIZE - h * cell) / 2

        fill(0, 0, SIZE, SIZE, backgroundColor.rgba)
        val rooms = if (example) exampleRooms else DungeonScan.rooms
        val tileRooms = HashMap<IVec2, DungeonRoom>()
        for (room in rooms) for (tile in tilesOf(room)) tileRooms[tile] = room

        pose().pushMatrix()
        pose().translate(ox, oy)
        pose().scale(cell / TILE)
        for (door in if (example) exampleDoors else DungeonScan.doors.values) drawDoor(door, tileRooms)
        for (room in rooms) roomColor(room)?.let { color -> fillTiles(tilesOf(room), color) }
        if (!showHidden && !example) for (hint in DungeonScan.pathHints) if (hint.room?.isViewable != true) fillTiles(listOf(hint.position), unknownColor)
        pose().popMatrix()

        for (room in rooms) if (roomColor(room) != null) drawRoomLabel(room, ox, oy, cell)
        val markers = if (example) emptyMap() else drawPlayers(ox, oy, cell)
        drawBorder()
        markers
    }

    init {
        // Listing system fonts takes a moment; do it off the render thread
        Thread { SystemFonts.names }.apply { isDaemon = true }.start()

        // Doors between rooms nobody opened aren't on the map item, so read them from the world until found
        on<TickEvent.End> {
            if (!showHidden || !DungeonUtils.inDungeons || DungeonUtils.inBoss || ++ticks % 20 != 0) return@on
            val level = mc.level ?: return@on
            for (x in -12..-2) for (z in -12..-2) {
                if ((x % 2 == 0) == (z % 2 == 0) || DungeonScan.doors.containsKey(IVec2(x, z)) || !level.hasChunk(x, z)) continue
                scanDoor(level.getChunk(x, z), x, z)
            }
        }
    }

    /** Dungeon size in tiles per floor (Odin's map offsets in [DungeonScan.initClient]). */
    private fun gridSize(): Pair<Int, Int> = when (DungeonUtils.floor?.floorNumber) {
        0 -> 4 to 4
        1 -> 4 to 5
        2, 3 -> 5 to 5
        4 -> 6 to 5
        else -> 6 to 6
    }

    /** Room tiles; Odin's example rooms only set shape and rotation, so derive their tiles from that. */
    private fun tilesOf(room: DungeonRoom): List<IVec2> {
        if (room.tiles.isNotEmpty()) return room.tiles
        val (x, z) = room.topLeft.x to room.topLeft.z
        val offsets = when (room.shape) {
            RoomShape.OneByOne -> listOf(0 to 0)
            RoomShape.TwoByTwo -> listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1)
            RoomShape.L -> when (room.rotation) {
                RoomRotation.WEST -> listOf(0 to 0, 1 to 0, 0 to 1)
                RoomRotation.NORTH -> listOf(0 to 0, 1 to 0, 1 to 1)
                else -> listOf(0 to 0, 0 to 1, 1 to 1)
            }
            else -> List(room.shape.tileAmount) { if (room.rotation == RoomRotation.SOUTH) it to 0 else 0 to it }
        }
        return offsets.map { (dx, dz) -> IVec2(x + dx, z + dz) }
    }

    private fun typeColor(type: RoomType): Color = when (type) {
        RoomType.ENTRANCE -> entranceColor
        RoomType.NORMAL -> normalColor
        RoomType.CHAMPION -> minibossColor
        RoomType.RARE -> rareColor
        RoomType.FAIRY -> fairyColor
        RoomType.BLOOD -> bloodColor
        RoomType.PUZZLE -> puzzleColor
        RoomType.TRAP -> trapColor
        else -> unknownColor
    }

    /** The world scan's type when known: the map item draws miniboss and rare rooms as normal ones. */
    private val DungeonRoom.realType get() = data?.type ?: type

    /** Fill color, or null when the room isn't shown. */
    private fun roomColor(room: DungeonRoom): Color? = when {
        room.isViewable -> if (room.mapSeen || room.type == RoomType.ENTRANCE) typeColor(room.realType) else unknownColor
        showHidden && room.realType != RoomType.UNDISCOVERED -> typeColor(room.realType).darker(darken.toFloat())
        else -> null
    }

    private val gap get() = ((1 - roomSize) * TILE / 2).toInt()

    /** Each tile, plus the gaps between tiles of the same room so 2x1, 2x2 and L rooms are one shape. */
    private fun GuiGraphicsExtractor.fillTiles(tiles: List<IVec2>, color: Color) {
        val o = gap
        val r = TILE - o * 2
        for (t in tiles) {
            val x0 = t.x * TILE + o
            val z0 = t.z * TILE + o
            val right = IVec2(t.x + 1, t.z) in tiles
            val down = IVec2(t.x, t.z + 1) in tiles
            fill(x0, z0, x0 + r, z0 + r, color.rgba)
            if (right) fill(x0 + r, z0, x0 + r + o * 2, z0 + r, color.rgba)
            if (down) fill(x0, z0 + r, x0 + r, z0 + r + o * 2, color.rgba)
            if (right && down && IVec2(t.x + 1, t.z + 1) in tiles) fill(x0 + r, z0 + r, x0 + r + o * 2, z0 + r + o * 2, color.rgba)
        }
    }

    private fun GuiGraphicsExtractor.drawDoor(door: DungeonDoor, tileRooms: Map<IVec2, DungeonRoom>) {
        val a = tileRooms[door.position]
        val b = tileRooms[IVec2(door.position.x + door.rotation.offset.x, door.position.z + door.rotation.offset.z)]
        val seen = (a?.isViewable == true) to (b?.isViewable == true)
        if (!seen.first && !seen.second && !showHidden) return

        var color = when {
            door.type == DoorType.Wither -> witherDoorColor
            door.type == DoorType.Blood -> bloodDoorColor
            door.type == DoorType.Fairy -> fairyColor
            a?.realType == RoomType.ENTRANCE || b?.realType == RoomType.ENTRANCE -> entranceDoorColor
            else -> listOfNotNull(a, b).firstOrNull { it.realType != RoomType.NORMAL && it.realType != RoomType.FAIRY }?.let { typeColor(it.realType) } ?: normalColor
        }
        // Wither and blood doors stay bright so the blood rush path stands out
        if ((!seen.first || !seen.second) && door.type != DoorType.Wither && door.type != DoorType.Blood) color = color.darker(darken.toFloat())

        val o = gap
        val half = (doorSize * TILE / 2).toInt()
        val (x, z) = door.position.x * TILE to door.position.z * TILE
        if (door.rotation == DoorRotation.Horizontal) fill(x + TILE - o, z + TILE / 2 - half, x + TILE + o, z + TILE / 2 + half, color.rgba)
        else fill(x + TILE / 2 - half, z + TILE - o, x + TILE / 2 + half, z + TILE + o, color.rgba)
    }

    /** Inside the map edge; drawn at 1/100 px so fractional widths work. Devonian's border unit is about half a GUI px. */
    private fun GuiGraphicsExtractor.drawBorder() {
        val t = (border * 50).toInt()
        if (t <= 0) return
        val s = SIZE * 100
        pose().pushMatrix()
        pose().scale(0.01f)
        fill(0, 0, s, t, borderColor.rgba)
        fill(0, s - t, s, s, borderColor.rgba)
        fill(0, t, t, s - t, borderColor.rgba)
        fill(s - t, t, s, s - t, borderColor.rgba)
        pose().popMatrix()
    }

    private fun GuiGraphicsExtractor.drawRoomLabel(room: DungeonRoom, ox: Float, oy: Float, cell: Float) {
        val tiles = tilesOf(room)
        // L rooms label their vertical arm (the corner tile and the tile above or below it), like Devonian
        val corner = if (room.shape == RoomShape.L) tiles.firstOrNull { t -> tiles.any { it.z == t.z && it != t } && tiles.any { it.x == t.x && it != t } } else null
        val box = if (corner != null) tiles.filter { it.x == corner.x } else tiles
        val cx = ox + (box.minOf { it.x } + box.maxOf { it.x } + 1) * cell / 2
        val cy = oy + (box.minOf { it.z } + box.maxOf { it.z } + 1) * cell / 2

        if (checkmarks) {
            when (room.checkmark) {
                MapCheckmark.WHITE -> white
                MapCheckmark.GREEN -> green
                MapCheckmark.RED -> cross
                MapCheckmark.QUESTION_MARK -> question
                else -> null
            }?.let { icon ->
                val size = iconSize.toFloat() * roomSize.toFloat() * cell
                pose().pushMatrix()
                pose().translate(cx - size / 2, cy - size / 2)
                pose().scale(size / 16)
                blit(RenderPipelines.GUI_TEXTURED, icon, 0, 0, 0f, 0f, 16, 16, 16, 16)
                pose().popMatrix()
            }
        }

        val name = room.name ?: return
        when (room.realType) {
            RoomType.ENTRANCE, RoomType.BLOOD, RoomType.FAIRY, RoomType.UNKNOWN, RoomType.UNDISCOVERED -> return
            RoomType.NORMAL -> if (!normalNames) return
            else -> {}
        }

        val color = if (!colorNames) 0xFFFFFFFF.toInt() else when (room.checkmark) {
            MapCheckmark.GREEN -> 0xFF55FF55.toInt()
            MapCheckmark.WHITE -> 0xFFFFFFFF.toInt()
            MapCheckmark.RED -> 0xFFFF5555.toInt()
            else -> 0xFFAAAAAA.toInt()
        }
        // Devonian sizes the font to a quarter of the room
        val size = roomSize.toFloat() * cell * 0.25f * textSize.toFloat()
        // Devonian's text box: textSize * roomSize of a tile, plus any extra tiles the box spans
        val maxWidth = (box.maxOf { it.x } - box.minOf { it.x } + textSize.toFloat() * roomSize.toFloat()) * cell
        val lines = name.split(" ").flatMap { hyphenate(it, maxWidth, size) }
        val lineHeight = size * 1.1f
        for ((i, line) in lines.withIndex()) drawText(line, cx, cy + (i - (lines.size - 1) / 2f) * lineHeight, size, color)
    }

    /** Splits a word too wide for the room into even pieces joined by hyphens ("Wither-", "mancer"). */
    private fun hyphenate(word: String, maxWidth: Float, size: Float): List<String> {
        val pieces = ceil(textWidth(word, size) / maxWidth).toInt()
        if (pieces <= 1 || word.length < 4) return listOf(word)
        val chunks = word.chunked(ceil(word.length / pieces.toFloat()).toInt())
        return chunks.dropLast(1).map { "$it-" } + chunks.last()
    }

    private fun currentFont(): java.awt.Font? {
        if (awtFont.first != font) awtFont = font to SystemFonts.find(font)
        return awtFont.second
    }

    private fun textWidth(text: String, size: Float): Float =
        currentFont()?.let { AwtText.width(text, it, size) } ?: (mc.font.width(text) * size / 8f)

    /** Centered on (x, y), [size] GUI px per em. Vanilla font when the font setting isn't an installed font. */
    private fun GuiGraphicsExtractor.drawText(text: String, x: Float, y: Float, size: Float, color: Int) {
        currentFont()?.let { return AwtText.draw(this, text, it, size, x, y, color, textShadow) }
        val scale = size / 8f // vanilla glyphs are about 8 px per em
        val tx = -mc.font.width(text) / 2
        val ty = -mc.font.lineHeight / 2
        pose().pushMatrix()
        pose().translate(x, y)
        pose().scale(scale)
        if (textShadow == AwtText.Shadow.OUTLINE) for ((dx, dy) in arrayOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) text(mc.font, text, tx + dx, ty + dy, 0xFF000000.toInt(), false)
        text(mc.font, text, tx, ty, color, textShadow == AwtText.Shadow.DROP)
        pose().popMatrix()
    }

    /** Odin's map units (a 20-unit tile with a 16-unit room in its top left) to GUI pixels on this map. */
    private fun playerPos(entity: Player?, mapPos: IVec2, ox: Float, oy: Float, cell: Float): Pair<Float, Float> {
        val (ux, uz) = entity?.let { (it.renderX.toFloat() + 200f) * 20f / 32f to (it.renderZ.toFloat() + 200f) * 20f / 32f }
            ?: ((mapPos.x + 128) / 2f - DungeonScan.startX to (mapPos.z + 128) / 2f - DungeonScan.startY)
        // Odin's room center is 8 units in, ours is half a tile in
        return ox + (ux / 20f + 0.1f) * cell to oy + (uz / 20f + 0.1f) * cell
    }

    private fun GuiGraphicsExtractor.drawPlayers(ox: Float, oy: Float, cell: Float): Map<String, Pair<Float, Float>> {
        val self = mc.player ?: return emptyMap()
        val markers = HashMap<String, Pair<Float, Float>>()
        val showNames = names && (!namesLeap || self.mainHandItem.itemId in setOf("SPIRIT_LEAP", "INFINITE_SPIRIT_LEAP"))

        for (player in DungeonUtils.dungeonTeammatesNoSelf) {
            if (player.isDead) continue
            val (x, y) = playerPos(player.entity, player.mapPos, ox, oy, cell)
            markers[player.name] = x to y
            val skin = player.playerSkin
            if (heads && skin != null) drawHead(skin, x, y, player.renderYaw) else drawArrow(x, y, player.renderYaw)
            if (showNames) {
                val size = 2.4f * nameScale.toFloat() // 0.3 * nameScale of a vanilla 8 px em
                drawText(player.name, x, y - 2f * headScale.toFloat() - size * 0.6f, size, 0xFFFFFFFF.toInt())
            }
        }

        val (x, y) = playerPos(self, IVec2(0, 0), ox, oy, cell)
        if (selfMarker || !heads) drawArrow(x, y, self.yRot) else drawHead(self.skin, x, y, self.yRot)
        return markers
    }

    private fun GuiGraphicsExtractor.drawHead(skin: PlayerSkin, x: Float, y: Float, yaw: Float) {
        pose().pushMatrix()
        pose().translate(x, y)
        pose().rotate(Math.toRadians(180.0 + yaw).toFloat())
        pose().scale(headScale.toFloat() / 2f) // 8 px face -> 4 * headScale px
        PlayerFaceExtractor.extractRenderState(this, skin, -4, -4, 8)
        pose().popMatrix()
    }

    /** Vanilla's map frame arrow (5x7 px of an 8x8 texture), like Odin's self marker. */
    private fun GuiGraphicsExtractor.drawArrow(x: Float, y: Float, yaw: Float) {
        pose().pushMatrix()
        pose().translate(x, y)
        pose().rotate(Math.toRadians(180.0 + yaw).toFloat())
        pose().scale(0.8f * markerScale.toFloat())
        pose().translate(-0.5f, -0.5f)
        blit(RenderPipelines.GUI_TEXTURED, frame, -2, -4, 2f, 0f, 5, 7, 8, 8)
        pose().popMatrix()
    }

    /** A door chunk (one odd chunk coordinate) has open sky above the door and a floor under it. */
    private fun scanDoor(chunk: LevelChunk, x: Int, z: Int) {
        val pos = IVec2(x, z)
        val x1 = x * 16 + 7
        val z1 = z * 16 + 7
        if ((86..160).any { val state = chunk.getBlockState(BlockPos(x1, it, z1)); !state.isAir && !state.`is`(Blocks.GOLD_BLOCK) }) return
        if (chunk.getBlockState(BlockPos(x1, 68, z1)).isAir) return

        val type = when (chunk.getBlockState(BlockPos(x1, 69, z1)).block) {
            Blocks.COAL_BLOCK -> DoorType.Wither
            Blocks.DYED_TERRACOTTA.pick(DyeColor.RED) -> DoorType.Blood
            else -> DoorType.Normal
        }

        DungeonScan.doors[pos] = DungeonDoor(((pos - 1) / 2) + 6, if (z % 2 == 0) DoorRotation.Horizontal else DoorRotation.Vertical, type, normalColor)
        DungeonScan.updateViewableDoors()
    }
}
