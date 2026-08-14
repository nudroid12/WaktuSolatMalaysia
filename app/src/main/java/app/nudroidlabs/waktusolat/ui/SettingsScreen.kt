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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nudroidlabs.waktusolat.BuildConfig
import app.nudroidlabs.waktusolat.data.JakimZones
import app.nudroidlabs.waktusolat.location.ZoneSuggestion
import app.nudroidlabs.waktusolat.notification.PrayerAlarmScheduler
import app.nudroidlabs.waktusolat.notification.PrayerAlertStyle

@Composable
fun SettingsScreen(
    modifier: Modifier,
    zoneCode: String,
    themeMode: AppearanceMode,
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
    azanAudioName: String?,
    azanEnabledPrayers: Map<String, Boolean>,
    onChooseZone: () -> Unit,
    onThemeModeChange: (AppearanceMode) -> Unit,
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
    onAzanPrayerChange: (String, Boolean) -> Unit,
    onTestAzan: () -> Unit,
    onStopAzan: () -> Unit,
    onClearAzanAudio: () -> Unit
) {
    val zone = JakimZones.byCode(zoneCode)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("Tetapan", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Waktu Solat Malaysia ${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.primary)
            }
        }

        item { SectionLabel("Paparan") }
        item { AppearanceCard(mode = themeMode, onModeChange = onThemeModeChange) }

        item { SectionLabel("Lokasi dan zon") }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onChooseZone),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Zon JAKIM", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
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

        item { SectionLabel("Peringatan") }
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

        item { SectionLabel("Azan") }
        item {
            AzanSettingsCard(
                enabled = azanEnabled,
                audioName = azanAudioName,
                prayerEnabled = azanEnabledPrayers,
                hasExactAlarmAccess = hasExactAlarmAccess,
                onEnabledChange = onAzanEnabledChange,
                onPrayerChange = onAzanPrayerChange,
                onPickAudio = onPickAzanAudio,
                onTestAudio = onTestAzan,
                onStopAudio = onStopAzan,
                onClearAudio = onClearAzanAudio,
                onRequestExactAlarm = onRequestExactAlarm
            )
        }

        item { SectionLabel("Tentang aplikasi") }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("NudroidLabs", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text("Waktu Solat Malaysia", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Sumber jadual: e-Solat JAKIM", fontSize = 13.sp)
                    Text("Lokasi hanya digunakan apabila diminta.", fontSize = 13.sp)
                    Text("Kompas hanya aktif ketika halaman Kiblat dibuka.", fontSize = 13.sp)
                    Text("Tiada analytics, iklan atau tracker.", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
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
    audioName: String?,
    prayerEnabled: Map<String, Boolean>,
    hasExactAlarmAccess: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onPrayerChange: (String, Boolean) -> Unit,
    onPickAudio: () -> Unit,
    onTestAudio: () -> Unit,
    onStopAudio: () -> Unit,
    onClearAudio: () -> Unit,
    onRequestExactAlarm: () -> Unit
) {
    val hasAudio = !audioName.isNullOrBlank()

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
                    Text("Azan penuh", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text("Gunakan fail audio pilihan sendiri dan pilih solat yang memainkannya.", fontSize = 12.sp)
                }
                Switch(
                    checked = enabled && hasAudio,
                    onCheckedChange = { value ->
                        if (hasAudio) onEnabledChange(value) else if (value) onPickAudio()
                    }
                )
            }

            Spacer(Modifier.height(10.dp))
            if (hasAudio) {
                Text("Audio: $audioName", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onTestAudio) { Text("Uji azan") }
                    OutlinedButton(onClick = onStopAudio) { Text("Henti") }
                    OutlinedButton(onClick = onPickAudio) { Text("Tukar audio") }
                    OutlinedButton(onClick = onClearAudio) { Text("Buang") }
                }
            } else {
                Button(onClick = onPickAudio) { Text("Pilih fail azan") }
            }

            if (enabled && hasAudio) {
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
                    "Azan penuh memerlukan akses alarm tepat supaya Android membenarkan tindakan tepat pada waktunya ketika app di latar belakang.",
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = onRequestExactAlarm) {
                    Text("Benarkan alarm tepat")
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "Ujian azan bermula serta-merta. Playback sebenar berhenti apabila audio tamat atau selepas had keselamatan 10 minit.",
                fontSize = 12.sp
            )
        }
    }
}
