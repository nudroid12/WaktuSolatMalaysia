package app.nudroidlabs.waktusolat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.waktusolat.data.JakimZones
import app.nudroidlabs.waktusolat.data.PrayerDay
import app.nudroidlabs.waktusolat.data.PrayerTimeDisplayFormatter
import app.nudroidlabs.waktusolat.data.TimeFormatMode
import app.nudroidlabs.waktusolat.location.ZoneSuggestion

@Composable
fun PrayerTimesCard(
    day: PrayerDay,
    title: String = "Hari ini",
    compact: Boolean = false,
    highlightedPrayer: String? = null,
    timeFormatMode: TimeFormatMode = TimeFormatMode.HOUR_24
) {
    val rows = if (compact) {
        listOf(
            "Subuh" to day.subuh,
            "Syuruk" to day.syuruk,
            "Zohor" to day.zohor,
            "Asar" to day.asar,
            "Maghrib" to day.maghrib,
            "Isyak" to day.isyak
        )
    } else {
        listOf(
            "Imsak" to day.imsak,
            "Subuh" to day.subuh,
            "Syuruk" to day.syuruk,
            "Duha" to day.dhuha,
            "Zohor" to day.zohor,
            "Asar" to day.asar,
            "Maghrib" to day.maghrib,
            "Isyak" to day.isyak
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                title,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(day.dateRaw, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Hijrah ${day.hijri}", fontSize = 13.sp)
            Spacer(Modifier.height(14.dp))

            rows.chunked(2).forEachIndexed { pairIndex, pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { (name, raw) ->
                        PrayerTimeCell(
                            modifier = Modifier.weight(1f),
                            name = name,
                            time = PrayerTimeDisplayFormatter.formatApiTime(raw, timeFormatMode),
                            highlighted = name == highlightedPrayer
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
                if (pairIndex != rows.chunked(2).lastIndex) Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun PrayerTimeCell(
    modifier: Modifier,
    name: String,
    time: String,
    highlighted: Boolean
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (highlighted) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        }
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                name,
                fontSize = 12.sp,
                color = if (highlighted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                }
            )
            Spacer(Modifier.height(3.dp))
            Text(time, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LocationCard(
    detecting: Boolean,
    message: String?,
    suggestion: ZoneSuggestion?,
    currentZoneCode: String,
    onDetect: () -> Unit,
    onUseSuggestion: (ZoneSuggestion) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                "Lokasi",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "Lokasi digunakan sekali apabila diminta. GPS tidak berjalan di latar belakang.",
                fontSize = 13.sp
            )
            Spacer(Modifier.height(10.dp))

            Button(onClick = onDetect, enabled = !detecting) {
                Text(if (detecting) "Mengesan lokasi..." else "Kesan lokasi dan zon")
            }

            message?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, fontSize = 13.sp)
            }

            suggestion?.let {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))
                Text("Alamat", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                Text(it.addressText, fontWeight = FontWeight.SemiBold)
                Text(
                    "Ketepatan ±${it.accuracyMetres.toInt().coerceAtLeast(1)} m",
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Cadangan ${it.zone.code}: ${it.zone.area}",
                    fontWeight = FontWeight.Bold
                )
                if (it.zone.code != currentZoneCode) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { onUseSuggestion(it) }) {
                        Text("Guna zon ${it.zone.code}")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoneSheet(
    current: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Pilih zon JAKIM", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("60 zon rasmi Malaysia", fontSize = 12.sp)
                }
                TextButton(onClick = onDismiss) { Text("Tutup") }
            }

            LazyColumn(contentPadding = PaddingValues(bottom = 28.dp)) {
                items(JakimZones.all, key = { it.code }) { zone ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(zone.code) }
                            .background(
                                if (zone.code == current) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                } else {
                                    Color.Transparent
                                }
                            )
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Text(
                            "${zone.code} · ${zone.state}",
                            color = if (zone.code == current) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(zone.area, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
