package com.anto.antoodin.features

import com.odtheking.odin.utils.formatNumber
import com.odtheking.odin.utils.modMessage
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToLong

/** `/ao calc <type>`: XP, drop chance, gem, USD and auction tax calculators ported from AntourAddons. */
object Calculators {
    private val usages = mapOf(
        "cata" to "<start> <end>",
        "skill" to "<start> <end>",
        "pet" to "<tier> <start> <end>",
        "drop" to "<base chance %> [looting] [magic find] [pet luck]",
        "coin" to "<gems> <coins> [cookie price]",
        "usd" to "<coins> <cookie price>",
        "tax" to "<price>"
    )
    val TYPES = usages.keys

    private val CATA_XP = intArrayOf(
        50, 75, 110, 160, 230, 330, 470, 670, 950, 1340, 1890, 2665, 3760, 5260, 7380, 10300, 14400, 20000, 27600, 38000,
        52500, 71500, 97000, 132000, 180000, 243000, 328000, 445000, 600000, 800000, 1065000, 1410000, 1900000, 2500000,
        3300000, 4300000, 5600000, 7200000, 9200000, 12000000, 15000000, 19000000, 24000000, 30000000, 38000000, 48000000,
        60000000, 75000000, 93000000, 116250000
    )

    private val SKILL_XP = intArrayOf(
        50, 125, 200, 300, 500, 750, 1000, 1500, 2000, 3500, 5000, 7500, 10000, 15000, 20000, 30000, 50000, 75000,
        100000, 200000, 300000, 400000, 500000, 600000, 700000, 800000, 900000, 1000000, 1100000, 1200000, 1300000,
        1400000, 1500000, 1600000, 1700000, 1800000, 1900000, 2000000, 2100000, 2200000, 2300000, 2400000, 2500000,
        2600000, 2750000, 2900000, 3100000, 3400000, 3700000, 4000000, 4300000, 4600000, 4900000, 5200000, 5500000,
        5800000, 6100000, 6400000, 6700000, 7000000
    )

    // XP for each pet level up; a rarity starts at its offset (common 1→2 = 100, legendary 1→2 = 660)
    private val PET_XP = intArrayOf(
        100, 110, 120, 130, 145, 160, 175, 190, 210, 230, 250, 275, 300, 330, 360, 400, 440, 490, 540, 600, 660, 730, 800,
        880, 960, 1050, 1150, 1260, 1380, 1510, 1650, 1800, 1960, 2130, 2310, 2500, 2700, 2920, 3160, 3420, 3700, 4000,
        4350, 4750, 5200, 5700, 6300, 7000, 7800, 8700, 9700, 10800, 12000, 13300, 14700, 16200, 17800, 19500, 21300,
        23200, 25200, 27400, 29800, 32400, 35200, 38200, 41400, 44800, 48400, 52200, 56200, 60400, 64800, 69400, 74200,
        79200, 84700, 90700, 97200, 104200, 111700, 119700, 128200, 137200, 146700, 156700, 167700, 179700, 192700,
        206700, 221700, 237700, 254700, 272700, 291700, 311700, 333700, 357700, 383700, 411700, 441700, 476700, 516700,
        561700, 611700, 666700, 726700, 791700, 861700, 936700, 1016700, 1101700, 1191700, 1286700, 1386700, 1496700,
        1616700, 1746700, 1886700
    )
    private const val PET_XP_PAST_100 = 1886700

    private val PET_TIERS = mapOf(
        "com" to "Common", "common" to "Common", "unc" to "Uncommon", "uncommon" to "Uncommon", "rare" to "Rare",
        "epic" to "Epic", "leg" to "Legendary", "legendary" to "Legendary", "myth" to "Mythic", "mythic" to "Mythic"
    )
    private val PET_OFFSETS = mapOf("Common" to 0, "Uncommon" to 6, "Rare" to 11, "Epic" to 16, "Legendary" to 20, "Mythic" to 20)

    private val units = mapOf('k' to 1e3, 'm' to 1e6, 'b' to 1e9)

    fun run(type: String, args: List<String>) {
        val usage = usages[type] ?: return modMessage("§7Usage: /ao calc <${TYPES.joinToString("|")}>")
        val error = when (type) {
            "cata" -> cata(args)
            "skill" -> skill(args)
            "pet" -> pet(args)
            "drop" -> drop(args)
            "coin" -> coin(args)
            "usd" -> usd(args)
            else -> tax(args)
        } ?: return
        modMessage("§c$error")
        modMessage("§7Usage: /ao calc $type $usage")
    }

    // Each calc returns an error message, or null after printing its result

    private fun cata(args: List<String>): String? {
        val (start, end) = levels(args, 0) ?: return "Invalid input."
        if (start < 0 || start > end) return "Invalid input."
        val xp = xpBetween(start, end) { if (it < CATA_XP.size) CATA_XP[it].toDouble() else 200_000_000.0 }
        modMessage("§7Catacombs XP Required: §6${"%,d".format(xp)}")
        return null
    }

    private fun skill(args: List<String>): String? {
        val (start, end) = levels(args, 0) ?: return "Invalid input."
        if (start < 0 || start > end || end > 60) return "Levels must be between 0 and 60."
        val xp = xpBetween(start, end) { SKILL_XP[it].toDouble() }
        modMessage("§7Skill XP Required: §6${"%,d".format(xp)}")
        return null
    }

    private fun pet(args: List<String>): String? {
        val tier = PET_TIERS[args.getOrNull(0)?.lowercase()] ?: return "Invalid tier. §fcom, §aunc, §9rare, §5epic, §6leg, §dmyth §c(or full names)."
        val (start, end) = levels(args, 1) ?: return "Invalid input."
        if (start < 1) return "Start level must be at least 1."
        if (end > if (tier == "Legendary") 200 else 100) return "End level too high."
        if (start > end) return "Start level is higher than end level."
        val offset = PET_OFFSETS.getValue(tier)
        // Index i is the level up from i + 1; every legendary level past 100 costs the same
        val xp = xpBetween(start - 1, end - 1) { if (it >= 99) PET_XP_PAST_100.toDouble() else PET_XP[offset + it].toDouble() }
        modMessage("§7$tier Pet XP Required: §6${"%,d".format(xp)}")
        return null
    }

    private fun drop(args: List<String>): String? {
        if (args.isEmpty() || args.size > 4) return "Invalid input."
        val (base, looting, magicFind, petLuck) = (args + List(4 - args.size) { "0" }).map { it.toDoubleOrNull() ?: return "Invalid number: $it" }
        if (base <= 0) return "Base drop chance must be above 0."
        if (looting < 0 || magicFind < 0 || petLuck < 0) return "Negative values not supported."
        val chance = base * (1 + (magicFind + petLuck) / 100) * (1 + 0.15 * looting)
        modMessage("§7Item has about §a${"%.4f".format(chance)}% §7chance of dropping.")
        return null
    }

    private fun coin(args: List<String>): String? {
        if (args.size !in 2..3) return "Invalid input."
        val gems = args[0].toDoubleOrNull()?.takeIf { it > 0 } ?: return "Invalid gem amount."
        val coins = coins(args[1]) ?: return "Invalid coin amount."
        val perGem = coins / gems
        var message = "§7About §6${short(perGem)} coins §7per gem."
        if (args.size == 3) {
            val cookie = coins(args[2]) ?: return "Invalid booster cookie price."
            val cookiePerGem = cookie / 325
            val diff = (perGem - cookiePerGem) / cookiePerGem * 100
            message += " This is §c${"%.2f".format(abs(diff))}% §7${if (diff >= 0) "better" else "worse"} than booster cookies."
        }
        modMessage(message)
        return null
    }

    private fun usd(args: List<String>): String? {
        if (args.size != 2) return "Invalid input."
        val coins = coins(args[0]) ?: return "Invalid coin amount."
        val cookie = coins(args[1]) ?: return "Invalid booster cookie price."
        val usd = coins / (cookie / 2.4074)
        modMessage("§6${short(coins)} §7is about §a$${"%.2f".format(usd)} §7worth of coins.")
        return null
    }

    private fun tax(args: List<String>): String? {
        if (args.size != 1) return "Invalid input."
        val price = coins(args[0]) ?: return "Invalid price."
        val listFee = price * when {
            price < 10_000_000 -> 0.01
            price < 100_000_000 -> 0.02
            else -> 0.025
        }
        val claimFee = if (price < 1_000_000) 0.0 else price * 0.01
        modMessage("§7Listing an item for §6${short(price)}")
        modMessage("§8---- §7List Fee: §c${short(listFee)}", "")
        modMessage("§8---- §7Claim Fee: §c${short(claimFee)}", "")
        modMessage("§8---- §7Total Profit: §6${short(price - listFee - claimFee)}", "")
        return null
    }

    /** Two levels starting at [from], or null if missing or not numbers. */
    private fun levels(args: List<String>, from: Int): Pair<Double, Double>? {
        if (args.size != from + 2) return null
        return (args[from].toDoubleOrNull() ?: return null) to (args[from + 1].toDoubleOrNull() ?: return null)
    }

    /** XP from [start] to [end]; [cost] gives the XP for the level up at that index, fractions count partially. */
    private fun xpBetween(start: Double, end: Double, cost: (Int) -> Double): Long {
        fun xpTo(level: Double): Double {
            val whole = floor(level).toInt()
            val fraction = level - whole
            return (0 until whole).sumOf(cost) + if (fraction > 0) fraction * cost(whole) else 0.0
        }
        return (xpTo(end) - xpTo(start)).roundToLong()
    }

    /** Parses "1.5m" style amounts (k, m, b); null unless above 0. */
    private fun coins(input: String): Double? {
        val multiplier = units[input.last().lowercaseChar()]
        val number = (if (multiplier != null) input.dropLast(1) else input).toDoubleOrNull() ?: return null
        return (number * (multiplier ?: 1.0)).takeIf { it > 0 }
    }

    private fun short(value: Double) = formatNumber(value.toString())
}
