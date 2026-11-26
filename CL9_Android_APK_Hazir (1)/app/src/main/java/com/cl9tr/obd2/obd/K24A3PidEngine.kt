package com.cl9tr.obd2.obd

import com.cl9tr.obd2.model.LivePidValue
import com.cl9tr.obd2.model.PidCategory
import com.cl9tr.obd2.model.PidDefinition
import java.util.Locale

object K24A3PidEngine {

    val allPids: List<PidDefinition> = listOf(
        PidDefinition("010C", "(RPM) Motor Devri", "(RPM) Devir", "d/dk", PidCategory.ENGINE, "Rölanti: 650–750 d/dk", 0.0, 7500.0, true, false, "((A*256)+B)/4",
            "Krank mili sensöründen motorun dakikadaki dönüş hızını ölçer.",
            "Sıcak motorda 650–750 d/dk sabit olmalıdır; dalgalanma kelebek kirliliği veya vakum kaçağına işaret eder."),
        PidDefinition("0105", "(ECT) Motor Soğutma Sıvısı Sıcaklığı", "(ECT) Soğutma Sıvısı", "°C", PidCategory.AIR_TEMP, "Sıcak: 85–95 °C", -40.0, 125.0, true, false, "A - 40",
            "2004 CL9 K24A3'te 2 sensör vardır: Silindir kapağındaki ana ECT (OE 37870-PNA-003, OBD2 01 05 bunu okur) ve radyatör altındaki Fan Müşürü (OE 37760-P00-003).",
            "Normal çalışma 85–95 °C'dir. Radyatör müşürü ~93 °C'de fanları açar; bozulursa fanlar 93 °C'de açmaz ve 103–105 °C'de ECU ana ECT1 verisiyle yedek koruma olarak fanları tetikler."),
        PidDefinition("010B", "(MAP) Emme Manifoldu Basıncı", "(MAP) Manifold", "kPa", PidCategory.ENGINE, "Rölanti: 26–32 kPa", 10.0, 165.0, true, false, "A (kPa)",
            "Emme manifoldu mutlak basıncını ölçer; K24A3'te yakıt hesabının temel sensörüdür.",
            "Rölantide 26–32 kPa sağlıklı vakumdur. 36 kPa üstü vakum kaçağı veya sıkı sübap ayarı belirtisidir."),
        PidDefinition("0134", "(B1S1) Wideband A/F Sensörü", "(B1S1) Wideband", "λ", PidCategory.FUEL_O2, "Stoik: 0.99–1.01 λ", 0.70, 1.30, true, false, "((A*256)+B)/32768",
            "Egzozdaki oksijen oranını geniş bant hassasiyetle ölçer.",
            "1.00 λ ideal yanmadır. 1.00 altı zengin (tam gaz ~0.85 λ), üstü fakir karışım demektir."),
        PidDefinition("0142", "(PCM V) Kontrol Modülü Voltajı", "(PCM V) ECU Voltajı", "V", PidCategory.ENGINE, "Şarjda: 13.8–14.4 V", 9.0, 16.0, true, false, "((A*256)+B)/1000",
            "Motor beynine (Matsushita RBB) ulaşan besleme gerilimini gösterir.",
            "Motor çalışırken 13.8–14.4 V olmalıdır; 12.2 V altı alternatör veya şase kaybına işaret eder."),
        PidDefinition("0106", "(STFT) Kısa Süreli Yakıt Düzeltme", "(STFT) Kısa Yakıt", "%", PidCategory.FUEL_O2, "-5% ile +5%", -25.0, 25.0, false, false, "(A-128)*100/128",
            "ECU'nun A/F sensörüne bakarak anlık yaptığı yakıt düzeltmesidir.",
            "Sıfır çevresinde dalgalanması normaldir; sürekli +8% üstü hava kaçağı veya yakıt besleme sorunudur."),
        PidDefinition("0107", "(LTFT) Uzun Süreli Yakıt Düzeltme", "(LTFT) Uzun Yakıt", "%", PidCategory.FUEL_O2, "-5% ile +5%", -25.0, 25.0, false, false, "(A-128)*100/128",
            "ECU'nun günler boyunca öğrendiği kalıcı yakıt düzeltme hafızasıdır.",
            "±5% arası sağlıklıdır; LPG'li araçlarda ayarın fakir (+%) veya zengin (-%) olduğunu en net gösterir."),
        PidDefinition("0103", "(Loop) Yakıt Sistemi Döngü Durumu", "(Loop) Döngü", "kod", PidCategory.FUEL_O2, "CL (2) = Kapalı Döngü", 0.0, 16.0, false, false, "Byte A",
            "ECU'nun O2 sensörünü aktif dinleyip dinlemediğini gösterir.",
            "Sıcak motorda CL (2) olmalıdır; OL (1) kalıyorsa A/F sensörü veya ısıtıcı devresi arızalı olabilir."),
        PidDefinition("010D", "(VSS) Araç Hızı", "(VSS) Hız", "km/sa", PidCategory.ENGINE, "0–240 km/sa", 0.0, 240.0, false, false, "A (km/sa)",
            "Şanzıman çıkış hız sensöründen okunan gerçek araç hızıdır.",
            "Kadran hızından genelde 3–5 km/sa daha hassastır."),
        PidDefinition("0104", "(Load) Hesaplanan Motor Yükü", "(Load) Yük", "%", PidCategory.ENGINE, "Rölanti: 18–26%", 0.0, 100.0, false, false, "A*100/255",
            "Motorun o anki devirde üretebileceği torkun kullanım yüzdesidir.",
            "Rölantide klima kapalıyken %18–26 normaldir."),
        PidDefinition("010E", "(Timing) Ateşleme Avansı", "(Timing) Avans", "°", PidCategory.ENGINE, "Rölanti: 6°–12°", -20.0, 50.0, false, false, "(A/2) - 64",
            "1. silindirde bujinin üst ölü noktadan kaç derece önce ateşlendiğini gösterir.",
            "Tam gazda ani avans düşüşü kötü yakıt veya vuruntu (knock) algılandığını gösterir."),
        PidDefinition("010F", "(IAT) Emme Havası Sıcaklığı", "(IAT) Emme Havası", "°C", PidCategory.AIR_TEMP, "Ortam + 5..20 °C", -40.0, 100.0, false, false, "A - 40",
            "Hava filtresinden geçen emme havasının sıcaklığını ölçer.",
            "Çok yüksek IAT (60 °C+) performans düşüşüne ve avans kısılmasına yol açar."),
        PidDefinition("0133", "(BARO) Barometrik Basınç", "(BARO) Atmosfer", "kPa", PidCategory.AIR_TEMP, "Deniz seviyesi ~101 kPa", 70.0, 110.0, false, false, "A (kPa)",
            "Bulunduğunuz rakımın atmosferik basıncını ölçer.",
            "Kontak açık motor kapalıyken MAP ile BARO eşit olmalıdır."),
        PidDefinition("0111", "(TPS) Gaz Kelebeği Konumu", "(TPS) Kelebek", "%", PidCategory.THROTTLE, "Rölanti: 12–16%", 0.0, 100.0, false, false, "A*100/255",
            "Elektronik gaz kelebeği klapesinin mutlak açıklık yüzdesidir.",
            "Rölantide %18 üstü kelebek kurumlanması belirtisidir."),
        PidDefinition("0145", "(Rel TPS) Göreceli Kelebek Konumu", "(Rel TPS) Göreceli", "%", PidCategory.THROTTLE, "Rölanti: 1–4%", 0.0, 100.0, false, false, "A*100/255",
            "Kapalı konuma göre gerçek kelebek açılma yüzdesini verir.",
            "Rölantide %1–4 arası düşük ve sabit olmalıdır."),
        PidDefinition("0147", "(TPS B) Mutlak Gaz Kelebeği B", "(TPS B) Kelebek B", "%", PidCategory.THROTTLE, "TPS yedek kanalı", 0.0, 100.0, false, false, "A*100/255",
            "Elektronik gaz kelebeğinin güvenlik amaçlı ikinci konum sensörüdür.",
            "Ana TPS ile uyumlu hareket etmelidir."),
        PidDefinition("0149", "(APP D) Gaz Pedalı Sensörü D", "(APP D) Pedal D", "%", PidCategory.THROTTLE, "Boşta: 14–16%", 0.0, 100.0, false, false, "A*100/255",
            "Kaput içindeki telli-elektronik gaz pedalı konum sensörünün ana kanalıdır.",
            "Basarken atlama yapıyorsa kronik APP sensörü aşınması (P2138) vardır."),
        PidDefinition("014A", "(APP E) Gaz Pedalı Sensörü E", "(APP E) Pedal E", "%", PidCategory.THROTTLE, "APP D yarısı (~7–8%)", 0.0, 100.0, false, false, "A*100/255",
            "Gaz pedalı sensörünün doğrulama kanalıdır.",
            "Her zaman APP D'nin tam yarısı (2:1) olmalıdır."),
        PidDefinition("014C", "(DBW) Hedeflenen Kelebek Komutu", "(DBW) Kelebek Emri", "%", PidCategory.THROTTLE, "0–100%", 0.0, 100.0, false, false, "A*100/255",
            "ECU'nun gaz kelebeği motoruna gönderdiği hedef açılma komutudur.",
            "Rel TPS bu komutu gecikmesiz takip etmelidir."),
        PidDefinition("0115", "(B1S2) Katalizör Sonrası O2 Sensörü", "(B1S2) Arka O2", "V", PidCategory.FUEL_O2, "Sağlıklı kat: 0.60–0.80 V", 0.0, 1.275, false, false, "A/200",
            "Katalitik konvertör çıkışındaki oksijeni izleyerek katalizör verimini denetler.",
            "0.60–0.80 V sabit kalmalıdır; sürekli dalgalanıyorsa katalizör zayıflamıştır."),
        PidDefinition("0144", "(Cmd λ) Hedeflenen Lambda", "(Cmd λ) Hedef λ", "λ", PidCategory.FUEL_O2, "Rölanti: 1.00 λ", 0.70, 1.30, false, false, "((A*256)+B)/32768",
            "ECU'nun o an motordan istediği hedef Lambda oranıdır.",
            "Gerçek B1S1 Lambda bu hedefi yakından takip etmelidir."),
        PidDefinition("012C", "(EGR) Komut Edilen EGR", "(EGR) Valf", "%", PidCategory.AIR_TEMP, "Rölanti: 0%, Seyir: 10–40%", 0.0, 100.0, false, false, "A*100/255",
            "EGR valfinin açılma oranını gösterir.",
            "Rölantide %0 olmalıdır; açık kalırsa motor sarsıntılı çalışır."),
        PidDefinition("012E", "(EVAP) Buhar Tahliye Komutu", "(EVAP) Kanister", "%", PidCategory.AIR_TEMP, "0–100%", 0.0, 100.0, false, false, "A*100/255",
            "Yakıt deposu buharını emmeye çeken kanister valfinin görev oranıdır.",
            "Açık takılı kalırsa yakıt dolumu sonrası zor çalışma görülür."),
        PidDefinition("0143", "(Abs Load) Mutlak Motor Yükü", "(Abs Load) Mutlak", "%", PidCategory.ENGINE, "Rölanti: 18–25%", 0.0, 120.0, false, false, "((A*256)+B)*100/255",
            "Hacimsel doluluk bazlı mutlak motor yükünü gösterir.",
            "Tam gazda %80 altı hava filtresi veya katalizör tıkanıklığına işaret edebilir."),
        PidDefinition("011F", "(RunTime) Motor Çalışma Süresi", "(RunTime) Süre", "sn", PidCategory.ENGINE, "Marştan beri sn", 0.0, 65535.0, false, false, "(A*256)+B",
            "Motor çalıştırıldığından beri geçen süreyi saniye olarak sayar.",
            "Isınma süresi ve termostat performansını ölçmek için kullanılır."),
        PidDefinition("0121", "(MIL km) Arıza Lambasıyla Mesafe", "(MIL km) Arıza km", "km", PidCategory.ENGINE, "Sağlıklı: 0 km", 0.0, 65535.0, false, false, "(A*256)+B",
            "Motor arıza lambası yanıkken gidilen mesafeyi kaydeder.",
            "0 km olmalıdır; 0'dan büyükse aktif arıza ile yol yapılmıştır."),
        PidDefinition("0130", "(WarmUps) Kod Silme Sonrası Isınma", "(WarmUps) Isınma", "kez", PidCategory.ENGINE, "Sürüş döngüsü", 0.0, 255.0, false, false, "A",
            "Kodlar silindikten sonra motorun kaç kez çalışma sıcaklığına ulaştığını sayar.",
            "0–2 arası ise arıza kodları çok yakın zamanda silinmiştir."),
        PidDefinition("0131", "(Clr km) Kod Silme Sonrası Mesafe", "(Clr km) Temiz km", "km", PidCategory.ENGINE, "Son sıfırlamadan beri", 0.0, 65535.0, false, false, "(A*256)+B",
            "Son Mode 04 silme işleminden bu yana gidilen mesafeyi gösterir.",
            "Düşük km ve eksik monitörler yakın zamanda arıza silindiğini gösterir."),
        PidDefinition("DER_TFT", "(Total FT) Toplam Yakıt Düzeltme", "(Total FT) Toplam", "%", PidCategory.DERIVED, "İdeal: ±5% (Limit ±10%)", -25.0, 25.0, true, true, "STFT + LTFT",
            "Kısa ve uzun süreli yakıt düzeltmelerinin toplamıdır.",
            "+10% üstü fakir karışım/vakum kaçağı, -10% altı fazla zengin çalışma belirtisidir."),
        PidDefinition("DER_AFR", "(AFR) Gerçek Hava/Yakıt Oranı", "(AFR) Hava/Yakıt", ":1", PidCategory.DERIVED, "Rölanti: 14.5–14.9 :1", 10.0, 19.0, false, true, "Lambda × 14.7",
            "Wideband Lambda değerini benzin hava/yakıt oranına çevirir.",
            "Rölantide ~14.7:1, tam gazda ~12.5–13.0:1 aralığı normaldir."),
        PidDefinition("DER_DTEMP", "(ΔT) Isı Farkı (ECT − IAT)", "(ΔT) ECT−IAT", "°C", PidCategory.DERIVED, "Sıcak motor: 45–65 °C", 0.0, 100.0, false, true, "ECT - IAT",
            "Motor soğutma sıvısı ile emme havası arasındaki sıcaklık farkıdır.",
            "Sabah soğuk motorda 0–3 °C olmalıdır; büyük fark sensör sapması gösterir."),
        PidDefinition("DER_VDROP", "(V-Drop) Tesisat / Şase Voltaj Düşümü", "(V-Drop) Kayıp", "V", PidCategory.DERIVED, "İdeal: < 0.30 V", -1.0, 3.0, false, true, "ATRV - PCM(01 42)",
            "OBD soketindeki akü voltajı ile ECU voltajı arasındaki düşümü gösterir.",
            "0.45 V üstü motor şase kablosu oksitlenmesi veya röle direncine işaret eder.")
    )

    fun parseRawResponse(pidHex: String, rawReply: String): Double? {
        val clean = rawReply.replace(" ", "").uppercase(Locale.US)
        val expectedHeader = "41" + pidHex.substring(2)
        val idx = clean.indexOf(expectedHeader)
        if (idx == -1) return null
        val dataHex = clean.substring(idx + 4)
        val bytes = dataHex.chunked(2).mapNotNull { it.toIntOrNull(16) }
        if (bytes.isEmpty()) return null
        val a = bytes[0]
        val b = if (bytes.size > 1) bytes[1] else 0

        return when (pidHex) {
            "010C" -> ((a * 256) + b) / 4.0
            "0105", "010F" -> a - 40.0
            "010B", "010D", "0133", "0103", "0130" -> a.toDouble()
            "011F", "0121", "0131" -> ((a * 256) + b).toDouble()
            "0106", "0107" -> (a - 128) * 100.0 / 128.0
            "0104", "0111", "012C", "012E", "0145", "0147", "0149", "014A", "014C" -> a * 100.0 / 255.0
            "010E" -> (a / 2.0) - 64.0
            "0115" -> a / 200.0
            "0134", "0144" -> ((a * 256) + b) / 32768.0
            "0142" -> ((a * 256) + b) / 1000.0
            "0143" -> ((a * 256) + b) * 100.0 / 255.0
            else -> null
        }
    }

    fun formatValue(def: PidDefinition, v: Double): LivePidValue {
        val formatted = when (def.pidHex) {
            "0103" -> if (v.toInt() == 2) "CL (2)" else "OL (${v.toInt()})"
            "010C", "010D", "0105", "010F", "010B", "0133", "011F", "0121", "0130", "0131", "DER_DTEMP" ->
                String.format(Locale.US, "%.0f", v)
            "0134", "0144", "0115", "DER_VDROP" -> String.format(Locale.US, "%.2f", v)
            "0106", "0107", "DER_TFT" -> String.format(Locale.US, "%+.1f", v)
            else -> String.format(Locale.US, "%.1f", v)
        }
        val warning = when (def.pidHex) {
            "0105" -> v > 102.0
            "0142" -> v < 12.2 || v > 14.9
            "DER_TFT", "0106", "0107" -> kotlin.math.abs(v) > 10.0
            "DER_VDROP" -> kotlin.math.abs(v) > 0.45
            "0121" -> v > 0.0
            else -> false
        }
        return LivePidValue(def, v, formatted, warning)
    }

    fun estimateKLineHz(pidCount: Int, avgLatencyMs: Int): Double {
        if (pidCount <= 0) return 0.0
        val effectiveMs = avgLatencyMs.coerceAtLeast(115)
        return (1000.0 / (pidCount * effectiveMs)).coerceIn(0.2, 8.0)
    }
}
