package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.features.MergedModule
import com.anto.antoodin.features.impl.fixes.DisconnectFix
import com.anto.antoodin.features.impl.fixes.DoorFix
import com.anto.antoodin.features.impl.fixes.DoubleUseFix
import com.anto.antoodin.features.impl.fixes.ItemCountFix
import com.anto.antoodin.features.impl.fixes.ModHider
import com.anto.antoodin.features.impl.noamm.CameraTweaks
import com.anto.antoodin.features.impl.noamm.FreezeDisplay
import com.anto.antoodin.features.impl.noamm.IHateDoors
import com.anto.antoodin.features.impl.noamm.LeapCounter
import com.anto.antoodin.features.impl.noamm.LeapMenuExtras
import com.anto.antoodin.features.impl.noamm.MonoAudio
import com.anto.antoodin.features.impl.noamm.RelicSpawnTimer
import com.anto.antoodin.features.impl.noamm.SoundManager
import com.anto.antoodin.features.impl.noamm.TimeChanger
import com.anto.antoodin.features.impl.odinclient.AutoGFS
import com.anto.antoodin.features.impl.odinclient.CameraHelper
import com.anto.antoodin.features.impl.odinclient.EscrowFix
import com.anto.antoodin.features.impl.odinclient.FuckDiorite

// Modules made of several former modules (see MergedModule). The features keep their own files and credits.

object Fixes : MergedModule("Fixes", "Fixes for vanilla, Hypixel and mod bugs.") {
    init {
        feature { DoubleUseFix }
        feature { DisconnectFix }
        feature { ItemCountFix }
        feature { DoorFix }
        feature { ModHider }
        feature { EscrowFix }
    }
}

object Sounds : MergedModule("Sounds", "Mono audio, dungeon sound tweaks and per-sound volumes (/ao sounds).") {
    init {
        feature { MonoAudio }
        val dungeon = group("Dungeon Sounds", "Sound tweaks for dungeons.")
        feature(dungeon) { MuteMagicFind }
        feature(dungeon) { QuietIceFill }
        feature(alwaysOn = true) { SoundManager }
    }
}

object AutoGfs : MergedModule("Auto GFS", "Refills items from your sacks automatically.") {
    init {
        feature { PearlRefill }
        feature { AutoGFS }
        feature { KuudraAutoGFS }
    }
}

object Relics : MergedModule("Relics", "M7 relic helpers.") {
    init {
        feature { RelicUtils }
        feature { RelicSpawnTimer }
    }
}

object ClearBlocks : MergedModule("Clear Blocks", "Renders dungeon doors and the Storm pillars as glass.") {
    init {
        feature { IHateDoors }
        feature { FuckDiorite }
    }
}

object Camera : MergedModule("Camera (A)", "FOV, camera distance, effects and time of day.") {
    init {
        feature { CameraTweaks }
        feature { CameraHelper }
        feature { TimeChanger }
    }
}

object Garden : MergedModule("Garden", "Garden helpers.") {
    init {
        feature { AloeHighlight }
        feature { JellybeanHider }
    }
}

object LeapExtras : MergedModule("Leap Extras", "Extras for Odin's Leap Menu and F7 P3 leaps.") {
    init {
        feature { LeapMenuExtras }
        feature { LeapCounter }
    }
}

object Alerts : MergedModule("Alerts", "Shadow Assassin, duplicate class and server freeze alerts.") {
    init {
        feature { ShadowAssassinNotify }
        feature { DupeClassAlert }
        feature { FreezeDisplay }
    }
}

object Chat : MergedModule("Chat", "Chat rules and command tooltips.") {
    init {
        feature { ChatRules }
        feature { CommandTooltip }
    }
}

object Guards : MergedModule("Guards", "Blocks misclicks, drops and ignored terminal clicks.") {
    init {
        feature { PaneClickGuard }
        feature { DropGuard }
        feature { TerminalClickLimit }
    }
}
