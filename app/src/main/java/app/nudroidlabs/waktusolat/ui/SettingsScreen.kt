package app.nudroidlabs.waktusolat.ui

import androidx.compose.foundation.clickable
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
import app.nudroidlabs.waktusolat.data.JakimZones
import app.nudroidlabs.waktusolat.location.ZoneSuggestion
import app.nudroidlabs.waktusolat.notification.PrayerAlarmScheduler

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
    leadMinutes: Int,
    hasNotificationPermission: Boolean,
    hasExactAlarmAccess: Boolean,
    scheduleMessage: String?,
    azanEnabled: Boolean,
    azanAudioName: String?,
    onChooseZone: () -> Unit,
    onThemeModeChange: (AppearanceMode) -> Unit,
    onDetectLocation: () -> Unit,
    onUseSuggestion: (ZoneSuggestion) -> Unit,
    onMasterNotificationChange: (Boolean) -> Unit,
    onPrayerChange: (String, Boolean) -> Unit,
    onLeadMinutesChange: (Int) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarm: () -> Unit,
    onPickAzanAudio: () -> Unit,
    onAzanEnabledChange: (Boolean) -> Unit,
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
                Text("Waktu Solat Malaysia 0.5.0", color = MaterialTheme.colorScheme.primary)
            }
        }

        item { SectionLabel("Paparan") }

        item {
            AppearanceCard(mode = themeMode, onModeChange = onThemeModeChange)
        }

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
                leadMinutes = leadMinutes,
                hasNotificationPermission = hasNotificationPermission,
                hasExactAlarmAccess = hasExactAlarmAccess,
                status = scheduleMessage,
                onMasterChange = onMasterNotificationChange,
                onPrayerChange = onPrayerChange,
                onLeadMinutesChange = onLeadMinutesChange,
                onRequestNotificationPermission = onRequestNotificationPermission,
                onRequestExactAlarm = onRequestExactAlarm
            )
        }

        item { SectionLabel("Azan") }

        item {
            AzanSettingsCard(
                enabled = azanEnabled,
                audioName = azanAudioName,
                hasExactAlarmAccess = hasExactAlarmAccess,
                onEnabledChange = onAzanEnabledChange,
                onPickAudio = onPickAzanAudio,
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
    leadMinutes: Int,
    hasNotificationPermission: Boolean,
    hasExactAlarmAccess: Boolean,
    status: String?,
    onMasterChange: (Boolean) -> Unit,
    onPrayerChange: (String, Boolean) -> Unit,
    onLeadMinutesChange: (Int) -> Unit,
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
                    Text("Notifikasi waktu solat", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text("Peringatan masuk waktu untuk solat fardu.", fontSize = 12.sp)
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

                Text("Peringatan awal", fontWeight = FontWeight.SemiBold)
                Text("Pilih peringatan sebelum masuk waktu jika diperlukan.", fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PrayerAlarmScheduler.supportedLeadMinutes.forEach { minutes ->
                        FilterChip(
                            selected = leadMinutes == minutes,
                            onClick = { onLeadMinutesChange(minutes) },
                            label = { Text(if (minutes == 0) "Tiada" else "$minutes m") }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
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
    hasExactAlarmAccess: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onPickAudio: () -> Unit,
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
                    Text("Gunakan fail audio pilihan sendiri.", fontSize = 12.sp)
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onPickAudio) { Text("Tukar audio") }
                    OutlinedButton(onClick = onClearAudio) { Text("Buang") }
                }
            } else {
                Button(onClick = onPickAudio) { Text("Pilih fail azan") }
            }

            if (!hasExactAlarmAccess) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Benarkan alarm tepat untuk memastikan azan boleh bermula pada waktu yang dijadualkan.",
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = onRequestExactAlarm) {
                    Text("Benarkan alarm tepat")
                }
            }

            Spacer(Modifier.height(10.dp))
            Text("Audio berhenti selepas tamat dan mempunyai had keselamatan 10 minit.", fontSize = 12.sp)
        }
    }
}
