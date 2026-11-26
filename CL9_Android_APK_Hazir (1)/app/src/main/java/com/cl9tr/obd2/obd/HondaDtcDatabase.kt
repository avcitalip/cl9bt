package com.cl9tr.obd2.obd

import com.cl9tr.obd2.model.DtcCode
import java.util.Locale

object HondaDtcDatabase {

    private data class DtcEntry(val system: String, val desc: String, val note: String)

    private val db: Map<String, DtcEntry> = mapOf(
        "P0134" to DtcEntry("Motor (K24A3)", "B1S1 Wideband A/F Sensörü Aktivite Yok", "2004 CL9'da orijinal Denso/NTK LAF sensörü ve ısıtıcı soketini kontrol edin."),
        "P0135" to DtcEntry("Motor (K24A3)", "B1S1 A/F Sensörü Isıtıcı Devresi Arızası", "Sensör ısıtıcı direnci veya röle/sigorta kontrolü gerekir."),
        "P0171" to DtcEntry("Motor (K24A3)", "Sistem Çok Fakir (Bank 1)", "Emme manifoldu vakum kaçağı, düşük yakıt basıncı veya kirli enjektör. Rölanti MAP ve STFT+LTFT değerlerini izleyin."),
        "P0172" to DtcEntry("Motor (K24A3)", "Sistem Çok Zengin (Bank 1)", "Kaçıran enjektör, yüksek yakıt basıncı veya hatalı A/F sensörü."),
        "P0300" to DtcEntry("Motor (K24A3)", "Rastgele / Çoklu Silindir Teklemesi", "Buji, bobin, sübap ayarı (K24A3'te kritik!) ve yakıt kalitesini kontrol edin."),
        "P0301" to DtcEntry("Motor (K24A3)", "1. Silindir Tekleme Algılandı", "1. silindir ateşleme bobini ve bujiyi diğer silindirle yer değiştirerek test edin."),
        "P0302" to DtcEntry("Motor (K24A3)", "2. Silindir Tekleme Algılandı", "2. silindir ateşleme bobini ve bujiyi kontrol edin."),
        "P0303" to DtcEntry("Motor (K24A3)", "3. Silindir Tekleme Algılandı", "3. silindir ateşleme bobini ve bujiyi kontrol edin."),
        "P0304" to DtcEntry("Motor (K24A3)", "4. Silindir Tekleme Algılandı", "4. silindir ateşleme bobini ve bujiyi kontrol edin."),
        "P0325" to DtcEntry("Motor (K24A3)", "Vuruntu (Knock) Sensörü Devre Arızası", "K24A3'te emme manifoldu altındaki knock sensörü kırılması/nemlenmesi yaygındır."),
        "P0341" to DtcEntry("Motor (K24A3)", "Eksantrik (CMP) / Krank Faz Uyumsuzluğu", "K24A3 triger zinciri uzaması veya VTC aktüatörü kontrolü kritik önem taşır!"),
        "P0401" to DtcEntry("Motor (K24A3)", "Yetersiz EGR Akışı", "EGR valfi ve emme manifoldu EGR kanallarında kurum tıkanıklığı."),
        "P0420" to DtcEntry("Motor (K24A3)", "Katalitik Konvertör Verimi Eşik Altında", "B1S2 O2 sensör voltajı dalgalanıyorsa katalizör ömrünü tamamlamış olabilir."),
        "P1009" to DtcEntry("Motor (K24A3)", "VTC İlerleme Arızası (i-VTEC)", "Yağ seviyesi, VTC yağ süzgeci ve VTC solenoidini kontrol edin."),
        "P2138" to DtcEntry("Motor (K24A3)", "Gaz Pedalı Konum Sensörü (APP D/E) Uyumsuzluğu", "CL9'da kaput içindeki telli-elektronik APP sensörü aşınması çok yaygındır. Canlı APP D/E grafiğinden doğrulayın."),
        "P2646" to DtcEntry("Motor (K24A3)", "VTEC Yağ Basınç Şalteri Düşük Voltaj / Takılı", "Motor yağ seviyesi, VTEC solenoid süzgeç contası ve basınç şalterini kontrol edin."),
        "P2647" to DtcEntry("Motor (K24A3)", "VTEC Yağ Basınç Şalteri Yüksek Voltaj", "VTEC solenoid valfi veya basınç müşürü devresi."),
        "P0700" to DtcEntry("Şanzıman (MCTA 5AT)", "Otomatik Şanzıman Kontrol Sistemi Arıza İsteği", "Genel şanzıman uyarı kodudur; beraberindeki P07xx/P08xx/P17xx koduna bakın."),
        "P0715" to DtcEntry("Şanzıman (MCTA 5AT)", "Giriş / Türbin Hız Sensörü Devresi", "Şanzıman ana mil hız sensörü veya kablo soketi."),
        "P0730" to DtcEntry("Şanzıman (MCTA 5AT)", "Hatalı Vites Oranı", "Honda DW-1 ATF seviyesi/durumu ve kavrama basıncı kontrolü gerekir."),
        "P0740" to DtcEntry("Şanzıman (MCTA 5AT)", "Tork Konvertörü Kilitleme (TCC) Devresi", "Lock-up solenoidi, tıkalı şanzıman harici filtresi veya tork konvertörü."),
        "P0753" to DtcEntry("Şanzıman (MCTA 5AT)", "Vites Solenoidi A Elektrik Arızası", "MCTA vites solenoidi A bobin direnci ve tesisatı."),
        "P0758" to DtcEntry("Şanzıman (MCTA 5AT)", "Vites Solenoidi B Elektrik Arızası", "MCTA vites solenoidi B bobin direnci ve tesisatı."),
        "P0845" to DtcEntry("Şanzıman (MCTA 5AT)", "3. Vites Şanzıman Yağ Basınç Şalteri", "CL9 MCTA şanzımanda 3. vites basınç müşürü sık görülür (ekonomik parça)."),
        "P0847" to DtcEntry("Şanzıman (MCTA 5AT)", "3. Vites Basınç Şalteri Düşük Voltaj", "3. vites basınç müşürü kısa devre veya arızalı müşür."),
        "P1739" to DtcEntry("Şanzıman (MCTA 5AT)", "3. Kavrama Basınç Şalteri Arızası", "MCTA 3. kavrama basınç şalteri kontrolü."),
        "P1768" to DtcEntry("Şanzıman (MCTA 5AT)", "Lineer Kavrama Basınç Kontrol Solenoidi A", "Çiftli lineer solenoid temizliği/değişimi ve DW-1 yağ değişimi önerilir.")
    )

    fun lookup(code: String, isPending: Boolean): DtcCode {
        val norm = code.uppercase(Locale.US)
        val entry = db[norm]
        return if (entry != null) {
            DtcCode(norm, isPending, entry.system, entry.desc, entry.note)
        } else {
            val sys = if (norm.startsWith("P07") || norm.startsWith("P08") || norm.startsWith("P17"))
                "Şanzıman (MCTA 5AT)" else "Motor (K24A3)"
            DtcCode(norm, isPending, sys, "Standart OBD-II Arıza Kodu ($norm)", "K24A3 servis kataloğundan detaylı kontrol önerilir.")
        }
    }

    fun parseDtcResponse(raw: String, expectedHeader: String, isPending: Boolean): List<DtcCode> {
        val clean = raw.replace(" ", "").uppercase(Locale.US)
        if (clean.contains("NODATA") || !clean.contains(expectedHeader)) return emptyList()
        val idx = clean.indexOf(expectedHeader)
        val payload = clean.substring(idx + 2)
        val codes = mutableListOf<DtcCode>()
        var i = 0
        while (i + 3 < payload.length) {
            val pair = payload.substring(i, i + 4)
            if (pair != "0000" && pair.all { it in '0'..'9' || it in 'A'..'F' }) {
                val firstNibble = pair[0].digitToInt(16)
                val prefix = when (firstNibble shr 2) {
                    0 -> "P"
                    1 -> "C"
                    2 -> "B"
                    else -> "U"
                }
                val secondDigit = (firstNibble and 0x03).toString()
                val fullCode = prefix + secondDigit + pair.substring(1)
                if (codes.none { it.code == fullCode }) {
                    codes.add(lookup(fullCode, isPending))
                }
            }
            i += 4
        }
        return codes
    }
}
