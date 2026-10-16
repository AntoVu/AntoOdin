// Feature idea from NoFrills (GPL-3.0). Written from scratch for AntoOdin, no NoFrills code is used.
package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.features.Module
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSequence

/** @see com.anto.antoodin.mixin.mixins.GuiGraphicsExtractorMixin */
object CommandTooltip : Module(
    name = "Command Tooltip",
    description = "Shows the command a chat message runs when clicked, in its hover tooltip.",
    category = Skit.ANTO
) {
    @JvmStatic
    fun withCommand(lines: List<FormattedCharSequence>, style: Style): List<FormattedCharSequence> {
        val command = (style.clickEvent as? ClickEvent.RunCommand)?.command ?: return lines
        if (!enabled) return lines
        // Same wrap width as vanilla's hover text; the leading newline gives a blank spacer line
        val width = maxOf(mc.window.guiScaledWidth / 2, 200)
        return lines + mc.font.split(Component.literal("\n§7Command: §f/${command.removePrefix("/")}"), width)
    }
}
