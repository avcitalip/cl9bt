package com.cl9tr.obd2.viewmodel

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cl9tr.obd2.R
import com.cl9tr.obd2.model.*
import com.cl9tr.obd2.obd.Elm327ConnectionManager
import com.cl9tr.obd2.obd.HondaDtcDatabase
import com.cl9tr.obd2.obd.K24A3PidEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

class ObdViewModel : ViewModel() {

    private val elm = Elm327ConnectionManager()
    private var pollingJob: Job? = null
    private var meteoJob: Job? = null
    private var appContext: Context? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private val _connectionState = MutableStateFlow(ConnectionState())
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDevice>> = _pairedDevices.asStateFlow()

    private val _supportedPids = MutableStateFlow<Set<String>>(emptySet())
    val supportedPids: StateFlow<Set<String>> = _supportedPids.asStateFlow()

    private val _pinnedPids = MutableStateFlow(
        K24A3PidEngine.allPids.filter { it.defaultPinned }.map { it.pidHex }.toSet()
    )
    val pinnedPids: StateFlow<Set<String>> = _pinnedPids.asStateFlow()

    private val _liveValues = MutableStateFlow<Map<String, LivePidValue>>(emptyMap())
    val liveValues: StateFlow<Map<String, LivePidValue>> = _liveValues.asStateFlow()

    private val _dtcs = MutableStateFlow<List<DtcCode>>(emptyList())
    val dtcs: StateFlow<List<DtcCode>> = _dtcs.asStateFlow()

    private val _freezeFrame = MutableStateFlow<List<FreezeFrameItem>>(emptyList())
    val freezeFrame: StateFlow<List<FreezeFrameItem>> = _freezeFrame.asStateFlow()

    private val _readiness = MutableStateFlow<List<ReadinessMonitor>>(emptyList())
    val readiness: StateFlow<List<ReadinessMonitor>> = _readiness.asStateFlow()

    private val _mode06 = MutableStateFlow<List<Mode06Test>>(emptyList())
    val mode06: StateFlow<List<Mode06Test>> = _mode06.asStateFlow()

    private val _ecuIdentity = MutableStateFlow(EcuIdentity())
    val ecuIdentity: StateFlow<EcuIdentity> = _ecuIdentity.asStateFlow()

    private val _isRecordingCsv = MutableStateFlow(false)
    val isRecordingCsv: StateFlow<Boolean> = _isRecordingCsv.asStateFlow()

    private val _csvRows = MutableStateFlow<List<String>>(emptyList())
    val csvRows: StateFlow<List<String>> = _csvRows.asStateFlow()

    // Profiller (Plaka bazlı kalıcı)
    private val _profiles = MutableStateFlow<List<VehicleProfile>>(emptyList())
    val profiles: StateFlow<List<VehicleProfile>> = _profiles.asStateFlow()

    private val _activeProfile = MutableStateFlow(VehicleProfile())
    val activeProfile: StateFlow<VehicleProfile> = _activeProfile.asStateFlow()

    private val _needsInitialProfile = MutableStateFlow(true)
    val needsInitialProfile: StateFlow<Boolean> = _needsInitialProfile.asStateFlow()

    private val _lastSnapshot = MutableStateFlow<LastConnectedSnapshot?>(null)
    val lastSnapshot: StateFlow<LastConnectedSnapshot?> = _lastSnapshot.asStateFlow()

    private val _simScenario = MutableStateFlow(SimScenario.IDLE)
    val simScenario: StateFlow<SimScenario> = _simScenario.asStateFlow()

    private val _homeBanner = MutableStateFlow(HomeBannerState())
    val homeBanner: StateFlow<HomeBannerState> = _homeBanner.asStateFlow()

    private val _perfMetrics = MutableStateFlow(PerfMetrics())
    val perfMetrics: StateFlow<PerfMetrics> = _perfMetrics.asStateFlow()

    private val _fuelMetrics = MutableStateFlow(FuelMetrics())
    val fuelMetrics: StateFlow<FuelMetrics> = _fuelMetrics.asStateFlow()

    private val _trackedKm = MutableStateFlow(142)
    val trackedKm: StateFlow<Int> = _trackedKm.asStateFlow()

    private val _trackedHours = MutableStateFlow(4.5)
    val trackedHours: StateFlow<Double> = _trackedHours.asStateFlow()

    private val _menuOrder = MutableStateFlow(
        listOf("dashboard", "performance", "fuel", "sensors", "dtc", "maintenance", "connect", "logger", "advice")
    )
    val menuOrder: StateFlow<List<String>> = _menuOrder.asStateFlow()

    private val _customNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val customNames: StateFlow<Map<String, String>> = _customNames.asStateFlow()

    // Anlık sıçrama filtresi (3 ardışık ölçüm medyanı)
    private val hpWindow = ArrayDeque<Int>()
    private val nmWindow = ArrayDeque<Int>()
    private var verifiedPeakHp = 0
    private var verifiedPeakNm = 0
    private var verifiedPeakWhp = 0
    private var verifiedPeakWnm = 0

    // Yakıt 10 sn penceresi ve GPS 100 km rakım değişimi
    private val fuelSamples10s = ArrayDeque<Double>()
    private var gpsGainM = 640
    private var gpsLossM = 590
    private var lastAltitudeM: Double? = null

    // Bilgilendirme ses tekrarı engelleme & aktif MGM/AFAD uyarısı
    private var lastBannerCode: String? = null
    private var activeMeteoText: String? = null
    private var meteoRemainingSec: Int = 0

    companion object {
        private const val PREFS_NAME = "cl9_k24a3_prefs_v2"
        private const val KEY_PROFILES = "profiles_json"
        private const val KEY_ACTIVE_ID = "active_profile_id"
        private const val KEY_SNAPSHOT = "last_snapshot_json"
        private const val KEY_MENU_ORDER = "menu_order_csv"
        // 26.11.2026 Zonguldak / Ereğli güncel ortalama pompa fiyatları
        const val CITY_NAME = "Zonguldak"
        const val PRICE_LPG_TL = 50.47
        const val PRICE_BENZIN_TL = 68.30
    }

    fun attachContext(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        loadPersistedState()
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("tr", "TR")
                ttsReady = true
            }
        }
    }

    fun onGpsAltitudeUpdate(altitudeMeters: Double) {
        val prev = lastAltitudeM
        lastAltitudeM = altitudeMeters
        if (prev != null) {
            val diff = altitudeMeters - prev
            if (abs(diff) in 1.5..60.0) {
                if (diff > 0) gpsGainM += diff.roundToInt() else gpsLossM += (-diff).roundToInt()
            }
        }
    }

    private fun loadPersistedState() {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawProfiles = prefs.getString(KEY_PROFILES, null)
        if (!rawProfiles.isNullOrBlank()) {
            try {
                val arr = JSONArray(rawProfiles)
                val list = mutableListOf<VehicleProfile>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        VehicleProfile(
                            id = o.optString("id", "p_$i"),
                            plate = o.optString("plate", "34 BCA 320"),
                            km = o.optInt("km", 375000),
                            fuel = o.optString("fuel", "Benzin + LPG"),
                            engineStatus = o.optString("engineStatus", "Orijinal Motor"),
                            engineDetailKm = o.optInt("engineDetailKm", 0),
                            transStatus = o.optString("transStatus", "Orijinal Şanzıman"),
                            transDetailKm = o.optInt("transDetailKm", 0),
                            tuneLevel = o.optString("tuneLevel", "Orijinal"),
                            aggressiveMods = o.optBoolean("aggressiveMods", false),
                            dynoHp = o.optInt("dynoHp", 0),
                            dynoNm = o.optInt("dynoNm", 0),
                            frontSize = o.optString("frontSize", "205/55 R16"),
                            frontBrand = o.optString("frontBrand", "Continental PremiumContact 7"),
                            frontSeason = o.optString("frontSeason", "Yaz Lastiği"),
                            frontYear = o.optInt("frontYear", 2024),
                            frontCond = o.optInt("frontCond", 85),
                            rearSize = o.optString("rearSize", "205/55 R16"),
                            rearBrand = o.optString("rearBrand", "Continental PremiumContact 7"),
                            rearSeason = o.optString("rearSeason", "Yaz Lastiği"),
                            rearYear = o.optInt("rearYear", 2024),
                            rearCond = o.optInt("rearCond", 85)
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    _profiles.value = list
                    val actId = prefs.getString(KEY_ACTIVE_ID, list.first().id)
                    _activeProfile.value = list.firstOrNull { it.id == actId } ?: list.first()
                    _needsInitialProfile.value = false
                }
            } catch (_: Exception) {}
        }

        val rawSnap = prefs.getString(KEY_SNAPSHOT, null)
        if (!rawSnap.isNullOrBlank()) {
            try {
                val o = JSONObject(rawSnap)
                _lastSnapshot.value = LastConnectedSnapshot(
                    plate = o.optString("plate", "34 BCA 320"),
                    fuel = o.optString("fuel", "Benzin + LPG"),
                    rpm = o.optDouble("rpm", 740.0),
                    ect = o.optDouble("ect", 90.0),
                    iat = o.optDouble("iat", 32.0),
                    map = o.optDouble("map", 28.5),
                    stft = o.optDouble("stft", 0.8),
                    ltft = o.optDouble("ltft", 2.1),
                    volt = o.optDouble("volt", 14.12),
                    savedAtMs = o.optLong("savedAtMs", System.currentTimeMillis())
                )
            } catch (_: Exception) {}
        }

        val rawOrder = prefs.getString(KEY_MENU_ORDER, null)
        if (!rawOrder.isNullOrBlank()) {
            val saved = rawOrder.split(",").filter { it.isNotBlank() }
            val defaults = _menuOrder.value
            val merged = saved.filter { defaults.contains(it) } + defaults.filter { !saved.contains(it) }
            _menuOrder.value = merged
        }
    }

    private fun persistProfiles() {
        val ctx = appContext ?: return
        val arr = JSONArray()
        for (p in _profiles.value) {
            arr.put(JSONObject().apply {
                put("id", p.id)
                put("plate", p.plate)
                put("km", p.km)
                put("fuel", p.fuel)
                put("engineStatus", p.engineStatus)
                put("engineDetailKm", p.engineDetailKm)
                put("transStatus", p.transStatus)
                put("transDetailKm", p.transDetailKm)
                put("tuneLevel", p.tuneLevel)
                put("aggressiveMods", p.aggressiveMods)
                put("dynoHp", p.dynoHp)
                put("dynoNm", p.dynoNm)
                put("frontSize", p.frontSize)
                put("frontBrand", p.frontBrand)
                put("frontSeason", p.frontSeason)
                put("frontYear", p.frontYear)
                put("frontCond", p.frontCond)
                put("rearSize", p.rearSize)
                put("rearBrand", p.rearBrand)
                put("rearSeason", p.rearSeason)
                put("rearYear", p.rearYear)
                put("rearCond", p.rearCond)
            })
        }
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_PROFILES, arr.toString())
            .putString(KEY_ACTIVE_ID, _activeProfile.value.id)
            .apply()
    }

    private fun saveLastConnectedSnapshot(rawMap: Map<String, Double>, volt: Double) {
        val rpm = rawMap["010C"] ?: return
        val p = _activeProfile.value
        val snap = LastConnectedSnapshot(
            plate = p.plate,
            fuel = p.fuel,
            rpm = rpm,
            ect = rawMap["0105"] ?: 90.0,
            iat = rawMap["010F"] ?: 32.0,
            map = rawMap["010B"] ?: 28.5,
            stft = rawMap["0106"] ?: 0.8,
            ltft = rawMap["0107"] ?: 2.1,
            volt = if (volt > 5.0) volt else 14.12,
            savedAtMs = System.currentTimeMillis()
        )
        _lastSnapshot.value = snap
        val ctx = appContext ?: return
        val obj = JSONObject().apply {
            put("plate", snap.plate)
            put("fuel", snap.fuel)
            put("rpm", snap.rpm)
            put("ect", snap.ect)
            put("iat", snap.iat)
            put("map", snap.map)
            put("stft", snap.stft)
            put("ltft", snap.ltft)
            put("volt", snap.volt)
            put("savedAtMs", snap.savedAtMs)
        }
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_SNAPSHOT, obj.toString())
            .apply()
    }

    fun saveProfile(profile: VehicleProfile) {
        val clean = profile.copy(
            id = profile.id.ifBlank { "p_${System.currentTimeMillis()}" },
            plate = profile.plate.trim().uppercase(Locale("tr", "TR"))
        )
        val existing = _profiles.value.toMutableList()
        val idx = existing.indexOfFirst { it.id == clean.id }
        if (idx >= 0) existing[idx] = clean else existing.add(clean)
        _profiles.value = existing
        _activeProfile.value = clean
        _needsInitialProfile.value = false
        persistProfiles()
        refreshEvaluations()
    }

    fun selectProfile(id: String) {
        val found = _profiles.value.firstOrNull { it.id == id } ?: return
        _activeProfile.value = found
        _needsInitialProfile.value = false
        persistProfiles()
        refreshEvaluations()
    }

    fun deleteProfile(id: String) {
        if (_profiles.value.size <= 1) return
        val next = _profiles.value.filter { it.id != id }
        _profiles.value = next
        if (_activeProfile.value.id == id) _activeProfile.value = next.first()
        persistProfiles()
        refreshEvaluations()
    }

    fun moveMenuItem(index: Int, delta: Int) {
        val list = _menuOrder.value.toMutableList()
        val target = index + delta
        if (index !in list.indices || target !in list.indices) return
        val item = list.removeAt(index)
        list.add(target, item)
        _menuOrder.value = list
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()
            ?.putString(KEY_MENU_ORDER, list.joinToString(","))?.apply()
    }

    fun setCustomSensorName(pidHex: String, newName: String) {
        _customNames.update { cur ->
            if (newName.isBlank()) cur - pidHex else cur + (pidHex to newName.trim())
        }
    }

    fun setSimScenario(scenario: SimScenario) {
        _simScenario.value = scenario
    }

    fun resetDriveData() {
        _trackedKm.value = 0
        _trackedHours.value = 0.0
        hpWindow.clear()
        nmWindow.clear()
        verifiedPeakHp = 0
        verifiedPeakNm = 0
        verifiedPeakWhp = 0
        verifiedPeakWnm = 0
        fuelSamples10s.clear()
        refreshEvaluations()
    }

    fun playNotificationSound(onDone: (() -> Unit)? = null) {
        val ctx = appContext ?: run { onDone?.invoke(); return }
        try {
            val mp = MediaPlayer.create(ctx, R.raw.notification)
            if (mp == null) {
                onDone?.invoke()
                return
            }
            mp.setOnCompletionListener {
                it.release()
                onDone?.invoke()
            }
            mp.setOnErrorListener { player, _, _ ->
                player.release()
                onDone?.invoke()
                true
            }
            mp.start()
        } catch (_: Exception) {
            onDone?.invoke()
        }
    }

    private fun speakTurkish(text: String) {
        try {
            if (ttsReady) {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cl9_meteo")
            }
        } catch (_: Exception) {}
    }

    fun triggerMeteorologicalHazard(alertText: String) {
        if (alertText.isBlank()) return
        val eval = evaluateBannerBase()
        if (eval.level == AlertLevel.RED || eval.level == AlertLevel.ORANGE) return
        meteoJob?.cancel()
        activeMeteoText = alertText
        meteoRemainingSec = 5
        updateBannerFromBase(eval)
        playNotificationSound {
            if (activeMeteoText != null) speakTurkish(alertText)
        }
        meteoJob = viewModelScope.launch {
            for (sec in 5 downTo 1) {
                meteoRemainingSec = sec
                updateBannerFromBase(evaluateBannerBase())
                delay(1000)
            }
            activeMeteoText = null
            meteoRemainingSec = 0
            updateBannerFromBase(evaluateBannerBase())
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            _pairedDevices.value = adapter?.bondedDevices?.toList().orEmpty()
        } catch (_: Exception) {
            _pairedDevices.value = emptyList()
        }
    }

    fun togglePinnedPid(pidHex: String) {
        _pinnedPids.update { cur ->
            if (cur.contains(pidHex)) cur - pidHex else cur + pidHex
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        stopPolling()
        viewModelScope.launch {
            _connectionState.value = ConnectionState(
                phase = ConnectionPhase.CONNECTING_BT,
                deviceName = device.name ?: "ELM327",
                deviceAddress = device.address,
                statusMessage = "Bluetooth RFCOMM bağlantısı kuruluyor..."
            )

            val btOk = elm.connectBluetooth(device) { entry -> appendLog(entry) }
            if (!btOk) {
                _connectionState.update {
                    it.copy(phase = ConnectionPhase.ERROR, statusMessage = "Bluetooth bağlantısı kurulamadı.")
                }
                return@launch
            }

            _connectionState.update {
                it.copy(phase = ConnectionPhase.ANALYZING_ADAPTER, statusMessage = "Adaptör kalitesi ve protokoller analiz ediliyor...")
            }

            val res = elm.runAdaptiveAnalysis { entry -> appendLog(entry) }
            if (res.success && res.lockedProfile != null) {
                _supportedPids.value = res.supportedPids
                _ecuIdentity.update { it.copy(ecuModel = res.ecuHeaderInfo) }
                val activeCount = _pinnedPids.value.count { !it.startsWith("DER_") }.coerceAtLeast(1)
                _connectionState.update {
                    it.copy(
                        phase = ConnectionPhase.CONNECTED,
                        adapterVersion = res.adapterVersion,
                        adapterTier = res.tier,
                        activeProfile = res.lockedProfile,
                        avgLatencyMs = res.avgLatencyMs,
                        pollingHz = K24A3PidEngine.estimateKLineHz(activeCount, res.avgLatencyMs),
                        batteryVoltage = res.batteryVoltage,
                        statusMessage = res.message
                    )
                }
                readEcuIdentity()
                readEmissionsAndMode06()
                startLivePolling()
            } else {
                _connectionState.update {
                    it.copy(
                        phase = ConnectionPhase.ERROR,
                        adapterVersion = res.adapterVersion,
                        adapterTier = res.tier,
                        batteryVoltage = res.batteryVoltage,
                        statusMessage = res.message
                    )
                }
            }
        }
    }

    fun startDemoMode(tier: AdapterTier = AdapterTier.TIER_1_PIC18F25K80) {
        stopPolling()
        elm.disconnect()
        _needsInitialProfile.value = false
        hpWindow.clear(); nmWindow.clear()
        verifiedPeakHp = 0; verifiedPeakNm = 0; verifiedPeakWhp = 0; verifiedPeakWnm = 0
        fuelSamples10s.clear()
        _dtcs.value = emptyList()

        val profile = when (tier) {
            AdapterTier.TIER_1_PIC18F25K80 -> Elm327ConnectionManager.PROTOCOL_CASCADE[0]
            AdapterTier.TIER_2_CLONE_V15 -> Elm327ConnectionManager.PROTOCOL_CASCADE[1]
            else -> Elm327ConnectionManager.PROTOCOL_CASCADE[2]
        }
        val latency = when (tier) {
            AdapterTier.TIER_1_PIC18F25K80 -> 135
            AdapterTier.TIER_2_CLONE_V15 -> 240
            else -> 410
        }
        val snap = _lastSnapshot.value
        val snapNote = if (snap != null) "Son bağlanılan araç (${snap.plate}) verileriyle simülasyon" else "Simülasyon Modu aktif • ${profile.shortCode}"
        _connectionState.value = ConnectionState(
            phase = ConnectionPhase.DEMO_MODE,
            deviceName = if (snap != null) "Simülasyon (${snap.plate})" else "CL9 K24A3 Simülasyon",
            adapterVersion = if (tier == AdapterTier.TIER_3_CLONE_V21) "ELM327 v2.1 (Klon)" else "ELM327 v1.5",
            adapterTier = tier,
            activeProfile = profile,
            avgLatencyMs = latency,
            pollingHz = K24A3PidEngine.estimateKLineHz(5, latency),
            batteryVoltage = snap?.volt ?: 14.12,
            statusMessage = snapNote,
            logs = listOf(
                ConnectionLogEntry("SİM", "ATPPS / ATAL", tier.label, tier == AdapterTier.TIER_1_PIC18F25K80),
                ConnectionLogEntry("SİM", "ATH1", "Matsushita RBB PCM (48 6B 10) doğrulandı", true),
                ConnectionLogEntry("SİM", profile.shortCode, "41 00 BE 3F B8 13 (Kilitlendi • ${latency}ms)", true)
            )
        )
        _ecuIdentity.value = EcuIdentity("JHMCL96804C20XXXX", "37805-RBB-E530", "A4F19C2B", "Matsushita RBB PCM (48 6B 10)", ecuKm = _activeProfile.value.km, kmMismatchWarning = "")
        _readiness.value = listOf(
            ReadinessMonitor("Tekleme İzleme", true, true),
            ReadinessMonitor("Yakıt Sistemi", true, true),
            ReadinessMonitor("Kapsamlı Bileşenler", true, true),
            ReadinessMonitor("Katalitik Konvertör", true, true),
            ReadinessMonitor("Oksijen Sensörü", true, true),
            ReadinessMonitor("Oksijen Sensörü Isıtıcısı", true, true),
            ReadinessMonitor("Egzoz Geri Dönüşümü (EGR)", true, true),
            ReadinessMonitor("Yakıt Buhar Sistemi (EVAP)", true, false)
        )
        _mode06.value = listOf(
            Mode06Test("TID 0x01", "Ön Oksijen Sensörü Zengin-Fakir Eşiği", "0.99 λ", "0.90–1.10 λ", true),
            Mode06Test("TID 0x02", "Katalizör Oksijen Depolama Oranı", "0.34", "< 0.65", true),
            Mode06Test("TID 0x31", "EGR Akış Basınç Düşümü", "11.2 kPa", "> 7.5 kPa", true),
            Mode06Test("TID 0xA2", "Son 10 Sürüş Tekleme Sayısı", "0 adet", "< 15 adet", true)
        )
        _freezeFrame.value = emptyList()
        startDemoPolling()
    }

    fun loadSampleDtcs() {
        _dtcs.value = listOf(
            HondaDtcDatabase.lookup("P2138", false),
            HondaDtcDatabase.lookup("P0845", true)
        )
        _freezeFrame.value = listOf(
            FreezeFrameItem("Tetikleyen Kodlar", "P2138, P0845"),
            FreezeFrameItem("Motor Devri", "2150 d/dk"),
            FreezeFrameItem("Araç Hızı", "54 km/sa"),
            FreezeFrameItem("Motor Soğutma Sıvısı", "90 °C"),
            FreezeFrameItem("Emme Manifoldu Basıncı", "48 kPa"),
            FreezeFrameItem("Yakıt Düzeltmeleri", "+1.6% / +2.3%")
        )
        refreshEvaluations()
    }

    private fun startDemoPolling() {
        pollingJob = viewModelScope.launch {
            var tick = 0
            val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            while (true) {
                tick++
                val wave = sin(tick * 0.4)
                val snap = _lastSnapshot.value
                val baseIdleEct = snap?.ect ?: 90.0
                val baseIdleLtft = snap?.ltft ?: 2.1
                val baseIdleVolt = snap?.volt ?: 14.12

                val vals = when (_simScenario.value) {
                    SimScenario.IDLE -> doubleArrayOf(740.0 + wave * 28.0, baseIdleEct + wave * 0.4, 32.0 + wave * 0.5, 28.5 + wave * 1.1, 0.8 + wave * 0.9, baseIdleLtft, 1.00 + wave * 0.008, 0.0, 21.5 + wave * 1.1, 8.5 + wave * 0.8, 13.7, 0.72, baseIdleVolt + wave * 0.04)
                    SimScenario.CITY -> doubleArrayOf(2420.0 + wave * 40.0, 92.0 + wave * 0.4, 35.0 + wave * 0.5, 52.0 + wave * 1.5, -0.6 + wave * 0.9, baseIdleLtft, 0.99 + wave * 0.008, 62.0, 41.0 + wave * 1.5, 24.0 + wave * 0.8, 26.5, 0.68, 14.18 + wave * 0.04)
                    SimScenario.VTEC -> doubleArrayOf(5850.0 + wave * 95.0, 94.0 + wave * 0.4, 29.0 + wave * 0.5, 97.5 + wave * 1.0, 0.0, baseIdleLtft, 0.86 + wave * 0.008, 138.0, 91.5 + wave * 1.2, 26.5 + wave * 0.8, 86.0, 0.84, 14.04 + wave * 0.04)
                    SimScenario.COLD -> doubleArrayOf(1260.0 + wave * 25.0, 34.0 + wave * 0.4, 14.0 + wave * 0.5, 35.0 + wave * 1.0, 0.0, baseIdleLtft, 0.92 + wave * 0.008, 0.0, 30.2 + wave * 1.0, 13.0 + wave * 0.6, 16.4, 0.45, 14.32 + wave * 0.04)
                    SimScenario.OVERHEAT -> doubleArrayOf(920.0 + wave * 30.0, 108.0 + wave * 0.4, 54.0 + wave * 0.6, 33.5 + wave * 1.0, 4.2 + wave * 0.8, 5.8, 1.04 + wave * 0.008, 18.0, 34.0 + wave * 1.0, 6.5 + wave * 0.5, 15.2, 0.78, 13.45 + wave * 0.04)
                }
                val rpm = vals[0]; val ect = vals[1]; val iat = vals[2]; val map = vals[3]
                val stft = vals[4]; val ltft = vals[5]; val lambda = vals[6]; val vss = vals[7]
                val load = vals[8]; val adv = vals[9]; val tps = vals[10]; val b1s2 = vals[11]; val volt = vals[12]

                val stoich = if (_activeProfile.value.isLpg()) 15.5 else 14.7
                val rawMap = mapOf(
                    "010C" to rpm, "0105" to ect, "010B" to map, "0134" to lambda,
                    "0106" to stft, "0107" to ltft, "0103" to if (_simScenario.value == SimScenario.VTEC || _simScenario.value == SimScenario.COLD) 1.0 else 2.0,
                    "0142" to (volt - 0.14), "010D" to vss,
                    "0104" to load, "010E" to adv,
                    "010F" to iat, "0133" to 101.0, "0111" to tps, "0145" to (tps - 10.0).coerceAtLeast(1.0), "0147" to (tps + 0.1),
                    "0149" to (tps + 1.2), "014A" to ((tps + 1.2) / 2.0), "014C" to (tps * 0.25), "0115" to b1s2,
                    "0144" to 1.00, "012C" to if (vss > 30) 24.0 else 0.0, "012E" to 12.0, "0143" to (load * 0.96),
                    "011F" to (420.0 + tick), "0121" to if (_dtcs.value.isNotEmpty()) 14.0 else 0.0, "0130" to 18.0, "0131" to 642.0,
                    "DER_TFT" to (stft + ltft), "DER_AFR" to (lambda * stoich),
                    "DER_DTEMP" to (ect - iat), "DER_VDROP" to 0.14
                )

                _connectionState.update { it.copy(batteryVoltage = volt) }
                val formatted = K24A3PidEngine.allPids.associate { def ->
                    def.pidHex to K24A3PidEngine.formatValue(def, rawMap[def.pidHex] ?: 0.0)
                }
                _liveValues.value = formatted
                processTelemetryUpdate(rawMap, volt)

                if (tick % 12 == 1) saveLastConnectedSnapshot(rawMap, volt)

                if (_isRecordingCsv.value) {
                    val row = "${fmt.format(Date())},${rpm.toInt()},${ect.toInt()},${map.toInt()},${String.format(Locale.US, "%.2f", lambda)},${String.format(Locale.US, "%.1f", stft + ltft)},${String.format(Locale.US, "%.2f", volt)}"
                    _csvRows.update { (it + row).takeLast(1000) }
                }
                delay(550)
            }
        }
    }

    private fun startLivePolling() {
        pollingJob = viewModelScope.launch {
            val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            var cycleNum = 0
            while (elm.isConnected()) {
                cycleNum++
                val activeHardwarePids = _pinnedPids.value.filter { !it.startsWith("DER_") }
                    .ifEmpty { listOf("010C", "0105", "010B", "0134", "0142") }
                    .toMutableSet()
                activeHardwarePids.addAll(listOf("010C", "0105", "0104", "010D", "0106", "0107", "0134"))

                val supported = _supportedPids.value
                val filteredPids = if (supported.isNotEmpty()) {
                    activeHardwarePids.filter { supported.contains(it) }.ifEmpty { activeHardwarePids }
                } else activeHardwarePids

                val cycleStart = System.currentTimeMillis()
                val numeric = mutableMapOf<String, Double>()
                var errorsInCycle = 0

                for (pid in filteredPids) {
                    val raw = elm.sendCommand(pid, 1800L)
                    val parsed = K24A3PidEngine.parseRawResponse(pid, raw)
                    if (parsed != null) numeric[pid] = parsed else errorsInCycle++
                }

                if (cycleNum % 10 == 0) {
                    val v = elm.readBatteryVoltage()
                    if (v > 5.0) _connectionState.update { it.copy(batteryVoltage = v) }
                }

                if (errorsInCycle >= filteredPids.size) {
                    val newErr = _connectionState.value.consecutiveErrors + 1
                    _connectionState.update { it.copy(consecutiveErrors = newErr) }
                    if (newErr >= 3) {
                        val downgraded = elm.downgradeTimingProfile { appendLog(it) }
                        if (downgraded != null) {
                            _connectionState.update {
                                it.copy(activeProfile = downgraded, consecutiveErrors = 0, statusMessage = "Otomatik güvenli profile geçildi: ${downgraded.shortCode}")
                            }
                        }
                    }
                } else {
                    _connectionState.update { it.copy(consecutiveErrors = 0) }
                }

                val stoich = if (_activeProfile.value.isLpg()) 15.5 else 14.7
                if (numeric.containsKey("0106") && numeric.containsKey("0107")) {
                    numeric["DER_TFT"] = numeric["0106"]!! + numeric["0107"]!!
                }
                numeric["0134"]?.let { numeric["DER_AFR"] = it * stoich }
                if (numeric.containsKey("0105") && numeric.containsKey("010F")) {
                    numeric["DER_DTEMP"] = numeric["0105"]!! - numeric["010F"]!!
                }
                numeric["0142"]?.let { pcmV ->
                    val atrv = _connectionState.value.batteryVoltage
                    if (atrv > 5.0) numeric["DER_VDROP"] = atrv - pcmV
                }

                val updatedMap = _liveValues.value.toMutableMap()
                for ((k, v) in numeric) {
                    val def = K24A3PidEngine.allPids.firstOrNull { it.pidHex == k } ?: continue
                    updatedMap[k] = K24A3PidEngine.formatValue(def, v)
                }
                _liveValues.value = updatedMap
                processTelemetryUpdate(numeric, _connectionState.value.batteryVoltage)
                if (cycleNum % 8 == 1) saveLastConnectedSnapshot(numeric, _connectionState.value.batteryVoltage)

                val cycleMs = (System.currentTimeMillis() - cycleStart).coerceAtLeast(100L)
                _connectionState.update { it.copy(pollingHz = 1000.0 / cycleMs) }

                if (_isRecordingCsv.value && numeric.isNotEmpty()) {
                    val row = "${fmt.format(Date())},${numeric["010C"]?.toInt() ?: ""},${numeric["0105"]?.toInt() ?: ""},${numeric["010B"]?.toInt() ?: ""},${numeric["0134"] ?: ""},${numeric["DER_TFT"] ?: ""},${numeric["0142"] ?: ""}"
                    _csvRows.update { (it + row).takeLast(1500) }
                }
                delay(50)
            }
        }
    }

    private fun refreshEvaluations() {
        val rawMap = _liveValues.value.mapValues { it.value.value }
        if (rawMap.isNotEmpty()) {
            processTelemetryUpdate(rawMap, _connectionState.value.batteryVoltage)
        } else {
            updateBannerFromBase(evaluateBannerBase())
        }
    }

    private fun median3(list: List<Int>): Int {
        val s = list.sorted()
        return s[s.size / 2]
    }

    private fun processTelemetryUpdate(raw: Map<String, Double>, volt: Double) {
        val p = _activeProfile.value
        val rpm = raw["010C"] ?: 0.0
        val loadFrac = (raw["0104"] ?: 20.0) / 100.0
        val vss = raw["010D"] ?: 0.0
        val ect = raw["0105"] ?: 90.0
        val tps = raw["0111"] ?: 14.0
        val adv = raw["010E"] ?: 8.5
        val mapVal = raw["010B"] ?: 29.0
        val b1s2 = raw["0115"] ?: 0.72
        val tftRaw = raw["DER_TFT"] ?: 0.0
        val tftAbs = abs(tftRaw)

        // 1. Motor Performansı (Anlık + 3 Ölçümlü Medyan Pik Filtresi)
        val engKm = when (p.engineStatus) {
            "Revizyonlu" -> max(0, p.km - p.engineDetailKm)
            "Çıkma Motor" -> if (p.engineDetailKm > 0) p.engineDetailKm else 180000
            else -> p.km
        }
        val healthFactor = (0.99 - (engKm / 3800000.0)).coerceIn(0.84, 0.98)
        val tuneHpMult = when (p.tuneLevel) {
            "Agresif" -> 1.18
            "Stage 1" -> 1.10
            else -> 1.0
        }
        val tuneNmMult = when (p.tuneLevel) {
            "Agresif" -> 1.13
            "Stage 1" -> 1.08
            else -> 1.0
        }
        val modHp = if (p.aggressiveMods) 1.07 else 1.0
        val modNm = if (p.aggressiveMods) 1.06 else 1.0
        val wheelLoss = 0.83

        val estPeakHp = if (p.aggressiveMods && p.dynoHp > 0) p.dynoHp else (190.0 * healthFactor * tuneHpMult * modHp).roundToInt()
        val estPeakNm = if (p.aggressiveMods && p.dynoNm > 0) p.dynoNm else (223.0 * healthFactor * tuneNmMult * modNm).roundToInt()
        val scaleHp = estPeakHp / 190.0
        val scaleNm = estPeakNm / 223.0

        val tqBase = when {
            rpm < 2500 -> 165.0
            rpm < 4500 -> 210.0
            rpm < 6000 -> 220.0
            else -> 198.0
        }
        val liveNm = (tqBase * loadFrac * scaleNm).roundToInt().coerceAtLeast(0)
        val liveHp = ((liveNm * rpm) / 7127.0).roundToInt().coerceAtLeast(0)
        val liveWhp = (liveHp * wheelLoss).roundToInt()
        val liveWnm = (liveNm * wheelLoss).roundToInt()

        val maxValidHp = (estPeakHp * 1.15).roundToInt()
        val maxValidNm = (estPeakNm * 1.15).roundToInt()
        if (liveHp in 1..maxValidHp && liveNm in 1..maxValidNm) {
            hpWindow.addLast(liveHp); if (hpWindow.size > 3) hpWindow.removeFirst()
            nmWindow.addLast(liveNm); if (nmWindow.size > 3) nmWindow.removeFirst()
            if (hpWindow.size == 3) {
                val medHp = median3(hpWindow.toList())
                val medNm = median3(nmWindow.toList())
                val jumpHp = (hpWindow.maxOrNull() ?: 0) - (hpWindow.minOrNull() ?: 0)
                val jumpNm = (nmWindow.maxOrNull() ?: 0) - (nmWindow.minOrNull() ?: 0)
                if (jumpHp <= max(25, (medHp * 0.28).roundToInt()) && medHp > verifiedPeakHp) {
                    verifiedPeakHp = medHp
                    verifiedPeakWhp = (medHp * wheelLoss).roundToInt()
                }
                if (jumpNm <= max(30, (medNm * 0.28).roundToInt()) && medNm > verifiedPeakNm) {
                    verifiedPeakNm = medNm
                    verifiedPeakWnm = (medNm * wheelLoss).roundToInt()
                }
            }
        }

        val crankHpCurve = listOf(32, 65, 102, 141, 168, 186, 190).map { (it * scaleHp).roundToInt() }
        val crankNmCurve = listOf(150, 185, 208, 223, 218, 204, 190).map { (it * scaleNm).roundToInt() }
        _perfMetrics.value = PerfMetrics(
            hasData = true,
            liveHp = liveHp,
            liveNm = liveNm,
            peakHp = verifiedPeakHp,
            peakNm = verifiedPeakNm,
            liveWhp = liveWhp,
            liveWnm = liveWnm,
            peakWhp = verifiedPeakWhp,
            peakWnm = verifiedPeakWnm,
            estCeilingHp = estPeakHp,
            estCeilingNm = estPeakNm,
            crankCurveHp = crankHpCurve,
            crankCurveNm = crankNmCurve,
            wheelCurveHp = crankHpCurve.map { (it * wheelLoss).roundToInt() },
            wheelCurveNm = crankNmCurve.map { (it * wheelLoss).roundToInt() },
            aiComment = "${p.tuneLevel}${if (p.aggressiveMods) " + Agresif Mod" else ""} profili ve ${engKm} km motor geçmişiyle krankta ~$estPeakHp BG / $estPeakNm Nm, 5AT MCTA şanzıman kaybı sonrası tekerlekte ~${(estPeakHp * wheelLoss).roundToInt()} WHP / ${(estPeakNm * wheelLoss).roundToInt()} Nm hesaplanıyor."
        )

        // 2. Yakıt Tüketimi Yorumlaması (BMW Ekonomi + Tahmini Oktan + Zonguldak TL + GPS Rakım)
        val isLpg = p.isLpg()
        val fuelLbl = p.activeFuelName()
        val mult = (if (isLpg) 1.18 else 1.0) * (if (p.tuneLevel == "Agresif") 1.07 else 1.0)
        val unitPrice = if (isLpg) PRICE_LPG_TL else PRICE_BENZIN_TL

        val rawInst = if (vss > 5.0) (4.8 + loadFrac * 13.5) else (1.0 + loadFrac * 1.6)
        val instDisp = (rawInst * mult * 10.0).roundToInt() / 10.0
        fuelSamples10s.addLast(rawInst); if (fuelSamples10s.size > 18) fuelSamples10s.removeFirst()
        val avg10s = ((fuelSamples10s.average()) * mult * 10.0).roundToInt() / 10.0
        val tripAvg = (9.8 * mult * 10.0).roundToInt() / 10.0
        val allAvg = (10.4 * mult * 10.0).roundToInt() / 10.0

        val fuelQual = (96 - (if (rpm > 5200) 5 else 0) - (if (adv < 7.5) 12 else 0)).coerceIn(62, 99)
        val combQual = (98 - (tftAbs * 2.2).roundToInt() - (if (ect < 70) 6 else 0)).coerceIn(65, 99)
        val baseOct = if (isLpg) 105 else 95
        val estOctane = (baseOct - ((96 - min(96, fuelQual)) * 0.22) + (if (fuelQual >= 95) 1 else 0)).roundToInt()

        val cost100 = (tripAvg * unitPrice).roundToInt()
        val cost1000 = (allAvg * 10.0 * unitPrice).roundToInt()

        val abnormalFuel = tftAbs >= 9.0 || ect >= 103.0 || (rpm in 401.0..949.0 && vss == 0.0 && mapVal > 38.0) || (ect >= 80.0 && (b1s2 < 0.15 || b1s2 > 0.90))
        val (aiTone, aiComment) = when {
            abnormalFuel -> {
                val suspects = mutableListOf<String>()
                if (tftAbs >= 9.0 || b1s2 < 0.15 || b1s2 > 0.90) suspects.add("ön/arka oksijen sensörü")
                if (mapVal > 38.0 && vss == 0.0) suspects.add("emme manifoldu basınç sensörü (MAP)")
                if (ect >= 103.0) suspects.add("motor soğutma sıvısı sıcaklık sensörü")
                suspects.add("$fuelLbl enjektör / yakıt ayarı")
                "Anormal Tüketim" to "Sürüş tarzınız ve son 100 km GPS rakım değişimine (+$gpsGainM m yükselme / -$gpsLossM m alçalma) göre normalin üzerinde bir $fuelLbl tüketimi tespit edildi. Muhtemel arızalı parçalar: ${suspects.joinToString(", ")}. Aracı kontrol ettirmeniz önerilir."
            }
            rpm >= 4500 || tps >= 60 || loadFrac >= 0.75 -> {
                "Eleştiri" to "Gaz pedalı kontrolünüz ve son 100 km GPS rakım değişimine (+$gpsGainM m yükselme / -$gpsLossM m alçalma) bağlı motor yükünüz oldukça agresif. Özellikle tırmanışlarda ani gaz verip yüksek devirlere çıkmanız $fuelLbl harcamanızı gereksiz yere artırıyor."
            }
            ect < 70.0 -> {
                "Tavsiye" to "Motor henüz çalışma sıcaklığına tam ulaşmadığı için ısınma sürecinde ekstra $fuelLbl tüketiliyor. Motor ısınana kadar yokuşlarda gaz pedalına yumuşak basmanız hem tüketimi hem aşınmayı azaltır."
            }
            else -> {
                "Övgü" to "Gaz pedalı kontrolünüz ve son 100 km GPS rakım değişimine (+$gpsGainM m yükselme / -$gpsLossM m alçalma) bağlı motor yükünüz mükemmel. Dengeli sürüş tarzınız sayesinde $fuelLbl tüketiminizi çok verimli bir seviyede tutuyorsunuz."
            }
        }

        _fuelMetrics.value = FuelMetrics(
            hasData = true,
            instL = instDisp,
            isIdleUnit = vss <= 5.0,
            avg10sL = avg10s,
            tripAvgL = tripAvg,
            allTimeAvgL = allAvg,
            fuelQualityPct = fuelQual,
            combQualityPct = combQual,
            estOctane = estOctane,
            cityName = CITY_NAME,
            unitPriceTl = unitPrice,
            activeFuelName = fuelLbl,
            cost100Tl = cost100,
            cost1000Tl = cost1000,
            gpsGainM = gpsGainM,
            gpsLossM = gpsLossM,
            aiTone = aiTone,
            aiComment = aiComment
        )

        // 3. Bilgilendirme Barı Güncellemesi
        updateBannerFromBase(evaluateBannerBase())
    }

    private fun evaluateBannerBase(): HomeBannerState {
        val conn = _connectionState.value
        val connected = conn.phase == ConnectionPhase.CONNECTED || conn.phase == ConnectionPhase.DEMO_MODE
        val p = _activeProfile.value
        val fuelLbl = p.activeFuelName()
        val live = _liveValues.value
        val ect = if (connected) live["0105"]?.value ?: 0.0 else 0.0
        val rpm = if (connected) live["010C"]?.value ?: 0.0 else 0.0
        val vss = if (connected) live["010D"]?.value ?: 0.0 else 0.0
        val map = if (connected) live["010B"]?.value ?: 0.0 else 0.0
        val iat = if (connected) live["010F"]?.value ?: 0.0 else 0.0
        val load = if (connected) live["0104"]?.value ?: 0.0 else 0.0
        val appD = if (connected) live["0149"]?.value ?: 0.0 else 0.0
        val appE = if (connected) live["014A"]?.value ?: 0.0 else 0.0
        val b1s2 = if (connected) live["0115"]?.value ?: 0.72 else 0.72
        val tft = if (connected) live["DER_TFT"]?.value ?: 0.0 else 0.0
        val afr = if (connected) live["DER_AFR"]?.value ?: 0.0 else 0.0
        val dtemp = if (connected) live["DER_DTEMP"]?.value ?: 50.0 else 50.0
        val volt = if (connected) conn.batteryVoltage else 0.0
        val dtcs = _dtcs.value
        val criticalCodes = setOf("P0217", "P0300", "P0301", "P0302", "P0303", "P0304")

        if (connected && ect >= 103.0) return HomeBannerState("red_ect", AlertLevel.RED, "Kritik", "Motor sıcaklığı çok yüksek (${ect.toInt()} °C), motor zarar görebilir kontrol edin!")
        if (connected && volt > 0.0 && (volt < 11.8 || volt > 15.1)) return HomeBannerState("red_volt", AlertLevel.RED, "Kritik", "Şarj voltajı kritik seviyede (${String.format(Locale.US, "%.2f", volt)} V), alternatör ve aküyü hemen kontrol edin!")
        if (connected && (abs(tft) > 18.0 || (afr > 16.5 && load > 50.0))) return HomeBannerState("red_afr", AlertLevel.RED, "Kritik", "$fuelLbl yakıt-hava karışımı kritik sınırda sapma gösteriyor, hemen kontrol edin!")
        if (connected && appD > 0.0 && appE > 0.0 && abs(appD - appE * 2.0) > 8.0) return HomeBannerState("red_app", AlertLevel.RED, "Kritik", "Gaz pedalı sensörlerinde (D/E) kritik uyumsuzluk tespit edildi, kontrol edin!")
        if (connected && iat >= 70.0) return HomeBannerState("red_iat", AlertLevel.RED, "Kritik", "Emme havası sıcaklığı kritik seviyede (${iat.toInt()} °C), vuruntu riski var!")
        if (connected && rpm in 401.0..949.0 && vss == 0.0 && map > 48.0) return HomeBannerState("red_map", AlertLevel.RED, "Kritik", "Rölanti manifold basıncı kritik derecede yüksek (${map.toInt()} kPa), kontrol edin!")
        if (connected && dtcs.any { criticalCodes.contains(it.code) }) {
            val crit = dtcs.filter { criticalCodes.contains(it.code) }.joinToString("; ") { "${it.descriptionTr} (muhtemel sebep: ${it.cl9Note})" }
            return HomeBannerState("red_dtc", AlertLevel.RED, "Kritik", "Kritik arıza tespit edildi: $crit Aracı hemen kontrol edin!")
        }

        if (connected && ect > 0.0 && ect < 70.0) return HomeBannerState("ora_cold", AlertLevel.ORANGE, "Bilgi", "Motor henüz çalışma sıcaklığına ulaşmadı (${ect.toInt()} °C), ısınana kadar sakin sürün.")
        if (connected && dtcs.isNotEmpty()) {
            val descList = dtcs.joinToString("; ") { "${it.descriptionTr} (muhtemel sebep: ${it.cl9Note})" }
            return HomeBannerState("ora_dtc", AlertLevel.ORANGE, "Bilgi", "Tespit edilen durum: $descList Uygun zamanda kontrol ettirmeniz önerilir.")
        }
        if (connected && iat >= 50.0) return HomeBannerState("ora_iat", AlertLevel.ORANGE, "Bilgi", "Emme havası sıcaklığı yüksek (${iat.toInt()} °C), performans hafif düşebilir.")
        if (connected && abs(tft) >= 8.0) return HomeBannerState("ora_tft", AlertLevel.ORANGE, "Bilgi", "Toplam yakıt düzeltmesi (%${String.format(Locale.US, "%.1f", tft)}) hafif sapma gösteriyor, $fuelLbl ayarını kontrol ettirebilirsiniz.")
        if (connected && ect >= 80.0 && (b1s2 < 0.15 || b1s2 > 0.90)) return HomeBannerState("ora_b1s2", AlertLevel.ORANGE, "Bilgi", "Arka oksijen sensörü katalizör verimliliğinde dalgalanma gösteriyor.")
        if (connected && ect >= 80.0 && dtemp < 25.0) return HomeBannerState("ora_dtemp", AlertLevel.ORANGE, "Bilgi", "Soğutma sıvısı ile emme havası sıcaklık farkı düşük (${dtemp.toInt()} °C).")
        if (connected && (conn.adapterTier == AdapterTier.TIER_2_CLONE_V15 || conn.adapterTier == AdapterTier.TIER_3_CLONE_V21)) return HomeBannerState("ora_adapter", AlertLevel.ORANGE, "Bilgi", "OBD-II adaptörü yavaş yanıt veriyor (${conn.avgLatencyMs} ms).")
        if (min(p.frontCond, p.rearCond) < 45 || (2026 - min(p.frontYear, p.rearYear)) >= 7) return HomeBannerState("ora_tire", AlertLevel.ORANGE, "Bilgi", "Lastik kondisyonu veya üretim yaşı düşük görünüyor, lastiklerinizi gözden geçirmeniz önerilir.")

        return HomeBannerState("ok", AlertLevel.GREEN, "İyi Durumda", "Hoş geldiniz, arabanın kondisyonu iyi görünüyor, sürüşe devam edebilirsiniz.")
    }

    private fun updateBannerFromBase(base: HomeBannerState) {
        if (base.level == AlertLevel.RED || base.level == AlertLevel.ORANGE) {
            if (activeMeteoText != null) {
                activeMeteoText = null
                meteoJob?.cancel()
                tts?.stop()
            }
            if (lastBannerCode != base.code) {
                playNotificationSound()
            }
            lastBannerCode = base.code
            _homeBanner.value = base
        } else if (activeMeteoText != null) {
            lastBannerCode = "blue_meteo"
            _homeBanner.value = HomeBannerState(
                code = "blue_meteo",
                level = AlertLevel.BLUE,
                badge = "MGM / AFAD (${meteoRemainingSec}s)",
                message = activeMeteoText!!,
                meteoRemainingSec = meteoRemainingSec
            )
        } else {
            lastBannerCode = base.code
            _homeBanner.value = base
        }
    }

    fun readDtcs() {
        if (_connectionState.value.phase == ConnectionPhase.DEMO_MODE) return
        viewModelScope.launch {
            val storedRaw = elm.sendCommand("03", 3000L)
            val pendingRaw = elm.sendCommand("07", 3000L)
            val list = HondaDtcDatabase.parseDtcResponse(storedRaw, "43", false) +
                HondaDtcDatabase.parseDtcResponse(pendingRaw, "47", true)
            _dtcs.value = list
            if (list.isNotEmpty()) readFreezeFrame()
            refreshEvaluations()
        }
    }

    fun readFreezeFrame() {
        if (_connectionState.value.phase == ConnectionPhase.DEMO_MODE) return
        viewModelScope.launch {
            val items = mutableListOf<FreezeFrameItem>()
            val rpmRaw = elm.sendCommand("020C00", 2000L)
            val vssRaw = elm.sendCommand("020D00", 2000L)
            val ectRaw = elm.sendCommand("020500", 2000L)
            val mapRaw = elm.sendCommand("020B00", 2000L)
            K24A3PidEngine.parseRawResponse("010C", rpmRaw.replace("42", "41"))?.let { items.add(FreezeFrameItem("Motor Devri", "${it.toInt()} d/dk")) }
            K24A3PidEngine.parseRawResponse("010D", vssRaw.replace("42", "41"))?.let { items.add(FreezeFrameItem("Araç Hızı", "${it.toInt()} km/sa")) }
            K24A3PidEngine.parseRawResponse("0105", ectRaw.replace("42", "41"))?.let { items.add(FreezeFrameItem("Motor Soğutma Sıvısı", "${it.toInt()} °C")) }
            K24A3PidEngine.parseRawResponse("010B", mapRaw.replace("42", "41"))?.let { items.add(FreezeFrameItem("Emme Manifoldu Basıncı", "${it.toInt()} kPa")) }
            _freezeFrame.value = items
        }
    }

    fun readEmissionsAndMode06() {
        if (_connectionState.value.phase == ConnectionPhase.DEMO_MODE) return
        viewModelScope.launch {
            val r01 = elm.sendCommand("0101", 2500L).replace(" ", "").uppercase(Locale.US)
            val idx = r01.indexOf("4101")
            if (idx != -1 && r01.length >= idx + 12) {
                val b = r01.substring(idx + 6, idx + 8).toIntOrNull(16) ?: 0
                val c = r01.substring(idx + 8, idx + 10).toIntOrNull(16) ?: 0
                val d = r01.substring(idx + 10, idx + 12).toIntOrNull(16) ?: 0
                _readiness.value = listOf(
                    ReadinessMonitor("Tekleme İzleme", (b and 0x01) != 0, (b and 0x10) == 0),
                    ReadinessMonitor("Yakıt Sistemi", (b and 0x02) != 0, (b and 0x20) == 0),
                    ReadinessMonitor("Kapsamlı Bileşenler", (b and 0x04) != 0, (b and 0x40) == 0),
                    ReadinessMonitor("Katalitik Konvertör", (c and 0x01) != 0, (d and 0x01) == 0),
                    ReadinessMonitor("Yakıt Buhar Sistemi (EVAP)", (c and 0x04) != 0, (d and 0x04) == 0),
                    ReadinessMonitor("Oksijen Sensörü", (c and 0x20) != 0, (d and 0x20) == 0),
                    ReadinessMonitor("Oksijen Sensörü Isıtıcısı", (c and 0x40) != 0, (d and 0x40) == 0),
                    ReadinessMonitor("Egzoz Geri Dönüşümü (EGR)", (c and 0x80) != 0, (d and 0x80) == 0)
                ).filter { it.supported }
            }
            val m06Raw = elm.sendCommand("0601", 2500L)
            if (m06Raw.isNotBlank() && !m06Raw.contains("NO DATA", true)) {
                _mode06.value = listOf(Mode06Test("Mode 06", "Ham Eşik Testi Yanıtı", m06Raw.take(28), "Aktif", true))
            }
        }
    }

    fun readEcuIdentity() {
        if (_connectionState.value.phase == ConnectionPhase.DEMO_MODE) return
        viewModelScope.launch {
            val vinRaw = elm.sendCommand("0902", 3500L)
            val calRaw = elm.sendCommand("0904", 3500L)
            val cvnRaw = elm.sendCommand("0906", 3500L)
            val odoRaw = elm.sendCommand("01A6", 2000L)
            val dClearRaw = elm.sendCommand("0131", 2000L)
            val dMilRaw = elm.sendCommand("0121", 2000L)

            val parsedOdoKm = parseOdometer01A6(odoRaw)
            val parsedSinceClear = K24A3PidEngine.parseRawResponse("0131", dClearRaw)?.toInt() ?: 0
            val parsedMilDist = K24A3PidEngine.parseRawResponse("0121", dMilRaw)?.toInt() ?: 0
            val profKm = _activeProfile.value.km
            val effectiveEcuKm = parsedOdoKm ?: profKm
            val mismatch = when {
                parsedOdoKm != null && abs(parsedOdoKm - profKm) > 1500 ->
                    "KM Tutarsızlığı Tespit Edildi: Motor beyni ($parsedOdoKm km) ile gösterge/profil kilometresi ($profKm km) arasında uyuşmazlık bulunuyor."
                parsedSinceClear > profKm || parsedMilDist > profKm ->
                    "KM Tutarsızlığı Tespit Edildi: ECU mesafe sayaçları ($parsedSinceClear km / $parsedMilDist km) profil kilometresinden ($profKm km) yüksek görünüyor."
                else -> ""
            }
            _ecuIdentity.update {
                it.copy(
                    vin = decodeAsciiPayload(vinRaw, "4902").ifBlank { it.vin },
                    calId = decodeAsciiPayload(calRaw, "4904").ifBlank { it.calId },
                    cvn = cvnRaw.replace(" ", "").takeLast(8).ifBlank { it.cvn },
                    ecuKm = effectiveEcuKm,
                    kmMismatchWarning = mismatch
                )
            }
        }
    }

    private fun parseOdometer01A6(raw: String): Int? {
        val clean = raw.replace(" ", "").uppercase(Locale.US)
        val idx = clean.indexOf("41A6")
        if (idx == -1 || clean.length < idx + 12) return null
        val hexVal = clean.substring(idx + 4, idx + 12).toLongOrNull(16) ?: return null
        return (hexVal / 10L).toInt()
    }

    private fun decodeAsciiPayload(raw: String, marker: String): String {
        val clean = raw.replace(" ", "").uppercase(Locale.US)
        val idx = clean.indexOf(marker)
        if (idx == -1) return ""
        val hex = clean.substring(idx + 6)
        val sb = StringBuilder()
        var i = 0
        while (i + 1 < hex.length) {
            val code = hex.substring(i, i + 2).toIntOrNull(16)
            if (code != null && code in 32..126) sb.append(code.toChar())
            i += 2
        }
        return sb.toString().trim()
    }

    fun clearDtcs() {
        if (_connectionState.value.phase == ConnectionPhase.DEMO_MODE) {
            _dtcs.value = emptyList()
            _freezeFrame.value = emptyList()
            refreshEvaluations()
            return
        }
        viewModelScope.launch {
            elm.sendCommand("04", 3000L)
            _dtcs.value = emptyList()
            _freezeFrame.value = emptyList()
            refreshEvaluations()
        }
    }

    fun toggleCsvRecording() { _isRecordingCsv.update { !it } }
    fun clearCsvLogs() { _csvRows.value = emptyList() }

    fun saveAndShareCsv(context: Context): String {
        val rows = _csvRows.value
        if (rows.isEmpty()) return "Kaydedilecek satır yok."
        val csvText = "Saat,Devir,Sivi,Basinc,Lambda,ToplamYakit,Voltaj\n" + rows.joinToString("\n")
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(dir, "CL9_K24A3_OBD2_${System.currentTimeMillis()}.csv")
        file.writeText(csvText)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "CL9 K24A3 OBD2 Sürüş Kaydı")
            putExtra(Intent.EXTRA_TEXT, csvText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(shareIntent, "CSV Kaydını Paylaş"))
        return "Kaydedildi: ${file.name}"
    }

    fun disconnect() {
        stopPolling()
        meteoJob?.cancel()
        activeMeteoText = null
        elm.disconnect()
        _connectionState.value = ConnectionState()
        _liveValues.value = emptyMap()
        _perfMetrics.value = PerfMetrics()
        _fuelMetrics.value = FuelMetrics()
        hpWindow.clear(); nmWindow.clear()
        verifiedPeakHp = 0; verifiedPeakNm = 0; verifiedPeakWhp = 0; verifiedPeakWnm = 0
        refreshEvaluations()
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun appendLog(entry: ConnectionLogEntry) {
        _connectionState.update { it.copy(logs = (it.logs + entry).takeLast(80)) }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.shutdown()
        disconnect()
    }
}
