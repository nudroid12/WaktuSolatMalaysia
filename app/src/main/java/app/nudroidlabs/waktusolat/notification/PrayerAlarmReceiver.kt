package app.nudroidlabs.waktusolat.notification

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.nudroidlabs.waktusolat.MainActivity
import app.nudroidlabs.waktusolat.R
import app.nudroidlabs.waktusolat.audio.AzanPlaybackService
import app.nudroidlabs.waktusolat.audio.AzanPreferences

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
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

        val isEntry = kind == PrayerAlarmScheduler.KIND_ENTRY
        val notificationEnabled = PrayerAlarmScheduler.notificationsEnabled(context) &&
            PrayerAlarmScheduler.prayerEnabled(context, prayerName)
        val canPostNotification = notificationEnabled &&
            PrayerAlarmScheduler.hasNotificationPermission(context)
        val canPlayAzan = isEntry &&
            PrayerAlarmScheduler.canScheduleExact(context) &&
            AzanPreferences.enabledForPrayer(context, prayerName)

        if (!canPostNotification && !canPlayAzan) return

        if (canPostNotification) {
            PrayerAlarmScheduler.createNotificationChannels(context)

            val openApp = PendingIntent.getActivity(
                context,
                1001,
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

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

            val channelId = PrayerAlarmScheduler.channelFor(context, canPlayAzan)
            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(openApp)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSilent(
                    canPlayAzan || PrayerAlarmScheduler.alertStyle(context) == PrayerAlertStyle.SILENT
                )

            if (canPlayAzan) {
                builder.addAction(0, "Henti azan", AzanPlaybackService.stopPendingIntent(context))
            }

            val prayerIndex = PrayerAlarmScheduler.prayerNames.indexOf(prayerName).coerceAtLeast(0)
            val kindOffset = if (kind == PrayerAlarmScheduler.KIND_EARLY) 50 else 0
            try {
                postNotification(
                    context = context,
                    notificationId = 2000 + prayerIndex + kindOffset,
                    builder = builder
                )
            } catch (_: SecurityException) {
                if (!canPlayAzan) return
            }
        }

        if (canPlayAzan) {
            runCatching { AzanPlaybackService.start(context, prayerName) }
        }
    }
    @SuppressLint("MissingPermission")
    private fun postNotification(
        context: Context,
        notificationId: Int,
        builder: NotificationCompat.Builder
    ) {
        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

}
