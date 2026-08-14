package app.nudroidlabs.waktusolat.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.waktusolat.data.JakimPrayerRepository
import app.nudroidlabs.waktusolat.data.JakimZones
import app.nudroidlabs.waktusolat.data.PrayerDay
import app.nudroidlabs.waktusolat.data.PrayerResponse
import app.nudroidlabs.waktusolat.data.PrayerTimeEngine
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

private val AppColours = darkColorScheme(
    primary = Color(0xFF9BD7B7),
    onPrimary = Color(0xFF08291F),
    background = Color(0xFF071A14),
    surface = Color(0xFF0E2B21),
    surfaceVariant = Color(0xFF173C2F),
    onBackground = Color(0xFFF2F7F4),
    onSurface = Color(0xFFF2F7F4)
)

@Composable
fun WaktuSolatApp() {
    MaterialTheme(colorScheme = AppColours) {
        Surface(modifier = Modifier.fillMaxSize()) {
            PrayerHome()
        }
    }
}

@Composable
private fun PrayerHome() {
    val context = LocalContext.current
    val repository = remember { JakimPrayerRepository(context.applicationContext) }
    var zoneCode by remember { mutableStateOf(repository.savedZone()) }
    var data by remember { mutableStateOf<PrayerResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showZones by remember { mutableStateOf(false) }
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var reloadNonce by remember { mutableLongStateOf(0L) }

    fun setZone(code: String) {
        zoneCode = code
        repository.saveZone(code)
        data = null
        error = null
        loading = true
    }

    LaunchedEffect(zoneCode, reloadNonce) {
        loading = true
        error = null
        repository.loadWeek(zoneCode).fold(
            onSuccess = { data = it },
            onFailure = { error = it.message ?: "Tidak dapat memuatkan data JAKIM." }
        )
        loading = false
    }

    LaunchedEffect(Unit) {
        while (true) {
            tick = System.currentTimeMillis()
            delay(1_000)
        }
    }

    val zone = JakimZones.byCode(zoneCode)
    val malaysiaZone = remember { ZoneId.of("Asia/Kuala_Lumpur") }
    val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(tick), malaysiaZone)
    val today = data?.days?.firstOrNull { PrayerTimeEngine.apiDate(it.dateRaw) == now.toLocalDate() }
        ?: data?.days?.firstOrNull()
    val upcoming = data?.let { PrayerTimeEngine.findUpcoming(it.days, now) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Waktu Solat Malaysia", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("NudroidLabs", color = MaterialTheme.colorScheme.primary)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { showZones = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(zone.state, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(zone.area, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("Zon $zoneCode · Tekan untuk tukar zon", fontSize = 13.sp)
                    }
                }
            }

            if (upcoming != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                    Box(Modifier.fillMaxWidth().padding(36.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(18.dp)) {
                            Text("Data tidak dapat dikemas kini", fontWeight = FontWeight.Bold)
                            Text(error ?: "Ralat tidak diketahui")
                            Spacer(Modifier.height(10.dp))
                            Button(onClick = {
                                loading = true
                                error = null
                                reloadNonce++
                            }) { Text("Cuba lagi") }
                        }
                    }
                }
                today != null -> item { PrayerTimesCard(today) }
            }

            data?.let { response ->
                item {
                    Column {
                        Text("Sumber: e-Solat JAKIM", fontWeight = FontWeight.SemiBold)
                        Text("Masa pelayan JAKIM: ${response.serverTime}", fontSize = 12.sp)
                        if (response.bearing.isNotBlank()) {
                            Text("Arah kiblat zon: ${response.bearing}", fontSize = 12.sp)
                        }
                    }
                }
            }

            item {
                Text(
                    "M1 menggunakan jadual yang diterima terus daripada e-Solat JAKIM. Waktu tidak dikira sendiri oleh aplikasi.",
                    fontSize = 12.sp
                )
            }
        }
    }

    if (showZones) {
        ZoneSheet(
            current = zoneCode,
            onDismiss = { showZones = false },
            onSelect = {
                showZones = false
                setZone(it)
            }
        )
    }
}

@Composable
private fun PrayerTimesCard(day: PrayerDay) {
    val rows = listOf(
        "Imsak" to day.imsak,
        "Subuh" to day.subuh,
        "Syuruk" to day.syuruk,
        "Duha" to day.dhuha,
        "Zohor" to day.zohor,
        "Asar" to day.asar,
        "Maghrib" to day.maghrib,
        "Isyak" to day.isyak
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("Hari ini", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Text(day.dateRaw, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Hijrah ${day.hijri}", fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            rows.forEachIndexed { index, (name, raw) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(name)
                    Text(PrayerTimeEngine.displayTime(raw), fontWeight = FontWeight.Bold)
                }
                if (index != rows.lastIndex) HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZoneSheet(current: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pilih zon JAKIM", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = onDismiss) { Text("Tutup") }
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 28.dp)) {
                items(JakimZones.all, key = { it.code }) { zone ->
                    Column(
                        Modifier.fillMaxWidth()
                            .clickable { onSelect(zone.code) }
                            .background(if (zone.code == current) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Text("${zone.code} · ${zone.state}", fontWeight = FontWeight.SemiBold)
                        Text(zone.area, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
