package com.cl9tr.obd2.model

enum class AlertLevel { GREEN, BLUE, ORANGE, RED }

enum class SimScenario(val label: String) {
    IDLE("Rölanti"),
    CITY("Şehir İçi"),
    VTEC("Tam Gaz"),
    COLD("Soğuk"),
    OVERHEAT("Hararet")
}

data class HomeBannerState(
    val code: String = "ok",
    val level: AlertLevel = AlertLevel.GREEN,
    val badge: String = "İyi Durumda",
    val message: String = "Hoş geldiniz, arabanın kondisyonu iyi görünüyor, sürüşe devam edebilirsiniz.",
    val meteoRemainingSec: Int = 0
)

data class VehicleProfile(
    val id: String = "p_default",
    val plate: String = "34 BCA 320",
    val km: Int = 375000,
    val fuel: String = "Benzin + LPG",
    val engineStatus: String = "Orijinal Motor",
    val engineDetailKm: Int = 0,
    val transStatus: String = "Orijinal Şanzıman",
    val transDetailKm: Int = 0,
    val tuneLevel: String = "Orijinal",
    val aggressiveMods: Boolean = false,
    val dynoHp: Int = 0,
    val dynoNm: Int = 0,
    val frontSize: String = "205/55 R16",
    val frontBrand: String = "Continental PremiumContact 7",
    val frontSeason: String = "Yaz Lastiği",
    val frontYear: Int = 2024,
    val frontCond: Int = 85,
    val rearSize: String = "205/55 R16",
    val rearBrand: String = "Continental PremiumContact 7",
    val rearSeason: String = "Yaz Lastiği",
    val rearYear: Int = 2024,
    val rearCond: Int = 85
) {
    fun isLpg(): Boolean = fuel.contains("LPG", ignoreCase = true)
    fun activeFuelName(): String = if (isLpg()) "LPG" else "Benzin"
}

data class LastConnectedSnapshot(
    val plate: String,
    val fuel: String,
    val rpm: Double,
    val ect: Double,
    val iat: Double,
    val map: Double,
    val stft: Double,
    val ltft: Double,
    val volt: Double,
    val savedAtMs: Long
)

data class PerfMetrics(
    val hasData: Boolean = false,
    val liveHp: Int = 0,
    val liveNm: Int = 0,
    val peakHp: Int = 0,
    val peakNm: Int = 0,
    val liveWhp: Int = 0,
    val liveWnm: Int = 0,
    val peakWhp: Int = 0,
    val peakWnm: Int = 0,
    val estCeilingHp: Int = 190,
    val estCeilingNm: Int = 223,
    val crankCurveHp: List<Int> = emptyList(),
    val crankCurveNm: List<Int> = emptyList(),
    val wheelCurveHp: List<Int> = emptyList(),
    val wheelCurveNm: List<Int> = emptyList(),
    val aiComment: String = ""
)

data class FuelMetrics(
    val hasData: Boolean = false,
    val instL: Double = 0.0,
    val isIdleUnit: Boolean = true,
    val avg10sL: Double = 0.0,
    val tripAvgL: Double = 0.0,
    val allTimeAvgL: Double = 0.0,
    val fuelQualityPct: Int = 0,
    val combQualityPct: Int = 0,
    val estOctane: Int = 0,
    val cityName: String = "Zonguldak",
    val unitPriceTl: Double = 50.47,
    val activeFuelName: String = "LPG",
    val cost100Tl: Int = 0,
    val cost1000Tl: Int = 0,
    val gpsGainM: Int = 640,
    val gpsLossM: Int = 590,
    val aiTone: String = "Övgü",
    val aiComment: String = ""
)

data class TireEntry(
    val keys: List<String>,
    val fullName: String,
    val season: String,
    val trait: String
)

object TireCatalog {
    val items = listOf(
        TireEntry(listOf("sf3", "cinturato sf3", "pirelli sf3", "all season sf3"), "Pirelli Cinturato All Season SF3", "4 Mevsim Lastik", "Islak ve soğuk zeminde üst düzey 4 mevsim"),
        TireEntry(listOf("cp7", "pc7", "premiumcontact 7", "premium contact 7", "kontinental 7"), "Continental PremiumContact 7", "Yaz Lastiği", "Islak/kuru fren lideri yaz lastiği"),
        TireEntry(listOf("cc2", "crossclimate 2", "michelin cc2"), "Michelin CrossClimate 2", "4 Mevsim Lastik", "Kar ve ıslak zeminde yüksek ömürlü 4 mevsim"),
        TireEntry(listOf("ps5", "pilot sport 5", "michelin ps5"), "Michelin Pilot Sport 5", "Yaz Lastiği", "Sportif direksiyon tepkili yaz lastiği"),
        TireEntry(listOf("ps4", "pilot sport 4"), "Michelin Pilot Sport 4", "Yaz Lastiği", "Yüksek yol tutuşlu yaz lastiği"),
        TireEntry(listOf("primacy 4", "p4", "primacy 4+"), "Michelin Primacy 4+", "Yaz Lastiği", "Konfor ve sessizlik odaklı yaz lastiği"),
        TireEntry(listOf("as6", "asymmetric 6", "eagle f1"), "Goodyear Eagle F1 Asymmetric 6", "Yaz Lastiği", "Dengeli spor yaz lastiği"),
        TireEntry(listOf("vector 4", "4seasons", "goodyear 4 mevsim"), "Goodyear Vector 4Seasons Gen-3", "4 Mevsim Lastik", "Dengeli 4 mevsim lastiği"),
        TireEntry(listOf("turanza 6", "t005", "bridgestone turanza"), "Bridgestone Turanza 6", "Yaz Lastiği", "Islak yol konfor yaz lastiği"),
        TireEntry(listOf("a005", "weather control", "bridgestone 4 mevsim"), "Bridgestone Weather Control A005 Evo", "4 Mevsim Lastik", "Islak fren güçlü 4 mevsim"),
        TireEntry(listOf("blizzak", "lm005"), "Bridgestone Blizzak LM005", "Kış Lastiği", "Islak ve karda çok güçlü kış lastiği"),
        TireEntry(listOf("ts870", "wintercontact", "ts 870"), "Continental WinterContact TS 870", "Kış Lastiği", "Buz ve karda referans kış lastiği"),
        TireEntry(listOf("alpin 6", "alpin 7", "michelin alpin"), "Michelin Alpin 6", "Kış Lastiği", "Uzun ömürlü kış lastiği"),
        TireEntry(listOf("ug9", "ultragrip", "ug performance"), "Goodyear UltraGrip Performance 3", "Kış Lastiği", "Kış ve soğuk ıslak zemin lastiği"),
        TireEntry(listOf("s1 evo3", "ventus", "hankook"), "Hankook Ventus S1 evo3", "Yaz Lastiği", "Fiyat/performans spor yaz lastiği"),
        TireEntry(listOf("kinergy 4s2", "4s2", "hankook 4 mevsim"), "Hankook Kinergy 4S2", "4 Mevsim Lastik", "Dengeli 4 mevsim lastiği"),
        TireEntry(listOf("snoways", "lassa kis", "lassa kış"), "Lassa Snoways 4", "Kış Lastiği", "Türkiye yollarına dayanıklı kış lastiği"),
        TireEntry(listOf("revolas", "driveways", "lassa yaz"), "Lassa Driveways", "Yaz Lastiği", "Konforlu günlük yaz lastiği"),
        TireEntry(listOf("imperium", "petlas yaz"), "Petlas Imperium PT515", "Yaz Lastiği", "Ekonomik günlük yaz lastiği"),
        TireEntry(listOf("snowmaster", "petlas kis", "petlas kış"), "Petlas SnowMaster W651", "Kış Lastiği", "Ekonomik kış lastiği")
    )

    fun detect(raw: String): TireEntry {
        val q = raw.trim().lowercase()
            .replace("ı", "i").replace("ğ", "g").replace("ü", "u")
            .replace("ş", "s").replace("ö", "o").replace("ç", "c")
        if (q.isEmpty()) return items[1]
        // 1. Direct key or fullName substring match
        items.firstOrNull { entry ->
            entry.keys.any { q.contains(it) || it.contains(q) } ||
                entry.fullName.lowercase().contains(q)
        }?.let { return it }
        // 2. Token overlap
        val tokens = q.split(Regex("\\s+")).filter { it.length >= 2 }
        var best: TireEntry? = null
        var bestScore = 0
        for (entry in items) {
            val hay = (entry.fullName + " " + entry.keys.joinToString(" ")).lowercase()
            val score = tokens.count { hay.contains(it) }
            if (score > bestScore) {
                bestScore = score
                best = entry
            }
        }
        if (best != null && bestScore > 0) return best
        val inferredSeason = when {
            q.contains("kis") || q.contains("snow") || q.contains("winter") || q.contains("alpin") -> "Kış Lastiği"
            q.contains("4") || q.contains("all") || q.contains("mevsim") || q.contains("season") -> "4 Mevsim Lastik"
            else -> "Yaz Lastiği"
        }
        val cleanName = raw.trim().split(Regex("\\s+")).joinToString(" ") { w ->
            w.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        return TireEntry(listOf(q), cleanName, inferredSeason, "Kullanıcı tanımlı $inferredSeason")
    }
}
