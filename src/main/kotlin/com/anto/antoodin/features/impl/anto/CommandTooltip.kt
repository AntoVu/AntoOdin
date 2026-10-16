// Feature idea from NoFrills (GPL-3.0). Written from scratch for AntoOdin, no NoFrills code is used.
package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.features.Module
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style

/** @see com.anto.antoodin.mixin.mixins.GuiGraphicsExtractorMixin */
object CommandTooltip : Module(
    name = "Command Tooltip",
    description = "Shows the command a chat message runs when clicked, in its hover tooltip.",
    category = Skit.ANTO
) {
    @JvmStatic
    fun withCommand(tooltip: Component, style: Style): Component {
        val command = (style.clickEvent as? ClickEvent.RunCommand)?.command ?: return tooltip
        if (!enabled) return tooltip
        return tooltip.copy().append("\n\n§7Command: §f/${command.removePrefix("/")}")
    }
}
