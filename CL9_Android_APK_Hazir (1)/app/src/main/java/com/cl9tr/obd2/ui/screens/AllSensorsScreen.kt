package com.cl9tr.obd2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cl9tr.obd2.model.LivePidValue
import com.cl9tr.obd2.model.PidCategory
import com.cl9tr.obd2.model.PidDefinition
import com.cl9tr.obd2.obd.K24A3PidEngine
import com.cl9tr.obd2.ui.theme.*

@Composable
fun AllSensorsScreen(
    liveValues: Map<String, LivePidValue>,
    pinnedPids: Set<String>,
    onTogglePin: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<PidCategory?>(null) }
    var infoPid by remember { mutableStateOf<PidDefinition?>(null) }
    val filtered = K24A3PidEngine.allPids.filter { selectedCategory == null || it.category == selectedCategory }

    infoPid?.let { pid ->
        val live = liveValues[pid.pidHex]
        AlertDialog(
            onDismissRequest = { infoPid = null },
            containerColor = CardSlate,
            title = { Text("${pid.pidHex} • ${pid.nameTr}", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Anlık: ${live?.formattedValue ?: "--"} ${pid.unit}  |  Normal: ${pid.normalRange}", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Ne İşe Yarar?\n${pid.roleTr}", color = TextPrimary, fontSize = 12.sp)
                    Text("Veri Ne Anlama Gelir?\n${pid.meaningTr}", color = TextSecondary, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(onClick = { infoPid = null }, colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)) {
                    Text("Anladım", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgObsidian)
            .padding(14.dp)
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("Tümü (${K24A3PidEngine.allPids.size})") }
                )
            }
            items(PidCategory.entries) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat.title) }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered) { def ->
                val live = liveValues[def.pidHex]
                val pinned = pinnedPids.contains(def.pidHex)
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSlate),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().clickable { infoPid = def }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(14.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(def.nameTr, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Normal: ${def.normalRange}", color = TextSecondary, fontSize = 11.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (live != null) "${live.formattedValue} ${def.unit}" else "-- ${def.unit}",
                                color = if (live?.isWarning == true) AccentRed else AccentCyan,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            TextButton(
                                onClick = { onTogglePin(def.pidHex) },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(if (pinned) "Panelde ★" else "+ Panele", fontSize = 11.sp, color = if (pinned) AccentGreen else TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}
