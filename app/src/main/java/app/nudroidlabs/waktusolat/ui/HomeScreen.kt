package app.nudroidlabs.waktusolat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.waktusolat.R
import app.nudroidlabs.waktusolat.data.JakimZones
import app.nudroidlabs.waktusolat.data.PrayerDataOrigin
import app.nudroidlabs.waktusolat.data.PrayerDay
import app.nudroidlabs.waktusolat.data.PrayerResponse
import app.nudroidlabs.waktusolat.data.PrayerTimeDisplayFormatter
import app.nudroidlabs.waktusolat.data.PrayerTimeEngine
import app.nudroidlabs.waktusolat.data.TimeFormatMode
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        val tight = maxHeight < 610.dp
        val gap = if (tight) 5.dp else 7.dp
        val titleSize = if (tight) 21.sp else 24.sp
        val nextPrayerSize = if (tight) 28.sp else 32.sp

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Waktu Solat & Kiblat",
                        fontSize = titleSize,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        now.format(dateFormatter),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                        fontSize = 11.sp
                    )
                }
                if (data != null) {
                    TextButton(onClick = onRefresh, enabled = !loading) {
                        Text(if (loading) "..." else "↻", fontSize = 20.sp)
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = if (tight) 7.dp else 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onChooseZone)
                    ) {
                        Text(
                            "${zone.state} · $zoneCode",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            zone.area,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Tukar",
                            modifier = Modifier.clickable(onClick = onChooseZone),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = onDetectLocation,
                            enabled = !detectingLocation
                        ) {
                            if (detectingLocation) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(11.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.ic_location_target),
                                    contentDescription = "Kesan lokasi automatik",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            homeLocationMessage?.takeIf(String::isNotBlank)?.let { message ->
                Text(
                    message,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }

            if (upcoming != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = if (tight) 10.dp else 13.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "SOLAT SETERUSNYA",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                upcoming.name,
                                fontSize = nextPrayerSize,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                PrayerTimeDisplayFormatter.formatLocalTime(
                                    upcoming.target.toLocalTime(),
                                    timeFormatMode
                                ),
                                fontSize = if (tight) 20.sp else 23.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Lagi ${PrayerTimeEngine.countdown(upcoming.target, now)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            when {
                loading && data == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                error != null && data == null -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Data tidak dapat dimuatkan", fontWeight = FontWeight.Bold)
                            Text(error, fontSize = 12.sp)
                            Button(onClick = onRefresh) { Text("Cuba lagi") }
                        }
                    }
                }

                today != null -> {
                    CompactTodayCard(
                        modifier = Modifier.weight(1f),
                        day = today,
                        highlightedPrayer = upcoming?.name,
                        timeFormatMode = timeFormatMode,
                        tight = tight
                    )
                }
            }

            if (data != null) {
                HomeStatusLine(
                    origin = dataOrigin,
                    cacheSavedAt = cacheSavedAt,
                    loading = loading,
                    error = error,
                    malaysiaZone = malaysiaZone,
                    timeFormatMode = timeFormatMode
                )
            }
        }
    }
}

@Composable
private fun CompactTodayCard(
    modifier: Modifier,
    day: PrayerDay,
    highlightedPrayer: String?,
    timeFormatMode: TimeFormatMode,
    tight: Boolean
) {
    val rows = listOf(
        "Imsak" to day.imsak,
        "Subuh" to day.subuh,
        "Syuruk" to day.syuruk,
        "Dhuha" to day.dhuha,
        "Zohor" to day.zohor,
        "Asar" to day.asar,
        "Maghrib" to day.maghrib,
        "Isyak" to day.isyak
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = if (tight) 7.dp else 9.dp
            ),
            verticalArrangement = Arrangement.spacedBy(if (tight) 5.dp else 7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Waktu hari ini",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(day.dateRaw, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Text("Hijrah ${day.hijri}", fontSize = 10.sp)
            }

            rows.chunked(4).forEach { group ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    group.forEach { (name, raw) ->
                        CompactPrayerCell(
                            modifier = Modifier.weight(1f),
                            name = name,
                            time = PrayerTimeDisplayFormatter.formatApiTime(raw, timeFormatMode),
                            highlighted = name == highlightedPrayer,
                            tight = tight
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactPrayerCell(
    modifier: Modifier,
    name: String,
    time: String,
    highlighted: Boolean,
    tight: Boolean
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (highlighted) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
        }
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 5.dp,
                vertical = if (tight) 5.dp else 7.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                name,
                fontSize = 9.sp,
                color = if (highlighted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                },
                maxLines = 1
            )
            Text(
                time,
                fontSize = if (tight) 13.sp else 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HomeStatusLine(
    origin: PrayerDataOrigin?,
    cacheSavedAt: Long,
    loading: Boolean,
    error: String?,
    malaysiaZone: ZoneId,
    timeFormatMode: TimeFormatMode
) {
    val status = when (origin) {
        PrayerDataOrigin.NETWORK -> "JAKIM · online"
        PrayerDataOrigin.CACHE_FRESH -> "JAKIM · cache"
        PrayerDataOrigin.CACHE_FALLBACK -> "JAKIM · offline cache"
        null -> "e-Solat JAKIM"
    }

    val cache = if (cacheSavedAt > 0L) {
        val saved = LocalDateTime.ofInstant(Instant.ofEpochMilli(cacheSavedAt), malaysiaZone)
        PrayerTimeDisplayFormatter.formatShortDateTime(saved, timeFormatMode)
    } else {
        null
    }

    val detail = when {
        loading -> "Mengemas kini"
        error != null -> "Kemas kini gagal"
        cache != null -> "$status · $cache"
        else -> status
    }

    Text(
        detail,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        fontSize = 9.sp,
        maxLines = 1
    )
}
