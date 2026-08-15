package app.nudroidlabs.waktusolat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.waktusolat.data.JakimZones
import app.nudroidlabs.waktusolat.data.PrayerResponse
import app.nudroidlabs.waktusolat.data.PrayerTimeEngine
import app.nudroidlabs.waktusolat.data.TimeFormatMode
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun WeekScreen(
    modifier: Modifier,
    zoneCode: String,
    data: PrayerResponse?,
    loading: Boolean,
    error: String?,
    timeFormatMode: TimeFormatMode,
    onRefresh: () -> Unit
) {
    val zone = JakimZones.byCode(zoneCode)
    val today = LocalDate.now(ZoneId.of("Asia/Kuala_Lumpur"))

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("Jadual 7 Hari", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(
                    "$zoneCode · ${zone.area}",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp
                )
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
                        Text(error, fontSize = 13.sp)
                        Button(onClick = onRefresh) { Text("Cuba lagi") }
                    }
                }
            }
        }

        data?.let { response ->
            items(response.days, key = { it.dateRaw }) { day ->
                val isToday = PrayerTimeEngine.apiDate(day.dateRaw) == today
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isToday) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Column(Modifier.padding(4.dp)) {
                        PrayerTimesCard(
                            day = day,
                            title = if (isToday) "Hari ini · ${day.dayRaw}" else day.dayRaw.ifBlank { "Jadual" },
                            compact = true,
                            timeFormatMode = timeFormatMode
                        )
                    }
                }
            }
        }

        if (data != null && error != null) {
            item {
                Text(
                    "Data sedia ada masih dipaparkan. Kemas kini terbaru gagal: $error",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
                )
            }
        }
    }
}
