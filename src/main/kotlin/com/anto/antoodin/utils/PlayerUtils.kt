package com.anto.antoodin.utils

// Implementation based on skies-starred OdinClient
import com.odtheking.odin.OdinMod.mc
import net.minecraft.client.KeyMapping
import net.minecraft.world.inventory.ContainerInput
import com.anto.antoodin.mixin.accessors.KeyMappingAccessor

fun rightClick() {
    val options = mc.options ?: return
    val key = (options.keyUse as KeyMappingAccessor).boundKey
    KeyMapping.set(key, true)
    KeyMapping.click(key)
    KeyMapping.set(key, false)
}

fun leftClick() {
    val options = mc.options ?: return
    val key = (options.keyAttack as KeyMappingAccessor).boundKey
    KeyMapping.set(key, true)
    KeyMapping.click(key)
    KeyMapping.set(key, false)
}

fun guiClick(id: Int, index: Int, button: Int = 0, clickType: ContainerInput = ContainerInput.PICKUP) {
    val player = mc.player ?: return
    //~ if >= 26.1 'handleInventoryMouseClick' -> 'handleContainerInput'
    mc.gameMode?.handleContainerInput(id, index, button, clickType, player)
}