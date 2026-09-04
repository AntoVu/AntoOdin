package com.anto.antoodin.features.impl.anto

// Implementation based on skies-starred OdinClient
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.GuiEvent
import com.odtheking.odin.events.ScreenEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.hasGlint
import com.odtheking.odin.utils.noControlCodes
import com.anto.antoodin.utils.Skit
import com.anto.antoodin.utils.guiClick
import com.anto.antoodin.utils.rightClick
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.Items
import java.util.concurrent.ConcurrentHashMap

object ExperimentAddon : Module(
    name = "Auto Experiments",
    description = "Automatically click on the Chronomatron and Ultrasequencer experiments.",
    category = Skit.ANTO
) {
    private val clickDelay by NumberSetting("Click Delay", 200, 100, 1000, 10, unit = "ms", desc = "Time in ms between automatic test clicks.")
    private val delayVariety by NumberSetting("Delay variety", 50, 0, 1000, 10, unit = "ms", desc = "Variance in delays")
    private val firstClickDelay by NumberSetting("First Click Delay", 750, 100, 1500, 10, unit = "ms", desc = "Delay in ms between Chronomatron first click.")
    private val autoClose by BooleanSetting("Auto Close", true, desc = "Automatically close the GUI after completing the experiment.")
    private val subtractOne by BooleanSetting("Close One Early", false, desc = "Automatically close Ultrasequencer experiment one click earlier.")
    private val serumCount by NumberSetting("Serum Count", 0, 0, 3, 1, desc = "Consumed Metaphysical Serum count.")
    private val startUltrasequencer by BooleanSetting("Start Ultrasequencer", false, desc = "Automatically start Ultrasequencer after Chronomatron. The delay between actions will be 3x your regular delay.")

    private var handler: ExperimentHandler? = null
    private var lastClick: Long = 0

    private enum class TransitionState {
        IDLE,
        WAIT_CLOSE_GUI,
        WAIT_RIGHT_CLICK,
        WAIT_CLICK_SLOT_33,
        WAIT_CLICK_SLOT_23,
        DONE
    }
    private var transitionState = TransitionState.IDLE
    private var transitionStateTime: Long = 0

    private fun beginTransition() {
        transitionState = TransitionState.WAIT_CLOSE_GUI
        transitionStateTime = System.currentTimeMillis()
    }

    private fun cancelTransition() {
        transitionState = TransitionState.IDLE
    }

    init {
        on<ScreenEvent.Open> {
            val title = screen.title.string

            handler = when {
                title.startsWith("Chronomatron (") -> ChronomatronHandler()
                title.startsWith("Ultrasequencer (") -> UltrasequencerHandler()
                else -> null
            }

            if (handler != null) {
                lastClick = System.currentTimeMillis()
            }
        }

        on<ScreenEvent.MouseClick> {
            if (handler == null) return@on
            if (mc.screen !is AbstractContainerScreen<*>) return@on
            cancel()
        }

        on<ScreenEvent.MouseRelease> {
            if (handler == null) return@on
            if (mc.screen !is AbstractContainerScreen<*>) return@on
            cancel()
        }

        onReceive<ClientboundContainerSetSlotPacket> {
            handler?.onSlotUpdate(this)
        }

        on<TickEvent.Start> {
            if (transitionState != TransitionState.IDLE) {
                handleTransition()
                return@on
            }

            val handler = handler ?: return@on
            val screen = mc.screen as? AbstractContainerScreen<*> ?: return@on

            val now = System.currentTimeMillis()
            val isFirst = handler.isFirstClick()
            val activeDelay = if (isFirst) firstClickDelayDelay() else delay()
            if (now - lastClick < activeDelay) return@on

            handler.nextClick()?.let { slotId ->
                guiClick(screen.menu.containerId, slotId, clickType = ContainerInput.CLONE)
                lastClick = now
            }

            if (!handler.shouldClose(autoClose)) return@on

            val wasChronomatron = handler is ChronomatronHandler

            mc.player?.closeContainer()
            ExperimentAddon.handler = null

            if (wasChronomatron && startUltrasequencer) {
                beginTransition()
            }
        }
    }

    private fun handleTransition() {
        val now = System.currentTimeMillis()
        if (now - transitionStateTime < delay() * 2) return

        when (transitionState) {
            TransitionState.WAIT_CLOSE_GUI -> {
                // Close Chronomatron done gui
                mc.player?.closeContainer()
                transitionState = TransitionState.WAIT_RIGHT_CLICK
                transitionStateTime = now
            }

            TransitionState.WAIT_RIGHT_CLICK -> {
                rightClick()
                transitionState = TransitionState.WAIT_CLICK_SLOT_33
                transitionStateTime = now
            }

            TransitionState.WAIT_CLICK_SLOT_33 -> {
                val screen = mc.screen as? AbstractContainerScreen<*>
                val title = screen?.title?.string

                if (title == null || !title.startsWith("Experimentation Table")) {
                    cancelTransition()
                    return
                }

                guiClick(screen.menu.containerId, 33, clickType = ContainerInput.CLONE)
                transitionState = TransitionState.WAIT_CLICK_SLOT_23
                transitionStateTime = now
            }

            TransitionState.WAIT_CLICK_SLOT_23 -> {
                val screen = mc.screen as? AbstractContainerScreen<*>
                val title = screen?.title?.string

                if (title == null || !title.startsWith("Ultrasequencer ➜ Stakes")) {
                    cancelTransition()
                    return
                }

                guiClick(screen.menu.containerId, 23, clickType = ContainerInput.CLONE)
                transitionState = TransitionState.DONE
                transitionStateTime = now
            }

            TransitionState.DONE -> {
                transitionState = TransitionState.IDLE
            }

            TransitionState.IDLE -> { /* no-op */ }
        }
    }

    private class ChronomatronHandler : ExperimentHandler() {
        private val order = mutableListOf<Int>()
        private var lastAddedSlot = -1
        private var close = false
        private var isFirstRound = true

        override fun isFirstClick(): Boolean = isFirstRound && clicks == 0 && hasData

        override fun onSlotUpdate(packet: ClientboundContainerSetSlotPacket) {
            val slots = (mc.screen as? AbstractContainerScreen<*>)?.menu?.slots ?: return
            val center = slots[49].item

            if (
                lastAddedSlot != -1 &&
                center.item == Items.GLOWSTONE &&
                !slots[lastAddedSlot].item.hasGlint()
            ) {
                close = order.size > 11 - serumCount
                hasData = false
                return
            }

            if (hasData || center.item != Items.CLOCK) return

            val slot = slots.firstOrNull { it.index in 10..43 && it.item.hasGlint() } ?: return

            order.add(slot.index)
            lastAddedSlot = slot.index
            hasData = true
            clicks = 0
        }

        override fun nextClick(): Int? =
            if (hasData && clicks < order.size)
                order[clicks++].also { isFirstRound = false }
            else null

        override fun shouldClose(autoClose: Boolean): Boolean {
            if (!autoClose || !close) return false
            if (clicks < order.size) return false

            close = false
            return true
        }
    }

    private class UltrasequencerHandler : ExperimentHandler() {
        private val order = ConcurrentHashMap<Int, Int>()

        override fun onSlotUpdate(packet: ClientboundContainerSetSlotPacket) {
            val slots = (mc.screen as? AbstractContainerScreen<*>)?.menu?.slots ?: return
            val center = slots[49].item

            if (center.item == Items.CLOCK) {
                hasData = false
                return
            }

            if (hasData || center.item != Items.GLOWSTONE) return

            order.clear()

            for (slot in slots) {
                if (slot.index in 9..44 && slot.item.hoverName.string.noControlCodes.matches(Regex("\\d+"))) order[slot.item.count - 1] = slot.index
            }

            hasData = true
            clicks = 0
        }

        override fun nextClick(): Int? = if (!hasData) order[clicks++] else null

        override fun shouldClose(autoClose: Boolean): Boolean = autoClose && order.size > 9 - serumCount - if (subtractOne) 1 else 0
    }

    private abstract class ExperimentHandler {
        protected var clicks = 0
        protected var hasData = false

        abstract fun onSlotUpdate(packet: ClientboundContainerSetSlotPacket)
        abstract fun nextClick(): Int?
        abstract fun shouldClose(autoClose: Boolean): Boolean

        open fun isFirstClick(): Boolean = false
    }

    private fun firstClickDelayDelay(): Long =
        (firstClickDelay + (0..delayVariety).random()).toLong()

    private fun delay(): Long =
        (clickDelay + (0..delayVariety).random()).toLong()
}