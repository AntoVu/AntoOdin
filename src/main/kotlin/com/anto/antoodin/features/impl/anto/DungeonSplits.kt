package com.anto.antoodin.features.impl.anto

import com.anto.antoodin.utils.Skit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.odtheking.odin.OdinMod
import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.LevelEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.features.impl.skyblock.Splits
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.formatTime
import com.odtheking.odin.utils.modMessage
import com.odtheking.odin.utils.render.getStringWidth
import com.odtheking.odin.utils.render.text
import com.odtheking.odin.utils.skyblock.SplitRow
import com.odtheking.odin.utils.skyblock.SplitsManager
import com.odtheking.odin.utils.skyblock.dungeon.DungeonListener
import com.odtheking.odin.utils.skyblock.dungeon.Floor
import com.odtheking.odin.utils.skyblock.floor7SplitGroup
import com.odtheking.odin.utils.toFixed
import java.io.File

// Reads Odin's SplitsManager, which tracks every run and records PBs even while Odin's Splits module is off.
object DungeonSplits : Module(
    name = "Dungeon Splits",
    description = "Dungeon splits with tick time, projected run time and time lost to lag.",
    category = Skit.ANTO
) {
    private const val MAX_RUNS = 10
    private const val TOTAL = "§1Total"

    private val hud by HUD("Splits Display HUD", "Shows timers for each split.") { example ->
        val timeWidth = 75 + if (showTickTime) 20 else -20

        if (example) {
            val labels = floor7SplitGroup.map { "${it.name}:" } + if (projectedHud) listOf("§dProjected:") else emptyList()
            val labelWidth = labels.maxOf { getStringWidth(it) }
            val timeExample = "59m 59s" + if (showTickTime) " §8(§759.9§8)" else ""
            val totalWidth = labelWidth + 4 + timeWidth + 2

            labels.forEachIndexed { i, label ->
                if (fixedWidth) {
                    text(label, 0, i * 9, Colors.WHITE)
                    text(timeExample, totalWidth - getStringWidth(timeExample) - 2, i * 9, Colors.WHITE)
                } else text("$label $timeExample", 0, i * 9, Colors.WHITE)
            }
            return@HUD totalWidth to 9 * labels.size
        }

        val rows = SplitsManager.currentRows()
        if (rows.isEmpty()) return@HUD 0 to 0
        val segments = rows.dropLast(1)

        val lines = mutableListOf<Pair<String, String>>()
        segments.forEachIndexed { index, row ->
            if (row.time != 0L || show0Time) lines.add(row.name to timeText(row.time, row.tickTime))
            if (bossEntrySplit && index == 2 && rows.size > 3) {
                val boss = bossEntry(segments)
                if (boss.time != 0L || show0Time) lines.add(boss.name to timeText(boss.time, boss.tickTime))
            }
        }
        if (projectedHud) DungeonListener.floor?.let { projected(segments, completedCount(rows), it) }?.let { lines.add("§dProjected" to formatTime(it)) }

        val labelWidth = lines.maxOfOrNull { getStringWidth(it.first) } ?: 0
        val totalWidth = labelWidth + 4 + timeWidth + 2

        lines.forEachIndexed { i, (label, time) ->
            text(label, 0, i * 9, Colors.WHITE)
            text(time, if (fixedWidth) labelWidth + 4 + timeWidth - getStringWidth(time) else labelWidth + 4, i * 9, Colors.WHITE)
        }
        totalWidth to lines.size * 9
    }

    private val fixedWidth by BooleanSetting("Fixed Width", true, desc = "Always use a fixed HUD width, right-aligning the times.")
    private val bossEntrySplit by BooleanSetting("Boss Entry Split", true, desc = "Split for boss entry.")
    private val show0Time by BooleanSetting("Show 0 splits", false, desc = "Shows splits which have their time at 0.")
    private val showTickTime by BooleanSetting("Show Tick Time", true, desc = "Show tick-based time alongside real time.")
    private val splitMessages by BooleanSetting("Split Messages", true, desc = "Sends each split's time in chat when it ends.")
    private val showPb by BooleanSetting("Show PB", true, desc = "Compares each split to your PB in the split messages.").withDependency { splitMessages }
    private val projectedHud by BooleanSetting("Projected In HUD", true, desc = "Shows the projected run time at the bottom of the HUD.")
    private val projectedChat by BooleanSetting("Projected In Chat", true, desc = "Sends the projected run time in chat after each split.")
    private val projectionRuns by NumberSetting("Projection Runs", 5, 1, MAX_RUNS, 1, desc = "How many recent runs to average for the projected run time.")
        .withDependency { projectedHud || projectedChat }
    private val lagMessage by BooleanSetting("Time Lost To Lag", true, desc = "Sends the time lost to server lag when the run ends.")

    // Kept out of AntoOdin.json so the module config stays readable
    private val runsFile = File(OdinMod.configFile, "addons/antoodin-recent-runs.json")
    private val recentRuns: MutableMap<String, MutableList<List<Long>>> by lazy {
        runCatching {
            Gson().fromJson<MutableMap<String, MutableList<List<Long>>>>(
                runsFile.readText(), object : TypeToken<MutableMap<String, MutableList<List<Long>>>>() {}.type
            )
        }.getOrNull() ?: mutableMapOf()
    }

    private var lastCompleted = 0
    private var pbSnapshot: Map<String, Float?>? = null

    init {
        on<TickEvent.End> {
            val rows = SplitsManager.currentRows()
            if (rows.isEmpty() || rows.last().time == 0L) return@on
            val floor = DungeonListener.floor ?: return@on
            val segments = rows.dropLast(1)

            // Taken before the first split, since Odin overwrites PBs as soon as a split ends
            val pbs = pbSnapshot ?: Splits.dungeonPBsList[floor.ordinal].let { pb -> rows.associate { it.name to pb.get(it.name) } }
                .also { pbSnapshot = it }

            val completed = completedCount(rows)
            if (completed <= lastCompleted) return@on

            if (splitMessages) for (i in lastCompleted until completed) {
                splitMessage(segments[i].name, segments[i], pbs[segments[i].name])
                if (bossEntrySplit && i == 2 && rows.size > 3) splitMessage("§9Boss Entry", bossEntry(segments), null)
            }
            lastCompleted = completed

            if (completed < segments.size) {
                if (projectedChat) projected(segments, completed, floor)?.let { modMessage("§dProjected Run Time: §a${formatTime(it)}") }
                return@on
            }

            val total = rows.last()
            if (splitMessages) splitMessage("§6Total", total, pbs[total.name])
            if (lagMessage) modMessage("§bTime lost to lag: §a${formatTime((total.time - total.tickTime * 50).coerceAtLeast(0))}")

            val runs = recentRuns.getOrPut(floor.name) { mutableListOf() }
            runs.add(segments.map { it.time })
            while (runs.size > MAX_RUNS) runs.removeAt(0)
            saveRuns()
        }

        on<LevelEvent.Load> { reset() }
    }

    override fun onEnable() {
        reset()
        super.onEnable()
    }

    private fun reset() {
        lastCompleted = 0
        pbSnapshot = null
    }

    // One run per line, so the file stays short and readable
    private fun saveRuns() = runCatching {
        runsFile.writeText(recentRuns.entries.joinToString(",\n", "{\n", "\n}\n") { (floor, runs) ->
            "  \"$floor\": [\n${runs.joinToString(",\n") { "    $it" }}\n  ]"
        })
    }.onFailure { it.printStackTrace() }

    fun printBest(arg: String?) {
        val floor = if (arg == null) DungeonListener.floor ?: Floor.M7
        else Floor.entries.find { it.name.equals(arg, true) } ?: return modMessage("§cUnknown floor: $arg")

        @Suppress("UNCHECKED_CAST")
        val pbs = (Splits.settings["Dungeon${floor.name}"]?.value as? Map<String, Float>)?.takeIf { it.isNotEmpty() }
            ?: return modMessage("§cNo PBs recorded for ${floor.name}.")
        val segments = pbs.filterKeys { it != TOTAL }
        val best = segments.values.sum()

        modMessage("§6${floor.name} PB splits:")
        segments.forEach { (name, time) -> modMessage("$name§7: §6${time.toFixed()}s") }
        modMessage("§6Theoretical best: §a${formatTime((best * 1000).toLong())}")
        pbs[TOTAL]?.let { modMessage("§7Actual PB: §6${formatTime((it * 1000).toLong())} §8(${(it - best).toFixed()}s to gain)") }
    }

    private fun splitMessage(label: String, row: SplitRow, pb: Float?) {
        val seconds = row.time / 1000f
        val pbText = when {
            !showPb || pb == null -> ""
            seconds < pb -> " §d§lNew PB§r§8 (${pb.toFixed()}s)"
            else -> " §8(PB ${pb.toFixed()}s)"
        }
        modMessage("$label §7took §6${formatTime(row.time)} §8(§7${(row.tickTime / 20f).toFixed()}s§8)$pbText")
    }

    private fun timeText(time: Long, ticks: Long) =
        if (showTickTime) "${formatTime(time)} §8(§7${(ticks / 20f).toFixed()}§8)" else formatTime(time)

    private fun bossEntry(segments: List<SplitRow>) =
        SplitRow("§9Boss Entry", segments.take(3).sumOf { it.time }, segments.take(3).sumOf { it.tickTime }, isCurrent = false)

    // Segments finished so far: all of them once the run ends, none before it starts
    private fun completedCount(rows: List<SplitRow>): Int =
        if (rows.last().time == 0L) 0 else rows.indexOfFirst { it.isCurrent }.takeIf { it != -1 } ?: (rows.size - 1)

    private fun projected(segments: List<SplitRow>, completed: Int, floor: Floor): Long? {
        val runs = recentRuns[floor.name]?.filter { it.size == segments.size }?.takeLast(projectionRuns)
        if (runs.isNullOrEmpty()) return null
        return segments.take(completed).sumOf { it.time } + (completed until segments.size).sumOf { i -> runs.sumOf { it[i] } / runs.size }
    }
}
