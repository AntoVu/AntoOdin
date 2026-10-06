// Pack view adapted from Odin's WaypointPackSelectorScreen (Copyright (c) 2025, odtheking, BSD 3-Clause)
package com.anto.antoodin.features.impl.anto

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.config.DungeonWaypointConfig
import com.odtheking.odin.features.impl.dungeon.dungeonwaypoints.DungeonWaypoints.DungeonWaypoint
import com.odtheking.odin.features.impl.dungeon.dungeonwaypoints.TextPromptScreen
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.skyblock.Island
import net.minecraft.client.gui.components.AbstractScrollArea
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.ScrollableLayout
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class SkyblockWaypointsScreen(private val previous: Screen?) : Screen(Component.literal("Skyblock Waypoints")) {

    /** Null shows the pack manager, otherwise the island key whose waypoints are listed. */
    private var view: String? = null
    private var bossOpen = false
    private var sidebarArea: AbstractScrollArea? = null
    private var panelArea: AbstractScrollArea? = null
    private var sidebarScroll = 0.0
    private var panelScroll = 0.0

    // Delete pack / clear island need a second click within 3 s
    private var confirmKey: String? = null
    private var confirmTime = 0L

    init {
        SkyblockWaypointPacks.reload()
    }

    override fun init() = rebuild()

    override fun repositionElements() = rebuild()

    private fun rebuild() {
        sidebarScroll = sidebarArea?.scrollAmount() ?: sidebarScroll
        panelScroll = panelArea?.scrollAmount() ?: panelScroll
        clearWidgets()

        val sidebar = LinearLayout.vertical().spacing(2)
        sidebar.addChild(sideButton("Packs", null))
        ISLANDS.forEach { sidebar.addChild(sideButton(it.displayName, it.name)) }
        sidebar.addChild(Button.builder(Component.literal((if (bossOpen) "▼ " else "▶ ") + "Dungeon Boss")) {
            bossOpen = !bossOpen
            rebuild()
        }.size(SIDEBAR_WIDTH, 18).build())
        if (bossOpen) (1..7).forEach { sidebar.addChild(sideButton("Floor $it", SkyblockWaypoints.bossKey(it))) }
        sidebarArea = addScrollable(sidebar, 10, 10, height - 20, SIDEBAR_WIDTH, sidebarScroll)

        val panelX = SIDEBAR_WIDTH + 30
        val panelWidth = width - panelX - 10
        val header = LinearLayout.vertical().spacing(6)
        header.defaultCellSetting().alignHorizontallyCenter()
        val rows = LinearLayout.vertical().spacing(4)
        rows.defaultCellSetting().alignHorizontallyCenter()
        view?.let { islandView(it, header, rows) } ?: packsView(header, rows)

        header.arrangeElements()
        header.setPosition(panelX + panelWidth / 2 - header.width / 2, 10)
        header.visitWidgets(this::addRenderableWidget)
        val top = 10 + header.height + 8
        panelArea = addScrollable(rows, panelX, top, height - top - 10, panelWidth, panelScroll)
    }

    private fun sideButton(label: String, key: String?) =
        Button.builder(Component.literal(if (view == key) "§e$label" else label)) {
            view = key
            panelScroll = 0.0
            rebuild()
        }.size(SIDEBAR_WIDTH, 18).build()

    private fun addScrollable(content: LinearLayout, x: Int, y: Int, height: Int, minWidth: Int, scroll: Double): AbstractScrollArea? {
        content.arrangeElements()
        val layout = ScrollableLayout(mc, content, height.coerceAtLeast(40))
        layout.setMinWidth(minWidth)
        layout.setMaxHeight(height.coerceAtLeast(40))
        layout.arrangeElements()
        layout.setPosition(x, y)
        var area: AbstractScrollArea? = null
        layout.visitWidgets {
            addRenderableWidget(it)
            if (it is AbstractScrollArea) area = it
        }
        area?.setScrollAmount(scroll)
        return area
    }

    private fun packsView(header: LinearLayout, rows: LinearLayout) {
        header.addChild(StringWidget(Component.literal("§6§lWaypoint Packs"), font))
        val actions = LinearLayout.horizontal().spacing(8)
        actions.addChild(Button.builder(Component.literal("Create Pack")) {
            prompt("Create Pack") { SkyblockWaypointPacks.create(it) }
        }.width(100).build())
        actions.addChild(Button.builder(Component.literal("Import")) { importFromClipboard() }.width(80).build())
        header.addChild(actions)

        val packs = SkyblockWaypointPacks.packs
        packs.forEach { (name, waypoints) ->
            val row = LinearLayout.horizontal().spacing(4)
            val isSelected = name in SkyblockWaypoints.selectedPackIds
            val isEdit = name == SkyblockWaypoints.editPackId

            row.addChild(Button.builder(Component.literal(if (isSelected) "§a☑" else "§7☐")) { SkyblockWaypointPacks.toggle(name); rebuild() }
                .size(24, 20).tooltip(Tooltip.create(Component.literal(if (isSelected) "Enabled" else "Disabled"))).build())
            row.addChild(Button.builder(Component.literal((if (isEdit) "§e★ " else "§7") + name)) { SkyblockWaypointPacks.setEdit(name); rebuild() }
                .size(180, 20).tooltip(Tooltip.create(Component.literal(if (isEdit) "Currently editing" else "Click to edit"))).build())
            row.addChild(StringWidget(Component.literal("§a${waypoints.values.sumOf { it.size }} §7wp"), font).apply { setWidth(50) })
            row.addChild(Button.builder(Component.literal("✎")) {
                prompt("Rename Pack") { if (it != name) SkyblockWaypointPacks.rename(name, it) }
            }.size(24, 20).tooltip(Tooltip.create(Component.literal("Rename"))).build())
            row.addChild(confirmButton("pack:$name", "Delete", packs.size > 1) { SkyblockWaypointPacks.delete(name) })
            rows.addChild(row)
        }
    }

    private fun islandView(key: String, header: LinearLayout, rows: LinearLayout) {
        header.addChild(StringWidget(Component.literal("§6§l${label(key)}"), font))
        val actions = LinearLayout.horizontal().spacing(8)
        actions.defaultCellSetting().alignVerticallyMiddle()
        actions.addChild(StringWidget(Component.literal("§7Editing pack: §e${SkyblockWaypoints.editPackId}"), font))
        val waypoints = SkyblockWaypointPacks.editable(key)
        actions.addChild(Button.builder(Component.literal(if (isConfirming("island:$key")) "§cConfirm?" else "Clear Island")) {
            if (confirm("island:$key")) {
                waypoints.clear()
                SkyblockWaypointPacks.save()
            }
            rebuild()
        }.width(80).build().apply { active = waypoints.isNotEmpty() })
        header.addChild(actions)

        if (waypoints.isEmpty()) rows.addChild(StringWidget(Component.literal("§7No waypoints here in this pack. Place them in game with Allow Edits on."), font))
        waypoints.toList().forEach { waypoint ->
            val row = LinearLayout.horizontal().spacing(4)
            row.defaultCellSetting().alignVerticallyMiddle()
            row.addChild(StringWidget(Component.literal("■").withColor(waypoint.color.rgba and 0xFFFFFF), font).apply { setWidth(10) })
            row.addChild(StringWidget(Component.literal(waypoint.title ?: "§7Untitled"), font).apply { setWidth(150) })
            row.addChild(StringWidget(Component.literal("§7${waypoint.blockPos.x}, ${waypoint.blockPos.y}, ${waypoint.blockPos.z}"), font).apply { setWidth(100) })
            row.addChild(Button.builder(Component.literal("✎")) {
                prompt("Waypoint Name", allowBlank = true) { text -> replace(waypoints, waypoint, waypoint.copy(title = text.ifBlank { null })) }
            }.size(24, 20).tooltip(Tooltip.create(Component.literal("Retitle"))).build())
            row.addChild(Button.builder(Component.literal("§c✕")) { replace(waypoints, waypoint, null); rebuild() }
                .size(24, 20).tooltip(Tooltip.create(Component.literal("Delete"))).build())
            rows.addChild(row)
        }
    }

    private fun replace(waypoints: MutableList<DungeonWaypoint>, old: DungeonWaypoint, new: DungeonWaypoint?) {
        val index = waypoints.indexOf(old).takeIf { it >= 0 } ?: return
        if (new == null) waypoints.removeAt(index) else waypoints[index] = new
        SkyblockWaypointPacks.save()
    }

    private fun confirmButton(key: String, tooltip: String, active: Boolean, action: () -> Unit): Button =
        Button.builder(Component.literal(if (isConfirming(key)) "§c✓?" else "§c✕")) {
            if (confirm(key)) action()
            rebuild()
        }.size(24, 20).tooltip(Tooltip.create(Component.literal(if (isConfirming(key)) "Confirm?" else tooltip))).build()
            .also { it.active = active }

    private fun isConfirming(key: String) = confirmKey == key && System.currentTimeMillis() - confirmTime < 3000

    /** True on the second click; the first one arms the button. */
    private fun confirm(key: String): Boolean {
        if (isConfirming(key)) {
            confirmKey = null
            return true
        }
        confirmKey = key
        confirmTime = System.currentTimeMillis()
        return false
    }

    override fun tick() {
        super.tick()
        if (confirmKey != null && !isConfirming(confirmKey!!)) {
            confirmKey = null
            rebuild()
        }
    }

    private fun prompt(title: String, allowBlank: Boolean = false, onDone: (String) -> Unit) {
        mc.setScreen(TextPromptScreen(title).setCallback { text ->
            if (allowBlank || text.isNotBlank()) onDone(text.trim())
            mc.setScreen(this)
        })
    }

    private fun importFromClipboard() {
        val clipboard = mc.keyboardHandler.clipboard.trim()
        if (clipboard.isBlank()) return modMessage("§cClipboard is empty!")
        val waypoints = DungeonWaypointConfig.decodeWaypoints(clipboard) ?: return modMessage("§cFailed to decode waypoints from clipboard.")
        prompt("Import as New Pack") { name ->
            if (SkyblockWaypointPacks.create(name, waypoints)) modMessage("§aImported waypoints as pack '$name'!")
        }
    }

    override fun onClose() = mc.setScreen(previous)

    override fun isPauseScreen() = false

    private companion object {
        const val SIDEBAR_WIDTH = 110

        // Mineshafts are randomized, and dungeon clear is Odin's Dungeon Waypoints
        val ISLANDS = Island.entries - setOf(Island.SinglePlayer, Island.Dungeon, Island.Mineshaft, Island.Unknown)

        fun label(key: String) = Island.entries.find { it.name == key }?.displayName ?: "Floor ${key.removePrefix("F").removeSuffix("Boss")} Boss"
    }
}
