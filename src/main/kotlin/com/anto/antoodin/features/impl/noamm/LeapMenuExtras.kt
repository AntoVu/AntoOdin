// Ported from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.noamm

import com.anto.antoodin.features.impl.anto.CleanMap
import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.RenderableSetting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.MessageEvent
import com.odtheking.odin.events.ScreenEvent
import com.odtheking.odin.events.core.EventPriority
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.features.impl.dungeon.LeapMenu
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.clickSlot
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.render.roundedOutline
import com.odtheking.odin.utils.render.roundedRect
import com.odtheking.odin.utils.skyblock.dungeon.DungeonClass
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.ui.compose.Animatable
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import kotlin.math.abs

/**
 * Draws over Odin's Leap Menu tiles (Odin draws first at HIGHEST priority, then cancels, which doesn't stop us),
 * and hides teammates you just leaped onto.
 *
 * Map Leap draws Dungeon Map (A) over the tiles; clicking a teammate's marker leaps to them. Odin's click handler
 * still runs (cancelling doesn't stop it), so `LeapMenuMixin` skips its quadrant leap while the map is shown.
 *
 * @see com.anto.antoodin.mixin.mixins.EntityRendererMixin
 */
object LeapMenuExtras : Module(
    name = "Leap Menu Extras",
    description = "Tints dead players and rings the last door opener in Odin's Leap Menu, and hides players after you leap.",
    category = Skit.NOAMM
) {
    private val tintDead by BooleanSetting("Tint Dead", true, desc = "Tints dead players red in the leap menu.")
    private val highlightDoorOpener by BooleanSetting("Highlight Door Opener", true, desc = "Draws a ring around the face of whoever opened the last wither door.")
    private val doorOpenerColor by ColorSetting("Door Opener Color", Colors.MINECRAFT_GOLD, true, desc = "Color of the door opener ring.").withDependency { highlightDoorOpener }
    private val hideAfterLeap by BooleanSetting("Hide After Leap", true, desc = "Hides teammates standing on you right after you leap to them.")
    private val hideTime by NumberSetting("Hide Time", 3.5f, 0.5..5.0, 0.5f, desc = "How long to hide them for.", unit = "s").withDependency { hideAfterLeap }
    private val mapLeap by BooleanSetting("Map Leap", false, desc = "Shows Dungeon Map (A) in the leap menu during clear. Click a teammate on it to leap to them. Keybinds still work.")
    private val mapLeapScale by NumberSetting("Map Leap Scale", 2.5f, 1.0..5.0, 0.1f, desc = "Size of the map in the leap menu.", unit = "x").withDependency { mapLeap }

    private val deadTint = Color(255, 0, 0, 0.2f)
    private const val SQRT_2 = 1.4142135f
    private const val RING_GAP = 3f
    private const val RING_THICKNESS = 3f

    private val doorOpenedRegex = Regex("^(?:\\[.+?] )?(\\w{1,16}) opened a WITHER door!$")
    private val leapedRegex = Regex("^You have teleported to (\\w{1,16})!$")

    private var doorOpener: String? = null
    private var hideUntil = 0L
    private var mapLeapTarget: String? = null

    // Mirrors Odin's per-tile hover animation so the overlay grows with the tile
    private val hoverProgress = List(4) { Animatable(0f) }

    init {
        on<MessageEvent.Chat> {
            doorOpenedRegex.find(message)?.let { doorOpener = it.groupValues[1] }
            if (message == "The BLOOD DOOR has been opened!") doorOpener = null
            if (hideAfterLeap && leapedRegex.matches(message)) hideUntil = System.currentTimeMillis() + (hideTime * 1000).toLong()
        }

        on<LevelEvent.Load> {
            doorOpener = null
            hideUntil = 0L
        }

        on<ScreenEvent.Render>(EventPriority.HIGH) {
            if (!LeapMenu.enabled || !isLeapScreen(screen)) return@on
            if (mapLeapShown()) drawMapLeap(this) else drawTints(this)
        }

        on<ScreenEvent.MouseClick> {
            if (!mapLeapShown()) return@on
            val name = mapLeapTarget ?: return@on
            val chest = screen as? AbstractContainerScreen<*> ?: return@on
            if (DungeonUtils.leapTeammates.find { it.name == name }?.isDead == true) return@on modMessage("This player is dead, can't leap.")
            // Same slot lookup as Odin's leapTo: heads in slots 11-15 named "<rank> <name>"
            val slot = chest.menu.slots.subList(11, 16).firstOrNull { it.item.hoverName.string.substringAfter(' ').equals(name, true) } ?: return@on
            mc.player?.clickSlot(slot.index)
            modMessage("Teleporting to $name.")
        }
    }

    private fun isLeapScreen(screen: Screen?): Boolean =
        (screen as? AbstractContainerScreen<*>)?.title?.string.let { it == "Spirit Leap" || it == "Teleport to Player" }

    /** Map Leap replaces the tiles in clear; in boss the map is hidden, so Odin's tiles are used. */
    fun mapLeapShown() = enabled && mapLeap && LeapMenu.enabled && DungeonUtils.inDungeons && !DungeonUtils.inBoss && isLeapScreen(mc.gui.screen())

    private fun drawMapLeap(event: ScreenEvent.Render) {
        val graphics = event.guiGraphics
        val width = mc.window.guiScaledWidth
        val height = mc.window.guiScaledHeight
        val scale = mapLeapScale
        val ox = (width - 100 * scale) / 2
        val oy = (height - 100 * scale) / 2

        // Above Odin's tiles, which are drawn first and can't be skipped
        graphics.nextStratum()
        graphics.fill(0, 0, width, height, 0xD0000000.toInt())
        graphics.pose().pushMatrix()
        graphics.pose().translate(ox, oy)
        graphics.pose().scale(scale)
        val markers = CleanMap.drawMap(graphics, false)
        graphics.pose().popMatrix()

        val r = CleanMap.markerRadius * scale + 2
        mapLeapTarget = markers.entries.firstOrNull { (_, pos) ->
            abs(event.mouseX - (ox + pos.first * scale)) <= r && abs(event.mouseY - (oy + pos.second * scale)) <= r
        }?.key
        val (x, y) = mapLeapTarget?.let { markers[it] } ?: return
        val cx = (ox + x * scale).toInt()
        val cy = (oy + y * scale).toInt()
        graphics.nextStratum()
        graphics.roundedOutline(cx - r.toInt(), cy - r.toInt(), cx + r.toInt(), cy + r.toInt(), Colors.WHITE.rgba, 2f, 3f)
    }

    private class Ring(val x: Float, val y: Float, val radius: Float, val thickness: Float)

    private fun drawTints(event: ScreenEvent.Render) {
        val graphics = event.guiGraphics
        val halfW = mc.window.guiScaledWidth / 2
        val halfH = mc.window.guiScaledHeight / 2
        val scale = (LeapMenu.settings["Render Scale"]?.value as? Number)?.toFloat() ?: 1f
        val width = LeapMenu.BOX_WIDTH
        val height = LeapMenu.BOX_HEIGHT
        val face = (height * 0.76).toInt()
        val rings = ArrayList<Ring>()

        repeat(4) { i ->
            val col = i % 2
            val row = i / 2
            val hovered = (if (col == 0) event.mouseX < halfW else event.mouseX >= halfW) && (if (row == 0) event.mouseY < halfH else event.mouseY >= halfH)
            hoverProgress[i].animateTo(if (hovered) 1f else 0f, 200)

            val player = DungeonUtils.leapTeammates.getOrNull(i) ?: return@repeat
            if (player.clazz == DungeonClass.EMPTY) return@repeat
            val dead = tintDead && player.isDead
            val opener = highlightDoorOpener && player.name == doorOpener
            if (!dead && !opener) return@repeat

            // Same transform as Odin's tile
            val grow = hoverProgress[i].value * 5f
            val nearX = (if (col == 0) halfW - 24 else halfW + 24).toFloat()
            val nearY = (if (row == 0) halfH - 24 else halfH + 24).toFloat()
            val scaleX = scale * (width + grow * 2f) / width
            val scaleY = scale * (height + grow * 2f) / height
            val localX = if (col == 0) -width else 0
            val localY = if (row == 0) -height else 0

            if (dead) {
                graphics.pose().pushMatrix()
                graphics.pose().translate(nearX, nearY)
                graphics.pose().scale(scaleX, scaleY)
                graphics.roundedRect(localX, localY, localX + width, localY + height, deadTint.rgba, 9f)
                graphics.pose().popMatrix()
            }

            // Odin draws the face as a square 9 px into the tile; the ring goes just outside its corners
            if (opener) {
                val avgScale = (scaleX + scaleY) / 2f
                rings.add(Ring(
                    nearX + (localX + 9 + face / 2f) * scaleX,
                    nearY + (localY + 9 + face / 2f) * scaleY,
                    (face / 2f * SQRT_2 + RING_GAP) * avgScale,
                    RING_THICKNESS * avgScale
                ))
            }
        }

        if (rings.isEmpty()) return
        // New stratum so the ring is drawn above Odin's tiles and faces. A fully rounded outline is an anti-aliased circle.
        graphics.nextStratum()
        rings.forEach {
            val r = it.radius.toInt()
            graphics.roundedOutline(it.x.toInt() - r, it.y.toInt() - r, it.x.toInt() + r, it.y.toInt() + r, doorOpenerColor.rgba, it.thickness, r.toFloat())
        }
    }

    @JvmStatic
    fun shouldHideEntity(entity: Entity): Boolean {
        if (!enabled || System.currentTimeMillis() > hideUntil || entity !is Player) return false
        val self = mc.player ?: return false
        if (entity == self || entity.distanceToSqr(self) > 4) return false
        return DungeonUtils.dungeonTeammates.any { it.name == entity.name.string }
    }
}
