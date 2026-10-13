// Implementation based on skies-starred OdinClient (Copyright (c) 2026, Starred, BSD 3-Clause)
package com.anto.antoodin.features.impl.odinclient

import com.anto.antoodin.events.TickStartEvent
import com.anto.antoodin.mixin.accessors.KeyMappingAccessor
import com.anto.antoodin.utils.Skit
import com.anto.antoodin.utils.legacyText
import com.anto.antoodin.utils.leftClick
import com.anto.antoodin.utils.rightClick
import com.mojang.blaze3d.platform.InputConstants
import com.odtheking.odin.clickgui.settings.RenderableSetting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.KeybindSetting
import com.odtheking.odin.clickgui.settings.impl.MapSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.features.ModuleManager
import com.odtheking.odin.utils.getChatBreak
import com.odtheking.odin.utils.itemId
import com.odtheking.odin.utils.itemUUID
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.noControlCodes
import net.minecraft.client.KeyMapping
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.BlockHitResult
import org.lwjgl.glfw.GLFW

object AutoClicker : Module(
    name = "Auto Clicker",
    description = "Auto clicker with options for left-click, right-click, or both.",
    category = Skit.ODINCLIENT
) {
    private val whiteListOnly by BooleanSetting("Whitelist only", false, desc = "Only click when holding a whitelisted item, whitelist using \"/ao ac add [left|right]\" while holding the item.")
    private val allowBreaking by BooleanSetting("Allow breaking blocks", false, desc = "Allows you to break blocks when auto clicking.")
    private val blockBreaker by BooleanSetting("Block dungeon breaker", true, desc = "Prevents auto clicker from working with Dungeon Breaker.")
    private val terminatorOnly by BooleanSetting("Terminator Only", true, desc = "Only click when the terminator and right click are held.")
    private val cps by NumberSetting("Clicks Per Second", 5.0f, 3.0..20.0, .5, desc = "The amount of clicks per second to perform.").withDependency { terminatorOnly }

    private val enableLeftClick by BooleanSetting("Enable Left Click", true, desc = "Enable auto-clicking for left-click.").withDependency { !terminatorOnly }
    private val enableRightClick by BooleanSetting("Enable Right Click", true, desc = "Enable auto-clicking for right-click.").withDependency { !terminatorOnly }
    private val leftCps by NumberSetting("Left Clicks Per Second", 5.0f, 3.0..20.0, .5, desc = "The amount of left clicks per second to perform.").withDependency { !terminatorOnly }
    private val rightCps by NumberSetting("Right Clicks Per Second", 5.0f, 3.0..20.0, .5, desc = "The amount of right clicks per second to perform.").withDependency { !terminatorOnly }
    private val leftClickKeybind by KeybindSetting("Left Click", GLFW.GLFW_KEY_UNKNOWN, desc = "The keybind to hold for the auto clicker to click left click.").withDependency { !terminatorOnly }
    private val rightClickKeybind by KeybindSetting("Right Click", GLFW.GLFW_KEY_UNKNOWN, desc = "The keybind to hold for the auto clicker to click right click.").withDependency { !terminatorOnly }

    // Item key (uuid, else skyblock id, else name; an empty hand is "Air") -> colored item name, blank until the item is seen
    private val left by MapSetting("Left Whitelist", mutableMapOf<String, String>())
    private val right by MapSetting("Right Whitelist", mutableMapOf<String, String>())

    private var nlc = .0
    private var nrc = .0

    init {
        on<TickStartEvent> {
            if (mc.gui.screen() != null) return@on
            val player = mc.player ?: return@on
            if (player.isUsingItem) return@on
            if (mc.gameMode?.isDestroying ?: false) return@on
            val now = System.currentTimeMillis()

            if (terminatorOnly) {
                if (player.mainHandItem.itemId != "TERMINATOR" || !mc.options.keyUse.isDown) return@on
                if (now < nrc) return@on

                nrc = now + ((1000 / cps) + ((Math.random() - .5) * 60.0))
                leftClick()
                return@on
            }

            if (blockBreaker && player.mainHandItem.itemId == "DUNGEONBREAKER") return@on

            val held = key(player.mainHandItem)
            val a = !whiteListOnly || held in left
            val b = !whiteListOnly || held in right
            if (!a && !b) return@on
            refreshName(player.mainHandItem)

            val level = mc.level ?: return@on
            val hit = mc.hitResult as? BlockHitResult

            val lc = a && enableLeftClick && isHeld(leftClickKeybind)
            val rc = b && enableRightClick && isHeld(rightClickKeybind)

            if (hit != null && !level.getBlockState(hit.blockPos).isAir && lc && allowBreaking) {
                KeyMapping.set((mc.options.keyAttack as KeyMappingAccessor).boundKey, true)
                return@on
            }

            if (lc && now >= nlc) {
                nlc = now + ((1000 / leftCps) + ((Math.random() - .5) * 60.0))
                leftClick()
            }

            if (rc && now >= nrc) {
                nrc = now + ((1000 / rightCps) + ((Math.random() - .5) * 60.0))
                rightClick()
            }
        }
    }

    // Mouse binds aren't keyboard keys, so InputConstants.isKeyDown can't check them
    private fun isHeld(key: InputConstants.Key): Boolean = when {
        key == InputConstants.UNKNOWN -> false
        key.type == InputConstants.Type.MOUSE -> GLFW.glfwGetMouseButton(mc.window.handle(), key.value) == GLFW.GLFW_PRESS
        else -> InputConstants.isKeyDown(mc.window, key.value)
    }

    private fun key(stack: ItemStack): String =
        stack.itemUUID.ifEmpty { stack.itemId.ifEmpty { stack.hoverName.string } }

    private fun name(stack: ItemStack): String = stack.hoverName.legacyText()

    // Records the colored name whenever it differs (imported entries, renames, reforges), saving only on a change
    private fun refreshName(stack: ItemStack) {
        val key = key(stack)
        val name = name(stack)
        var changed = false
        for (list in listOf(left, right)) if (list[key]?.let { it != name } == true) {
            list[key] = name
            changed = true
        }
        if (changed) ModuleManager.saveConfigurations()
    }

    private fun list(side: String) = if (side == "left") left else right

    fun add(side: String) {
        val stack = mc.player?.mainHandItem ?: return
        val key = key(stack)
        val list = list(side)
        if (key in list) return modMessage("\"${name(stack)}§r\" is already in the $side whitelist.")

        list[key] = name(stack)
        ModuleManager.saveConfigurations()
        modMessage("Added \"${name(stack)}§r\" to the $side whitelist.")
    }

    fun remove(side: String) {
        val stack = mc.player?.mainHandItem ?: return
        val list = list(side)
        if (list.remove(key(stack)) == null) return modMessage("\"${name(stack)}§r\" isn't in the $side whitelist.")

        ModuleManager.saveConfigurations()
        modMessage("Removed \"${name(stack)}§r\" from the $side whitelist.")
    }

    fun clear(side: String) {
        if (side != "right") left.clear()
        if (side != "left") right.clear()
        ModuleManager.saveConfigurations()
        modMessage(if (side == "all") "All whitelists cleared." else "${side.replaceFirstChar { it.uppercase() }} whitelist cleared.")
    }

    private val uuidRegex = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

    fun list() {
        mc.player?.inventory?.forEach(::refreshName)

        // One row per item; the uuid or SkyBlock id only shows on hover, vanilla items (keyed by name) have none
        fun send(side: String, list: Map<String, String>) {
            modMessage("§b$side:${if (list.isEmpty()) " §7empty" else ""}")
            for ((key, name) in list) {
                val row = Component.literal("§8 - §r${name.ifEmpty { key }}")
                val hover = when {
                    uuidRegex.matches(key) -> "§7UUID: §f$key"
                    name.isNotEmpty() && key != name.noControlCodes -> "§7SkyBlock ID: §f$key"
                    else -> null
                }
                hover?.let { text -> row.withStyle { it.withHoverEvent(HoverEvent.ShowText(Component.literal(text))) } }
                modMessage(row, prefix = "")
            }
        }

        modMessage("Autoclicker whitelist:")
        send("Left", left)
        modMessage(getChatBreak().drop(20))
        send("Right", right)
    }
}
