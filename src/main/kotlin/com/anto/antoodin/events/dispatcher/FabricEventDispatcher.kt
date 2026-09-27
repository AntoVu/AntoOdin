package com.anto.antoodin.events.dispatcher

// Implementation based on skies-starred OdinClient
import com.anto.antoodin.events.TickStartEvent
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents

object FabricEventDispatcher {
    init {
        ClientTickEvents.START_CLIENT_TICK.register {
            TickStartEvent.postAndCatch()
        }
    }
}
