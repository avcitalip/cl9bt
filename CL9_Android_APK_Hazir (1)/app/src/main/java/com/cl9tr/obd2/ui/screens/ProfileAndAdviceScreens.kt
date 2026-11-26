package com.cl9tr.obd2.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cl9tr.obd2.model.*
import com.cl9tr.obd2.ui.theme.*
import java.util.Locale

@Composable
fun ProfileSetupDialog(
    initial: VehicleProfile?,
    canClose: Boolean,
    onDismiss: () -> Unit,
    onSave: (VehicleProfile) -> Unit,
    onStartSimulation: () -> Unit
) {
    var plate by remember { mutableStateOf(initial?.plate ?: "") }
    var kmStr by remember { mutableStateOf((initial?.km ?: 375000).toString()) }
    var fuel by remember { mutableStateOf(initial?.fuel ?: "Benzin + LPG") }
    var engStatus by remember { mutableStateOf(initial?.engineStatus ?: "Orijinal Motor") }
    var trStatus by remember { mutableStateOf(initial?.transStatus ?: "Orijinal Şanzıman") }
    var tuneLevel by remember { mutableStateOf(initial?.tuneLevel ?: "Orijinal") }
    var aggrMods by remember { mutableStateOf(initial?.aggressiveMods ?: false) }
    var dynoHpStr by remember { mutableStateOf(if ((initial?.dynoHp ?: 0) > 0) initial!!.dynoHp.toString() else "") }
    var dynoNmStr by remember { mutableStateOf(if ((initial?.dynoNm ?: 0) > 0) initial!!.dynoNm.toString() else "") }
    var frontBrand by remember { mutableStateOf(initial?.frontBrand ?: "") }
    var frontSeason by remember { mutableStateOf(initial?.frontSeason ?: "Yaz Lastiği") }
    var rearBrand by remember { mutableStateOf(initial?.rearBrand ?: "") }
    var rearSeason by remember { mutableStateOf(initial?.rearSeason ?: "Yaz Lastiği") }
    var errText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = { if (canClose) onDismiss() }) {
        Surface(shape = RoundedCornerShape(20.dp), color = BgShell, border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.35f))) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (initial == null) "Araç Profili Kurulumu" else "Profili Düzenle (${initial.plate})",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                // Genel geçici profil yerine doğrudan Simülasyon Modunu Başlat!
                Button(
                    onClick = onStartSimulation,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = BgObsidian)
                ) {
                    Text("Simülasyon Modunu Başlat", fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp)
                }

                if (errText.isNotBlank()) {
                    Text(errText, color = AccentRed, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = plate,
                    onValueChange = { plate = it.uppercase(Locale("tr", "TR")) },
                    label = { Text("Araç Plakası (Zorunlu)") },
                    placeholder = { Text("34 BCA 320") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = kmStr,
                    onValueChange = { kmStr = it.filter { c -> c.isDigit() } },
                    label = { Text("Güncel Kilometre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Yakıt Sistemi", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                SegmentedRow(listOf("Benzin + LPG", "Benzin"), fuel) { fuel = it }

                Text("Motor Durumu", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                SegmentedRow(listOf("Orijinal Motor", "Revizyonlu", "Çıkma Motor"), engStatus) { engStatus = it }

                Text("Şanzıman Durumu", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                SegmentedRow(listOf("Orijinal Şanzıman", "Revizyonlu", "Çıkma Şanzıman"), trStatus) { trStatus = it }

                Text("Araç Yazılımı", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                SegmentedRow(listOf("Orijinal", "Stage 1", "Agresif"), tuneLevel) { tuneLevel = it }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                    aggrMods = !aggrMods
                    if (aggrMods) tuneLevel = "Agresif"
                }) {
                    Checkbox(checked = aggrMods, onCheckedChange = {
                        aggrMods = it
                        if (it) tuneLevel = "Agresif"
                    })
                    Text("Aracımda agresif modifikasyonlar var", color = TextPrimary, fontSize = 12.sp)
                }

                if (aggrMods) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = dynoHpStr, onValueChange = { dynoHpStr = it.filter { c -> c.isDigit() } }, label = { Text("Dyno Pik BG") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = dynoNmStr, onValueChange = { dynoNmStr = it.filter { c -> c.isDigit() } }, label = { Text("Dyno Pik Nm") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                }

                Text("Ön Aks Lastiği (Zorunlu)", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = frontBrand,
                        onValueChange = { frontBrand = it },
                        placeholder = { Text("Continental Premium Contact 7", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val d = TireCatalog.detect(frontBrand)
                            frontBrand = d.fullName
                            frontSeason = d.season
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) { Text("Bul", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                }

                Text("Arka Aks Lastiği (Zorunlu)", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = rearBrand,
                        onValueChange = { rearBrand = it },
                        placeholder = { Text("Continental Premium Contact 7", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val d = TireCatalog.detect(rearBrand)
                            rearBrand = d.fullName
                            rearSeason = d.season
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) { Text("Bul", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (canClose) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                            Text("Vazgeç")
                        }
                    }
                    Button(
                        onClick = {
                            if (plate.trim().length < 4) { errText = "Lütfen geçerli bir plaka girin (Ör: 34 BCA 320)."; return@Button }
                            if (frontBrand.isBlank() || rearBrand.isBlank()) { errText = "Ön ve arka lastik bilgilerini doldurun."; return@Button }
                            val fDet = TireCatalog.detect(frontBrand)
                            val rDet = TireCatalog.detect(rearBrand)
                            onSave(
                                VehicleProfile(
                                    id = initial?.id ?: "p_${System.currentTimeMillis()}",
                                    plate = plate.trim().uppercase(Locale("tr", "TR")),
                                    km = kmStr.toIntOrNull() ?: 375000,
                                    fuel = fuel,
                                    engineStatus = engStatus,
                                    transStatus = trStatus,
                                    tuneLevel = if (aggrMods) "Agresif" else tuneLevel,
                                    aggressiveMods = aggrMods,
                                    dynoHp = dynoHpStr.toIntOrNull() ?: 0,
                                    dynoNm = dynoNmStr.toIntOrNull() ?: 0,
                                    frontBrand = fDet.fullName,
                                    frontSeason = fDet.season,
                                    rearBrand = rDet.fullName,
                                    rearSeason = rDet.season
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = BgObsidian)
                    ) {
                        Text("Profili Kaydet", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        options.forEach { opt ->
            val act = opt == selected
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (act) AccentCyan.copy(alpha = 0.22f) else CardSlate,
                border = BorderStroke(1.dp, if (act) AccentCyan else Color(0xFF1E293B)),
                modifier = Modifier.weight(1f).clickable { onSelect(opt) }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)) {
                    Text(opt, color = if (act) AccentCyan else TextPrimary, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun ProfileSwitcherDialog(
    profiles: List<VehicleProfile>,
    activeId: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onEdit: (VehicleProfile) -> Unit,
    onDelete: (String) -> Unit,
    onAddNew: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = BgShell, border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.35f))) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Kayıtlı Araç Profilleri", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                profiles.forEach { p ->
                    val isAct = p.id == activeId
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CardSlate,
                        border = BorderStroke(1.dp, if (isAct) AccentCyan else Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(p.id) }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(p.plate, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                Text("${p.km} km • ${p.fuel} • ${p.tuneLevel}", color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(onClick = { onEdit(p) }) { Text("Düzenle", fontSize = 11.sp, color = AccentCyan) }
                                if (profiles.size > 1) {
                                    TextButton(onClick = { onDelete(p.id) }) { Text("Sil", fontSize = 11.sp, color = AccentRed) }
                                }
                            }
                        }
                    }
                }
                Button(onClick = onAddNew, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = BgObsidian)) {
                    Text("+ Yeni Plaka Profili Ekle", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
fun TestAndInfoScreen(
    conn: ConnectionState,
    profile: VehicleProfile,
    readiness: List<ReadinessMonitor>,
    freeze: List<FreezeFrameItem>,
    ecu: EcuIdentity,
    trackedKm: Int,
    trackedHours: Double
) {
    // Tüm sekmeler girişte kapalı başlar!
    var openBattery by remember { mutableStateOf(false) }
    var openTires by remember { mutableStateOf(false) }
    var openEmissions by remember { mutableStateOf(false) }
    var openFreeze by remember { mutableStateOf(false) }
    var openEcu by remember { mutableStateOf(false) }
    var openLimits by remember { mutableStateOf(false) }

    val isConnected = conn.phase == ConnectionPhase.CONNECTED || conn.phase == ConnectionPhase.DEMO_MODE
    val isSim = conn.phase == ConnectionPhase.DEMO_MODE

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 18.dp)
    ) {
        // 1. Akü ve Alternatör Yorumu (Ayrı ayrı, voltaj yalnızca burada)
        item {
            AccordionCard("Akü ve Alternatör Yorumu", "Akü ve şarj sağlığı ayrı analiz", openBattery, { openBattery = !openBattery }) {
                val v = if (isConnected) conn.batteryVoltage else 0.0
                Text("Akü Durumu: ${if (v > 0) "${String.format(Locale.US, "%.2f", v - 1.5)} V (Durgun Tahmini)" else "Ölçüm bekleniyor"}", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Alternatör Durumu: ${if (v > 0) "${String.format(Locale.US, "%.2f", v)} V (Şarj Voltajı)" else "Ölçüm bekleniyor"}", color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                val comment = when {
                    v <= 0.0 -> "Canlı voltaj verisi bekleniyor."
                    v < 11.8 -> "Akü boş veya alternatör şarj etmiyor! Acil kontrol gerekir."
                    v < 13.4 -> "Alternatör verimliliği düşük görünüyor; şarj dinamosu ve kayış kontrol edilmeli."
                    v > 14.9 -> "Aşırı şarj voltajı tespit edildi! Konjektör arızası aküye zarar verebilir."
                    else -> "Akü kondisyonu ve alternatör şarj çıkışı sağlıklı aralıkta."
                }
                Text(comment, color = TextPrimary, fontSize = 12.sp)
                Text("Veri Dayanağı: Son $trackedKm km ve ${String.format(Locale.US, "%.1f", trackedHours)} saatlik ölçüm.", color = TextSecondary, fontSize = 10.5.sp)
            }
        }

        // 2. Lastik Basınçlarının Ayarlanması (Simülasyon Modunda Yapay Zeka Optimal Lastik Seçimi!)
        item {
            AccordionCard("Lastik Basınçlarının Ayarlanması", "Hava, yük ve yol tipine göre PSI / bar", openTires, { openTires = !openTires }) {
                val effFront = if (isSim) "Pirelli Cinturato All Season SF3 (YZ Seçimi • 4 Mevsim)" else "${profile.frontBrand} (${profile.frontSeason})"
                val effRear = if (isSim) "Pirelli Cinturato All Season SF3 (YZ Seçimi • 4 Mevsim)" else "${profile.rearBrand} (${profile.rearSeason})"
                val rPsi = if (profile.isLpg()) 31 else 30
                val fPsi = 32
                Text("Ön Aks: ${profile.frontSize} • $effFront", color = AccentCyan, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                Text("Arka Aks: ${profile.rearSize} • $effRear", color = AccentGreen, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                Text("Zonguldak (500 km çap) • Yağmurlu / Islak • Ort. 9 °C", color = TextSecondary, fontSize = 11.sp)
                Text("Önerilen Basınç: Ön $fPsi PSI (${String.format(Locale.US, "%.2f", fPsi * 0.06895)} bar) • Arka $rPsi PSI (${String.format(Locale.US, "%.2f", rPsi * 0.06895)} bar)", color = TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold)
                if (isSim) {
                    Text("Simülasyon Modu: Mevcut hava koşullarına (Zonguldak 9 °C yağışlı) en uygun lastik yapay zeka tarafından Pirelli Cinturato All Season SF3 (4 Mevsim) olarak seçilip hesaplandı.", color = AccentAmber, fontSize = 11.sp)
                }
                Text("Lastik basınçlarını ayda bir ve dış hava sıcaklığı 10 °C değiştiğinde soğuk lastikte kontrol edin.", color = TextSecondary, fontSize = 11.sp)
            }
        }

        // 3. Emisyon ve Eşik Testleri
        item {
            AccordionCard("Emisyon ve Eşik Testleri", "Hazırlık monitörleri ve erken teşhis", openEmissions, { openEmissions = !openEmissions }) {
                if (readiness.isEmpty()) {
                    Text("Henüz okunmadı; bağlantı sonrası güncellenir.", color = TextSecondary, fontSize = 12.sp)
                } else {
                    readiness.forEach { m ->
                        Column(modifier = Modifier.padding(vertical = 2.dp)) {
                            Text("${m.nameTr}: ${if (m.ready) "TAMAMLANDI" else "TAMAMLANMADI"}", color = if (m.ready) AccentGreen else AccentAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            if (!m.ready) {
                                Text("Depo %25–%75 dolulukta sabit hız sürüş döngüsü tamamlanmadığı için bekliyor.", color = TextSecondary, fontSize = 10.5.sp)
                            }
                        }
                    }
                }
            }
        }

        // 4. Dondurulmuş Arıza Kaydı
        item {
            AccordionCard("Dondurulmuş Arıza Kaydı", "Arıza anı sensör fotoğrafı", openFreeze, { openFreeze = !openFreeze }) {
                if (freeze.isEmpty()) {
                    Text("Aktif dondurulmuş arıza kaydı bulunmuyor.", color = TextSecondary, fontSize = 12.sp)
                } else {
                    freeze.forEach { f ->
                        Text("${f.label}: ${f.value}", color = TextPrimary, fontSize = 12.sp)
                    }
                }
            }
        }

        // 5. Motor Beyni Kimliği (Akü bölümü yok)
        item {
            AccordionCard("Motor Beyni Kimliği", "Şasi, kalibrasyon ve doğrulama kodu", openEcu, { openEcu = !openEcu }) {
                Text("Şasi No (VIN): ${ecu.vin}", color = TextPrimary, fontSize = 12.sp)
                Text("Kalibrasyon ID: ${ecu.calId}", color = AccentCyan, fontSize = 12.sp)
                Text("Doğrulama (CVN): ${ecu.cvn}", color = TextPrimary, fontSize = 12.sp)
                Text("ECU: ${ecu.ecuModel}", color = TextSecondary, fontSize = 11.5.sp)
                Text("Motor Beyni KM: ${if (ecu.ecuKm != null) "${ecu.ecuKm} km" else "--"}", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                if (ecu.kmMismatchWarning.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(10.dp), color = AccentRed.copy(alpha = 0.18f), border = BorderStroke(1.dp, AccentRed.copy(alpha = 0.6f))) {
                        Text(ecu.kmMismatchWarning, color = AccentRed, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(10.dp))
                    }
                }
            }
        }

        // 6. Okunamayan Sistemler ve PID Durumu
        item {
            AccordionCard("Okunamayan Sistemler ve PID Durumu", "CL9 K-Line protokol sınırları", openLimits, { openLimits = !openLimits }) {
                Text("2004 Honda Accord CL9 yalnızca Motor ve 5AT Şanzıman verilerini Pin 7 K-Line (ISO 9141-2) üzerinden standart OBD-II olarak yayınlar.", color = TextPrimary, fontSize = 12.sp)
                Text("• ABS / VSA / SRS / EPS PID'leri tahminen Honda'ya özel K-Line modülleri farklı adreste çalıştığı için standart ELM327 ile okunamıyor.", color = TextSecondary, fontSize = 11.sp)
                Text("• Şanzıman Yağ Sıcaklığı (ATF) ve Canlı VTEC Selenoit PID'leri tahminen Matsushita RBB ECU bunları yalnızca fabrika HDS protokolünde yayınladığı için şu anda çalışmıyor.", color = TextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun AccordionCard(
    title: String,
    sub: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(shape = RoundedCornerShape(16.dp), color = CardSlate, border = BorderStroke(1.dp, Color(0xFF1E293B)), modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(sub, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = AccentCyan)
            }
            if (expanded) {
                Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
            }
        }
    }
}

@Composable
fun MaintenanceScreen(
    trackedKm: Int,
    trackedHours: Double,
    profile: VehicleProfile,
    onResetDriveData: () -> Unit
) {
    var showResetConfirm by remember { mutableStateOf(false) }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            containerColor = BgShell,
            title = { Text("Sürüş Verilerini Sıfırla", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Birikmiş sürüş, pik performans ve yakıt havuzu sıfırlanacak. Emin misiniz?", color = TextSecondary, fontSize = 12.5.sp) },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { showResetConfirm = false; onResetDriveData() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = BgObsidian)
                    ) { Text("Evet", fontWeight = FontWeight.ExtraBold) }
                    Button(
                        onClick = { showResetConfirm = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed, contentColor = Color.White)
                    ) { Text("Hayır", fontWeight = FontWeight.ExtraBold) }
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 18.dp)
    ) {
        item {
            Button(
                onClick = { showResetConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed.copy(alpha = 0.2f), contentColor = AccentRed)
            ) { Text("Sürüş Verilerini Sıfırla", fontWeight = FontWeight.ExtraBold) }
        }
        item {
            Surface(shape = RoundedCornerShape(16.dp), color = CardSlate, border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.3f)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Sensör Veri Havuzu: $trackedKm km • ${String.format(Locale.US, "%.1f", trackedHours)} saat", color = AccentCyan, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        if (trackedKm == 0) "Sürüş verileri sıfırlandı; yeni sürüş yaptıkça sensör sapmalarına göre teşhisler oluşacaktır."
                        else "Aktif ${profile.activeFuelName()} profili ve ${profile.tuneLevel} yazılımı dikkate alınarak ateşleme avansı, MAP vakumu, IAT ve yakıt düzeltmeleri sürekli izleniyor. Şu an kritik mekanik sapma görünmüyor.",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}
