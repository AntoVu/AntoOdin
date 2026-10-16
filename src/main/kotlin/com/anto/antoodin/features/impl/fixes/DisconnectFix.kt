// Feature idea from NoFrills (GPL-3.0). Written from scratch for AntoOdin, no NoFrills code is used.
package com.anto.antoodin.features.impl.fixes

import com.anto.antoodin.utils.Skit
import com.odtheking.odin.features.Module
import org.slf4j.LoggerFactory

/**
 * Hypixel sometimes sends entity data with the wrong type for a field (Tarantula slayer). Vanilla throws on it and
 * disconnects with "Network Protocol Error"; this skips the bad field and applies the rest of the packet.
 *
 * @see com.anto.antoodin.mixin.mixins.SynchedEntityDataMixin
 */
object DisconnectFix : Module(
    name = "Disconnect Fix",
    description = "Stops corrupted entity data (Tarantula slayer) from disconnecting you.",
    category = Skit.FIXES,
    toggled = true
) {
    private val logger = LoggerFactory.getLogger("AntoOdin")

    @JvmStatic
    fun onBadData(exception: Exception) = logger.warn("Disconnect Fix skipped corrupted entity data: {}", exception.message)
}
