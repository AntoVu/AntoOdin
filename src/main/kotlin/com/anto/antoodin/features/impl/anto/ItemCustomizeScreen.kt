// Feature idea from NotEnoughUpdates (LGPL-3.0). Written from scratch for AntoOdin, no NEU code is used.
package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.features.impl.anto.ItemCustomizer.Glint
import com.anto.antoodin.utils.ItemIcons
import com.odtheking.odin.OdinMod.mc
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

/** Edits one item's looks. [stack] is the held stack itself, so the preview goes through the same restyle as the real item. */
class ItemCustomizeScreen(private val stack: ItemStack, private val uuid: String) : Screen(Component.literal("Item Customizer")) {
    private val custom = ItemCustomizer.customs.getOrPut(uuid) { ItemCustomizer.Custom() }
    private val realName = stack.hoverName

    override fun init() {
        super.init()
        val list = RowList(width, height - 96, 92, 360)
        addRenderableWidget(list)

        val model = textBox(custom.model, "Any item id (diamond_sword, minecraft:chest) or skull:<texture hash>. Leave blank to keep the real model.") {}
        model.setResponder { custom.model = it; edited(); model.setTextColor(validColor(it.isBlank(), !ItemIcons.parse(it).isEmpty)) }
        model.setTextColor(validColor(custom.model.isBlank(), !ItemIcons.parse(custom.model).isEmpty))

        val dye = textBox(custom.dye, "Hex color like FF8800. Only items with a dyeable model (leather armor) change color.") {}
        dye.setResponder { custom.dye = it; edited(); dye.setTextColor(validColor(it.isBlank(), ItemCustomizer.parseHex(it) != null)) }
        dye.setTextColor(validColor(custom.dye.isBlank(), ItemCustomizer.parseHex(custom.dye) != null))

        val name = textBox(custom.name, "Shown instead of the real name. Use & for color codes. Leave blank to keep the real name.") { custom.name = it; edited() }

        list.show(listOf(
            Row("Name", null, name),
            Row("Enchant Glint", "Force the glint on or off, or keep the item's own.", null, listOf(
                CycleButton.builder<Glint>({ Component.literal(it.name) }, custom.glint)
                    .withValues(Glint.entries)
                    .displayOnlyValue()
                    .create(0, 0, 80, 20, Component.literal("Enchant Glint")) { _, value -> custom.glint = value; edited() }
            )),
            Row("Model", null, model, listOf(Button.builder(Component.literal("Hotbar 1")) {
                model.value = ItemIcons.textOf(mc.player?.inventory?.getItem(0) ?: return@builder)
            }.width(56).tooltip(Tooltip.create(Component.literal("Copies the model (or head skin) of the item in your first hotbar slot."))).build())),
            Row("Dye Color", null, dye),
            Row(null, null, Button.builder(Component.literal("§cReset All")) {
                ItemCustomizer.customs[uuid] = ItemCustomizer.Custom()
                ItemCustomizer.changed()
                mc.gui.setScreen(ItemCustomizeScreen(stack, uuid))
            }.build())
        ))
    }

    private fun validColor(blank: Boolean, valid: Boolean) = if (blank) WHITE else if (valid) GREEN else RED

    private fun edited() = ItemCustomizer.changed()

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, delta)
        guiGraphics.centeredText(font, "§lItem Customizer §r§7(only you see this)", width / 2, 8, -1)

        // Preview at 3x, then the name as it will show
        guiGraphics.pose().pushMatrix()
        guiGraphics.pose().translate(width / 2f - 24f, 22f)
        guiGraphics.pose().scale(3f, 3f)
        guiGraphics.item(stack, 0, 0)
        guiGraphics.pose().popMatrix()
        guiGraphics.centeredText(font, ItemCustomizer.customName(stack) ?: realName, width / 2, 76, -1)
        if (!ItemCustomizer.enabled) guiGraphics.centeredText(font, "§cItem Customizer is disabled in the ClickGUI", width / 2, height - 14, -1)
    }

    override fun onClose() {
        ItemCustomizer.save()
        mc.gui.setScreen(null)
    }

    override fun isPauseScreen(): Boolean = false
}
