package com.anto.antoodin

import com.odtheking.odin.config.ModuleConfig
import com.odtheking.odin.events.core.EventBus
import com.odtheking.odin.features.ModuleManager
import com.odtheking.odin.utils.skyblock.Split
import com.odtheking.odin.utils.skyblock.floor7SplitGroup
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
            MonoAudio, SoundManager, ExplosiveShot, ChatFilter, LeapCounter, MaxorsCrystals, ItemTooltip, DamageSplash,
            LavaToWater, FreezeDisplay, CustomScoreboard, LeapMenuExtras, DragonExtras, CameraTweaks, RejoinTimer, PartyFinderExtras,
            WaypointPlacer, DoorHighlight, AutoClicker, Trajectories, Highlight, SpiritBear, LividSolver, EscrowFix, FuckDiorite,
            SecretHitboxes, AutoGFS, Ghosts, CameraHelper, HighlightTrash, ChatRules,
            ItemCountFix, DisconnectFix, DoubleUseFix)

        JellybeanHider.registerModelHook()

        // Hypixel renamed "The Wither King", so Odin 0.3.6's Cleared split never fires
        // ponytail: delete once the pinned Odin includes its fix (odtheking/Odin 6a08fc5)
        floor7SplitGroup.replaceAll { if (it.name == "§4Cleared") Split(Regex("""^\[BOSS] (The )?Wither King: You\.\.\. again\?$"""), it.name) else it }

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ -> aoCommand.register(dispatcher) }
    }
}
