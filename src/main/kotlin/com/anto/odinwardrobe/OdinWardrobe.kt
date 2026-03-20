package com.anto.odinwardrobe

import com.odtheking.odin.config.ModuleConfig
import com.odtheking.odin.events.core.EventBus
import com.odtheking.odin.features.ModuleManager
import com.anto.odinwardrobe.features.impl.skyblock.WardrobeAddon
import com.anto.odinwardrobe.features.impl.skyblock.QueueWardrobe
import net.fabricmc.api.ClientModInitializer

object OdinWardrobe : ClientModInitializer {

    override fun onInitializeClient() {
        listOf(this).forEach { EventBus.subscribe(it) }

        ModuleManager.registerModules(ModuleConfig("WardrobeAddon.json"), WardrobeAddon, QueueWardrobe)
    }
}
