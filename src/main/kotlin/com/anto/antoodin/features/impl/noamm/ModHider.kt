// Ported from NoammAddons by Noamm9 (CC0-1.0)
package com.anto.antoodin.features.impl.noamm

import com.anto.antoodin.mixin.accessors.LanguageInvoker
import com.anto.antoodin.mixin.accessors.LanguageManagerAccessor
import com.anto.antoodin.utils.Skit
import com.odtheking.odin.features.Module
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.resources.language.ClientLanguage
import net.minecraft.locale.Language
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentContents
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.contents.KeybindContents
import net.minecraft.network.chat.contents.TranslatableContents
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.repository.PackSource
import net.minecraft.server.packs.resources.MultiPackResourceManager
import java.util.IdentityHashMap
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

/**
 * Servers can open a sign or anvil containing translation keys and read back the resolved text to see which mods
 * are installed. Keys that only a mod's language file knows are left untranslated here.
 *
 * @see com.anto.antoodin.mixin.mixins.AbstractSignEditScreenMixin
 * @see com.anto.antoodin.mixin.mixins.AnvilMenuMixin
 * @see com.anto.antoodin.mixin.mixins.AnvilScreenMixin
 */
object ModHider : Module(
    name = "Mod Hider",
    description = "Stops servers from detecting your mods through translation keys in signs and anvils.",
    category = Skit.NOAMM,
    toggled = true
) {
    private val serverLanguages = IdentityHashMap<ClientPacketListener?, Language>()
    private val vanillaLanguage by lazy { LanguageInvoker.invokeLoadDefault() }

    @JvmStatic
    fun getString(component: Component): String {
        if (!enabled || component !is MutableComponent) return component.string
        return buildString {
            visit(component.contents)?.let(::append)
            for (sibling in component.siblings) append(getString(sibling))
        }
    }

    private fun visit(contents: ComponentContents): String? = when {
        contents is KeybindContents && !canTranslate(contents.name) -> contents.name
        contents is TranslatableContents && !canTranslate(contents.key) -> contents.fallback ?: contents.key
        else -> contents.visit { Optional.of(it) }.getOrNull()
    }

    private fun canTranslate(key: String): Boolean {
        if (mc.currentServer?.resourcePackStatus != ServerData.ServerPackStatus.ENABLED) return vanillaLanguage.has(key)

        val language = serverLanguages[mc.connection] ?: run {
            serverLanguages.clear()
            createServerLanguage().also { serverLanguages[mc.connection] = it }
        }
        return language.has(key)
    }

    // Vanilla plus the server resource pack, without any mod's language files
    private fun createServerLanguage(): Language {
        val allPacks = mc.resourceManager.listPacks().toList()
        val packs = mutableListOf(allPacks.first())
        allPacks.drop(1).filterTo(packs) {
            it.location().source().let { source -> source == PackSource.FEATURE || source == PackSource.WORLD || source == PackSource.SERVER }
        }

        val resourceManager = MultiPackResourceManager(PackType.CLIENT_RESOURCES, packs)
        val selected = mc.languageManager.selected
        val codes = mutableListOf("en_us")
        var bidirectional = LanguageManagerAccessor.getDefaultLanguage().bidirectional()
        if (selected != "en_us") LanguageManagerAccessor.invokeExtractLanguages(resourceManager.listPacks())[selected]?.let {
            codes.add(selected)
            bidirectional = it.bidirectional()
        }

        return ClientLanguage.loadFrom(resourceManager, codes, bidirectional)
    }
}
