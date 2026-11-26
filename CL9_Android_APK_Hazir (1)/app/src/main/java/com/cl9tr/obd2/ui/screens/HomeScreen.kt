package com.cl9tr.obd2.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cl9tr.obd2.model.*
import com.cl9tr.obd2.ui.theme.*

data class HomeMenuDef(val id: String, val title: String, val sub: String, val icon: ImageVector, val color: Color)

private val ALL_MENU_DEFS = mapOf(
    "dashboard" to HomeMenuDef("dashboard", "Canlı Gösterge Paneli", "Devir, soğutma sıvısı, basınç ve AFR", Icons.Default.Speed, AccentCyan),
    "performance" to HomeMenuDef("performance", "Hesaplanan Motor Performansı", "Anlık ve pik beygir, tork, WHP ve WTork", Icons.Default.LocalFireDepartment, AccentAmber),
    "fuel" to HomeMenuDef("fuel", "Yakıt Tüketimi Yorumlaması", "Ekonomi ibresi, oktan, TL harcama ve yorum", Icons.Default.LocalGasStation, AccentGreen),
    "sensors" to HomeMenuDef("sensors", "Tüm Sensörler", "Bilgi, isim düzenleme ve canlı değerler", Icons.Default.ShowChart, AccentGreen),
    "dtc" to HomeMenuDef("dtc", "Arıza Kodları", "Motor ve otomatik şanzıman kodları", Icons.Default.Warning, AccentRed),
    "maintenance" to HomeMenuDef("maintenance", "Muhtemel Arızalar ve Bakımsızlıklar", "Sensör verilerine dayalı yapay zeka teşhisi", Icons.Default.Build, AccentAmber),
    "connect" to HomeMenuDef("connect", "Adaptör ve Protokol Analizi", "Bağlantı kalitesi ve 6 kademeli tarama", Icons.Default.Bluetooth, AccentBlue),
    "logger" to HomeMenuDef("logger", "Sürüş Veri Kaydı", "Canlı kayıt al ve tablo olarak paylaş", Icons.Default.Description, AccentViolet),
    "advice" to HomeMenuDef("advice", "Test ve Bilgilendirme", "Akü/alternatör, lastik, emisyon, beyin", Icons.Default.AutoAwesome, AccentViolet)
)

@Composable
fun HomeScreen(
    conn: ConnectionState,
    profile: VehicleProfile,
    banner: HomeBannerState,
    dtcCount: Int,
    liveRpm: String,
    menuOrder: List<String>,
    reorderMode: Boolean,
    onMoveItem: (Int, Int) -> Unit,
    onNavigate: (String) -> Unit,
    onOpenProfileSwitcher: () -> Unit,
    onToggleSim: () -> Unit,
    onTestMeteoAlert: () -> Unit
) {
    val isConnected = conn.phase == ConnectionPhase.CONNECTED || conn.phase == ConnectionPhase.DEMO_MODE

    val bgColor = when (banner.level) {
        AlertLevel.GREEN -> Color(0x33059669)
        AlertLevel.BLUE -> Color(0x3D0284C7)
        AlertLevel.ORANGE -> Color(0x33D97706)
        AlertLevel.RED -> Color(0x4DE11D48)
    }
    val borderColor = when (banner.level) {
        AlertLevel.GREEN -> Color(0xAA10B981)
        AlertLevel.BLUE -> Color(0xBB0EA5E9)
        AlertLevel.ORANGE -> Color(0xBBF59E0B)
        AlertLevel.RED -> Color(0xDDF43F5E)
    }
    val accentColor = when (banner.level) {
        AlertLevel.GREEN -> AccentGreen
        AlertLevel.BLUE -> AccentBlue
        AlertLevel.ORANGE -> AccentAmber
        AlertLevel.RED -> AccentRed
    }
    val bannerIcon = when (banner.level) {
        AlertLevel.GREEN -> Icons.Default.CheckCircle
        AlertLevel.BLUE -> Icons.Default.Thunderstorm
        AlertLevel.ORANGE -> Icons.Default.Info
        AlertLevel.RED -> Icons.Default.Report
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 18.dp)
    ) {
        // 1. EN ÜST: BİLGİLENDİRME BARI (Yeşil / Mavi MGM-AFAD 5s / Turuncu / Kırmızı)
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = bgColor,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = bannerIcon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(26.dp).padding(top = 2.dp)
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "BİLGİLENDİRME",
                                color = accentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (conn.phase == ConnectionPhase.DEMO_MODE) {
                                    IconButton(
                                        onClick = onTestMeteoAlert,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Thunderstorm,
                                            contentDescription = "MGM/AFAD Test",
                                            tint = AccentBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(99.dp),
                                    color = accentColor.copy(alpha = 0.22f)
                                ) {
                                    Text(
                                        text = banner.badge,
                                        color = TextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = banner.message,
                            color = TextPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }

        // 2. PLAKA / PROFİL GEÇİŞ VE DÜZENLEME ÇUBUĞU
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardSlate,
                border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth().clickable { onOpenProfileSwitcher() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = profile.plate.ifBlank { "34 BCA 320" },
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${profile.km} km • ${profile.fuel} • ${profile.tuneLevel}",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text("Değiştir", color = AccentCyan, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 3. SİMÜLASYON MODU BAŞLAT / BAĞLANTIYI KES
        item {
            Button(
                onClick = onToggleSim,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) AccentRed.copy(alpha = 0.2f) else AccentGreen,
                    contentColor = if (isConnected) AccentRed else BgObsidian
                )
            ) {
                Icon(if (isConnected) Icons.Default.StopCircle else Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isConnected) "Bağlantıyı Kes" else "Simülasyon Modunu Başlat",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
        }

        // 4. YATAY DİKDÖRTGEN MENÜ KARTLARI (SIRALANABİLİR)
        itemsIndexed(menuOrder) { idx, id ->
            val def = ALL_MENU_DEFS[id] ?: return@itemsIndexed
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardSlate,
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth().clickable(enabled = !reorderMode) { onNavigate(def.id) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = def.color.copy(alpha = 0.16f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(def.icon, contentDescription = null, tint = def.color, modifier = Modifier.size(21.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = def.title,
                                color = TextPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = def.sub,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (reorderMode) {
                        Row {
                            IconButton(onClick = { onMoveItem(idx, -1) }, enabled = idx > 0, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Yukarı", tint = AccentCyan)
                            }
                            IconButton(onClick = { onMoveItem(idx, 1) }, enabled = idx < menuOrder.lastIndex, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Aşağı", tint = AccentCyan)
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (def.id == "dashboard") {
                                Text(if (isConnected) liveRpm else "-- d/dk", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            } else if (def.id == "dtc") {
                                val dtcLabel = if (!isConnected) "--" else if (dtcCount > 0) "$dtcCount Kod" else "Temiz"
                                Text(dtcLabel, color = if (dtcCount > 0) AccentRed else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
