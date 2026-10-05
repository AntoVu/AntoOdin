// Ported from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.noamm

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
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
import com.odtheking.odin.utils.render.roundedFill
import com.odtheking.odin.utils.skyblock.dungeon.DungeonClass
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.ui.HoverHandler
import com.odtheking.odin.utils.ui.rendering.NVGPIPRenderer
import com.odtheking.odin.utils.ui.rendering.NVGRenderer
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player

/**
 * Draws over Odin's Leap Menu tiles (Odin draws first at HIGHEST priority, then cancels, which doesn't stop us),
 * and hides teammates you just leaped onto.
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
    private val hideTime by NumberSetting("Hide Time", 3.5f, 0.5f, 5f, 0.5f, desc = "How long to hide them for.", unit = "s").withDependency { hideAfterLeap }

    private val deadTint = Color(255, 0, 0, 0.2f)
    private const val SQRT_2 = 1.4142135f
    private const val RING_GAP = 3f
    private const val RING_THICKNESS = 3f

    private val doorOpenedRegex = Regex("^(?:\\[.+?] )?(\\w{1,16}) opened a WITHER door!$")
    private val leapedRegex = Regex("^You have teleported to (\\w{1,16})!$")

    private var doorOpener: String? = null
    private var hideUntil = 0L

    // Mirrors Odin's per-tile hover animation so the overlay grows with the tile
    private val hoverHandlers = List(4) { HoverHandler(200L) }

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
            if (!LeapMenu.enabled) return@on
            val title = (screen as? AbstractContainerScreen<*>)?.title?.string ?: return@on
            if (title != "Spirit Leap" && title != "Teleport to Player") return@on
            drawTints(this)
        }
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
            val hover = hoverHandlers[i]
            val hovered = (if (col == 0) event.mouseX < halfW else event.mouseX >= halfW) && (if (row == 0) event.mouseY < halfH else event.mouseY >= halfH)
            if (hovered != hover.isHovered) {
                hover.anim.start()
                hover.isHovered = hovered
            }

            val player = DungeonUtils.leapTeammates.getOrNull(i) ?: return@repeat
            if (player.clazz == DungeonClass.EMPTY) return@repeat
            val dead = tintDead && player.isDead
            val opener = highlightDoorOpener && player.name == doorOpener
            if (!dead && !opener) return@repeat

            // Same transform as Odin's tile
            val grow = hover.anim.get(0f, 5f, !hover.isHovered)
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
                graphics.roundedFill(localX, localY, localX + width, localY + height, deadTint.rgba, 9)
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
        // NanoVG gives an anti-aliased circle; it draws in window pixels, so scale up from GUI coordinates
        val guiScale = mc.window.guiScale.toFloat()
        // New stratum so the ring is drawn above Odin's tiles and faces
        graphics.nextStratum()
        NVGPIPRenderer.draw(graphics, 0, 0, graphics.guiWidth(), graphics.guiHeight()) {
            NVGRenderer.scale(guiScale, guiScale)
            rings.forEach { NVGRenderer.hollowRect(it.x - it.radius, it.y - it.radius, it.radius * 2, it.radius * 2, it.thickness, doorOpenerColor.rgba, it.radius) }
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
