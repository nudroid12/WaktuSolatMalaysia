package app.nudroidlabs.waktusolat.ui

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.waktusolat.data.PrayerTimeDisplayFormatter
import app.nudroidlabs.waktusolat.data.TimeFormatMode
import app.nudroidlabs.waktusolat.location.SavedLocation
import app.nudroidlabs.waktusolat.qibla.QiblaAlignmentGate
import app.nudroidlabs.waktusolat.qibla.QiblaCalculator
import app.nudroidlabs.waktusolat.qibla.QiblaSensor
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun QiblaScreen(
    modifier: Modifier,
    location: SavedLocation?,
    jakimBearing: String?,
    detectingLocation: Boolean,
    locationMessage: String?,
    timeFormatMode: TimeFormatMode,
    onDetectLocation: () -> Unit
) {
    val context = LocalContext.current
    var trueHeading by remember(location) { mutableStateOf<Float?>(null) }
    var sensorAvailable by remember(location) { mutableStateOf(true) }

    val qiblaBearing = remember(location) {
        location?.let {
            QiblaCalculator.bearingDegrees(it.latitude, it.longitude)
        }
    }

    DisposableEffect(location) {
        if (location == null) {
            trueHeading = null
            sensorAvailable = true
            onDispose { }
        } else {
            val sensor = QiblaSensor(context, location) { heading ->
                trueHeading = heading
            }
            sensorAvailable = sensor.start()
            onDispose { sensor.stop() }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Arah Kiblat", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                "Kompas aktif hanya ketika halaman ini dibuka",
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (location == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Lokasi diperlukan", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Untuk kompas kiblat yang mengambil kira kedudukan sebenar dan " +
                                "deklinasi magnet, kesan lokasi sekali dahulu."
                        )
                        jakimBearing?.takeIf(String::isNotBlank)?.let {
                            Spacer(Modifier.height(8.dp))
                            Text("Rujukan arah kiblat zon JAKIM: $it", fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onDetectLocation, enabled = !detectingLocation) {
                            Text(if (detectingLocation) "Mengesan lokasi..." else "Kesan lokasi")
                        }
                        locationMessage?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, fontSize = 13.sp)
                        }
                    }
                }
            }
        } else {
            item {
                QiblaCompassCard(
                    qiblaBearing = qiblaBearing ?: 0.0,
                    trueHeading = trueHeading,
                    sensorAvailable = sensorAvailable
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(
                            "Lokasi kiblat",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(location.addressText ?: "Lokasi semasa", fontWeight = FontWeight.Bold)
                        Text(
                            "Ketepatan lokasi terakhir: ±${location.accuracyMetres.toInt().coerceAtLeast(1)} m",
                            fontSize = 12.sp
                        )
                        if (location.capturedAtMillis > 0L) {
                            Text(
                                "Dikesan: ${PrayerTimeDisplayFormatter.formatFullMalaysiaDateTime(location.capturedAtMillis, timeFormatMode)}",
                                fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = onDetectLocation, enabled = !detectingLocation) {
                            Text(if (detectingLocation) "Mengesan..." else "Kemas kini lokasi")
                        }
                        locationMessage?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        item {
            Text(
                "Jauhkan telefon daripada magnet, casing bermagnet dan permukaan logam. " +
                    "Jika kompas tidak stabil, gerakkan telefon dalam bentuk angka 8 untuk kalibrasi.",
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun QiblaCompassCard(
    qiblaBearing: Double,
    trueHeading: Float?,
    sensorAvailable: Boolean
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val hapticFeedback = LocalHapticFeedback.current
    val relative = trueHeading?.let { QiblaCalculator.relativeDegrees(qiblaBearing, it.toDouble()) }
    val alignmentGate = remember(qiblaBearing) { QiblaAlignmentGate() }
    val toneGenerator = remember {
        runCatching {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70)
        }.getOrNull()
    }

    DisposableEffect(toneGenerator) {
        onDispose {
            toneGenerator?.release()
        }
    }

    LaunchedEffect(relative) {
        if (alignmentGate.shouldNotify(relative)) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 260)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Kiblat ${"%.1f".format(Locale.US, qiblaBearing)}° dari utara benar")
            Spacer(Modifier.height(14.dp))

            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(260.dp)) {
                    val radius = size.minDimension * 0.43f
                    val centre = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(
                        color = onSurface.copy(alpha = 0.22f),
                        radius = radius,
                        center = centre,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                    )

                    for (degree in 0 until 360 step 30) {
                        val rad = Math.toRadians(degree.toDouble())
                        val inner = radius * if (degree % 90 == 0) 0.82f else 0.88f
                        val start = Offset(
                            centre.x + sin(rad).toFloat() * inner,
                            centre.y - cos(rad).toFloat() * inner
                        )
                        val end = Offset(
                            centre.x + sin(rad).toFloat() * radius,
                            centre.y - cos(rad).toFloat() * radius
                        )
                        drawLine(
                            color = onSurface.copy(alpha = 0.45f),
                            start = start,
                            end = end,
                            strokeWidth = if (degree % 90 == 0) 5f else 3f,
                            cap = StrokeCap.Round
                        )
                    }

                    if (relative != null) {
                        val rad = Math.toRadians(relative)
                        val end = Offset(
                            centre.x + sin(rad).toFloat() * radius * 0.75f,
                            centre.y - cos(rad).toFloat() * radius * 0.75f
                        )
                        drawLine(
                            color = primary,
                            start = centre,
                            end = end,
                            strokeWidth = 12f,
                            cap = StrokeCap.Round
                        )
                        drawCircle(primary, radius = 12f, center = end)
                    }
                    drawCircle(surfaceVariant, radius = 13f, center = centre)
                    drawCircle(primary, radius = 7f, center = centre)
                }

                Text(
                    "↑",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp),
                    color = onSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(10.dp))
            when {
                !sensorAvailable -> Text(
                    "Sensor kompas tidak tersedia pada peranti ini.",
                    fontWeight = FontWeight.SemiBold
                )
                trueHeading == null -> Text("Menunggu bacaan kompas...")
                else -> {
                    Text(
                        "Haluan telefon ${"%.0f".format(Locale.US, trueHeading)}°",
                        fontWeight = FontWeight.SemiBold
                    )
                    val offset = relative ?: 0.0
                    val guidance = when {
                        kotlin.math.abs(offset) <= 3.0 -> "Arah kiblat sejajar"
                        offset > 0 -> "Pusing ke kanan ${"%.0f".format(Locale.US, kotlin.math.abs(offset))}°"
                        else -> "Pusing ke kiri ${"%.0f".format(Locale.US, kotlin.math.abs(offset))}°"
                    }
                    Text(guidance, color = primary, fontWeight = FontWeight.Bold)
                    if (kotlin.math.abs(offset) <= 3.0) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Bunyi dan getaran mengesahkan arah sejajar.",
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
