package com.anto.antoodin

import com.anto.antoodin.features.impl.nether.KuudraAutoGFS
import com.odtheking.odin.config.ModuleConfig
import com.odtheking.odin.events.core.EventBus
import com.odtheking.odin.features.ModuleManager
import com.anto.antoodin.features.impl.skyblock.WardrobeAddon
import com.anto.antoodin.features.impl.skyblock.QueueWardrobe
import net.fabricmc.api.ClientModInitializer

object AntoOdin : ClientModInitializer {

    override fun onInitializeClient() {
        listOf(this).forEach { EventBus.subscribe(it) }

        ModuleManager.registerModules(ModuleConfig("AntoOdin.json"), WardrobeAddon, QueueWardrobe, KuudraAutoGFS)
    }
}
