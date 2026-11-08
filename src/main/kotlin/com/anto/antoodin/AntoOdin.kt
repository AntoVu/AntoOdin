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
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback

object AntoOdin : ClientModInitializer {

    override fun onInitializeClient() {
        listOf(this, FabricEventDispatcher).forEach { EventBus.subscribe(it) }

        ModuleManager.registerModules(ModuleConfig("AntoOdin.json"), WardrobeAddon, QueueLoadout, KuudraAutoGFS,
            CPSDisplay, PearlRefill, DianaAutoWarp, MinionHelper, LoadoutAddon, ExperimentAddon, DropGuard, JellybeanHider, AloeHighlight, DungeonSplits,
            AuctionPriceInput, IHateDoors, HiddenMobs, GateHighlight, DoorFix, ModHider, SnappyTappy,
            MonoAudio, SoundManager, ExplosiveShot, LeapCounter, MaxorsCrystals, ItemTooltip, DamageSplash,
            LavaToWater, FreezeDisplay, RelicSpawnTimer, TimeChanger, CustomScoreboard, LeapMenuExtras, DragonExtras, CameraTweaks, RejoinTimer, PartyFinderExtras,
            WaypointPlacer, DoorHighlight, AutoClicker, Trajectories, Highlight, SpiritBear, LividSolver, EscrowFix, FuckDiorite,
            SecretHitboxes, AutoGFS, Ghosts, CameraHelper, HighlightTrash, ChatRules, UnfocusedTweaks, CommandTooltip, LevelColors, CalendarDate, DupeClassAlert, DeathBowSwap, BloodRushWaypoints, ExperimentSolver, CorpseHighlight, InventoryButtons, ItemCustomizer,
            AutoTip, PaneClickGuard, CleanMap, TerminalClickLimit,
            ItemCountFix, DisconnectFix, DoubleUseFix, CosmeticDisabler)

        JellybeanHider.registerModelHook()

        //? if < 26.3 {
        // Hypixel renamed "The Wither King", so Odin 0.3.6's Cleared split never fires (fixed in Odin 0.3.7)
        floor7SplitGroup.replaceAll { if (it.name == "§4Cleared") Split(Regex("""^\[BOSS] (The )?Wither King: You\.\.\. again\?$"""), it.name) else it }
        //?}

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ -> aoCommand.register(dispatcher) }
    }
}
