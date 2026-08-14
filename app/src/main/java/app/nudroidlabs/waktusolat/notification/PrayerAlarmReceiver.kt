package app.nudroidlabs.waktusolat.notification

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.nudroidlabs.waktusolat.MainActivity
import app.nudroidlabs.waktusolat.R
import app.nudroidlabs.waktusolat.audio.AzanPlaybackService
import app.nudroidlabs.waktusolat.audio.AzanPreferences

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!PrayerAlarmScheduler.notificationsEnabled(context)) return
        if (!PrayerAlarmScheduler.hasNotificationPermission(context)) return

        val prayerName = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_NAME)
            ?.takeIf(String::isNotBlank)
            ?: return
        val prayerTime = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_TIME)
            ?.takeIf(String::isNotBlank)
            ?: return
        val zoneCode = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_ZONE_CODE)
            ?.takeIf(String::isNotBlank)
            ?: return
        val kind = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_KIND)
            ?: PrayerAlarmScheduler.KIND_ENTRY
        val leadMinutes = intent.getIntExtra(PrayerAlarmScheduler.EXTRA_LEAD_MINUTES, 0)

        PrayerAlarmScheduler.createNotificationChannel(context)

        val openApp = PendingIntent.getActivity(
            context,
            1001,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isEntry = kind == PrayerAlarmScheduler.KIND_ENTRY
        val canPlayAzan = isEntry &&
            AzanPreferences.enabled(context) &&
            !AzanPreferences.audioUri(context).isNullOrBlank() &&
            PrayerAlarmScheduler.canScheduleExact(context)

        val title = if (kind == PrayerAlarmScheduler.KIND_EARLY && leadMinutes > 0) {
            "$prayerName dalam $leadMinutes minit"
        } else {
            "Waktu $prayerName"
        }
        val content = if (kind == PrayerAlarmScheduler.KIND_EARLY && leadMinutes > 0) {
            "$prayerName pada $prayerTime · Zon $zoneCode"
        } else {
            "$prayerName masuk pada $prayerTime · Zon $zoneCode"
        }

        val channelId = if (canPlayAzan) {
            PrayerAlarmScheduler.CHANNEL_ID_SILENT
        } else {
            PrayerAlarmScheduler.CHANNEL_ID
        }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(canPlayAzan)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val prayerIndex = PrayerAlarmScheduler.prayerNames.indexOf(prayerName).coerceAtLeast(0)
        val kindOffset = if (kind == PrayerAlarmScheduler.KIND_EARLY) 50 else 0
        try {
            NotificationManagerCompat.from(context)
                .notify(2000 + prayerIndex + kindOffset, notification)
        } catch (_: SecurityException) {
            return
        }

        if (canPlayAzan) {
            runCatching { AzanPlaybackService.start(context, prayerName) }
        }
    }
}
