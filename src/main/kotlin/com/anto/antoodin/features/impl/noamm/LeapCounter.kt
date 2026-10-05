// Ported from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.noamm

import com.anto.antoodin.utils.P3Section
import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.StringSetting
import com.odtheking.odin.events.EntityEvent
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.createSoundSettings
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.playSoundSettings
import com.odtheking.odin.utils.render.textDim
import com.odtheking.odin.utils.setTitle
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

/**
 * Counts teammates arriving at the F7 P3 spot you are standing on. Like Noamm, a teammate counts once their
 * position is in the spot's section, whether they leaped or walked.
 */
object LeapCounter : Module(
    name = "Leap Counter",
    description = "Shows how many players have leaped to you at the F7 P3 spots.",
    category = Skit.NOAMM
) {
    private val alertComplete by BooleanSetting("Alert Complete", true, desc = "Shows a title and plays a sound when everyone has arrived.")
    private val completeText by StringSetting("Complete Text", "§aEveryone Leaped!", desc = "Title shown when everyone has arrived.").withDependency { alertComplete }
    private val completeSound = createSoundSettings("Complete Sound", "entity.experience_orb.pickup") { alertComplete }

    private class Spot(val name: String, val box: AABB, val maxCount: Int, val counts: (Vec3) -> Boolean) {
        val arrived = HashSet<String>()
        var completed = false
    }

    private val coreBox = AABB(51.0, 114.0, 54.0, 58.0, 117.0, 49.0)
    private val inCoreArea = AABB(68.0, 106.0, 54.0, 42.0, 155.0, 119.0)
    private val spots = listOf(
        Spot("SS", AABB(106.0, 119.0, 92.0, 109.0, 121.0, 96.0), 3) { P3Section.of(it) == 1 },
        Spot("EE2", AABB(57.0, 108.0, 130.0, 59.0, 110.0, 132.0), 4) { P3Section.of(it) == 2 },
        Spot("High EE2", AABB(57.0, 132.0, 138.0, 62.0, 133.0, 140.0), 4) { P3Section.of(it) == 2 },
        Spot("EE3", AABB(1.0, 108.0, 101.0, 3.0, 110.0, 107.0), 3) { P3Section.of(it) == 3 },
        Spot("Core", coreBox, 4) { P3Section.of(it) == 4 },
        Spot("In Core", coreBox.move(0.0, 0.0, 6.0), 4) { inCoreArea.contains(it) },
        // P5 starts below y 45
        Spot("Relic", AABB(51.5, 3.0, 73.5, 57.5, 8.0, 79.5), 4) { it.y <= 45 }
    )

    private val hud by HUD(name, "Players that have arrived at your P3 spot.", false) { example ->
        val spot = if (example) spots[2] else currentSpot() ?: return@HUD 0 to 0
        val max = if (example) spot.maxCount else maxCount(spot).takeIf { it > 0 } ?: return@HUD 0 to 0
        val count = spot.arrived.size
        val color = if (max - count <= 1) "§9" else "§4"
        textDim("$color$count§9/$max Players Leaped", 0, 0)
    }

    init {
        on<EntityEvent.Move> { update(entity) }
        on<EntityEvent.Add> { update(entity) }

        on<LevelEvent.Load> {
            spots.forEach {
                it.arrived.clear()
                it.completed = false
            }
        }
    }

    private fun inF7Boss() = DungeonUtils.inBoss && DungeonUtils.isFloor(7)

    private fun currentSpot(): Spot? {
        if (!inF7Boss()) return null
        val pos = mc.player?.position() ?: return null
        return spots.firstOrNull { !it.completed && it.box.contains(pos) }
    }

    private fun maxCount(spot: Spot) = minOf(DungeonUtils.dungeonTeammatesNoSelf.size, spot.maxCount)

    private fun update(entity: Entity) {
        val spot = currentSpot() ?: return
        val name = entity.name.string
        if (name in spot.arrived || DungeonUtils.dungeonTeammatesNoSelf.none { it.name == name }) return
        // EntityEvent.Move can carry a zero position, the entity itself is already moved
        if (!spot.counts(entity.position())) return
        spot.arrived.add(name)

        val max = maxCount(spot)
        if (spot.arrived.size != max) return
        if (alertComplete) {
            setTitle(completeText)
            playSoundSettings(completeSound())
        }
        schedule(20) { spot.completed = true }
    }
}
