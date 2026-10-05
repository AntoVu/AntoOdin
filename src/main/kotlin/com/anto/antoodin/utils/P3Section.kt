package com.anto.antoodin.utils

import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.skyblock.dungeon.M7Phases
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

// F7 P3 terminal section (1-4) the player is standing in. Bounds from NoammAddons by Noamm9 (CC0-1.0)
object P3Section {
    private val sections = arrayOf(
        AABB(90.0, 158.0, 123.0, 111.0, 105.0, 32.0),
        AABB(16.0, 158.0, 122.0, 111.0, 105.0, 143.0),
        AABB(19.0, 158.0, 48.0, -3.0, 106.0, 142.0),
        AABB(91.0, 158.0, 50.0, -3.0, 106.0, 30.0)
    )

    fun current(): Int? = mc.player?.position()?.let(::of)

    // Section of any position, while the player is in P3
    fun of(pos: Vec3): Int? {
        if (DungeonUtils.getF7Phase() != M7Phases.P3) return null
        return sections.indexOfFirst { it.contains(pos) }.takeIf { it != -1 }?.plus(1)
    }
}
