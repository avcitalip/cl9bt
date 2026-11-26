package com.cl9tr.obd2.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cl9tr.obd2.model.*
import com.cl9tr.obd2.obd.K24A3PidEngine
import com.cl9tr.obd2.ui.theme.*

@Composable
fun DashboardScreen(
    conn: ConnectionState,
    liveValues: Map<String, LivePidValue>,
    pinnedPids: Set<String>,
    customNames: Map<String, String>,
    simScenario: SimScenario,
    onSelectScenario: (SimScenario) -> Unit,
    onStartSim: () -> Unit
) {
    val isConnected = conn.phase == ConnectionPhase.CONNECTED || conn.phase == ConnectionPhase.DEMO_MODE
    val isSim = conn.phase == ConnectionPhase.DEMO_MODE
    val pinnedDefs = K24A3PidEngine.allPids.filter { pinnedPids.contains(it.pidHex) && it.pidHex != "0142" }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 18.dp)
    ) {
        if (!isConnected) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AccentAmber.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Canlı göstergeler için bağlantı veya simülasyon başlatın.", color = TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = onStartSim,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = BgObsidian),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Başlat", fontWeight = FontWeight.ExtraBold, fontSize = 11.5.sp)
                        }
                    }
                }
            }
        }

        // Sürüş Senaryosu YALNIZCA Simülasyon Modunda görünür!
        if (isSim) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CardSlate,
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Sürüş Senaryosu (Simülasyon)", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SimScenario.entries.forEach { sc ->
                                val active = sc == simScenario
                                val hot = sc == SimScenario.OVERHEAT
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = when {
                                        active && hot -> AccentRed
                                        active -> AccentCyan
                                        else -> Color(0xFF0F172A)
                                    },
                                    modifier = Modifier.weight(1f),
                                    onClick = { onSelectScenario(sc) }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                                    ) {
                                        Text(
                                            text = sc.label,
                                            color = if (active) (if (hot) Color.White else BgObsidian) else (if (hot) AccentRed else TextPrimary),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        items(pinnedDefs) { def ->
            val live = liveValues[def.pidHex]
            val title = customNames[def.pidHex] ?: def.nameTr.replace("Motor Suyu", "Motor Soğutma Sıvısı")
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardSlate,
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(def.normalRange, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.width(12.dp))
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isConnected && live != null) live.formattedValue else "--",
                            color = AccentCyan,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = if (def.unit == "rpm") "d/dk" else def.unit,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
