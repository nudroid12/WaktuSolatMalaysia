package app.nudroidlabs.waktusolat.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.waktusolat.data.JakimZones
import app.nudroidlabs.waktusolat.data.PrayerDataOrigin
import app.nudroidlabs.waktusolat.R
import app.nudroidlabs.waktusolat.data.PrayerResponse
import app.nudroidlabs.waktusolat.data.PrayerTimeDisplayFormatter
import app.nudroidlabs.waktusolat.data.TimeFormatMode
import app.nudroidlabs.waktusolat.data.PrayerTimeEngine
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    modifier: Modifier,
    zoneCode: String,
    data: PrayerResponse?,
    dataOrigin: PrayerDataOrigin?,
    cacheSavedAt: Long,
    loading: Boolean,
    error: String?,
    timeFormatMode: TimeFormatMode,
    detectingLocation: Boolean,
    homeLocationMessage: String?,
    onChooseZone: () -> Unit,
    onDetectLocation: () -> Unit,
    onRefresh: () -> Unit
) {
    val malaysiaZone = remember { ZoneId.of("Asia/Kuala_Lumpur") }
    val malayLocale = remember { Locale.forLanguageTag("ms-MY") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", malayLocale) }
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            tick = System.currentTimeMillis()
            delay(1_000)
        }
    }

    val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(tick), malaysiaZone)
    val today = data?.days?.firstOrNull {
        PrayerTimeEngine.apiDate(it.dateRaw) == now.toLocalDate()
    }
    val upcoming = data?.let { PrayerTimeEngine.findUpcoming(it.days, now) }
    val zone = JakimZones.byCode(zoneCode)
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.padding(end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_launcher_monochrome),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(66.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        "Waktu Solat",
                        fontSize = 25.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(now.format(dateFormatter), color = muted, fontSize = 13.sp, lineHeight = 17.sp)
                    today?.let {
                        Text(formatHomeHijri(it.hijri), color = muted, fontSize = 13.sp, lineHeight = 17.sp)
                    }
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onChooseZone)
                            .padding(vertical = 4.dp)
                    ) {
                        Text(zone.area, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(zoneCode, color = muted, fontSize = 12.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onChooseZone) {
                            Text("Tukar", fontWeight = FontWeight.SemiBold)
                        }
                        IconButton(
                            onClick = onDetectLocation,
                            enabled = !detectingLocation,
                            modifier = Modifier.size(48.dp)
                        ) {
                            if (detectingLocation) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.ic_location_target),
                                    contentDescription = "Kesan lokasi automatik",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }
                    }
                }
            }

            homeLocationMessage?.takeIf(String::isNotBlank)?.let { message ->
                Spacer(Modifier.height(4.dp))
                Text(
                    message,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                    fontSize = 11.sp
                )
            }
        }

        if (upcoming != null) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.5.dp, accent)
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text("Solat seterusnya", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(5.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                upcoming.name,
                                modifier = Modifier.weight(1f),
                                color = accent,
                                fontSize = 29.sp,
                                lineHeight = 34.sp,
                                fontWeight = FontWeight.Bold
                            )
                            VerticalDivider(
                                modifier = Modifier.height(28.dp),
                                thickness = 1.dp,
                                color = accent
                            )
                            Text(
                                PrayerTimeDisplayFormatter.formatLocalTime(
                                    upcoming.target.toLocalTime(),
                                    timeFormatMode
                                ),
                                modifier = Modifier.weight(1f),
                                style = TabularTimeStyle,
                                textAlign = TextAlign.End,
                                color = accent,
                                fontSize = 29.sp,
                                lineHeight = 34.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Lagi ${PrayerTimeEngine.countdown(upcoming.target, now)}",
                            modifier = Modifier.fillMaxWidth(),
                            style = TabularTimeStyle,
                            textAlign = TextAlign.Center,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        if (loading && data == null) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        if (error != null && data == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Data tidak dapat dimuatkan", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(error, fontSize = 13.sp)
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = onRefresh) { Text("Cuba lagi") }
                    }
                }
            }
        }

        if (today != null) {
            item {
                PrayerTimesCard(
                    day = today,
                    title = "Waktu hari ini",
                    highlightedPrayer = upcoming?.takeIf { it.target.toLocalDate() == now.toLocalDate() }?.name,
                    timeFormatMode = timeFormatMode
                )
            }
        }

        if (data != null) {
            item {
                DataStatusCard(
                    origin = dataOrigin,
                    cacheSavedAt = cacheSavedAt,
                    serverTime = data.serverTime,
                    loading = loading,
                    error = error,
                    onRefresh = onRefresh,
                    malaysiaZone = malaysiaZone,
                    timeFormatMode = timeFormatMode
                )
            }
        }
    }
}

private val HijriMonths = listOf(
    "Muharam", "Safar", "Rabiulawal", "Rabiulakhir", "Jamadilawal", "Jamadilakhir",
    "Rejab", "Syaaban", "Ramadan", "Syawal", "Zulkaedah", "Zulhijah"
)

private fun formatHomeHijri(raw: String): String {
    val parts = raw.split('-')
    if (parts.size != 3) return "Hijrah $raw"
    val year = parts[0].toIntOrNull() ?: return "Hijrah $raw"
    val month = parts[1].toIntOrNull()?.let { HijriMonths.getOrNull(it - 1) }
        ?: return "Hijrah $raw"
    val day = parts[2].toIntOrNull()?.takeIf { it in 1..30 } ?: return "Hijrah $raw"
    return "$day $month ${year}H"
}

internal val TabularTimeStyle = TextStyle(fontFeatureSettings = "tnum")

@Composable
private fun DataStatusCard(
    origin: PrayerDataOrigin?,
    cacheSavedAt: Long,
    serverTime: String,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    malaysiaZone: ZoneId,
    timeFormatMode: TimeFormatMode
) {
    val status = when (origin) {
        PrayerDataOrigin.NETWORK -> "Dikemas kini daripada e-Solat JAKIM"
        PrayerDataOrigin.CACHE_FRESH -> "Menggunakan cache yang masih segar"
        PrayerDataOrigin.CACHE_FALLBACK -> "Offline: menggunakan data cache terakhir"
        null -> "Sumber: e-Solat JAKIM"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Status data",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        status,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    enabled = !loading,
                    modifier = Modifier.size(36.dp)
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_refresh),
                            contentDescription = "Kemas kini data",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }

            if (cacheSavedAt > 0L) {
                val saved = LocalDateTime.ofInstant(Instant.ofEpochMilli(cacheSavedAt), malaysiaZone)
                Text(
                    "Cache ${PrayerTimeDisplayFormatter.formatShortDateTime(saved, timeFormatMode)}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                )
            }
            if (serverTime.isNotBlank()) {
                Text(
                    "Pelayan JAKIM $serverTime",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                )
            }
            error?.let {
                Spacer(Modifier.height(3.dp))
                Text(
                    "Kemas kini gagal: $it",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
