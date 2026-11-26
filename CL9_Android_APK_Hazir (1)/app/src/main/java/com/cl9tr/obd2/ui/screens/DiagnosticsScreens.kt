package com.cl9tr.obd2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cl9tr.obd2.model.*
import com.cl9tr.obd2.ui.theme.*

@Composable
fun DtcScreen(
    dtcs: List<DtcCode>,
    onScan: () -> Unit,
    onClearConfirmed: () -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Mode 04 • Arıza Kodlarını Sil") },
            text = { Text("Arıza kodları, Freeze Frame verisi, yakıt düzeltmeleri (LTFT) ve emisyon hazırlık monitörleri sıfırlanacaktır. Kontak açık ve motor stop halinde olmalıdır. Onaylıyor musunuz?") },
            confirmButton = {
                Button(onClick = { showClearDialog = false; onClearConfirmed() }, colors = ButtonDefaults.buttonColors(containerColor = AccentRed)) {
                    Text("Evet, Sil (Mode 04)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("İptal") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(BgObsidian).padding(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onScan, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)) {
                Text("Tara (Mode 03 & 07)", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = { showClearDialog = true }, modifier = Modifier.weight(1f)) {
                Text("Kodları Sil", color = AccentRed)
            }
        }
        Spacer(Modifier.height(12.dp))
        if (dtcs.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = CardSlate), modifier = Modifier.fillMaxWidth()) {
                Text("Kayıtlı veya bekleyen arıza kodu bulunamadı (K24A3 Motor & MCTA 5AT temiz).", color = AccentGreen, modifier = Modifier.padding(16.dp))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(dtcs) { d ->
                    Card(colors = CardDefaults.cardColors(containerColor = CardSlate), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(d.code, color = if (d.isPending) AccentAmber else AccentRed, fontSize = 18.sp, fontWeight = FontWeight.Black)
                                Text("${d.system} • ${if (d.isPending) "Bekleyen (07)" else "Kayıtlı (03)"}", color = TextSecondary, fontSize = 11.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(d.descriptionTr, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("CL9 Notu: ${d.cl9Note}", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FreezeFrameScreen(itemsList: List<FreezeFrameItem>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize().background(BgObsidian).padding(14.dp)) {
        item { Text("Mode 02 • Arıza Anı Dondurulmuş Kare", color = AccentCyan, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
        items(itemsList) { item ->
            Card(colors = CardDefaults.cardColors(containerColor = CardSlate), modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Text(item.label, color = TextSecondary, fontSize = 13.sp)
                    Text(item.value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EmissionsMode06Screen(readiness: List<ReadinessMonitor>, mode06: List<Mode06Test>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize().background(BgObsidian).padding(14.dp)) {
        item { Text("Mode 01 (01 01) • Emisyon Hazırlık Monitörleri", color = AccentCyan, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
        items(readiness) { r ->
            Card(colors = CardDefaults.cardColors(containerColor = CardSlate), modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(r.nameTr, color = TextPrimary, fontSize = 13.sp)
                    Text(if (r.ready) "HAZIR" else "TAMAMLANMADI", color = if (r.ready) AccentGreen else AccentAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)); Text("Mode 06 • Eşik Değer & Erken Teşhis Testleri", color = AccentCyan, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
        items(mode06) { m ->
            Card(colors = CardDefaults.cardColors(containerColor = CardSlate), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("${m.tid} • ${m.nameTr}", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Ölçülen: ${m.value}  |  Limit: ${m.limit}", color = if (m.passed) AccentGreen else AccentRed, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun EcuIdentityScreen(ecu: EcuIdentity, batteryV: Double) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize().background(BgObsidian).padding(14.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = CardSlate), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Mode 09 • Matsushita RBB ECU Kimliği", color = AccentCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("ECU Donanımı: ${ecu.ecuModel}", color = TextPrimary, fontSize = 13.sp)
                    Text("Şasi No (09 02 VIN): ${ecu.vin}", color = TextPrimary, fontSize = 13.sp)
                    Text("Kalibrasyon ID (09 04 CalID): ${ecu.calId}", color = AccentGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Yazılım Sağlaması (09 06 CVN): ${ecu.cvn}", color = AccentAmber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Not: CalID ve CVN değerleri ECU yazılımının orijinal olup olmadığını doğrulamanızı sağlar.", color = TextSecondary, fontSize = 11.sp)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = CardSlate), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("ATRV • Akü & Alternatör Voltajı", color = AccentCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("${batteryV} V", color = if (batteryV in 13.5..14.6) AccentGreen else AccentAmber, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    Text("Kontak açık: 12.4–12.7 V • Rölanti şarjda: 13.8–14.4 V", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun CsvLoggerScreen(isRecording: Boolean, rows: List<String>, onToggle: () -> Unit, onClear: () -> Unit, onShare: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(BgObsidian).padding(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onToggle,
                colors = ButtonDefaults.buttonColors(containerColor = if (isRecording) AccentRed else AccentGreen),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isRecording) "Durdur" else "Başlat", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onShare,
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                modifier = Modifier.weight(1f)
            ) {
                Text("CSV Paylaş", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) {
                Text("Sil (${rows.size})", color = TextPrimary)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Format: Saat,RPM,ECT,MAP,Lambda,ToplamFT,Voltaj", color = TextSecondary, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(rows.reversed()) { r ->
                Text(r, color = AccentCyan, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun Cl9GuideScreen() {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize().background(BgObsidian).padding(14.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = CardSlate), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Klima (HVAC) Dahili Self-Test", color = AccentGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Kontak kapalıyken klima OFF tuşuna basılı tutun, kontağı ON (II) konumuna alın ve 10 saniye içinde Arka Cam Rezistansı tuşuna 5 kez basın. Ekranda sensör/klape arıza segmentleri görünür.", color = TextPrimary, fontSize = 12.sp)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = CardSlate), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Dürüstlük Notu: 2004 CL9'da ELM327 İle Okunamayanlar", color = AccentRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("• ABS / VSA, SRS Airbag, EPS, MICU ve Immobilizer (Honda HDS / HIM gerekir)\n• Otomatik Şanzıman Yağ Sıcaklığı (ATF Temp) ve tork konvertörü canlı basıncı\n• VTEC/VTC canlı egzantrik açısı, Knock sayısı, MAF (01 10), Yağ Sıcaklığı (01 5C)\n• ECU yazılım atma (Reflash) veya rölanti/CKP öğretme", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}
