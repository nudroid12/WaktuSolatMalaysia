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
import androidx.core.content.edit
import app.nudroidlabs.waktusolat.data.PrayerDay
import app.nudroidlabs.waktusolat.data.PrayerTimeEngine
import java.time.LocalDate
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
    const val CHANNEL_ID_SILENT = "prayer_times_silent"
    const val EXTRA_PRAYER_NAME = "prayer_name"
    const val EXTRA_PRAYER_TIME = "prayer_time"
    const val EXTRA_ZONE_CODE = "zone_code"
    const val EXTRA_KIND = "kind"
    const val EXTRA_LEAD_MINUTES = "lead_minutes"
    const val KIND_ENTRY = "entry"
    const val KIND_EARLY = "early"

    val prayerNames: List<String> = listOf("Subuh", "Zohor", "Asar", "Maghrib", "Isyak")
    val supportedLeadMinutes: List<Int> = listOf(0, 5, 10, 15)

    private val malaysiaZone = ZoneId.of("Asia/Kuala_Lumpur")
    private val apiTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val displayTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun createNotificationChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Waktu solat",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Peringatan masuk waktu solat dan peringatan awal"
            enableVibration(true)
        }
        val silentChannel = NotificationChannel(
            CHANNEL_ID_SILENT,
            "Waktu solat dengan azan",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Peringatan masuk waktu apabila audio azan penuh dimainkan"
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
        manager.createNotificationChannel(silentChannel)
    }

    fun notificationsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_MASTER_ENABLED, false)

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_MASTER_ENABLED, enabled) }
        if (!enabled) cancelAll(context)
    }

    fun prayerEnabled(context: Context, prayerName: String): Boolean =
        prefs(context).getBoolean("$KEY_PRAYER_PREFIX$prayerName", true)

    fun setPrayerEnabled(context: Context, prayerName: String, enabled: Boolean) {
        require(prayerName in prayerNames) { "Unknown prayer name: $prayerName" }
        prefs(context).edit { putBoolean("$KEY_PRAYER_PREFIX$prayerName", enabled) }
    }

    fun leadMinutes(context: Context): Int {
        val stored = prefs(context).getInt(KEY_LEAD_MINUTES, 0)
        return stored.takeIf { it in supportedLeadMinutes } ?: 0
    }

    fun setLeadMinutes(context: Context, minutes: Int) {
        require(minutes in supportedLeadMinutes) { "Unsupported lead minutes: $minutes" }
        prefs(context).edit { putInt(KEY_LEAD_MINUTES, minutes) }
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
        val earlyMinutes = leadMinutes(context)
        var count = 0

        days.forEach { day ->
            val date = PrayerTimeEngine.apiDate(day.dateRaw) ?: return@forEach
            prayerSpecs(day).forEachIndexed { prayerIndex, (name, rawTime) ->
                if (!prayerEnabled(context, name)) return@forEachIndexed

                val time = runCatching { LocalTime.parse(rawTime, apiTimeFormatter) }
                    .getOrNull() ?: return@forEachIndexed
                val prayerTarget = LocalDateTime.of(date, time)

                val entryCode = requestCode(date, prayerIndex, 0)
                if (scheduleOne(
                        context = context,
                        alarmManager = alarmManager,
                        exact = exact,
                        trigger = prayerTarget,
                        nowMillis = nowMillis,
                        requestCode = entryCode,
                        prayerName = name,
                        prayerTime = time.format(displayTimeFormatter),
                        zoneCode = zoneCode,
                        kind = KIND_ENTRY,
                        leadMinutes = 0
                    )
                ) {
                    scheduledIds += entryCode.toString()
                    count++
                }

                if (earlyMinutes > 0) {
                    val earlyCode = requestCode(date, prayerIndex, 1)
                    if (scheduleOne(
                            context = context,
                            alarmManager = alarmManager,
                            exact = exact,
                            trigger = prayerTarget.minusMinutes(earlyMinutes.toLong()),
                            nowMillis = nowMillis,
                            requestCode = earlyCode,
                            prayerName = name,
                            prayerTime = time.format(displayTimeFormatter),
                            zoneCode = zoneCode,
                            kind = KIND_EARLY,
                            leadMinutes = earlyMinutes
                        )
                    ) {
                        scheduledIds += earlyCode.toString()
                        count++
                    }
                }
            }
        }

        prefs(context).edit {
            putStringSet(KEY_SCHEDULED_IDS, scheduledIds)
        }

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

        prefs(context).edit { remove(KEY_SCHEDULED_IDS) }
    }

    private fun scheduleOne(
        context: Context,
        alarmManager: AlarmManager,
        exact: Boolean,
        trigger: LocalDateTime,
        nowMillis: Long,
        requestCode: Int,
        prayerName: String,
        prayerTime: String,
        zoneCode: String,
        kind: String,
        leadMinutes: Int
    ): Boolean {
        val triggerAtMillis = trigger
            .atZone(malaysiaZone)
            .toInstant()
            .toEpochMilli()
        if (triggerAtMillis <= nowMillis + 30_000L) return false

        val pendingIntent = alarmPendingIntent(
            context = context,
            requestCode = requestCode,
            prayerName = prayerName,
            prayerTime = prayerTime,
            zoneCode = zoneCode,
            kind = kind,
            leadMinutes = leadMinutes
        )

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
        return true
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
        zoneCode: String,
        kind: String,
        leadMinutes: Int
    ): PendingIntent {
        val intent = Intent(context, PrayerAlarmReceiver::class.java)
            .setAction("$ACTION_ALARM.$requestCode")
            .putExtra(EXTRA_PRAYER_NAME, prayerName)
            .putExtra(EXTRA_PRAYER_TIME, prayerTime)
            .putExtra(EXTRA_ZONE_CODE, zoneCode)
            .putExtra(EXTRA_KIND, kind)
            .putExtra(EXTRA_LEAD_MINUTES, leadMinutes)

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCode(date: LocalDate, prayerIndex: Int, kindIndex: Int): Int {
        val dayPart = Math.floorMod(date.toEpochDay(), 100_000L).toInt()
        return dayPart * 100 + prayerIndex * 2 + kindIndex
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private const val PREFS_NAME = "prayer_notification_settings"
    private const val KEY_MASTER_ENABLED = "master_enabled"
    private const val KEY_PRAYER_PREFIX = "prayer_"
    private const val KEY_LEAD_MINUTES = "lead_minutes"
    private const val KEY_SCHEDULED_IDS = "scheduled_alarm_ids"
    private const val ACTION_ALARM = "app.nudroidlabs.waktusolat.PRAYER_ALARM"
}
