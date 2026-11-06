package com.anto.antoodin.features.impl.fixes

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import com.odtheking.odin.features.impl.render.PlayerSize

/**
 * Turns off cosmetics other mods give players, so names and sizes match the real player:
 * - Odin's dev names and sizes (downloaded from Odin's dev server). Name replacement regex-scans every string the
 *   font draws (about 3.6% of the render thread in a spark profile), so turning it off also saves that.
 * - Donor names, emblems and sizes from skies-starred's kontext library, bundled in Detexturify. Names and emblems
 *   flip kontext's own settings (the same ones `/kontext name|emblem toggle` change), so kontext skips its text work.
 *
 * @see com.anto.antoodin.mixin.mixins.CustomNameReplacerMixin
 * @see com.anto.antoodin.mixin.mixins.PlayerSizeMixin
 * @see com.anto.antoodin.mixin.mixins.KontextDonatorSizeMixin
 */
object CosmeticDisabler : Module(
    name = "Cosmetic Disabler",
    description = "Turns off custom names, sizes and emblems that other mods give players.",
    category = Skit.FIXES
) {
    private val odinNames by BooleanSetting("Odin Dev Names", true, desc = "Odin's dev custom names. Also stops Odin scanning all on-screen text for them every frame.")
    private val odinSizes by BooleanSetting("Odin Dev Sizes", true, desc = "Odin's dev player sizes. Your own Player Size settings still apply.")
    private val starredNames by BooleanSetting("Starred Donor Names", true, desc = "Donor names from skies-starred's mods (Detexturify). Also stops their on-screen text scanning.")
    private val starredEmblems by BooleanSetting("Starred Donor Emblems", true, desc = "Donor icons next to names from skies-starred's mods.")
    private val starredSizes by BooleanSetting("Starred Donor Sizes", true, desc = "Donor player sizes from skies-starred's mods. Applies to players loaded after the change, so from the next lobby.")

    private val kontextNames = KontextToggle("foo.starred.kontext.core.misc.DonatorTextReplacer")
    private val kontextEmblems = KontextToggle("foo.starred.kontext.core.misc.DonatorEmblem")

    init {
        // Settings have no change listener, so kontext's settings follow ours every tick (a no-op unless changed)
        on<TickEvent.End> { syncKontext() }
    }

    override fun onDisable() {
        super.onDisable()
        syncKontext()
    }

    private fun syncKontext() {
        kontextNames.sync(enabled && starredNames)
        kontextEmblems.sync(enabled && starredEmblems)
    }

    @JvmStatic
    fun disableOdinNames() = enabled && odinNames

    /** True when Odin shouldn't scale this player: everyone but you, so your own Player Size still works. */
    @JvmStatic
    fun skipOdinSize(state: AvatarRenderState): Boolean {
        if (!enabled || !odinSizes) return false
        val profile = state.getData(PlayerSize.GAME_PROFILE_KEY) ?: return false
        return profile.name != mc.player?.gameProfile?.name
    }

    @JvmStatic
    fun disableStarredSizes() = enabled && starredSizes

    /**
     * One of kontext's `enabled` settings, set by reflection because kontext is optional (not a build dependency).
     * Resolved on first use, after kontext has loaded its config. Does nothing when kontext isn't installed.
     */
    private class KontextToggle(private val className: String) {
        private val handle by lazy {
            runCatching {
                val property = Class.forName(className).getField("enabled").get(null)
                val setter = Class.forName("foo.starred.kontext.api.storage.impl.generic.property.base.AbstractStorageProperty")
                    .getMethod("setValue", Any::class.java)
                property to setter
            }.getOrNull()
        }
        private var lastDisabled: Boolean? = null

        fun sync(disable: Boolean) {
            if (disable == lastDisabled) return
            lastDisabled = disable
            val (property, setter) = handle ?: return
            runCatching { setter.invoke(property, !disable) }
        }
    }
}
