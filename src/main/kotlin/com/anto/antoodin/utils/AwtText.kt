package com.anto.antoodin.utils

import com.odtheking.odin.OdinMod.mc
import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier
import java.awt.BasicStroke
import java.awt.Font
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Text drawn with Java AWT into a texture at the exact screen pixel size, so system fonts come out smooth.
 * Minecraft's font atlas samples NEAREST, which makes a scaled TTF font blocky.
 */
object AwtText {
    enum class Shadow { NONE, DROP, OUTLINE }

    private data class Key(val text: String, val font: Font, val px: Int, val color: Int, val shadow: Shadow)
    private class Label(val id: Identifier, val width: Int, val height: Int, var lastUsed: Long = System.currentTimeMillis())

    private val labels = HashMap<Key, Label>()
    private val frc = FontRenderContext(null, true, true)
    private var nextId = 0

    /** Width of [text] in GUI pixels at an em size of [size] GUI pixels. */
    fun width(text: String, font: Font, size: Float): Float = font.deriveFont(size).getStringBounds(text, frc).width.toFloat()

    /** Draws [text] centered on (x, y) with an em size of [size] GUI pixels. The pose must not be rotated. */
    fun draw(g: GuiGraphicsExtractor, text: String, font: Font, size: Float, x: Float, y: Float, color: Int, shadow: Shadow) {
        val pose = g.pose()
        pose.pushMatrix()
        pose.translate(x, y)
        val guiScale = mc.window.guiScale
        val pixels = pose.m00() * guiScale // screen pixels per unit here
        val label = labels.getOrPut(Key(text, font, (size * pixels).roundToInt(), color, shadow)) { render(text, font, size * pixels, color, shadow) }
        label.lastUsed = System.currentTimeMillis()

        pose.scale(1f / pixels)
        // Snap the top left corner to a whole screen pixel so the 1:1 texture stays sharp
        val left = pose.m20() * guiScale - label.width / 2
        val top = pose.m21() * guiScale - label.height / 2
        pose.translate(left.roundToInt() - left - label.width / 2, top.roundToInt() - top - label.height / 2)
        g.blit(RenderPipelines.GUI_TEXTURED, label.id, 0, 0, 0f, 0f, label.width, label.height, label.width, label.height)
        pose.popMatrix()
    }

    /** Frees textures not drawn for a second. Safe to call any number of times per frame. */
    fun sweep() {
        val cutoff = System.currentTimeMillis() - 1000
        labels.values.removeIf { label -> (label.lastUsed < cutoff).also { if (it) mc.textureManager.release(label.id) } }
    }

    private fun render(text: String, base: Font, px: Float, color: Int, shadow: Shadow): Label {
        val font = base.deriveFont(max(px, 1f))
        val layout = TextLayout(text.ifEmpty { " " }, font, frc)
        val offset = px * 0.1f
        val pad = ceil(offset * 2).toInt() + 1
        val width = ceil(layout.advance).toInt() + pad * 2
        val height = ceil(layout.ascent + layout.descent).toInt() + pad * 2

        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        val glyphs = layout.getOutline(AffineTransform.getTranslateInstance(pad.toDouble(), pad + layout.ascent.toDouble()))
        when (shadow) {
            Shadow.DROP -> {
                // Same color at a quarter brightness, offset down-right
                g.color = java.awt.Color((color shr 16 and 255) / 4, (color shr 8 and 255) / 4, (color and 255) / 4, color ushr 24)
                g.fill(AffineTransform.getTranslateInstance(offset.toDouble(), offset.toDouble()).createTransformedShape(glyphs))
            }
            Shadow.OUTLINE -> {
                g.color = java.awt.Color.BLACK
                g.stroke = BasicStroke(max(offset * 2, 1f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                g.draw(glyphs)
            }
            Shadow.NONE -> {}
        }
        g.color = java.awt.Color(color, true)
        g.fill(glyphs)
        g.dispose()

        val native = NativeImage(width, height, true)
        for (y in 0 until height) for (x in 0 until width) native.setPixel(x, y, image.getRGB(x, y))
        val id = Identifier.fromNamespaceAndPath("antoodin", "awt_text/${nextId++}")
        mc.textureManager.register(id, DynamicTexture({ "AntoOdin text" }, native))
        return Label(id, width, height)
    }
}
