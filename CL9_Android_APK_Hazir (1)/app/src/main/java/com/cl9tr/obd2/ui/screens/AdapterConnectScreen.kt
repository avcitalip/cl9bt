package com.cl9tr.obd2.ui.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cl9tr.obd2.model.AdapterTier
import com.cl9tr.obd2.model.ConnectionState
import com.cl9tr.obd2.ui.theme.*

@SuppressLint("MissingPermission")
@Composable
fun AdapterConnectScreen(
    conn: ConnectionState,
    pairedDevices: List<BluetoothDevice>,
    onRefreshDevices: () -> Unit,
    onSelectDevice: (BluetoothDevice) -> Unit,
    onStartDemo: (AdapterTier) -> Unit,
    onDisconnect: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(BgObsidian)
            .padding(14.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = CardSlate), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Adaptif ELM327 Analiz Sonucu", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(conn.adapterTier.label, color = Color(conn.adapterTier.badgeColorHex), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Text(conn.adapterTier.description, color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Kilitlenen Profil: ${conn.activeProfile?.shortCode ?: "Henüz yok"}", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onRefreshDevices, modifier = Modifier.weight(1f)) {
                            Text("Cihazları Yenile")
                        }
                        OutlinedButton(onClick = onDisconnect, modifier = Modifier.weight(1f)) {
                            Text("Bağlantıyı Kes", color = AccentRed)
                        }
                    }
                }
            }
        }

        item {
            Text("Eşleşmiş Bluetooth Cihazları (OBDII / ELM327)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        if (pairedDevices.isEmpty()) {
            item {
                Text("Eşleşmiş Bluetooth cihazı bulunamadı. Önce telefonun Bluetooth ayarlarından ELM327 (PIN: 1234 veya 0000) cihazını eşleştirin.", color = TextSecondary, fontSize = 12.sp)
            }
        } else {
            items(pairedDevices) { dev ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSlate),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onSelectDevice(dev) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(dev.name ?: "İsimsiz Cihaz", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(dev.address, color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text("Demo Modu (Adaptör Kalitesi Simülasyonu)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { onStartDemo(AdapterTier.TIER_1_PIC18F25K80) }, modifier = Modifier.weight(1f)) {
                    Text("PIC18F", fontSize = 11.sp, color = AccentGreen)
                }
                OutlinedButton(onClick = { onStartDemo(AdapterTier.TIER_2_CLONE_V15) }, modifier = Modifier.weight(1f)) {
                    Text("Klon v1.5", fontSize = 11.sp, color = AccentAmber)
                }
                OutlinedButton(onClick = { onStartDemo(AdapterTier.TIER_3_CLONE_V21) }, modifier = Modifier.weight(1f)) {
                    Text("Sahte v2.1", fontSize = 11.sp, color = AccentRed)
                }
            }
        }

        item {
            Text("Canlı AT Protokol Konsolu", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        items(conn.logs.reversed()) { log ->
            Row(modifier = Modifier.fillMaxWidth().background(CardSlate, RoundedCornerShape(8.dp)).padding(8.dp)) {
                Text(
                    "[${log.timestamp}] ${log.command} -> ${log.response}",
                    color = if (log.isSuccess) AccentGreen else AccentAmber,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        }
    }
}
