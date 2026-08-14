package app.nudroidlabs.waktusolat.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.waktusolat.data.JakimZones
import app.nudroidlabs.waktusolat.data.PrayerDataOrigin
import app.nudroidlabs.waktusolat.data.PrayerResponse
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
    onChooseZone: () -> Unit,
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
    } ?: data?.days?.firstOrNull()
    val upcoming = data?.let { PrayerTimeEngine.findUpcoming(it.days, now) }
    val zone = JakimZones.byCode(zoneCode)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("Waktu Solat Malaysia", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(
                    now.format(dateFormatter),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                    fontSize = 13.sp
                )
            }
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onChooseZone),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${zone.state} · $zoneCode",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(zone.area, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("Tukar", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (upcoming != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    )
                ) {
                    Column(Modifier.padding(22.dp)) {
                        Text(
                            "SOLAT SETERUSNYA",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(5.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(upcoming.name, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                            Text(upcoming.time, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                        ) {
                            Text(
                                "Lagi ${PrayerTimeEngine.countdown(upcoming.target, now)}",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
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
                    highlightedPrayer = upcoming?.name
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
                    malaysiaZone = malaysiaZone
                )
            }
        }
    }
}

@Composable
private fun DataStatusCard(
    origin: PrayerDataOrigin?,
    cacheSavedAt: Long,
    serverTime: String,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    malaysiaZone: ZoneId
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
        Column(Modifier.padding(18.dp)) {
            Text("Status data", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(status, fontWeight = FontWeight.Bold)
            if (cacheSavedAt > 0L) {
                val saved = LocalDateTime.ofInstant(Instant.ofEpochMilli(cacheSavedAt), malaysiaZone)
                Text(
                    "Cache terakhir ${saved.format(DateTimeFormatter.ofPattern("dd/MM, HH:mm"))}",
                    fontSize = 12.sp
                )
            }
            if (serverTime.isNotBlank()) {
                Text("Masa pelayan JAKIM: $serverTime", fontSize = 12.sp)
            }
            error?.let {
                Spacer(Modifier.height(6.dp))
                Text("Kemas kini gagal: $it", fontSize = 12.sp)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onRefresh, enabled = !loading) {
                Text(if (loading) "Mengemas kini..." else "Kemas kini sekarang")
            }
        }
    }
}
