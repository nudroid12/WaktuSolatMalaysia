package app.nudroidlabs.waktusolat.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import app.nudroidlabs.waktusolat.data.PrayerDay
import app.nudroidlabs.waktusolat.data.PrayerTimeEngine
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class NotificationScheduleReport(
    val scheduledCount: Int,
    val exact: Boolean
)

object PrayerAlarmScheduler {
    const val CHANNEL_ID = "prayer_times"
    const val EXTRA_PRAYER_NAME = "prayer_name"
    const val EXTRA_PRAYER_TIME = "prayer_time"
    const val EXTRA_ZONE_CODE = "zone_code"

    val prayerNames: List<String> = listOf("Subuh", "Zohor", "Asar", "Maghrib", "Isyak")

    private val malaysiaZone = ZoneId.of("Asia/Kuala_Lumpur")
    private val apiTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Waktu solat",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Peringatan apabila masuk waktu solat"
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun notificationsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_MASTER_ENABLED, false)

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_MASTER_ENABLED, enabled).apply()
        if (!enabled) cancelAll(context)
    }

    fun prayerEnabled(context: Context, prayerName: String): Boolean =
        prefs(context).getBoolean("$KEY_PRAYER_PREFIX$prayerName", true)

    fun setPrayerEnabled(context: Context, prayerName: String, enabled: Boolean) {
        require(prayerName in prayerNames) { "Unknown prayer name: $prayerName" }
        prefs(context).edit().putBoolean("$KEY_PRAYER_PREFIX$prayerName", enabled).apply()
    }

    fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    fun reschedule(
        context: Context,
        days: List<PrayerDay>,
        zoneCode: String
    ): NotificationScheduleReport {
        cancelAll(context)
        createNotificationChannel(context)

        if (!notificationsEnabled(context)) {
            return NotificationScheduleReport(0, canScheduleExact(context))
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val exact = canScheduleExact(context)
        val nowMillis = System.currentTimeMillis()
        val scheduledIds = mutableSetOf<String>()
        var count = 0

        days.forEach { day ->
            val date = PrayerTimeEngine.apiDate(day.dateRaw) ?: return@forEach
            prayerSpecs(day).forEachIndexed { prayerIndex, (name, rawTime) ->
                if (!prayerEnabled(context, name)) return@forEachIndexed

                val time = runCatching {
                    LocalTime.parse(rawTime, apiTimeFormatter)
                }.getOrNull() ?: return@forEachIndexed

                val target = LocalDateTime.of(date, time)
                val triggerAtMillis = target
                    .atZone(malaysiaZone)
                    .toInstant()
                    .toEpochMilli()

                if (triggerAtMillis <= nowMillis + 30_000L) return@forEachIndexed

                val requestCode = requestCode(date.toEpochDay(), prayerIndex)
                val pendingIntent = alarmPendingIntent(
                    context = context,
                    requestCode = requestCode,
                    prayerName = name,
                    prayerTime = time.format(DateTimeFormatter.ofPattern("HH:mm")),
                    zoneCode = zoneCode
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (exact) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }

                scheduledIds += requestCode.toString()
                count++
            }
        }

        prefs(context).edit()
            .putStringSet(KEY_SCHEDULED_IDS, scheduledIds)
            .apply()

        return NotificationScheduleReport(count, exact)
    }

    fun cancelAll(context: Context) {
        val stored = prefs(context).getStringSet(KEY_SCHEDULED_IDS, emptySet())
            ?.toSet()
            .orEmpty()

        if (stored.isEmpty()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        stored.forEach { raw ->
            val requestCode = raw.toIntOrNull() ?: return@forEach
            val intent = Intent(context, PrayerAlarmReceiver::class.java)
                .setAction("$ACTION_ALARM.$requestCode")
            val pending = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pending)
            pending.cancel()
        }

        prefs(context).edit().remove(KEY_SCHEDULED_IDS).apply()
    }

    private fun prayerSpecs(day: PrayerDay): List<Pair<String, String>> = listOf(
        "Subuh" to day.subuh,
        "Zohor" to day.zohor,
        "Asar" to day.asar,
        "Maghrib" to day.maghrib,
        "Isyak" to day.isyak
    )

    private fun alarmPendingIntent(
        context: Context,
        requestCode: Int,
        prayerName: String,
        prayerTime: String,
        zoneCode: String
    ): PendingIntent {
        val intent = Intent(context, PrayerAlarmReceiver::class.java)
            .setAction("$ACTION_ALARM.$requestCode")
            .putExtra(EXTRA_PRAYER_NAME, prayerName)
            .putExtra(EXTRA_PRAYER_TIME, prayerTime)
            .putExtra(EXTRA_ZONE_CODE, zoneCode)

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCode(epochDay: Long, prayerIndex: Int): Int {
        val dayPart = Math.floorMod(epochDay, 100_000L).toInt()
        return dayPart * 10 + prayerIndex
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private const val PREFS_NAME = "prayer_notification_settings"
    private const val KEY_MASTER_ENABLED = "master_enabled"
    private const val KEY_PRAYER_PREFIX = "prayer_"
    private const val KEY_SCHEDULED_IDS = "scheduled_alarm_ids"
    private const val ACTION_ALARM = "app.nudroidlabs.waktusolat.PRAYER_ALARM"
}
