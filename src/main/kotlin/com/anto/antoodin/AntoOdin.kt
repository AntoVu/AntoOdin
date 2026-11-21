package com.anto.antoodin

import com.odtheking.odin.config.ModuleConfig
import com.odtheking.odin.events.core.EventBus
import com.odtheking.odin.features.ModuleManager
//? if < 26.3 {
import com.odtheking.odin.utils.skyblock.Split
import com.odtheking.odin.utils.skyblock.floor7SplitGroup
//?}
import com.anto.antoodin.features.impl.anto.*
import com.anto.antoodin.features.impl.noamm.*
import com.anto.antoodin.features.impl.odinclient.*
import com.anto.antoodin.features.impl.fixes.*
import com.anto.antoodin.events.dispatcher.FabricEventDispatcher
import com.anto.antoodin.commands.aoCommand
import com.anto.antoodin.features.DwpPresets
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback

object AntoOdin : ClientModInitializer {

    override fun onInitializeClient() {
        listOf(this, FabricEventDispatcher, DwpPresets).forEach { EventBus.subscribe(it) }

        ModuleManager.registerModules(ModuleConfig("AntoOdin.json"), WardrobeAddon, QueueLoadout, CPSDisplay, DianaAutoWarp, MinionHelper, LoadoutAddon, ExperimentAddon, DungeonSplits, AuctionPriceInput,
            HiddenMobs, GateHighlight, SnappyTappy, ExplosiveShot, MaxorsCrystals, ItemTooltip, DamageSplash, LavaToWater, NoAotvPaths,
            CustomScoreboard, DragonExtras, RejoinTimer, PartyFinderExtras, WaypointPlacer, DoorHighlight, AutoClicker, Trajectories, Highlight,
            SpiritBear, LividSolver, SecretHitboxes, Ghosts, HighlightTrash, UnfocusedTweaks, LevelColors, CalendarDate, DeathBowSwap,
            BloodRushWaypoints, ExperimentSolver, CorpseHighlight, InventoryButtons, ItemCustomizer, AutoTip, CleanMap, MimicChestHighlight, FastLeap,
            CosmeticDisabler, Fixes, Sounds, AutoGfs, Relics, ClearBlocks, Camera, Garden, LeapExtras, Alerts, Chat, Guards)

        JellybeanHider.registerModelHook()

        //? if < 26.3 {
        // Hypixel renamed "The Wither King", so Odin 0.3.6's Cleared split never fires (fixed in Odin 0.3.7)
        floor7SplitGroup.replaceAll { if (it.name == "§4Cleared") Split(Regex("""^\[BOSS] (The )?Wither King: You\.\.\. again\?$"""), it.name) else it }
        //?}

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ -> aoCommand.register(dispatcher) }
    }
}
