package com.cl9tr.obd2.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.cl9tr.obd2.model.AdapterTier
import com.cl9tr.obd2.model.ConnectionLogEntry
import com.cl9tr.obd2.model.ProtocolProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * v1.1 Adaptif ELM327 Bağlantı Motoru:
 * - Mutex ile tek şeritli seri K-Line erişimi (canlı polling ile DTC/Mode06/09 çakışmasını önler)
 * - Donanım parmak izi (ATI, AT@1, ATPPS, ATAL, ATRV)
 * - 6 kademeli adaptif protokol + 3-ping gecikme/kararlılık ölçümü
 * - Dinamik 01 00 / 01 20 / 01 40 bitmask PID keşfi
 * - ISO 9141-2 ATSW 92 keep-alive + ATH1 Matsushita PCM header kontrolü + ATCV voltaj kalibrasyonu
 */
class Elm327ConnectionManager {

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        val PROTOCOL_CASCADE: List<ProtocolProfile> = listOf(
            ProtocolProfile(1, "ISO 9141-2 Hızlı (PIC18F25K80)", "ATSP3 • ATAT2 • ATST32", "ATSP3", "ATAT2", "ATST32", AdapterTier.TIER_1_PIC18F25K80, "2004 CL9 K-Line için en yüksek yenileme hızı (~200 ms timeout)."),
            ProtocolProfile(2, "ISO 9141-2 Dengeli (Standart v1.5)", "ATSP3 • ATAT1 • ATST64", "ATSP3", "ATAT1", "ATST64", AdapterTier.TIER_2_CLONE_V15, "İyi kalite v1.5 klonlar için kararlı zamanlama (~400 ms timeout)."),
            ProtocolProfile(3, "ISO 9141-2 Güvenli (Zayıf Klon)", "ATSP3 • ATAT0 • ATST96", "ATSP3", "ATAT0", "ATST96", AdapterTier.TIER_3_CLONE_V21, "Uyarlamalı zamanlaması bozuk klonlar için sabit 600 ms zaman aşımı."),
            ProtocolProfile(4, "KWP2000 5-Baud Yedek", "ATSP4 • ATAT1 • ATST64", "ATSP4", "ATAT1", "ATST64", AdapterTier.TIER_2_CLONE_V15, "ISO 9141-2 başarısız olursa K-Line 5-baud KWP denemesi."),
            ProtocolProfile(5, "KWP2000 Fast Init Yedek", "ATSP5 • ATAT1 • ATST64", "ATSP5", "ATAT1", "ATST64", AdapterTier.TIER_2_CLONE_V15, "Hızlı başlatmalı KWP2000 yedek profili."),
            ProtocolProfile(6, "Otomatik Son Çare (Sahte v2.1)", "ATSP0 • ATAT0 • ATSTFF", "ATSP0", null, "ATSTFF", AdapterTier.TIER_3_CLONE_V21, "En kısıtlı klonlar için maksimum zaman aşımıyla otomatik protokol.")
        )
    }

    private val busMutex = Mutex()
    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    var activeProfileIndex: Int = 0
        private set

    fun isConnected(): Boolean = socket?.isConnected == true

    @SuppressLint("MissingPermission")
    suspend fun connectBluetooth(device: BluetoothDevice, onLog: (ConnectionLogEntry) -> Unit): Boolean =
        withContext(Dispatchers.IO) {
            disconnect()
            try { BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery() } catch (_: Exception) {}

            try {
                onLog(log("BT", "RFCOMM SPP soketi açılıyor: ${device.name ?: device.address}", true))
                val s = device.createRfcommSocketToServiceRecord(SPP_UUID)
                s.connect()
                socket = s
                input = s.inputStream
                output = s.outputStream
                onLog(log("BT", "Bluetooth SPP bağlantısı kuruldu.", true))
                true
            } catch (e: Exception) {
                onLog(log("BT", "Standart soket başarısız, yedek RFCOMM kanal 1 deneniyor...", false))
                try {
                    val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    val fallbackSocket = m.invoke(device, 1) as BluetoothSocket
                    fallbackSocket.connect()
                    socket = fallbackSocket
                    input = fallbackSocket.inputStream
                    output = fallbackSocket.outputStream
                    onLog(log("BT", "Yedek RFCOMM bağlantısı başarılı.", true))
                    true
                } catch (e2: Exception) {
                    onLog(log("BT", "Bluetooth bağlantı hatası: ${e2.localizedMessage}", false))
                    disconnect()
                    false
                }
            }
        }

    data class AdaptiveResult(
        val success: Boolean,
        val adapterVersion: String,
        val tier: AdapterTier,
        val lockedProfile: ProtocolProfile?,
        val avgLatencyMs: Int,
        val batteryVoltage: Double,
        val supportedPids: Set<String>,
        val ecuHeaderInfo: String,
        val message: String
    )

    suspend fun runAdaptiveAnalysis(onLog: (ConnectionLogEntry) -> Unit): AdaptiveResult =
        withContext(Dispatchers.IO) {
            val atz = sendCommand("ATZ", 2500L)
            onLog(log("ATZ", atz.ifBlank { "YANIT YOK" }, atz.isNotBlank()))
            delay(350)

            val ati = sendCommand("ATI", 1500L)
            onLog(log("ATI", ati.ifBlank { "Bilinmiyor" }, ati.isNotBlank()))

            val at1 = sendCommand("AT@1", 1200L)
            if (at1.isNotBlank() && !at1.contains("?")) onLog(log("AT@1", at1, true))

            sendAndLog("ATE0", onLog)
            sendAndLog("ATL0", onLog)
            sendAndLog("ATS0", onLog)
            sendAndLog("ATH0", onLog)

            val atrvRaw = sendCommand("ATRV", 1500L)
            val voltage = Regex("(\\d+\\.\\d+)").find(atrvRaw)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
            onLog(log("ATRV", if (voltage > 0) "${voltage}V" else atrvRaw, voltage > 5.0))

            val atpps = sendCommand("ATPPS", 2000L)
            val supportsPps = atpps.contains("00:") || atpps.contains("FF") || atpps.length > 25
            onLog(log("ATPPS", if (supportsPps) "DESTEKLENİYOR (PIC18F25K80)" else "DESTEKLENMİYOR ($atpps)", supportsPps))

            val atal = sendCommand("ATAL", 1500L)
            val supportsAl = atal.contains("OK", ignoreCase = true)
            onLog(log("ATAL", if (supportsAl) "OK" else "REDDEDİLDİ ($atal)", supportsAl))

            val isV21 = ati.contains("2.1", ignoreCase = true)
            val detectedTier = when {
                supportsPps && supportsAl && !isV21 -> AdapterTier.TIER_1_PIC18F25K80
                supportsAl && !isV21 -> AdapterTier.TIER_2_CLONE_V15
                else -> AdapterTier.TIER_3_CLONE_V21
            }
            onLog(log("ANALİZ", "Adaptör Sınıfı: ${detectedTier.label}", true))

            val startIndex = when (detectedTier) {
                AdapterTier.TIER_1_PIC18F25K80 -> 0
                AdapterTier.TIER_2_CLONE_V15 -> 1
                AdapterTier.TIER_3_CLONE_V21 -> 2
                else -> 0
            }

            val candidates = PROTOCOL_CASCADE.drop(startIndex) + PROTOCOL_CASCADE.take(startIndex)
            for (profile in candidates) {
                onLog(log("TEST", "Deneniyor -> ${profile.name} (${profile.shortCode})", true))
                sendAndLog(profile.protocolCmd, onLog)
                profile.adaptiveTimingCmd?.let { sendAndLog(it, onLog) }
                profile.timeoutCmd?.let { sendAndLog(it, onLog) }

                // ISO 9141-2 Keep-Alive (5 sn uyku koruması)
                if (profile.protocolCmd == "ATSP3" && detectedTier != AdapterTier.TIER_3_CLONE_V21) {
                    val sw = sendCommand("ATSW92", 1000L)
                    if (sw.contains("OK", true)) onLog(log("ATSW92", "ISO 9141-2 Keep-Alive aktif", true))
                }

                val initReply = sendCommand("0100", 6500L)
                val cleanInit = initReply.replace(" ", "").uppercase(Locale.US)
                if (cleanInit.contains("4100")) {
                    onLog(log("0100", "CL9 ECU YANIT VERDİ: $initReply", true))

                    var totalMs = 0L
                    var successPings = 0
                    for (ping in 1..3) {
                        val t0 = System.currentTimeMillis()
                        val r = sendCommand("010C", 2500L).replace(" ", "").uppercase(Locale.US)
                        val dt = System.currentTimeMillis() - t0
                        if (r.contains("410C")) {
                            successPings++
                            totalMs += dt
                        }
                    }

                    if (successPings >= 2) {
                        val avgMs = (totalMs / successPings).toInt()
                        activeProfileIndex = PROTOCOL_CASCADE.indexOfFirst { it.id == profile.id }.coerceAtLeast(0)

                        // Dinamik 01 00 / 01 20 / 01 40 bitmask PID keşfi
                        val discovered = mutableSetOf<String>()
                        discovered.addAll(parseBitmask(cleanInit, "4100", 0x01))
                        if (discovered.contains("0120")) {
                            val r20 = sendCommand("0120", 2500L).replace(" ", "").uppercase(Locale.US)
                            discovered.addAll(parseBitmask(r20, "4120", 0x21))
                        }
                        if (discovered.contains("0140")) {
                            val r40 = sendCommand("0140", 2500L).replace(" ", "").uppercase(Locale.US)
                            discovered.addAll(parseBitmask(r40, "4140", 0x41))
                        }
                        onLog(log("BİTMASK", "${discovered.size} adet desteklenen OBD-II PID keşfedildi.", true))

                        // ATH1 ile Matsushita PCM Header (48 6B 10) kontrolü
                        var headerInfo = "Standart OBD-II PCM"
                        if (sendCommand("ATH1", 1000L).contains("OK", true)) {
                            val hdrSample = sendCommand("0100", 2000L).uppercase(Locale.US)
                            if (hdrSample.contains("486B10") || hdrSample.contains("48 6B 10")) {
                                headerInfo = "Matsushita RBB PCM (Adres: 0x10 • 48 6B 10)"
                                onLog(log("ATH1", "Doğrulandı: $headerInfo", true))
                            }
                            sendCommand("ATH0", 1000L)
                        }

                        onLog(log("KİLİTLENDİ", "${profile.name} • Başarı: $successPings/3 • Ort. ${avgMs}ms", true))
                        return@withContext AdaptiveResult(
                            success = true,
                            adapterVersion = ati.ifBlank { "ELM327" },
                            tier = detectedTier,
                            lockedProfile = profile,
                            avgLatencyMs = avgMs,
                            batteryVoltage = voltage,
                            supportedPids = discovered,
                            ecuHeaderInfo = headerInfo,
                            message = "${profile.name} kilitlendi (${avgMs} ms)."
                        )
                    } else {
                        onLog(log("KARARSIZ", "${profile.name} 3-ping testinde koptu ($successPings/3), alt kademeye geçiliyor...", false))
                    }
                } else {
                    onLog(log("0100", "Başarısız ($initReply), sonraki protokol deneniyor...", false))
                }
            }

            AdaptiveResult(
                success = false,
                adapterVersion = ati.ifBlank { "ELM327" },
                tier = detectedTier,
                lockedProfile = null,
                avgLatencyMs = 0,
                batteryVoltage = voltage,
                supportedPids = emptySet(),
                ecuHeaderInfo = "-",
                message = "Kontak (ON/II) açık mı? Hiçbir protokolde CL9 ECU yanıt vermedi."
            )
        }

    private fun parseBitmask(cleanHex: String, header: String, basePid: Int): Set<String> {
        val idx = cleanHex.indexOf(header)
        if (idx == -1 || cleanHex.length < idx + 12) return emptySet()
        val maskHex = cleanHex.substring(idx + 4, idx + 12)
        val mask = maskHex.toLongOrNull(16) ?: return emptySet()
        val out = mutableSetOf<String>()
        for (bit in 0 until 32) {
            if ((mask and (1L shl (31 - bit))) != 0L) {
                val pidNum = basePid + bit
                out.add(String.format(Locale.US, "01%02X", pidNum))
            }
        }
        return out
    }

    suspend fun calibrateVoltage(targetVolts: Double): Boolean {
        val vInt = (targetVolts * 100).toInt().coerceIn(800, 1600)
        val cmd = String.format(Locale.US, "ATCV%04d", vInt)
        return sendCommand(cmd, 1500L).contains("OK", true)
    }

    suspend fun readBatteryVoltage(): Double {
        val raw = sendCommand("ATRV", 1500L)
        return Regex("(\\d+\\.\\d+)").find(raw)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    }

    suspend fun downgradeTimingProfile(onLog: (ConnectionLogEntry) -> Unit): ProtocolProfile? =
        withContext(Dispatchers.IO) {
            val nextIdx = activeProfileIndex + 1
            if (nextIdx >= PROTOCOL_CASCADE.size) return@withContext null
            val next = PROTOCOL_CASCADE[nextIdx]
            activeProfileIndex = nextIdx
            onLog(log("OTO-DÜŞÜRME", "Kararsızlık algılandı -> ${next.shortCode} geçiliyor", false))
            sendCommand(next.protocolCmd, 1500L)
            next.adaptiveTimingCmd?.let { sendCommand(it, 1200L) }
            next.timeoutCmd?.let { sendCommand(it, 1200L) }
            next
        }

    /**
     * Mutex korumalı tek-şeritli ELM327 komut gönderimi.
     */
    suspend fun sendCommand(cmd: String, timeoutMs: Long = 2500L): String = busMutex.withLock {
        withContext(Dispatchers.IO) {
            val out = output ?: return@withContext "NO_SOCKET"
            val inp = input ?: return@withContext "NO_SOCKET"
            try {
                while (inp.available() > 0) inp.read()
                out.write("$cmd\r".toByteArray(Charsets.US_ASCII))
                out.flush()

                val sb = StringBuilder()
                val deadline = System.currentTimeMillis() + timeoutMs
                while (System.currentTimeMillis() < deadline) {
                    if (inp.available() > 0) {
                        val b = inp.read()
                        if (b == -1) break
                        val ch = b.toChar()
                        if (ch == '>') break
                        sb.append(ch)
                    } else {
                        delay(12)
                    }
                }
                sb.toString()
                    .replace("\r", " ")
                    .replace("\n", " ")
                    .replace("SEARCHING...", "", ignoreCase = true)
                    .trim()
            } catch (e: Exception) {
                "ERR:${e.message}"
            }
        }
    }

    private suspend fun sendAndLog(cmd: String, onLog: (ConnectionLogEntry) -> Unit) {
        val res = sendCommand(cmd, 1500L)
        onLog(log(cmd, res, res.contains("OK", true)))
    }

    private fun log(cmd: String, resp: String, ok: Boolean): ConnectionLogEntry =
        ConnectionLogEntry(timeFmt.format(Date()), cmd, resp, ok)

    fun disconnect() {
        try { input?.close() } catch (_: Exception) {}
        try { output?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        input = null
        output = null
        socket = null
    }
}
