package com.cl9tr.obd2.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cl9tr.obd2.model.FuelMetrics
import com.cl9tr.obd2.model.PerfMetrics
import com.cl9tr.obd2.ui.theme.*
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PerformanceScreen(
    metrics: PerfMetrics,
    trackedKm: Int,
    trackedHours: Double,
    onStartSim: () -> Unit
) {
    if (!metrics.hasData) {
        Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            Surface(shape = RoundedCornerShape(18.dp), color = CardSlate, border = BorderStroke(1.dp, Color(0xFF1E293B)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Henüz Hesaplama Yapılmadı", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Anlık ve pik beygir, tork ile tekerlek gücü verileri yalnızca araca bağlanıp canlı veri toplandığında gösterilir.", color = TextSecondary, fontSize = 12.sp)
                    Button(onClick = onStartSim, colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = BgObsidian), shape = RoundedCornerShape(12.dp)) {
                        Text("Simülasyon ile Hesaplamayı Başlat", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 18.dp)
    ) {
        item {
            Surface(shape = RoundedCornerShape(14.dp), color = CardSlate, border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.3f)), modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Veri Dayanağı: Son $trackedKm km ve ${String.format(Locale.US, "%.1f", trackedHours)} saatlik sürüş verisi.",
                    color = AccentCyan,
                    fontSize = 11.5.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Motor (Krank) Anlık & Pik
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricBox("Anlık Beygir", "${metrics.liveHp}", "BG", AccentCyan, Modifier.weight(1f))
                    MetricBox("Anlık Tork", "${metrics.liveNm}", "Nm", AccentAmber, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricBox("Pik Beygir", if (metrics.peakHp > 0) "${metrics.peakHp}" else "--", "BG", TextPrimary, Modifier.weight(1f), AccentCyan)
                    MetricBox("Pik Tork", if (metrics.peakNm > 0) "${metrics.peakNm}" else "--", "Nm", TextPrimary, Modifier.weight(1f), AccentAmber)
                }
            }
        }

        item {
            CurveCard("Motor Beygir ve Tork Grafiği", metrics.crankCurveHp, metrics.crankCurveNm, AccentCyan, AccentAmber)
        }

        // Tekerlek (Wheel) Anlık & Pik
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricBox("Anlık WHP", "${metrics.liveWhp}", "WHP", AccentGreen, Modifier.weight(1f))
                    MetricBox("Anlık WTork", "${metrics.liveWnm}", "Nm", AccentViolet, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricBox("Pik WHP", if (metrics.peakWhp > 0) "${metrics.peakWhp}" else "--", "WHP", TextPrimary, Modifier.weight(1f), AccentGreen)
                    MetricBox("Pik WTork", if (metrics.peakWnm > 0) "${metrics.peakWnm}" else "--", "Nm", TextPrimary, Modifier.weight(1f), AccentViolet)
                }
            }
        }

        item {
            Text(
                text = "Pik değerler 3 ardışık ölçüm medyanı ve fiziksel üst sınır filtresi ile doğrulanır; anlık sensör sıçramaları elenir.",
                color = TextSecondary,
                fontSize = 10.5.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        item {
            CurveCard("Tekerlek Gücü ve Tork Grafiği", metrics.wheelCurveHp, metrics.wheelCurveNm, AccentGreen, AccentViolet)
        }

        item {
            Surface(shape = RoundedCornerShape(16.dp), color = CardSlate, border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.35f)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Yapay Zeka Performans Yorumu", color = AccentAmber, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    Text(metrics.aiComment, color = TextPrimary, fontSize = 12.sp, lineHeight = 17.sp)
                    Text(
                        "Not: Tam gaz (Açık Döngü) anındaki güç ve tork verileri standart OBD-II sensörlerinden algoritmalara dayalı teorik olarak hesaplanır.",
                        color = TextSecondary,
                        fontSize = 10.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    title: String,
    value: String,
    unit: String,
    valColor: Color,
    modifier: Modifier,
    borderAccent: Color = Color(0xFF1E293B)
) {
    Surface(shape = RoundedCornerShape(14.dp), color = CardSlate, border = BorderStroke(1.dp, borderAccent.copy(alpha = 0.45f)), modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(value, color = valColor, fontSize = 22.sp, fontWeight = FontWeight.Black, maxLines = 1)
                Text(unit, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
    }
}

@Composable
private fun CurveCard(title: String, hp: List<Int>, nm: List<Int>, c1: Color, c2: Color) {
    Surface(shape = RoundedCornerShape(16.dp), color = CardSlate, border = BorderStroke(1.dp, Color(0xFF1E293B)), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Canvas(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                if (hp.size < 2 || nm.size < 2) return@Canvas
                val maxVal = maxOf(hp.maxOrNull() ?: 200, nm.maxOrNull() ?: 220).coerceAtLeast(100).toFloat()
                val stepX = size.width / (hp.size - 1).coerceAtLeast(1)
                fun drawSeries(vals: List<Int>, color: Color) {
                    val path = Path()
                    vals.forEachIndexed { idx, v ->
                        val x = idx * stepX
                        val y = size.height - (v / maxVal) * (size.height * 0.9f) - 6f
                        if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, color = color, style = Stroke(width = 5f, cap = StrokeCap.Round))
                }
                drawSeries(hp, c1)
                drawSeries(nm, c2)
            }
        }
    }
}

@Composable
fun FuelAnalysisScreen(
    fuel: FuelMetrics,
    onStartSim: () -> Unit
) {
    if (!fuel.hasData) {
        Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            Surface(shape = RoundedCornerShape(18.dp), color = CardSlate, border = BorderStroke(1.dp, Color(0xFF1E293B)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Henüz Tüketim Verisi Yok", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Anlık ekonomi göstergesi, yakıt harcaması ve yapay zeka yorumu araca bağlanıp canlı veri toplandığında çalışır.", color = TextSecondary, fontSize = 12.sp)
                    Button(onClick = onStartSim, colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = BgObsidian), shape = RoundedCornerShape(12.dp)) {
                        Text("Simülasyon ile Hesaplamayı Başlat", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 18.dp)
    ) {
        // 1. EN ÜST: BMW Tarzı Anlık Ekonomi Göstergesi
        item {
            Surface(shape = RoundedCornerShape(16.dp), color = CardSlate, border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.3f)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Ekonomi Göstergesi (Anlık)", color = TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                        Surface(shape = RoundedCornerShape(99.dp), color = AccentGreen.copy(alpha = 0.2f)) {
                            Text("${fuel.activeFuelName} Aktif", color = AccentGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }
                    Canvas(modifier = Modifier.width(230.dp).height(124.dp)) {
                        val stroke = Stroke(width = 24f, cap = StrokeCap.Round)
                        val arcSize = Size(size.width - 28f, (size.width - 28f))
                        val topLeft = Offset(14f, 14f)
                        drawArc(Color(0xFF1E293B), 180f, 180f, false, topLeft, arcSize, style = stroke)
                        drawArc(AccentGreen, 180f, 72f, false, topLeft, arcSize, style = stroke)
                        drawArc(AccentAmber, 252f, 45f, false, topLeft, arcSize, style = stroke)
                        drawArc(AccentRed, 297f, 63f, false, topLeft, arcSize, style = stroke)

                        val clamped = fuel.instL.coerceIn(0.0, 20.0)
                        val angleRad = (180.0 + (clamped / 20.0) * 180.0) * PI / 180.0
                        val cx = size.width / 2f
                        val cy = size.width / 2f
                        val r = (size.width / 2f) - 26f
                        val ex = cx + (r * cos(angleRad)).toFloat()
                        val ey = cy + (r * sin(angleRad)).toFloat()
                        drawLine(Color(0xFFFF6B00), Offset(cx, cy), Offset(ex, ey), strokeWidth = 9f, cap = StrokeCap.Round)
                        drawCircle(Color(0xFFFF6B00), radius = 12f, center = Offset(cx, cy))
                    }
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(String.format(Locale.US, "%.1f", fuel.instL), color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Black)
                        Text(if (fuel.isIdleUnit) "L/sa (Rölanti)" else "L/100 km", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 3.dp))
                    }
                    val (zoneTxt, zoneCol) = when {
                        fuel.instL <= 8.0 -> "Ekonomik Bölge" to AccentGreen
                        fuel.instL <= 13.0 -> "Dengeli Tüketim" to AccentAmber
                        else -> "Yüksek Tüketim Bölgesi" to AccentRed
                    }
                    Text(zoneTxt, color = zoneCol, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. Yakıt Kalitesi (Tahmini Oktan dahil) & Yanma Kalitesi
        item {
            Surface(shape = RoundedCornerShape(16.dp), color = CardSlate, border = BorderStroke(1.dp, Color(0xFF1E293B)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Yakıt Kalitesi", color = TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(shape = RoundedCornerShape(99.dp), color = AccentAmber.copy(alpha = 0.16f)) {
                                    Text("Tahmini ~${fuel.estOctane} Oktan", color = AccentAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                }
                                Text("%${fuel.fuelQualityPct}", color = AccentGreen, fontSize = 12.5.sp, fontWeight = FontWeight.Black, maxLines = 1)
                            }
                        }
                        SimpleBar(fuel.fuelQualityPct / 100f, AccentGreen)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Yanma Kalitesi", color = TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("%${fuel.combQualityPct}", color = AccentCyan, fontSize = 12.5.sp, fontWeight = FontWeight.Black, maxLines = 1)
                        }
                        SimpleBar(fuel.combQualityPct / 100f, AccentCyan)
                    }
                }
            }
        }

        // 3. Yakıt Tüketimi 3 Bar (Ortalama Tüketim L/100 km başlığı tek satırda)
        item {
            Surface(shape = RoundedCornerShape(16.dp), color = CardSlate, border = BorderStroke(1.dp, Color(0xFF1E293B)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Yakıt Tüketimi", color = AccentGreen, fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                        Text("L/100 km (${fuel.activeFuelName})", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    LabeledBar("Anlık Tüketim (10 sn)", "${fuel.avg10sL} L/100km", (fuel.avg10sL / 20.0).toFloat(), AccentCyan)
                    LabeledBar("Ortalama Tüketim L/100 km", "${fuel.tripAvgL} L/100km", (fuel.tripAvgL / 20.0).toFloat(), AccentGreen)
                    LabeledBar("Tüm Zamanlar Ortalaması", "${fuel.allTimeAvgL} L/100km", (fuel.allTimeAvgL / 20.0).toFloat(), AccentAmber)
                }
            }
        }

        // 4. TL Cinsinden Harcama (Son 100 km & Son 1000 km)
        item {
            Surface(shape = RoundedCornerShape(16.dp), color = CardSlate, border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.35f)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Yakıt Harcaması (₺)", color = AccentAmber, fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                        Text("${fuel.cityName} • ${fuel.activeFuelName} ${String.format(Locale.US, "%.2f", fuel.unitPriceTl)} ₺/L", color = TextSecondary, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Son 100 km'de ${fuel.cost100Tl} ₺'lik yakıt harcandı", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        SimpleBar((fuel.cost100Tl / 1500f).coerceIn(0f, 1f), AccentGreen)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Son 1000 km'de ${fuel.cost1000Tl} ₺'lik yakıt harcandı", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        SimpleBar((fuel.cost1000Tl / 15000f).coerceIn(0f, 1f), AccentAmber)
                    }
                }
            }
        }

        // 5. En Altta: Yapay Zeka Yakıt Tüketimi Yorumu
        item {
            val toneCol = when (fuel.aiTone) {
                "Eleştiri", "Anormal Tüketim" -> AccentRed
                "Tavsiye" -> AccentAmber
                else -> AccentGreen
            }
            Surface(shape = RoundedCornerShape(16.dp), color = CardSlate, border = BorderStroke(1.dp, toneCol.copy(alpha = 0.4f)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Yapay Zeka Yakıt Tüketimi Yorumu", color = toneCol, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                        Surface(shape = RoundedCornerShape(99.dp), color = toneCol.copy(alpha = 0.2f)) {
                            Text(fuel.aiTone, color = toneCol, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }
                    Text(fuel.aiComment, color = TextPrimary, fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
        }
    }
}

@Composable
private fun SimpleBar(fraction: Float, color: Color) {
    Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(99.dp)).background(Color(0xFF0F172A))) {
        Box(modifier = Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(99.dp)).background(color))
    }
}

@Composable
private fun LabeledBar(label: String, value: String, fraction: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        }
        SimpleBar(fraction.coerceIn(0f, 1f), color)
    }
}
