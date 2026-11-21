package com.anto.antoodin.features

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.OdinMod
import com.odtheking.odin.clickgui.settings.RenderableSetting
import com.odtheking.odin.clickgui.settings.Setting
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.DropdownSetting
import com.odtheking.odin.clickgui.settings.impl.HUDSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.EventBus
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import net.minecraft.client.gui.GuiGraphicsExtractor
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A module that holds several [SubFeature]s, each with its own on/off toggle, to keep the ClickGUI short.
 * A sub-feature's settings sit in a dropdown named after it, with an "Enable <name>" toggle first;
 * one without settings is a plain toggle (or goes into a shared dropdown, see [feature]).
 *
 * Odin's `on<>` listeners are keyed by class and only fire while their object is subscribed to the [EventBus],
 * so each sub-feature is subscribed while this module and its toggle are both on.
 */
abstract class MergedModule(name: String, description: String) : Module(name, category = Skit.ANTO, description = description) {
    private val features = mutableListOf<SubFeature>()

    /**
     * Adds a sub-feature. [create] must be the sub-feature's first access, so it registers its settings here.
     * [group] puts its toggle in a shared dropdown; [alwaysOn] gives it no toggle (on whenever this module is).
     */
    protected fun feature(group: DropdownSetting? = null, alwaysOn: Boolean = false, create: () -> SubFeature) {
        val before = settings.keys.toSet()
        SubFeature.building = this
        val f = try { create() } finally { SubFeature.building = null }
        features += f
        if (alwaysOn) return

        // Pull the settings it just registered, to put the dropdown and toggle in front of them
        val own = settings.values.filter { it.name !in before }
        own.forEach { settings.remove(it.name) }
        val dropdown = group ?: if (own.isEmpty()) null else add(DropdownSetting(f.name, false, f.description))
        val toggle = add(BooleanSetting(if (group == null && own.isNotEmpty()) "Enable ${f.name}" else f.name, f.defaultOn, f.description))
        if (dropdown != null) toggle.visibleWith(dropdown)
        own.forEach { s ->
            add(s)
            if (dropdown != null && s is RenderableSetting<*>) s.visibleWith(dropdown)
        }
        f.switch = toggle
    }

    private fun <S : Setting<*>> add(s: S): S {
        check(s.name !in settings) { "Duplicate setting \"${s.name}\" in $name" }
        return registerSetting(s)
    }

    /** A dropdown for grouping several setting-less sub-features. */
    protected fun group(name: String, desc: String) = add(DropdownSetting(name, false, desc))

    init {
        // Settings have no change listener, so toggles are checked every tick
        on<TickEvent.End> { sync() }
    }

    override fun onEnable() {
        super.onEnable()
        sync()
    }

    override fun onDisable() {
        super.onDisable()
        sync()
    }

    internal fun sync() {
        for (f in features) {
            val want = enabled && f.switch?.value != false
            if (want == f.active) continue
            f.active = want
            if (want) {
                EventBus.subscribe(f)
                f.onEnable()
            } else {
                EventBus.unsubscribe(f)
                f.onDisable()
            }
        }
    }

    private fun RenderableSetting<*>.visibleWith(dropdown: DropdownSetting) {
        val own = visibilityDependency
        visibilityDependency = { dropdown.value && own?.invoke() != false }
    }
}

/**
 * One former module inside a [MergedModule]. Declares settings with `by XSetting(...)` and listeners with `on<>`
 * like a [Module]; both end up on the parent. [enabled] is true while the parent and this feature's toggle are on.
 */
abstract class SubFeature(val name: String, val description: String, val defaultOn: Boolean = false) {
    val parent: MergedModule = building ?: error("$name must be created through MergedModule.feature")
    internal var switch: BooleanSetting? = null
    internal var active = false

    val enabled: Boolean get() = active

    protected inline val mc get() = OdinMod.mc

    /** Flips this feature on or off (the whole parent for an always-on feature). Turning it on also enables the parent. */
    fun toggle() {
        val s = switch
        if (s == null) parent.toggle()
        else {
            s.value = !s.value
            if (s.value && !parent.enabled) parent.toggle()
        }
        parent.sync()
    }

    open fun onEnable() {}
    open fun onDisable() {}

    /** Registers a setting on the parent. Names must be unique across the whole parent module. */
    operator fun <T, S : Setting<T>> S.provideDelegate(thisRef: Any, property: KProperty<*>): ReadWriteProperty<Any, T> {
        check(name !in parent.settings) { "Duplicate setting \"$name\" in ${parent.name}" }
        parent.registerSetting(this)
        val setting = this
        return object : ReadWriteProperty<Any, T> {
            override fun getValue(thisRef: Any, property: KProperty<*>): T = setting.value
            override fun setValue(thisRef: Any, property: KProperty<*>, value: T) { setting.value = value }
        }
    }

    /** A HUD on the parent that draws nothing while this feature is off. */
    @Suppress("FunctionName")
    fun HUD(
        name: String,
        desc: String,
        toggleable: Boolean = true,
        x: Int = 10,
        y: Int = 10,
        scale: Float = 1f,
        block: GuiGraphicsExtractor.(example: Boolean) -> Pair<Int, Int>
    ): HUDSetting = parent.HUD(name, desc, toggleable, x, y, scale) { example -> if (!enabled) 0 to 0 else block(example) }

    companion object {
        internal var building: MergedModule? = null
    }
}
