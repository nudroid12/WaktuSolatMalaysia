package app.nudroidlabs.waktusolat.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import app.nudroidlabs.waktusolat.audio.AzanAudioSource
import app.nudroidlabs.waktusolat.audio.AzanPlaybackService
import app.nudroidlabs.waktusolat.audio.AzanPreferences
import app.nudroidlabs.waktusolat.data.JakimPrayerRepository
import app.nudroidlabs.waktusolat.data.PrayerDataOrigin
import app.nudroidlabs.waktusolat.data.PrayerResponse
import app.nudroidlabs.waktusolat.data.TimeFormatMode
import app.nudroidlabs.waktusolat.data.TimeFormatPreferences
import app.nudroidlabs.waktusolat.location.LocationZoneDetector
import app.nudroidlabs.waktusolat.location.SavedLocation
import app.nudroidlabs.waktusolat.location.ZoneSuggestion
import app.nudroidlabs.waktusolat.notification.PrayerAlarmScheduler
import app.nudroidlabs.waktusolat.notification.PrayerAlertStyle
import app.nudroidlabs.waktusolat.notification.PrayerRefreshWorker
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority

private val DarkColours = darkColorScheme(
    primary = Color(0xFFF4D58D),
    onPrimary = Color(0xFF2A2105),
    background = Color(0xFF071A14),
    surface = Color(0xFF0E2B21),
    surfaceVariant = Color(0xFF173C2F),
    onBackground = Color(0xFFF2F7F4),
    onSurface = Color(0xFFF2F7F4)
)

private val LightColours = lightColorScheme(
    primary = Color(0xFF2E6D57),
    onPrimary = Color.White,
    background = Color(0xFFF7FAF8),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE5F0EA),
    onBackground = Color(0xFF14201B),
    onSurface = Color(0xFF14201B)
)

enum class AppTab(val label: String, val shortLabel: String) {
    HOME("Utama", "U"),
    WEEK("7 Hari", "7"),
    QIBLA("Kiblat", "K"),
    SETTINGS("Tetapan", "T")
}

@Composable
fun WaktuSolatApp(resumeToken: Int = 0) {
    val context = LocalContext.current.applicationContext
    var appearanceMode by remember { mutableStateOf(AppearancePreferences.mode(context)) }
    var timeFormatMode by remember { mutableStateOf(TimeFormatPreferences.mode(context)) }
    val systemDark = isSystemInDarkTheme()
    val useDark = when (appearanceMode) {
        AppearanceMode.SYSTEM -> systemDark
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
    }

    MaterialTheme(colorScheme = if (useDark) DarkColours else LightColours) {
        Surface(modifier = Modifier.fillMaxSize()) {
            PrayerAppShell(
                appearanceMode = appearanceMode,
                resumeToken = resumeToken,
                timeFormatMode = timeFormatMode,
                onAppearanceModeChange = { mode ->
                    AppearancePreferences.setMode(context, mode)
                    appearanceMode = mode
                },
                onTimeFormatModeChange = { mode ->
                    TimeFormatPreferences.setMode(context, mode)
                    timeFormatMode = mode
                }
            )
        }
    }
}

@Composable
private fun PrayerAppShell(
    appearanceMode: AppearanceMode,
    resumeToken: Int,
    timeFormatMode: TimeFormatMode,
    onAppearanceModeChange: (AppearanceMode) -> Unit,
    onTimeFormatModeChange: (TimeFormatMode) -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val repository = remember { JakimPrayerRepository(appContext) }
    val detector = remember { LocationZoneDetector(appContext) }
    val locationSettingsClient = remember { LocationServices.getSettingsClient(context) }
    val locationSettingsRequest = remember {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            10_000L
        )
            .setMaxUpdates(1)
            .build()

        LocationSettingsRequest.Builder()
            .addLocationRequest(request)
            .setAlwaysShow(true)
            .build()
    }

    var tab by remember { mutableStateOf(AppTab.HOME) }
    var zoneCode by remember { mutableStateOf(repository.savedZone()) }
    var data by remember { mutableStateOf<PrayerResponse?>(null) }
    var dataOrigin by remember { mutableStateOf<PrayerDataOrigin?>(null) }
    var cacheSavedAt by remember { mutableLongStateOf(repository.cacheSavedAt(zoneCode)) }
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
    var autoApplyDetectedZone by remember { mutableStateOf(false) }
    var homeLocationMessage by remember { mutableStateOf<String?>(null) }
    var showLocationServicesDialog by remember { mutableStateOf(false) }

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
    var leadMinutesByPrayer by remember {
        mutableStateOf(
            PrayerAlarmScheduler.prayerNames.associateWith {
                PrayerAlarmScheduler.leadMinutes(appContext, it)
            }
        )
    }
    var alertStyle by remember { mutableStateOf(PrayerAlarmScheduler.alertStyle(appContext)) }
    var azanEnabled by remember { mutableStateOf(AzanPreferences.enabled(appContext)) }
    var azanSource by remember { mutableStateOf(AzanPreferences.source(appContext)) }
    var azanUri by remember { mutableStateOf(AzanPreferences.audioUri(appContext)) }
    var azanVolumePercent by remember {
        mutableStateOf(AzanPreferences.volumePercent(appContext))
    }
    var azanEnabledPrayers by remember {
        mutableStateOf(
            PrayerAlarmScheduler.prayerNames.associateWith {
                AzanPreferences.prayerEnabled(appContext, it)
            }
        )
    }
    var settingsRevision by remember { mutableLongStateOf(0L) }
    var scheduleMessage by remember { mutableStateOf<String?>(null) }

    fun setZone(code: String) {
        PrayerAlarmScheduler.cancelAll(appContext)
        zoneCode = code
        repository.saveZone(code)
        data = null
        dataOrigin = null
        cacheSavedAt = repository.cacheSavedAt(code)
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
            val message = "Kebenaran lokasi diperlukan untuk mengesan zon dan kiblat."
            locationMessage = message
            if (autoApplyDetectedZone) homeLocationMessage = message
            autoApplyDetectedZone = false
        }
    }

    val locationSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (detector.isLocationEnabled()) {
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
        } else {
            val message = "Location telefon masih dimatikan."
            locationMessage = message
            if (autoApplyDetectedZone) homeLocationMessage = message
            autoApplyDetectedZone = false
        }
    }

    val locationResolutionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK || detector.isLocationEnabled()) {
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
        } else {
            val message = "Location tidak dihidupkan."
            locationMessage = message
            if (autoApplyDetectedZone) homeLocationMessage = message
            autoApplyDetectedZone = false
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
            azanSource = AzanAudioSource.CUSTOM
            azanUri = uri.toString()
            azanEnabled = true
            settingsRevision++
        }
    }

    fun requestLocationDetection(autoApplyZone: Boolean = false) {
        autoApplyDetectedZone = autoApplyZone
        locationMessage = null
        zoneSuggestion = null
        if (autoApplyZone) homeLocationMessage = null

        fun continueDetection() {
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

        if (detector.isLocationEnabled()) {
            continueDetection()
            return
        }

        val playServicesAvailable = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
        if (!playServicesAvailable) {
            showLocationServicesDialog = true
            return
        }

        locationSettingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener {
                continueDetection()
            }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    val request = IntentSenderRequest.Builder(exception.resolution).build()
                    runCatching {
                        locationResolutionLauncher.launch(request)
                    }.onFailure {
                        showLocationServicesDialog = true
                    }
                } else {
                    showLocationServicesDialog = true
                }
            }
    }

    fun setNotifications(enabled: Boolean) {
        notificationsEnabled = enabled
        PrayerAlarmScheduler.setNotificationsEnabled(appContext, enabled)
        if (enabled) {
            PrayerAlarmScheduler.createNotificationChannels(appContext)
            PrayerRefreshWorker.ensureScheduled(appContext)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !PrayerAlarmScheduler.hasNotificationPermission(appContext)
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            scheduleMessage = "Notifikasi dimatikan."
            if (!PrayerAlarmScheduler.scheduleNeeded(appContext)) {
                PrayerAlarmScheduler.cancelAll(appContext)
                PrayerRefreshWorker.cancel(appContext)
            }
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

    LaunchedEffect(zoneCode, reloadNonce, resumeToken) {
        loading = true
        error = null
        val forceRefresh = reloadNonce > lastHandledRefreshNonce
        repository.loadWeek(zoneCode, forceRefresh = forceRefresh).fold(
            onSuccess = {
                data = it
                dataOrigin = repository.lastDataOrigin()
                cacheSavedAt = repository.cacheSavedAt(zoneCode)
            },
            onFailure = {
                error = it.message ?: "Tidak dapat memuatkan data JAKIM."
            }
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
                if (autoApplyDetectedZone) {
                    val detectedCode = suggestion.zone.code
                    homeLocationMessage = if (detectedCode == zoneCode) {
                        "Lokasi dikesan · zon $detectedCode sudah digunakan."
                    } else {
                        "Lokasi dikesan · zon $detectedCode digunakan."
                    }
                    locationMessage = homeLocationMessage
                    if (detectedCode != zoneCode) {
                        setZone(detectedCode)
                    } else {
                        zoneSuggestion = suggestion
                    }
                } else {
                    zoneSuggestion = suggestion
                    locationMessage = if (suggestion.zone.code == zoneCode) {
                        "Lokasi sepadan dengan zon semasa ${suggestion.zone.code}."
                    } else {
                        "Cadangan zon ditemui. Sahkan sebelum menukarnya."
                    }
                }
            },
            onFailure = {
                val message = it.message ?: "Lokasi tidak dapat dikesan."
                locationMessage = message
                if (autoApplyDetectedZone) homeLocationMessage = message
            }
        )
        autoApplyDetectedZone = false
        locationRevision++
        detectingLocation = false
    }

    LaunchedEffect(data, zoneCode, settingsRevision) {
        val current = data ?: return@LaunchedEffect
        if (!PrayerAlarmScheduler.scheduleNeeded(appContext)) {
            PrayerAlarmScheduler.cancelAll(appContext)
            PrayerRefreshWorker.cancel(appContext)
            scheduleMessage = if (AzanPreferences.enabled(appContext) &&
                !PrayerAlarmScheduler.canScheduleExact(appContext)
            ) {
                "Azan penuh menunggu akses alarm tepat."
            } else if (!PrayerAlarmScheduler.notificationsEnabled(appContext)) {
                "Tiada peringatan aktif."
            } else {
                scheduleMessage
            }
            return@LaunchedEffect
        }

        PrayerRefreshWorker.ensureScheduled(appContext)
        val report = PrayerAlarmScheduler.reschedule(appContext, current.days, zoneCode)
        scheduleMessage = when {
            PrayerAlarmScheduler.notificationsEnabled(appContext) &&
                !PrayerAlarmScheduler.hasNotificationPermission(appContext) ->
                "Jadual disediakan, tetapi kebenaran notifikasi belum diberikan."
            AzanPreferences.enabled(appContext) &&
                !PrayerAlarmScheduler.canScheduleExact(appContext) ->
                "Notifikasi dijadualkan. Azan penuh menunggu akses alarm tepat."
            report.exact ->
                "${report.scheduledCount} peringatan akan datang dijadualkan tepat."
            else ->
                "${report.scheduledCount} peringatan dijadualkan secara anggaran. " +
                    "Benarkan alarm tepat untuk ketepatan terbaik."
        }
    }

    LaunchedEffect(Unit) {
        PrayerAlarmScheduler.createNotificationChannels(appContext)
        if (PrayerAlarmScheduler.scheduleNeeded(appContext)) {
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
                        icon = { Text(item.shortLabel, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
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
                dataOrigin = dataOrigin,
                cacheSavedAt = cacheSavedAt,
                loading = loading,
                error = error,
                timeFormatMode = timeFormatMode,
                detectingLocation = detectingLocation,
                homeLocationMessage = homeLocationMessage,
                onChooseZone = { showZones = true },
                onDetectLocation = { requestLocationDetection(autoApplyZone = true) },
                onRefresh = { reloadNonce++ }
            )

            AppTab.WEEK -> WeekScreen(
                modifier = Modifier.padding(inner),
                zoneCode = zoneCode,
                data = data,
                loading = loading,
                error = error,
                timeFormatMode = timeFormatMode,
                onRefresh = { reloadNonce++ }
            )

            AppTab.QIBLA -> QiblaScreen(
                modifier = Modifier.padding(inner),
                location = savedLocation,
                jakimBearing = data?.bearing,
                detectingLocation = detectingLocation,
                locationMessage = locationMessage,
                timeFormatMode = timeFormatMode,
                onDetectLocation = { requestLocationDetection() }
            )

            AppTab.SETTINGS -> SettingsScreen(
                modifier = Modifier.padding(inner),
                zoneCode = zoneCode,
                themeMode = appearanceMode,
                timeFormatMode = timeFormatMode,
                detectingLocation = detectingLocation,
                locationMessage = locationMessage,
                zoneSuggestion = zoneSuggestion,
                notificationsEnabled = notificationsEnabled,
                enabledPrayers = enabledPrayers,
                leadMinutesByPrayer = leadMinutesByPrayer,
                alertStyle = alertStyle,
                hasNotificationPermission = PrayerAlarmScheduler.hasNotificationPermission(appContext),
                hasExactAlarmAccess = PrayerAlarmScheduler.canScheduleExact(appContext),
                scheduleMessage = scheduleMessage,
                azanEnabled = azanEnabled,
                azanSource = azanSource,
                azanAudioName = audioName,
                azanVolumePercent = azanVolumePercent,
                azanEnabledPrayers = azanEnabledPrayers,
                onChooseZone = { showZones = true },
                onThemeModeChange = onAppearanceModeChange,
                onTimeFormatModeChange = { mode ->
                    onTimeFormatModeChange(mode)
                    settingsRevision++
                },
                onDetectLocation = { requestLocationDetection() },
                onUseSuggestion = { setZone(it.zone.code) },
                onMasterNotificationChange = ::setNotifications,
                onPrayerChange = { prayer, enabled ->
                    PrayerAlarmScheduler.setPrayerEnabled(appContext, prayer, enabled)
                    enabledPrayers = enabledPrayers.toMutableMap().apply { put(prayer, enabled) }
                    settingsRevision++
                },
                onLeadMinutesChange = { prayer, minutes ->
                    PrayerAlarmScheduler.setLeadMinutes(appContext, prayer, minutes)
                    leadMinutesByPrayer = leadMinutesByPrayer.toMutableMap().apply {
                        put(prayer, minutes)
                    }
                    settingsRevision++
                },
                onAlertStyleChange = { style ->
                    PrayerAlarmScheduler.setAlertStyle(appContext, style)
                    alertStyle = style
                    settingsRevision++
                },
                onRequestNotificationPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onRequestExactAlarm = ::requestExactAlarmAccess,
                onOpenNotificationSettings = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    runCatching { context.startActivity(intent) }
                        .onFailure {
                            scheduleMessage = "Tetapan notifikasi Android tidak dapat dibuka."
                        }
                },
                onPickAzanAudio = { audioPicker.launch(arrayOf("audio/*")) },
                onAzanEnabledChange = { enabled ->
                    AzanPreferences.setEnabled(appContext, enabled)
                    azanEnabled = enabled
                    if (enabled && PrayerAlarmScheduler.canScheduleExact(appContext)) {
                        PrayerRefreshWorker.ensureScheduled(appContext)
                    } else if (!enabled && !PrayerAlarmScheduler.notificationsEnabled(appContext)) {
                        PrayerAlarmScheduler.cancelAll(appContext)
                        PrayerRefreshWorker.cancel(appContext)
                    }
                    settingsRevision++
                },
                onAzanSourceChange = { source ->
                    AzanPlaybackService.stop(appContext)
                    AzanPreferences.setSource(appContext, source)
                    azanSource = source
                    settingsRevision++
                },
                onAzanVolumeChange = { percent ->
                    val safePercent = percent.coerceIn(0, 100)
                    AzanPreferences.setVolumePercent(appContext, safePercent)
                    azanVolumePercent = safePercent
                    AzanPlaybackService.applyVolume(appContext)
                    settingsRevision++
                },
                onAzanPrayerChange = { prayer, enabled ->
                    AzanPreferences.setPrayerEnabled(appContext, prayer, enabled)
                    azanEnabledPrayers = azanEnabledPrayers.toMutableMap().apply {
                        put(prayer, enabled)
                    }
                    settingsRevision++
                },
                onTestAzan = { prayer, source ->
                    val canPreview = source == AzanAudioSource.BUILT_IN ||
                        !AzanPreferences.audioUri(appContext).isNullOrBlank()
                    if (canPreview) {
                        runCatching {
                            AzanPlaybackService.preview(
                                context = appContext,
                                prayerName = prayer,
                                source = source
                            )
                        }.onFailure {
                            scheduleMessage = "Ujian azan tidak dapat dimulakan."
                        }
                    }
                },
                onStopAzan = { AzanPlaybackService.stop(appContext) },
                onClearAzanAudio = {
                    AzanPlaybackService.stop(appContext)
                    AzanPreferences.clearAudioUri(appContext)
                    azanSource = AzanAudioSource.BUILT_IN
                    azanUri = null
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

    if (showLocationServicesDialog) {
        AlertDialog(
            onDismissRequest = {
                showLocationServicesDialog = false
                val message = "Location perlu dihidupkan untuk menggunakan fungsi ini."
                locationMessage = message
                if (autoApplyDetectedZone) homeLocationMessage = message
                autoApplyDetectedZone = false
            },
            title = { Text("Hidupkan Location") },
            text = {
                Text(
                    "Dialog sistem untuk hidupkan Location tidak tersedia pada peranti ini. " +
                        "Buka tetapan Location untuk meneruskan."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLocationServicesDialog = false
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        runCatching {
                            locationSettingsLauncher.launch(intent)
                        }.onFailure {
                            val fallback = Intent(Settings.ACTION_SETTINGS)
                            runCatching {
                                locationSettingsLauncher.launch(fallback)
                            }.onFailure {
                                val message = "Tetapan Location tidak dapat dibuka pada peranti ini."
                                locationMessage = message
                                if (autoApplyDetectedZone) homeLocationMessage = message
                                autoApplyDetectedZone = false
                            }
                        }
                    }
                ) {
                    Text("Buka tetapan lokasi")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLocationServicesDialog = false
                        val message = "Location perlu dihidupkan untuk menggunakan fungsi ini."
                        locationMessage = message
                        if (autoApplyDetectedZone) homeLocationMessage = message
                        autoApplyDetectedZone = false
                    }
                ) {
                    Text("Batal")
                }
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
