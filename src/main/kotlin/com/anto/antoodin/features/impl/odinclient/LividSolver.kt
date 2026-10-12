// Taken from OdinFabric (Copyright (c) 2023-2025, odtheking, BSD 3-Clause)
// Implementation based on skies-starred OdinClient (Copyright (c) 2026, Starred, BSD 3-Clause)
package com.anto.antoodin.features.impl.odinclient

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.events.BlockUpdateEvent
import com.odtheking.odin.events.EntityEvent
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.MessageEvent
import com.odtheking.odin.events.RenderExtractEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.addVec
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.render.drawTracer
import com.odtheking.odin.utils.render.drawWireFrameBox
import com.odtheking.odin.utils.render.textDim
import com.odtheking.odin.utils.renderBoundingBox
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.DyeColor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks

// Replaces Odin's Livid Solver (dropped in ModuleManagerMixin). Detection follows Odin 0.3.6, which has the 340 tick timer
object LividSolver : Module(
    name = "Livid Solver (C)",
    description = "Provides a visual cue for the correct Livid's location in the boss fight.",
    category = Skit.ODINCLIENT
) {
    private val depthCheck by BooleanSetting("Depth Check", false, desc = "Disable to enable ESP")
    private val tracer by BooleanSetting("Tracer", true, desc = "Displays a tracer to the livid")

    private val hud by HUD("Invulnerability Timer", "Shows time remaining on Livid's invulnerability.") { example ->
        if (!example && (!DungeonUtils.inBoss || !DungeonUtils.isFloor(5) || invulnTime <= 0)) return@HUD 0 to 0
        val time = if (example) 340 else invulnTime
        val color = when {
            time > 260 -> "§a"
            time > 130 -> "§e"
            else -> "§c"
        }
        textDim("${color}Livid: ${time}t ", 0, 0)
    }

    private val lividStartRegex = Regex("^\\[BOSS] Livid: Welcome, you've arrived right on time\\. I am Livid, the Master of Shadows\\.$")
    private val woolLocation = BlockPos(5, 108, 43)
    private var currentLivid = Livid.HOCKEY
    private var invulnTime = 0

    init {
        on<MessageEvent.Chat> {
            if (!DungeonUtils.inDungeons || !DungeonUtils.isFloor(5)) return@on
            if (message.matches(lividStartRegex)) invulnTime = 340
        }

        on<BlockUpdateEvent> {
            if (!DungeonUtils.inBoss || !DungeonUtils.isFloor(5) || pos != woolLocation) return@on
            currentLivid = Livid.entries.find { livid -> livid.wool.defaultBlockState() == updated.block.defaultBlockState() } ?: return@on
            modMessage("Found Livid: §${currentLivid.colorCode}${currentLivid.entityName}")
        }

        on<EntityEvent.SetData> {
            if (!DungeonUtils.inBoss || !DungeonUtils.isFloor(5)) return@on
            currentLivid.entity = (entity as? Player)?.takeIf { it.name.string == "${currentLivid.entityName} Livid" } ?: return@on
        }

        on<RenderExtractEvent> {
            if (!DungeonUtils.inBoss || !DungeonUtils.isFloor(5)) return@on
            currentLivid.entity?.let {
                drawWireFrameBox(it.renderBoundingBox, currentLivid.color, 8f, depthCheck)
                if (tracer) drawTracer(it.position().addVec(y = it.eyeHeight), currentLivid.color, depthCheck)
            }
        }

        on<TickEvent.Server> {
            if (DungeonUtils.isFloor(5) && invulnTime > 0) invulnTime--
        }

        on<LevelEvent.Load> {
            currentLivid = Livid.HOCKEY
            currentLivid.entity = null
            invulnTime = 0
        }
    }

    private enum class Livid(val entityName: String, val colorCode: Char, val color: Color, val wool: Block) {
        VENDETTA("Vendetta", 'f', Colors.WHITE, Blocks.WOOL.pick(DyeColor.WHITE)),
        CROSSED("Crossed", 'd', Colors.MINECRAFT_DARK_PURPLE, Blocks.WOOL.pick(DyeColor.MAGENTA)),
        ARCADE("Arcade", 'e', Colors.MINECRAFT_YELLOW, Blocks.WOOL.pick(DyeColor.YELLOW)),
        SMILE("Smile", 'a', Colors.MINECRAFT_GREEN, Blocks.WOOL.pick(DyeColor.LIME)),
        DOCTOR("Doctor", '7', Colors.MINECRAFT_GRAY, Blocks.WOOL.pick(DyeColor.GRAY)),
        PURPLE("Purple", '5', Colors.MINECRAFT_DARK_PURPLE, Blocks.WOOL.pick(DyeColor.PURPLE)),
        SCREAM("Scream", '9', Colors.MINECRAFT_BLUE, Blocks.WOOL.pick(DyeColor.BLUE)),
        FROG("Frog", '2', Colors.MINECRAFT_DARK_GREEN, Blocks.WOOL.pick(DyeColor.GREEN)),
        HOCKEY("Hockey", 'c', Colors.MINECRAFT_RED, Blocks.WOOL.pick(DyeColor.RED));

        var entity: Player? = null
    }
}
