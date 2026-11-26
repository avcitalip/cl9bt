package com.cl9tr.obd2.model

enum class AdapterTier(val label: String, val badgeColorHex: Long, val description: String) {
    UNKNOWN("Test Edilmedi", 0xFF64748B, "Adaptör henüz analiz edilmedi."),
    TIER_1_PIC18F25K80(
        "Sınıf 1 • Orijinal PIC18F25K80 v1.5",
        0xFF22C55E,
        "ATPPS ve ATAL tam destekli. En hızlı ISO 9141-2 (ATSP3 + ATAT2 + ATST32) profili aktif."
    ),
    TIER_2_CLONE_V15(
        "Sınıf 2 • İyi Klon v1.5",
        0xFFF59E0B,
        "Bazı gelişmiş AT komutları kısıtlı. Dengeli ISO 9141-2 (ATSP3 + ATAT1 + ATST64) profili ile kararlı bağlantı."
    ),
    TIER_3_CLONE_V21(
        "Sınıf 3 • Kısıtlı / Sahte v2.1 Klon",
        0xFFEF4444,
        "Kısıtlı komut seti algılandı. Kopmaları önlemek için güvenli/yavaş zamanlama profili kilitlendi."
    )
}

data class ProtocolProfile(
    val id: Int,
    val name: String,
    val shortCode: String,
    val protocolCmd: String,
    val adaptiveTimingCmd: String?,
    val timeoutCmd: String?,
    val targetTier: AdapterTier,
    val description: String
)

enum class ConnectionPhase {
    DISCONNECTED,
    CONNECTING_BT,
    ANALYZING_ADAPTER,
    TESTING_PROTOCOLS,
    CONNECTED,
    DEMO_MODE,
    ERROR
}

data class ConnectionLogEntry(
    val timestamp: String,
    val command: String,
    val response: String,
    val isSuccess: Boolean = true
)

data class ConnectionState(
    val phase: ConnectionPhase = ConnectionPhase.DISCONNECTED,
    val deviceName: String = "Bağlı Değil",
    val deviceAddress: String = "",
    val adapterVersion: String = "-",
    val adapterTier: AdapterTier = AdapterTier.UNKNOWN,
    val activeProfile: ProtocolProfile? = null,
    val avgLatencyMs: Int = 0,
    val pollingHz: Double = 0.0,
    val batteryVoltage: Double = 0.0,
    val statusMessage: String = "ELM327 cihazını seçin veya Demo Modu başlatın.",
    val logs: List<ConnectionLogEntry> = emptyList(),
    val consecutiveErrors: Int = 0
)

enum class PidCategory(val title: String) {
    ENGINE("Motor & Yük"),
    FUEL_O2("Yakıt & Wideband O2"),
    AIR_TEMP("Emme & Sıcaklık"),
    THROTTLE("Gaz & Kelebek"),
    DERIVED("K24A3 Türetilmiş")
}

data class PidDefinition(
    val pidHex: String,
    val nameTr: String,
    val shortName: String,
    val unit: String,
    val category: PidCategory,
    val normalRange: String,
    val minVal: Double,
    val maxVal: Double,
    val defaultPinned: Boolean = false,
    val isDerived: Boolean = false,
    val formulaDesc: String,
    val roleTr: String = "",
    val meaningTr: String = ""
)

data class LivePidValue(
    val definition: PidDefinition,
    val value: Double,
    val formattedValue: String,
    val isWarning: Boolean = false
)

data class DtcCode(
    val code: String,
    val isPending: Boolean,
    val system: String,
    val descriptionTr: String,
    val cl9Note: String
)

data class ReadinessMonitor(
    val nameTr: String,
    val supported: Boolean,
    val ready: Boolean
)

data class Mode06Test(
    val tid: String,
    val nameTr: String,
    val value: String,
    val limit: String,
    val passed: Boolean
)

data class EcuIdentity(
    val vin: String = "-",
    val calId: String = "-",
    val cvn: String = "-",
    val ecuModel: String = "Matsushita RBB (SH7055 / 512KB)",
    val ecuKm: Int? = null,
    val kmMismatchWarning: String = ""
)

data class FreezeFrameItem(
    val label: String,
    val value: String
)
