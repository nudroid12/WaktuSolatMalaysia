package app.nudroidlabs.waktusolat.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import app.nudroidlabs.waktusolat.audio.AzanPreferences
import app.nudroidlabs.waktusolat.data.JakimPrayerRepository
import app.nudroidlabs.waktusolat.data.PrayerResponse
import app.nudroidlabs.waktusolat.location.LocationZoneDetector
import app.nudroidlabs.waktusolat.location.SavedLocation
import app.nudroidlabs.waktusolat.location.ZoneSuggestion
import app.nudroidlabs.waktusolat.notification.PrayerAlarmScheduler
import app.nudroidlabs.waktusolat.notification.PrayerRefreshWorker

private val AppColours = darkColorScheme(
    primary = Color(0xFFF4D58D),
    onPrimary = Color(0xFF2A2105),
    background = Color(0xFF071A14),
    surface = Color(0xFF0E2B21),
    surfaceVariant = Color(0xFF173C2F),
    onBackground = Color(0xFFF2F7F4),
    onSurface = Color(0xFFF2F7F4)
)

enum class AppTab(val label: String, val shortLabel: String) {
    HOME("Utama", "U"),
    WEEK("7 Hari", "7"),
    QIBLA("Kiblat", "K"),
    SETTINGS("Tetapan", "T")
}

@Composable
fun WaktuSolatApp() {
    MaterialTheme(colorScheme = AppColours) {
        Surface(modifier = Modifier.fillMaxSize()) {
            PrayerAppShell()
        }
    }
}

@Composable
private fun PrayerAppShell() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val repository = remember { JakimPrayerRepository(appContext) }
    val detector = remember { LocationZoneDetector(appContext) }

    var tab by remember { mutableStateOf(AppTab.HOME) }
    var zoneCode by remember { mutableStateOf(repository.savedZone()) }
    var data by remember { mutableStateOf<PrayerResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showZones by remember { mutableStateOf(false) }
    var reloadNonce by remember { mutableLongStateOf(0L) }
    var lastHandledRefreshNonce by remember { mutableLongStateOf(0L) }

    var detectionNonce by remember { mutableLongStateOf(0L) }
    var detectingLocation by remember { mutableStateOf(false) }
    var locationMessage by remember { mutableStateOf<String?>(null) }
    var zoneSuggestion by remember { mutableStateOf<ZoneSuggestion?>(null) }
    var locationRevision by remember { mutableLongStateOf(0L) }

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
    var leadMinutes by remember {
        mutableIntStateOf(PrayerAlarmScheduler.leadMinutes(appContext))
    }
    var azanEnabled by remember { mutableStateOf(AzanPreferences.enabled(appContext)) }
    var azanUri by remember { mutableStateOf(AzanPreferences.audioUri(appContext)) }
    var settingsRevision by remember { mutableLongStateOf(0L) }
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
            locationMessage = "Kebenaran lokasi diperlukan untuk mengesan zon dan kiblat."
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        settingsRevision++
    }

    val exactAlarmLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        settingsRevision++
    }

    val audioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            AzanPreferences.setAudioUri(appContext, uri.toString())
            AzanPreferences.setEnabled(appContext, true)
            azanUri = uri.toString()
            azanEnabled = true
            settingsRevision++
        }
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

    fun setNotifications(enabled: Boolean) {
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
        settingsRevision++
    }

    fun requestExactAlarmAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val intent = Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            "package:${context.packageName}".toUri()
        )
        runCatching { exactAlarmLauncher.launch(intent) }
            .onFailure {
                scheduleMessage = "Tetapan alarm tepat tidak dapat dibuka pada peranti ini."
            }
    }

    LaunchedEffect(zoneCode, reloadNonce) {
        loading = true
        error = null
        val forceRefresh = reloadNonce > lastHandledRefreshNonce
        repository.loadWeek(zoneCode, forceRefresh = forceRefresh).fold(
            onSuccess = { data = it },
            onFailure = { error = it.message ?: "Tidak dapat memuatkan data JAKIM." }
        )
        if (forceRefresh) lastHandledRefreshNonce = reloadNonce
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
        locationRevision++
        detectingLocation = false
    }

    LaunchedEffect(data, zoneCode, settingsRevision) {
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
                    "Benarkan alarm tepat untuk ketepatan terbaik."
        }
    }

    LaunchedEffect(Unit) {
        PrayerAlarmScheduler.createNotificationChannel(appContext)
        if (PrayerAlarmScheduler.notificationsEnabled(appContext)) {
            PrayerRefreshWorker.ensureScheduled(appContext)
        }
    }

    val savedLocation: SavedLocation? = remember(locationRevision) {
        LocationZoneDetector.savedLocation(appContext)
    }
    val audioName = remember(azanUri) {
        azanUri?.let { queryDisplayName(context, it.toUri()) ?: "Fail audio dipilih" }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                AppTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Text(item.shortLabel) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { inner ->
        when (tab) {
            AppTab.HOME -> HomeScreen(
                modifier = Modifier.padding(inner),
                zoneCode = zoneCode,
                data = data,
                loading = loading,
                error = error,
                detectingLocation = detectingLocation,
                locationMessage = locationMessage,
                zoneSuggestion = zoneSuggestion,
                onChooseZone = { showZones = true },
                onDetectLocation = ::requestLocationDetection,
                onUseSuggestion = { setZone(it.zone.code) },
                onRefresh = { reloadNonce++ }
            )

            AppTab.WEEK -> WeekScreen(
                modifier = Modifier.padding(inner),
                data = data,
                loading = loading,
                error = error,
                onRefresh = { reloadNonce++ }
            )

            AppTab.QIBLA -> QiblaScreen(
                modifier = Modifier.padding(inner),
                location = savedLocation,
                jakimBearing = data?.bearing,
                detectingLocation = detectingLocation,
                locationMessage = locationMessage,
                onDetectLocation = ::requestLocationDetection
            )

            AppTab.SETTINGS -> SettingsScreen(
                modifier = Modifier.padding(inner),
                zoneCode = zoneCode,
                notificationsEnabled = notificationsEnabled,
                enabledPrayers = enabledPrayers,
                leadMinutes = leadMinutes,
                hasNotificationPermission = PrayerAlarmScheduler.hasNotificationPermission(appContext),
                hasExactAlarmAccess = PrayerAlarmScheduler.canScheduleExact(appContext),
                scheduleMessage = scheduleMessage,
                azanEnabled = azanEnabled,
                azanAudioName = audioName,
                onChooseZone = { showZones = true },
                onMasterNotificationChange = ::setNotifications,
                onPrayerChange = { prayer, enabled ->
                    PrayerAlarmScheduler.setPrayerEnabled(appContext, prayer, enabled)
                    enabledPrayers = enabledPrayers.toMutableMap().apply { put(prayer, enabled) }
                    settingsRevision++
                },
                onLeadMinutesChange = { minutes ->
                    PrayerAlarmScheduler.setLeadMinutes(appContext, minutes)
                    leadMinutes = minutes
                    settingsRevision++
                },
                onRequestNotificationPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onRequestExactAlarm = ::requestExactAlarmAccess,
                onPickAzanAudio = { audioPicker.launch(arrayOf("audio/*")) },
                onAzanEnabledChange = { enabled ->
                    AzanPreferences.setEnabled(appContext, enabled)
                    azanEnabled = enabled
                    settingsRevision++
                },
                onClearAzanAudio = {
                    AzanPreferences.clearAudioUri(appContext)
                    azanUri = null
                    azanEnabled = false
                    settingsRevision++
                }
            )
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

private fun queryDisplayName(context: android.content.Context, uri: Uri): String? = runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) cursor.getString(index) else null
        }
}.getOrNull()
