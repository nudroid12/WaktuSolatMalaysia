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
import app.nudroidlabs.waktusolat.notification.PrayerAlarmScheduler

@Composable
fun SettingsScreen(
    modifier: Modifier,
    zoneCode: String,
    notificationsEnabled: Boolean,
    enabledPrayers: Map<String, Boolean>,
    leadMinutes: Int,
    hasNotificationPermission: Boolean,
    hasExactAlarmAccess: Boolean,
    scheduleMessage: String?,
    azanEnabled: Boolean,
    azanAudioName: String?,
    onChooseZone: () -> Unit,
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
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Tetapan", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Waktu Solat Malaysia 0.3.0", color = MaterialTheme.colorScheme.primary)
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
                        "Zon JAKIM",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("$zoneCode · ${zone.state}", fontWeight = FontWeight.Bold)
                    Text(zone.area, fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Tekan untuk tukar zon", fontSize = 12.sp)
                }
            }
        }

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

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        "Ringan dan jimat bateri",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("• GPS hanya digunakan apabila anda menekan butang kesan lokasi.")
                    Text("• Sensor kompas hanya aktif ketika halaman Kiblat dibuka.")
                    Text("• Jadual JAKIM dicache dan kerja latar belakang disemak sekali sehari.")
                    Text("• Tiada analytics, iklan atau tracker.")
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
                    Text(
                        "Notifikasi waktu solat",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("Peringatan masuk waktu untuk solat fardu.", fontSize = 12.sp)
                }
                Switch(checked = masterEnabled, onCheckedChange = onMasterChange)
            }

            if (masterEnabled) {
                Spacer(Modifier.height(12.dp))

                if (!hasNotificationPermission) {
                    Text("Kebenaran notifikasi belum diberikan.", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = onRequestNotificationPermission) {
                        Text("Benarkan notifikasi")
                    }
                    Spacer(Modifier.height(10.dp))
                }

                if (!hasExactAlarmAccess) {
                    Text(
                        "Alarm tepat belum dibenarkan. Android boleh melambatkan peringatan.",
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = onRequestExactAlarm) {
                        Text("Benarkan alarm tepat")
                    }
                    Spacer(Modifier.height(10.dp))
                }

                Text("Peringatan awal", fontWeight = FontWeight.SemiBold)
                Text(
                    "Selain notifikasi tepat pada masuk waktu, pilih peringatan awal jika mahu.",
                    fontSize = 12.sp
                )
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
                    Text(
                        "Azan penuh",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Guna fail audio pilihan anda. Audio tidak dibundel supaya APK kekal kecil.",
                        fontSize = 12.sp
                    )
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
                Spacer(Modifier.height(10.dp))
                Text(
                    "Azan penuh memerlukan alarm tepat supaya Android membenarkan main balik " +
                        "bermula pada waktu yang dijadualkan.",
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = onRequestExactAlarm) {
                    Text("Benarkan alarm tepat")
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "Main balik berhenti sendiri apabila audio tamat dan mempunyai had keselamatan 10 minit.",
                fontSize = 12.sp
            )
        }
    }
}
