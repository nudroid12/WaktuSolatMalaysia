package app.nudroidlabs.waktusolat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.waktusolat.BuildConfig
import app.nudroidlabs.waktusolat.audio.AzanAudioSource
import app.nudroidlabs.waktusolat.data.JakimZones
import app.nudroidlabs.waktusolat.data.TimeFormatMode
import app.nudroidlabs.waktusolat.location.ZoneSuggestion
import app.nudroidlabs.waktusolat.notification.PrayerAlarmScheduler
import app.nudroidlabs.waktusolat.notification.PrayerAlertStyle

@Composable
fun SettingsScreen(
    modifier: Modifier,
    zoneCode: String,
    themeMode: AppearanceMode,
    timeFormatMode: TimeFormatMode,
    detectingLocation: Boolean,
    locationMessage: String?,
    zoneSuggestion: ZoneSuggestion?,
    notificationsEnabled: Boolean,
    enabledPrayers: Map<String, Boolean>,
    leadMinutesByPrayer: Map<String, Int>,
    alertStyle: PrayerAlertStyle,
    hasNotificationPermission: Boolean,
    hasExactAlarmAccess: Boolean,
    scheduleMessage: String?,
    azanEnabled: Boolean,
    azanSource: AzanAudioSource,
    azanAudioName: String?,
    azanVolumePercent: Int,
    azanEnabledPrayers: Map<String, Boolean>,
    updateChecking: Boolean,
    updateStatus: String?,
    onChooseZone: () -> Unit,
    onThemeModeChange: (AppearanceMode) -> Unit,
    onTimeFormatModeChange: (TimeFormatMode) -> Unit,
    onDetectLocation: () -> Unit,
    onUseSuggestion: (ZoneSuggestion) -> Unit,
    onMasterNotificationChange: (Boolean) -> Unit,
    onPrayerChange: (String, Boolean) -> Unit,
    onLeadMinutesChange: (String, Int) -> Unit,
    onAlertStyleChange: (PrayerAlertStyle) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarm: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onPickAzanAudio: () -> Unit,
    onAzanEnabledChange: (Boolean) -> Unit,
    onAzanSourceChange: (AzanAudioSource) -> Unit,
    onAzanVolumeChange: (Int) -> Unit,
    onAzanPrayerChange: (String, Boolean) -> Unit,
    onTestAzan: (String, AzanAudioSource) -> Unit,
    onStopAzan: () -> Unit,
    onClearAzanAudio: () -> Unit,
    onCheckUpdate: () -> Unit
) {
    val zone = JakimZones.byCode(zoneCode)
    var expandedSection by rememberSaveable { mutableStateOf<String?>(null) }

    fun toggle(section: String) {
        expandedSection = if (expandedSection == section) null else section
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item {
            Column {
                Text("Tetapan", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Waktu Solat & Kiblat ${BuildConfig.VERSION_NAME}",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        item {
            SettingsSectionHeader(
                title = "Paparan",
                summary = "${themeMode.label} · ${timeFormatMode.label}",
                expanded = expandedSection == "display",
                onClick = { toggle("display") }
            )
        }
        if (expandedSection == "display") {
            item { AppearanceCard(mode = themeMode, onModeChange = onThemeModeChange) }
            item {
                TimeFormatCard(
                    mode = timeFormatMode,
                    onModeChange = onTimeFormatModeChange
                )
            }
        }

        item {
            SettingsSectionHeader(
                title = "Lokasi & Zon",
                summary = "$zoneCode · ${zone.state}",
                expanded = expandedSection == "location",
                onClick = { toggle("location") }
            )
        }
        if (expandedSection == "location") {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onChooseZone),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(
                            "Zon JAKIM",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("$zoneCode · ${zone.state}", fontWeight = FontWeight.Bold)
                        Text(zone.area, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("Tekan untuk pilih zon secara manual", fontSize = 12.sp)
                    }
                }
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
        }

        item {
            SettingsSectionHeader(
                title = "Notifikasi & Peringatan",
                summary = if (notificationsEnabled) "Aktif" else "Tidak aktif",
                expanded = expandedSection == "alerts",
                onClick = { toggle("alerts") }
            )
        }
        if (expandedSection == "alerts") {
            item {
                NotificationSettingsCard(
                    masterEnabled = notificationsEnabled,
                    prayerEnabled = enabledPrayers,
                    leadMinutesByPrayer = leadMinutesByPrayer,
                    alertStyle = alertStyle,
                    hasNotificationPermission = hasNotificationPermission,
                    hasExactAlarmAccess = hasExactAlarmAccess,
                    status = scheduleMessage,
                    onMasterChange = onMasterNotificationChange,
                    onPrayerChange = onPrayerChange,
                    onLeadMinutesChange = onLeadMinutesChange,
                    onAlertStyleChange = onAlertStyleChange,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onRequestExactAlarm = onRequestExactAlarm,
                    onOpenNotificationSettings = onOpenNotificationSettings
                )
            }
        }

        item {
            SettingsSectionHeader(
                title = "Azan",
                summary = if (azanEnabled) "Aktif" else "Tidak aktif",
                expanded = expandedSection == "azan",
                onClick = { toggle("azan") }
            )
        }
        if (expandedSection == "azan") {
            item {
                AzanSettingsCard(
                    enabled = azanEnabled,
                    source = azanSource,
                    audioName = azanAudioName,
                    volumePercent = azanVolumePercent,
                    prayerEnabled = azanEnabledPrayers,
                    hasExactAlarmAccess = hasExactAlarmAccess,
                    onEnabledChange = onAzanEnabledChange,
                    onSourceChange = onAzanSourceChange,
                    onVolumeChange = onAzanVolumeChange,
                    onPrayerChange = onAzanPrayerChange,
                    onPickAudio = onPickAzanAudio,
                    onTestAudio = onTestAzan,
                    onStopAudio = onStopAzan,
                    onClearAudio = onClearAzanAudio,
                    onRequestExactAlarm = onRequestExactAlarm
                )
            }
        }

        item {
            SettingsSectionHeader(
                title = "Tentang aplikasi",
                summary = "NudroidLabs · e-Solat JAKIM",
                expanded = expandedSection == "about",
                onClick = { toggle("about") }
            )
        }
        if (expandedSection == "about") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(
                            "NudroidLabs",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Waktu Solat & Kiblat",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Sumber jadual: e-Solat JAKIM", fontSize = 13.sp)
                        Text("Lokasi hanya digunakan apabila diminta.", fontSize = 13.sp)
                        Text("Kompas hanya aktif ketika halaman Kiblat dibuka.", fontSize = 13.sp)
                        Text("Tiada analytics, iklan atau tracker.", fontSize = 13.sp)
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = onCheckUpdate,
                            enabled = !updateChecking
                        ) {
                            Text(if (updateChecking) "Menyemak..." else "Semak kemas kini")
                        }
                        updateStatus?.takeIf(String::isNotBlank)?.let { status ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                status,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    summary: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(
                    summary,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
            Text(
                if (expanded) "▲" else "▼",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AppearanceCard(
    mode: AppearanceMode,
    onModeChange: (AppearanceMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("Tema", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Text("Pilih paparan aplikasi atau ikut tetapan telefon.", fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppearanceMode.entries.forEach { item ->
                    FilterChip(
                        selected = mode == item,
                        onClick = { onModeChange(item) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeFormatCard(
    mode: TimeFormatMode,
    onModeChange: (TimeFormatMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                "Format masa",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text("Pilih paparan masa 24 jam atau 12 jam.", fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeFormatMode.entries.forEach { item ->
                    FilterChip(
                        selected = mode == item,
                        onClick = { onModeChange(item) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationSettingsCard(
    masterEnabled: Boolean,
    prayerEnabled: Map<String, Boolean>,
    leadMinutesByPrayer: Map<String, Int>,
    alertStyle: PrayerAlertStyle,
    hasNotificationPermission: Boolean,
    hasExactAlarmAccess: Boolean,
    status: String?,
    onMasterChange: (Boolean) -> Unit,
    onPrayerChange: (String, Boolean) -> Unit,
    onLeadMinutesChange: (String, Int) -> Unit,
    onAlertStyleChange: (PrayerAlertStyle) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarm: () -> Unit,
    onOpenNotificationSettings: () -> Unit
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
                    Text("Notifikasi waktu solat", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text("Kawalan berasingan untuk setiap solat fardu.", fontSize = 12.sp)
                }
                Switch(checked = masterEnabled, onCheckedChange = onMasterChange)
            }

            if (masterEnabled) {
                Spacer(Modifier.height(12.dp))

                if (!hasNotificationPermission) {
                    PermissionMessage(
                        text = "Kebenaran notifikasi belum diberikan.",
                        buttonText = "Benarkan notifikasi",
                        onClick = onRequestNotificationPermission
                    )
                }

                if (!hasExactAlarmAccess) {
                    PermissionMessage(
                        text = "Alarm tepat belum dibenarkan. Android boleh melambatkan peringatan.",
                        buttonText = "Benarkan alarm tepat",
                        onClick = onRequestExactAlarm
                    )
                }

                Text("Gaya peringatan", fontWeight = FontWeight.SemiBold)
                Text("Tetapan Android untuk channel masih mempunyai keutamaan akhir.", fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PrayerAlertStyle.entries.forEach { style ->
                        FilterChip(
                            selected = alertStyle == style,
                            onClick = { onAlertStyleChange(style) },
                            label = { Text(style.label) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(6.dp))

                PrayerAlarmScheduler.prayerNames.forEachIndexed { index, prayer ->
                    val enabled = prayerEnabled[prayer] != false
                    Column(Modifier.padding(vertical = 7.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(prayer, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (enabled) "Peringatan dihidupkan" else "Peringatan dimatikan",
                                    fontSize = 12.sp
                                )
                            }
                            Switch(
                                checked = enabled,
                                onCheckedChange = { onPrayerChange(prayer, it) }
                            )
                        }

                        if (enabled) {
                            Spacer(Modifier.height(6.dp))
                            Text("Peringatan awal", fontSize = 12.sp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PrayerAlarmScheduler.supportedLeadMinutes.forEach { minutes ->
                                    FilterChip(
                                        selected = leadMinutesByPrayer[prayer] == minutes,
                                        onClick = { onLeadMinutesChange(prayer, minutes) },
                                        label = { Text(if (minutes == 0) "Tiada" else "$minutes m") }
                                    )
                                }
                            }
                        }
                    }
                    if (index != PrayerAlarmScheduler.prayerNames.lastIndex) {
                        HorizontalDivider()
                    }
                }

                status?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, fontSize = 12.sp)
                }

                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onOpenNotificationSettings) {
                    Text("Tetapan notifikasi Android")
                }
            }
        }
    }
}

@Composable
private fun PermissionMessage(
    text: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(6.dp))
    OutlinedButton(onClick = onClick) { Text(buttonText) }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun AzanSettingsCard(
    enabled: Boolean,
    source: AzanAudioSource,
    audioName: String?,
    volumePercent: Int,
    prayerEnabled: Map<String, Boolean>,
    hasExactAlarmAccess: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onSourceChange: (AzanAudioSource) -> Unit,
    onVolumeChange: (Int) -> Unit,
    onPrayerChange: (String, Boolean) -> Unit,
    onPickAudio: () -> Unit,
    onTestAudio: (String, AzanAudioSource) -> Unit,
    onStopAudio: () -> Unit,
    onClearAudio: () -> Unit,
    onRequestExactAlarm: () -> Unit
) {
    val hasCustomAudio = !audioName.isNullOrBlank()
    val hasPlayableAudio = source == AzanAudioSource.BUILT_IN || hasCustomAudio

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
                        "Azan penuh",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Azan terbina dalam boleh digunakan offline. Fail sendiri masih disokong.",
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = enabled && hasPlayableAudio,
                    onCheckedChange = { value ->
                        if (hasPlayableAudio) {
                            onEnabledChange(value)
                        } else if (value) {
                            onPickAudio()
                        }
                    }
                )
            }

            Spacer(Modifier.height(12.dp))
            Text("Sumber audio", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AzanAudioSource.entries.forEach { item ->
                    FilterChip(
                        selected = source == item,
                        onClick = {
                            if (item == AzanAudioSource.CUSTOM && !hasCustomAudio) {
                                onPickAudio()
                            } else {
                                onSourceChange(item)
                            }
                        },
                        label = { Text(item.label) }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Volume azan", fontWeight = FontWeight.SemiBold)
                Text(
                    "${volumePercent.coerceIn(0, 100)}%",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = volumePercent.coerceIn(0, 100).toFloat(),
                onValueChange = { value ->
                    onVolumeChange(((value / 10f).toInt() * 10).coerceIn(0, 100))
                },
                valueRange = 0f..100f,
                steps = 9
            )
            Text(
                "Mengawal azan sahaja dan tidak mengubah volume sistem telefon.",
                fontSize = 11.sp
            )

            Spacer(Modifier.height(10.dp))
            if (source == AzanAudioSource.BUILT_IN) {
                Text(
                    "Subuh menggunakan rakaman Fajr. Zohor, Asar, Maghrib dan Isyak menggunakan rakaman azan biasa.",
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onTestAudio("Zohor", AzanAudioSource.BUILT_IN)
                        }
                    ) {
                        Text("Uji azan biasa")
                    }
                    OutlinedButton(
                        onClick = {
                            onTestAudio("Subuh", AzanAudioSource.BUILT_IN)
                        }
                    ) {
                        Text("Uji azan Subuh")
                    }
                    OutlinedButton(onClick = onStopAudio) {
                        Text("Henti")
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "Rakaman terbina dalam ditandai Public Domain Mark 1.0 pada Internet Archive.",
                    fontSize = 11.sp
                )
            } else {
                if (hasCustomAudio) {
                    Text("Audio: $audioName", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onTestAudio("Ujian", AzanAudioSource.CUSTOM)
                            }
                        ) {
                            Text("Uji azan")
                        }
                        OutlinedButton(onClick = onStopAudio) {
                            Text("Henti")
                        }
                        OutlinedButton(onClick = onPickAudio) {
                            Text("Tukar audio")
                        }
                        OutlinedButton(onClick = onClearAudio) {
                            Text("Buang")
                        }
                    }
                } else {
                    Button(onClick = onPickAudio) {
                        Text("Pilih fail azan")
                    }
                }
            }

            if (enabled && hasPlayableAudio) {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(Modifier.height(6.dp))

                PrayerAlarmScheduler.prayerNames.forEachIndexed { index, prayer ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Azan $prayer")
                        Switch(
                            checked = prayerEnabled[prayer] != false,
                            onCheckedChange = { onPrayerChange(prayer, it) }
                        )
                    }
                    if (index != PrayerAlarmScheduler.prayerNames.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }

            if (!hasExactAlarmAccess) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Azan penuh memerlukan akses alarm tepat untuk playback tepat pada waktu ketika app di latar belakang.",
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = onRequestExactAlarm) {
                    Text("Benarkan alarm tepat")
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "Azan berhenti apabila audio tamat, apabila audio focus hilang, atau selepas had keselamatan 10 minit.",
                fontSize = 12.sp
            )
        }
    }
}
