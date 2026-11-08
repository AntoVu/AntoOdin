// Taken from OdinFabric (Copyright (c) 2023-2025, odtheking, BSD 3-Clause)
// Implementation based on skies-starred OdinClient (Copyright (c) 2026, Starred, BSD 3-Clause)
package com.anto.antoodin.features.impl.odinclient

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.RenderableSetting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.MessageEvent
import com.odtheking.odin.events.RenderExtractEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Color.Companion.multiplyAlpha
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.addVec
import com.odtheking.odin.utils.itemId
import com.odtheking.odin.utils.render.drawFilledBox
import com.odtheking.odin.utils.render.drawLine
import com.odtheking.odin.utils.render.drawWireFrameBox
import com.odtheking.odin.utils.renderBoundingBox
import com.odtheking.odin.utils.renderPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.projectile.arrow.AbstractArrow
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.EnderpearlItem
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object Trajectories : Module(
    name = "Trajectories",
    description = "Shows the trajectories of arrows, snowballs, etc.",
    category = Skit.ODINCLIENT
) {
    private val bows by BooleanSetting("Bows", true, desc = "Render trajectories of bow arrows.")
    private val rapidFire by BooleanSetting("Rapid Fire", true, desc = "Render the trajectory of the Archer ultimate's arrows while it's active, whatever you're holding.")
    private val rapidSeconds by NumberSetting("Rapid Fire Duration", 7, 5..10, 1, unit = "s", desc = "5s plus 1s per 10 Archer levels (Archer 30 = 8s).")
        .withDependency { rapidFire }
    private val pearls by BooleanSetting("Pearls", true, desc = "Render trajectories of ender pearls.")
    private val plane by BooleanSetting("Show Plane", false, desc = "Shows a flat square rotated relative to the predicted block that will be hit.")
    private val boxes by BooleanSetting("Show Boxes", true, desc = "Shows boxes displaying where arrows or pearls will hit.")
    private val entities by BooleanSetting("Show Entities", true, desc = "Show boxes highlighting entities which the arrows will hit.")
    private val lines by BooleanSetting("Show Lines", true, desc = "Shows the trajectory as a line.")
    private val range by NumberSetting("Solver Range", 30, 1..120, 1, desc = "How many ticks are simulated.")
    private val width by NumberSetting("Line Width", 1f, 0.1..5.0, 0.1f, desc = "The width of the line.")
    private val planeSize by NumberSetting("Plane Size", 2f, 0.1..5.0, 0.1f, desc = "The size of the plane.").withDependency { plane }
    private val boxSize by NumberSetting("Box Size", 0.5f, 0.5..3.0, 0.1f, desc = "The size of the box.").withDependency { boxes }
    private val bowColor by ColorSetting("Bow Color", Colors.MINECRAFT_DARK_AQUA, true, desc = "The color of bow and shortbow trajectories.")
    private val pearlColor by ColorSetting("Pearl Color", Colors.MINECRAFT_DARK_AQUA, true, desc = "The color of ender pearl trajectories.")
    private val rapidColor by ColorSetting("Rapid Fire Color", Colors.MINECRAFT_GOLD, true, desc = "The color of the Rapid Fire trajectory.")
        .withDependency { rapidFire }
    private val depth by BooleanSetting("Depth Check", true, desc = "Whether or not to depth check the trajectory.")

    private val VEC_NULL = emptyList<Vec3>() to null

    private var charge = 0f
    private var lastCharge = 0f
    private val boxRenderQueue = mutableListOf<AABB>()
    private val entityRenderQueue = mutableListOf<Entity>()
    private var pearlImpactPos: AABB? = null
    // Client ticks left of Archer's Rapid Fire. Measured at 7s: an arrow every 4 ticks, the last about 143 ticks after the message
    private var rapidTicks = 0

    init {
        on<MessageEvent.Chat> { if ("Used Rapid Fire!" in message) rapidTicks = rapidSeconds * 20 + 5 }
        on<LevelEvent.Load> { rapidTicks = 0 }

        on<TickEvent.End> {
            if (rapidTicks > 0) rapidTicks--
            val player = mc.player ?: return@on
            lastCharge = charge
            val useCount = player.useItemRemainingTicks
            charge = min((72000 - useCount) / 20f, 1.0f) * 2f
            if ((lastCharge - charge) > 1f) lastCharge = charge
        }

        // Rapid Fire draws alongside a held bow or pearl; each kind clears the hit queues and draws in its own color
        on<RenderExtractEvent> {
            val player = mc.player ?: return@on
            val heldItem = player.mainHandItem

            if (rapidFire && rapidTicks > 0) drawTrajectories(rapidColor, isPearl = false) {
                listOf(calculateTrajectory(0f, isPearl = false, rapid = true))
            }

            if (bows && heldItem.item is BowItem) drawTrajectories(bowColor, isPearl = false) {
                if (heldItem.itemId == "TERMINATOR") listOf(calculateTrajectory(0f, isPearl = false), calculateTrajectory(-5f, isPearl = false), calculateTrajectory(5f, isPearl = false))
                else listOf(calculateTrajectory(0f, isPearl = false, useCharge = true))
            }
            else if (pearls && heldItem.item is EnderpearlItem && !heldItem.displayName.string.contains("Spirit"))
                drawTrajectories(pearlColor, isPearl = true) { listOf(calculateTrajectory(0f, isPearl = true)) }
        }
    }

    private inline fun RenderExtractEvent.drawTrajectories(color: Color, isPearl: Boolean, calculate: () -> List<Pair<List<Vec3>, BlockHitResult?>>) {
        entityRenderQueue.clear()
        boxRenderQueue.clear()
        pearlImpactPos = null
        val results = calculate()
        for ((a, b) in results) {
            if (lines) drawLine(a, color, depth, width)
            if (b != null && plane) drawPlaneCollision(b, color)
        }
        if (boxes) drawCollisionBoxes(isPearl, color)
    }

    // Rapid Fire arrows (fitted from logged flights): launched from the eye at speed 2.0, same drag and gravity as a bow arrow
    private fun calculateTrajectory(yawOffset: Float, isPearl: Boolean, useCharge: Boolean = false, rapid: Boolean = false): Pair<List<Vec3>, BlockHitResult?> {
        val player = mc.player ?: return VEC_NULL
        val level = mc.level ?: return VEC_NULL

        val yaw = Math.toRadians(player.yRot.toDouble())
        val x = -cos(yaw) * 0.16
        val z = -sin(yaw) * 0.16
        var pos = if (rapid) player.renderPos.add(0.0, player.eyeHeight.toDouble(), 0.0) else player.renderPos.add(Vec3(x, player.eyeHeight - 0.1, z))
        var prevPos = pos

        val speed = if (isPearl) 1.5 else if (rapid) 2.0 else pull(useCharge) * 3.0
        var motion = getLook(player.yRot + yawOffset, player.xRot).normalize().scale(speed)

        var hitResult = false
        val lines = mutableListOf<Vec3>()
        var rayTraceHit: BlockHitResult? = null

        repeat(range) {
            if (hitResult) return@repeat
            lines.add(pos)

            if (!isPearl) {
                val scanBox = AABB(prevPos, pos.add(motion)).inflate(1.0)
                val hit = level.getEntities(player, scanBox)
                    .filter { it !is AbstractArrow && it !is ArmorStand }
                    .mapNotNull { entity ->
                        entity.boundingBox.inflate(entity.pickRadius.toDouble())
                            .clip(prevPos, pos.add(motion))
                            .map { entity to it }
                            .orElse(null)
                    }
                    .minByOrNull { (_, hitPos) -> prevPos.distanceToSqr(hitPos) }

                if (hit != null) {
                    val (entity, hitPos) = hit
                    lines.add(hitPos)
                    entityRenderQueue.add(entity)
                    hitResult = true
                    return@repeat
                }
            }

            val blockHit = level.clip(ClipContext(pos, pos.add(motion), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
            if (blockHit.type == HitResult.Type.BLOCK) {
                rayTraceHit = blockHit
                lines.add(blockHit.location)

                if (boxes) {
                    val box = AABB(blockHit.location, blockHit.location).inflate(0.15 * boxSize)
                    if (isPearl) pearlImpactPos = box else boxRenderQueue.add(box)
                }

                hitResult = true
            }

            if (isPearl) {
                motion = Vec3(motion.x * 0.99, (motion.y - 0.03) * 0.99, motion.z * 0.99)
                pos = pos.add(motion)
                return@repeat
            }

            pos = pos.add(motion)
            motion = Vec3(motion.x * 0.99, motion.y * 0.99 - 0.05, motion.z * 0.99)
            prevPos = pos
        }

        return lines to rayTraceHit
    }

    private fun RenderExtractEvent.drawPlaneCollision(hit: BlockHitResult, color: Color) {
        val (vec1, vec2) = when (hit.direction) {
            Direction.DOWN, Direction.UP -> hit.location.addVec(-0.15 * planeSize, -0.02, -0.15 * planeSize) to hit.location.addVec(0.15 * planeSize, 0.02, 0.15 * planeSize)
            Direction.NORTH, Direction.SOUTH -> hit.location.addVec(-0.15 * planeSize, -0.15 * planeSize, -0.02) to hit.location.addVec(0.15 * planeSize, 0.15 * planeSize, 0.02)
            Direction.WEST, Direction.EAST -> hit.location.addVec(-0.02, -0.15 * planeSize, -0.15 * planeSize) to hit.location.addVec(0.02, 0.15 * planeSize, 0.15 * planeSize)
        }

        drawFilledBox(AABB(vec1, vec2), color.multiplyAlpha(0.5f), depth)
    }

    private fun RenderExtractEvent.drawCollisionBoxes(isPearl: Boolean, color: Color) {
        if (isPearl) {
            pearlImpactPos?.let { aabb ->
                drawWireFrameBox(aabb, color, width, depth)
                drawFilledBox(aabb, color.multiplyAlpha(0.3f), depth)
            }
            return
        }

        for (box in boxRenderQueue) {
            drawWireFrameBox(box, color, width, depth)
            drawFilledBox(box, color.multiplyAlpha(0.3f), depth)
        }

        if (!entities) return
        for (entity in entityRenderQueue) {
            val aabb = entity.renderBoundingBox
            drawWireFrameBox(aabb, color, width, depth)
            drawFilledBox(aabb, color.multiplyAlpha(0.3f), depth)
        }
    }

    private fun getLook(yaw: Float, pitch: Float): Vec3 {
        val f2 = -cos(-pitch * 0.017453292) * 1.0
        return Vec3(
            sin(-yaw * 0.017453292 - Math.PI) * f2,
            sin(-pitch * 0.017453292) * 1.0,
            cos(-yaw * 0.017453292 - Math.PI) * f2
        )
    }

    private fun pull(b: Boolean): Float {
        if (!b) return 1f

        val t = lastCharge + (charge - lastCharge) * mc.deltaTracker.getGameTimeDeltaPartialTick(true)
        val f = (t / 2f).coerceIn(0f, 1f)
        return (f * f + f * 2f) / 3f
    }
}
