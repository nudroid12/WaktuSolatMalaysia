package app.nudroidlabs.waktusolat.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.core.content.ContextCompat
import app.nudroidlabs.waktusolat.data.JakimPrayerRepository
import app.nudroidlabs.waktusolat.data.JakimZones
import app.nudroidlabs.waktusolat.data.PrayerDay
import app.nudroidlabs.waktusolat.data.PrayerResponse
import app.nudroidlabs.waktusolat.data.PrayerTimeEngine
import app.nudroidlabs.waktusolat.location.LocationZoneDetector
import app.nudroidlabs.waktusolat.location.ZoneSuggestion
import app.nudroidlabs.waktusolat.notification.PrayerAlarmScheduler
import app.nudroidlabs.waktusolat.notification.PrayerRefreshWorker
import kotlinx.coroutines.delay
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
    val appContext = context.applicationContext
    val repository = remember { JakimPrayerRepository(appContext) }
    val detector = remember { LocationZoneDetector(appContext) }

    var zoneCode by remember { mutableStateOf(repository.savedZone()) }
    var data by remember { mutableStateOf<PrayerResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showZones by remember { mutableStateOf(false) }
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var reloadNonce by remember { mutableLongStateOf(0L) }

    var detectionNonce by remember { mutableLongStateOf(0L) }
    var detectingLocation by remember { mutableStateOf(false) }
    var locationMessage by remember { mutableStateOf<String?>(null) }
    var zoneSuggestion by remember { mutableStateOf<ZoneSuggestion?>(null) }

    var notificationsEnabled by remember {
        mutableStateOf(PrayerAlarmScheduler.notificationsEnabled(appContext))
    }
    var enabledPrayers by remember {
        mutableStateOf(
            PrayerAlarmScheduler.prayerNames.associateWith {
                PrayerAlarmScheduler.prayerEnabled(appContext, it)
            }
        )
    }
    var notificationRevision by remember { mutableLongStateOf(0L) }
    var scheduleMessage by remember { mutableStateOf<String?>(null) }

    fun setZone(code: String) {
        PrayerAlarmScheduler.cancelAll(appContext)
        zoneCode = code
        repository.saveZone(code)
        data = null
        error = null
        loading = true
        zoneSuggestion = null
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            detectionNonce++
        } else {
            locationMessage = "Kebenaran lokasi diperlukan untuk mengesan cadangan zon."
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        notificationRevision++
    }

    val exactAlarmLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        notificationRevision++
    }

    fun requestLocationDetection() {
        locationMessage = null
        zoneSuggestion = null

        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            detectionNonce++
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    fun enableNotifications(enabled: Boolean) {
        notificationsEnabled = enabled
        PrayerAlarmScheduler.setNotificationsEnabled(appContext, enabled)

        if (enabled) {
            PrayerAlarmScheduler.createNotificationChannel(appContext)
            PrayerRefreshWorker.ensureScheduled(appContext)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !PrayerAlarmScheduler.hasNotificationPermission(appContext)
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            PrayerRefreshWorker.cancel(appContext)
            scheduleMessage = "Notifikasi dimatikan."
        }

        notificationRevision++
    }

    fun requestExactAlarmAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        val intent = Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:${context.packageName}")
        )
        runCatching { exactAlarmLauncher.launch(intent) }
            .onFailure {
                scheduleMessage = "Tetapan alarm tepat tidak dapat dibuka pada peranti ini."
            }
    }

    LaunchedEffect(zoneCode, reloadNonce) {
        loading = true
        error = null
        repository.loadWeek(zoneCode).fold(
            onSuccess = {
                data = it
                if (PrayerAlarmScheduler.notificationsEnabled(appContext)) {
                    val report = PrayerAlarmScheduler.reschedule(appContext, it.days, zoneCode)
                    scheduleMessage = if (report.exact) {
                        "${report.scheduledCount} peringatan akan datang dijadualkan."
                    } else {
                        "${report.scheduledCount} peringatan dijadualkan secara anggaran. " +
                            "Benarkan alarm tepat untuk ketepatan masa terbaik."
                    }
                }
            },
            onFailure = {
                error = it.message ?: "Tidak dapat memuatkan data JAKIM."
            }
        )
        loading = false
    }

    LaunchedEffect(detectionNonce) {
        if (detectionNonce == 0L) return@LaunchedEffect

        detectingLocation = true
        locationMessage = null
        zoneSuggestion = null

        detector.detect().fold(
            onSuccess = { suggestion ->
                zoneSuggestion = suggestion
                locationMessage = if (suggestion.zone.code == zoneCode) {
                    "Lokasi sepadan dengan zon semasa ${suggestion.zone.code}."
                } else {
                    "Cadangan zon ditemui. Sahkan sebelum menukarnya."
                }
            },
            onFailure = {
                locationMessage = it.message ?: "Lokasi tidak dapat dikesan."
            }
        )

        detectingLocation = false
    }

    LaunchedEffect(notificationRevision, data, zoneCode) {
        val current = data ?: return@LaunchedEffect
        if (!PrayerAlarmScheduler.notificationsEnabled(appContext)) {
            PrayerAlarmScheduler.cancelAll(appContext)
            return@LaunchedEffect
        }

        PrayerRefreshWorker.ensureScheduled(appContext)
        val report = PrayerAlarmScheduler.reschedule(appContext, current.days, zoneCode)
        scheduleMessage = when {
            !PrayerAlarmScheduler.hasNotificationPermission(appContext) ->
                "Kebenaran notifikasi belum diberikan."
            report.exact ->
                "${report.scheduledCount} peringatan akan datang dijadualkan."
            else ->
                "${report.scheduledCount} peringatan dijadualkan secara anggaran. " +
                    "Benarkan alarm tepat untuk ketepatan masa terbaik."
        }
    }

    LaunchedEffect(Unit) {
        PrayerAlarmScheduler.createNotificationChannel(appContext)
        if (PrayerAlarmScheduler.notificationsEnabled(appContext)) {
            PrayerRefreshWorker.ensureScheduled(appContext)
        }

        while (true) {
            tick = System.currentTimeMillis()
            delay(1_000)
        }
    }

    val zone = JakimZones.byCode(zoneCode)
    val malaysiaZone = remember { ZoneId.of("Asia/Kuala_Lumpur") }
    val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(tick), malaysiaZone)
    val today = data?.days?.firstOrNull {
        PrayerTimeEngine.apiDate(it.dateRaw) == now.toLocalDate()
    } ?: data?.days?.firstOrNull()
    val upcoming = data?.let { PrayerTimeEngine.findUpcoming(it.days, now) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
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
                        .clickable { showZones = true },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
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
                        Text("Zon $zoneCode · Tekan untuk tukar zon", fontSize = 13.sp)
                    }
                }
            }

            item {
                LocationCard(
                    detecting = detectingLocation,
                    message = locationMessage,
                    suggestion = zoneSuggestion,
                    currentZoneCode = zoneCode,
                    onDetect = ::requestLocationDetection,
                    onUseSuggestion = { suggestion -> setZone(suggestion.zone.code) }
                )
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
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text("Data tidak dapat dikemas kini", fontWeight = FontWeight.Bold)
                            Text(error ?: "Ralat tidak diketahui")
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    loading = true
                                    error = null
                                    reloadNonce++
                                }
                            ) {
                                Text("Cuba lagi")
                            }
                        }
                    }
                }

                today != null -> item {
                    PrayerTimesCard(today)
                }
            }

            item {
                NotificationSettingsCard(
                    masterEnabled = notificationsEnabled,
                    prayerEnabled = enabledPrayers,
                    hasNotificationPermission =
                        PrayerAlarmScheduler.hasNotificationPermission(appContext),
                    hasExactAlarmAccess = PrayerAlarmScheduler.canScheduleExact(appContext),
                    status = scheduleMessage,
                    onMasterChange = ::enableNotifications,
                    onPrayerChange = { prayerName, enabled ->
                        PrayerAlarmScheduler.setPrayerEnabled(
                            appContext,
                            prayerName,
                            enabled
                        )
                        enabledPrayers = enabledPrayers.toMutableMap().apply {
                            put(prayerName, enabled)
                        }
                        notificationRevision++
                    },
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        }
                    },
                    onRequestExactAlarm = ::requestExactAlarmAccess
                )
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
                    "M2 masih menggunakan jadual terus daripada e-Solat JAKIM. " +
                        "Pengesanan lokasi hanya mencadangkan zon berdasarkan alamat pentadbiran " +
                        "Android dan nama kawasan rasmi JAKIM. Jika hasil tidak unik, aplikasi " +
                        "meminta pilihan manual dan tidak meneka zon terdekat.",
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
private fun LocationCard(
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
                "Lokasi dan zon",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Kesan lokasi untuk mendapatkan cadangan zon JAKIM. " +
                    "Aplikasi tidak menukar zon secara automatik tanpa pengesahan.",
                fontSize = 13.sp
            )
            Spacer(Modifier.height(10.dp))

            Button(
                onClick = onDetect,
                enabled = !detecting
            ) {
                Text(if (detecting) "Mengesan lokasi..." else "Kesan zon melalui lokasi")
            }

            message?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, fontSize = 13.sp)
            }

            suggestion?.let {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))
                Text("Alamat: ${it.addressText}", fontSize = 13.sp)
                Text(
                    "Ketepatan lokasi: ±${it.accuracyMetres.toInt().coerceAtLeast(1)} m",
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(6.dp))
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

@Composable
private fun NotificationSettingsCard(
    masterEnabled: Boolean,
    prayerEnabled: Map<String, Boolean>,
    hasNotificationPermission: Boolean,
    hasExactAlarmAccess: Boolean,
    status: String?,
    onMasterChange: (Boolean) -> Unit,
    onPrayerChange: (String, Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarm: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Notifikasi waktu solat",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("Peringatan sistem tanpa audio azan penuh.", fontSize = 12.sp)
                }
                Switch(
                    checked = masterEnabled,
                    onCheckedChange = onMasterChange
                )
            }

            if (masterEnabled) {
                Spacer(Modifier.height(10.dp))

                if (!hasNotificationPermission) {
                    Text(
                        "Kebenaran notifikasi belum diberikan.",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = onRequestNotificationPermission) {
                        Text("Benarkan notifikasi")
                    }
                    Spacer(Modifier.height(10.dp))
                }

                if (!hasExactAlarmAccess) {
                    Text(
                        "Alarm tepat belum dibenarkan. Android mungkin melambatkan peringatan.",
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = onRequestExactAlarm) {
                        Text("Benarkan alarm tepat")
                    }
                    Spacer(Modifier.height(10.dp))
                }

                PrayerAlarmScheduler.prayerNames.forEachIndexed { index, prayer ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(prayer)
                        Switch(
                            checked = prayerEnabled[prayer] != false,
                            onCheckedChange = { onPrayerChange(prayer, it) }
                        )
                    }
                    if (index != PrayerAlarmScheduler.prayerNames.lastIndex) {
                        HorizontalDivider()
                    }
                }

                status?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, fontSize = 12.sp)
                }
            }
        }
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
            Text(
                "Hari ini",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(day.dateRaw, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Hijrah ${day.hijri}", fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))

            rows.forEachIndexed { index, (name, raw) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(name)
                    Text(
                        PrayerTimeEngine.displayTime(raw),
                        fontWeight = FontWeight.Bold
                    )
                }
                if (index != rows.lastIndex) HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZoneSheet(
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
                Text("Pilih zon JAKIM", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = onDismiss) {
                    Text("Tutup")
                }
            }

            LazyColumn(contentPadding = PaddingValues(bottom = 28.dp)) {
                items(JakimZones.all, key = { it.code }) { zone ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(zone.code) }
                            .background(
                                if (zone.code == current) {
                                    MaterialTheme.colorScheme.surfaceVariant
                                } else {
                                    Color.Transparent
                                }
                            )
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Text(
                            "${zone.code} · ${zone.state}",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(zone.area, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
