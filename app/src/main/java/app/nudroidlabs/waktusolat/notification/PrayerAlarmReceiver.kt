package app.nudroidlabs.waktusolat.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.nudroidlabs.waktusolat.MainActivity
import app.nudroidlabs.waktusolat.R

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

        PrayerAlarmScheduler.createNotificationChannel(context)

        val openApp = PendingIntent.getActivity(
            context,
            1001,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, PrayerAlarmScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Waktu $prayerName")
            .setContentText("$prayerName masuk pada $prayerTime · Zon $zoneCode")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val id = 2000 + PrayerAlarmScheduler.prayerNames.indexOf(prayerName).coerceAtLeast(0)
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
