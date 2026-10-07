// Ported from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.noamm

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.RenderableSetting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.RenderExtractEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.features.Module
import com.odtheking.odin.features.impl.boss.WitherDragonState
import com.odtheking.odin.features.impl.boss.WitherDragons
import com.odtheking.odin.features.impl.boss.WitherDragonsEnum
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.render.drawLine
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Reads Odin's dragon state, so Odin's Wither Dragons module has to be enabled. */
object DragonExtras : Module(
    name = "Dragon Extras",
    description = "M7 dragon aim marker for arrow stacks and an arrows-hit count. Needs Odin's Wither Dragons.",
    category = Skit.NOAMM
) {
    private val aimMarker by BooleanSetting("Aim Marker", true, desc = "Shows where to aim at a spawning dragon, accounting for arrow drop.")
    private val markerColor by ColorSetting("Marker Color", Colors.MINECRAFT_AQUA, desc = "Color of the aim marker.").withDependency { aimMarker }
    private val markerSize by NumberSetting("Marker Size", 2f, 0.1..5.0, 0.1f, desc = "Size of the aim marker.").withDependency { aimMarker }
    private val markerThickness by NumberSetting("Marker Thickness", 3f, 1.0..10.0, 0.5f, desc = "Line thickness of the aim marker.").withDependency { aimMarker }
    private val arrowsHit by BooleanSetting("Arrows Hit", true, desc = "Says how many of your arrows hit the priority dragon in its kill window when it dies.")

    // Where to stack arrows for dragons that don't fly straight up from their spawn
    private val fixedStackPositions = mapOf(
        WitherDragonsEnum.Green to Vec3(27.0, 14.0, 90.0),
        WitherDragonsEnum.Red to Vec3(28.0, 14.0, 58.0),
        WitherDragonsEnum.Blue to Vec3(84.0, 14.0, 97.0)
    )

    // Server ticks after spawning in which hits count towards the kill
    private val killWindowTicks = mapOf(
        WitherDragonsEnum.Red to 50, WitherDragonsEnum.Orange to 62, WitherDragonsEnum.Green to 52,
        WitherDragonsEnum.Blue to 47, WitherDragonsEnum.Purple to 38
    )

    private const val RING_POINTS = 32

    // Written on the main thread, read by the sound handler on the netty thread
    @Volatile private var tracked: WitherDragonsEnum? = null
    @Volatile private var hits = 0

    init {
        on<RenderExtractEvent> {
            if (!aimMarker || !WitherDragons.enabled) return@on
            val eye = mc.player?.eyePosition ?: return@on
            WitherDragonsEnum.entries.forEach { dragon ->
                if (dragon.state != WitherDragonState.SPAWNING) return@forEach
                val target = (fixedStackPositions[dragon] ?: Vec3.atLowerCornerOf(dragon.spawnPos)).add(0.5, 3.5, 0.5)
                val aim = leadPosition(eye, target) ?: return@forEach
                val radius = markerSize * sqrt(eye.distanceTo(target) / 50.0).coerceAtLeast(0.5)
                drawLine(ring(eye, aim, radius), markerColor, false, markerThickness)
            }
        }

        // The priority dragon is cleared when it dies, so remember which one was being killed
        on<TickEvent.End> {
            val priority = WitherDragons.priorityDragon
            if (priority != null && priority.state == WitherDragonState.ALIVE && tracked != priority) {
                tracked = priority
                hits = 0
            }
            val dragon = tracked ?: return@on
            if (dragon.state != WitherDragonState.DEAD) return@on
            tracked = null
            if (arrowsHit) modMessage("§${dragon.colorCode}${dragon.name} §farrows hit: §e$hits")
        }

        // The server plays this sound to the shooter for each arrow that hits
        onReceive<ClientboundSoundPacket> {
            if (sound.value() != SoundEvents.ARROW_HIT_PLAYER) return@onReceive
            val dragon = tracked ?: return@onReceive
            val window = killWindowTicks[dragon] ?: return@onReceive
            if (dragon.state == WitherDragonState.ALIVE && WitherDragons.currentTick - dragon.spawnedTime <= window) hits++
        }
    }

    // Steps an arrow (speed 3, drag 0.99, gravity 0.05) until it has travelled as far as the target, then aims above it by the drop
    private fun leadPosition(eye: Vec3, target: Vec3): Vec3? {
        val distanceSq = target.distanceToSqr(eye)
        var travelled = 0.0
        var speed = 3.0
        var yVelocity = 0.0
        var drop = 0.0
        repeat(160) {
            travelled += speed
            speed *= 0.99
            drop += yVelocity
            yVelocity = (yVelocity - 0.05) * 0.99
            if (travelled * travelled >= distanceSq) return target.subtract(0.0, drop, 0.0)
        }
        return null
    }

    // Circle facing the player
    private fun ring(eye: Vec3, center: Vec3, radius: Double): List<Vec3> {
        val forward = center.subtract(eye).normalize()
        val right = forward.cross(Vec3(0.0, 1.0, 0.0)).normalize()
        val up = right.cross(forward).normalize()
        return (0..RING_POINTS).map { i ->
            val angle = 2 * Math.PI * i / RING_POINTS
            center.add(right.scale(cos(angle) * radius)).add(up.scale(sin(angle) * radius))
        }
    }
}
