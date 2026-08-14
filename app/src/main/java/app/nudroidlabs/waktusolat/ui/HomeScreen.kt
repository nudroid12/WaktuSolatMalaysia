package app.nudroidlabs.waktusolat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import app.nudroidlabs.waktusolat.data.PrayerResponse
import app.nudroidlabs.waktusolat.data.PrayerTimeEngine
import app.nudroidlabs.waktusolat.location.ZoneSuggestion
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Composable
fun HomeScreen(
    modifier: Modifier,
    zoneCode: String,
    data: PrayerResponse?,
    loading: Boolean,
    error: String?,
    detectingLocation: Boolean,
    locationMessage: String?,
    zoneSuggestion: ZoneSuggestion?,
    onChooseZone: () -> Unit,
    onDetectLocation: () -> Unit,
    onUseSuggestion: (ZoneSuggestion) -> Unit,
    onRefresh: () -> Unit
) {
    val malaysiaZone = remember { ZoneId.of("Asia/Kuala_Lumpur") }
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
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Waktu Solat Malaysia", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("NudroidLabs", color = MaterialTheme.colorScheme.primary)
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onChooseZone),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        zone.state,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(zone.area, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Zon $zoneCode · Tekan untuk tukar", fontSize = 13.sp)
                }
            }
        }

        if (upcoming != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Solat seterusnya", color = MaterialTheme.colorScheme.primary)
                        Text(upcoming.name, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                        Text(upcoming.time, fontSize = 26.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Lagi ${PrayerTimeEngine.countdown(upcoming.target, now)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        when {
            loading -> item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            error != null -> item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Data tidak dapat dikemas kini", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(error)
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = onRefresh) { Text("Cuba lagi") }
                    }
                }
            }

            today != null -> item { PrayerTimesCard(today) }
        }

        item {
            LocationCard(
                detecting = detectingLocation,
                message = locationMessage,
                suggestion = zoneSuggestion,
                currentZoneCode = zoneCode,
                onDetect = onDetectLocation,
                onUseSuggestion = onUseSuggestion
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        "Sumber rasmi",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("e-Solat JAKIM", fontWeight = FontWeight.Bold)
                    data?.let { response ->
                        Spacer(Modifier.height(4.dp))
                        Text("Masa pelayan: ${response.serverTime}", fontSize = 12.sp)
                        if (response.bearing.isNotBlank()) {
                            Text("Arah kiblat zon: ${response.bearing}", fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = onRefresh, enabled = !loading) {
                        Text("Kemas kini sekarang")
                    }
                }
            }
        }
    }
}
