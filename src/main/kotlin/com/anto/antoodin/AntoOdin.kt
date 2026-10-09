package com.anto.antoodin

import com.odtheking.odin.config.ModuleConfig
import com.odtheking.odin.events.core.EventBus
import com.odtheking.odin.features.ModuleManager
import com.anto.antoodin.features.impl.anto.*
import com.anto.antoodin.features.impl.noamm.*
import com.anto.antoodin.events.dispatcher.FabricEventDispatcher
import com.anto.antoodin.commands.aoCommand
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback

object AntoOdin : ClientModInitializer {

    override fun onInitializeClient() {
        listOf(this, FabricEventDispatcher).forEach { EventBus.subscribe(it) }

        ModuleManager.registerModules(ModuleConfig("AntoOdin.json"), WardrobeAddon, QueueWardrobe, KuudraAutoGFS,
            CPSDisplay, PearlRefill, DianaAutoWarp, MinionHelper, LoadoutAddon, ExperimentAddon, DropGuard, JellybeanHider, AloeHighlight, DungeonSplits,
            AuctionPriceInput, ArchitectDraft, IHateDoors, HiddenMobs, GateHighlight, DoorFix, ModHider, SnappyTappy,
            MonoAudio, SoundManager, ExplosiveShot, ChatFilter, LeapCounter, MaxorsCrystals, ItemTooltip, DamageSplash,
            LavaToWater, FreezeDisplay, CustomScoreboard, LeapMenuExtras, DragonExtras, CameraTweaks, RejoinTimer, PartyFinderExtras,
            WaypointPlacer)

        JellybeanHider.registerModelHook()

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ -> aoCommand.register(dispatcher) }
    }
}
