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
import app.nudroidlabs.waktusolat.audio.AzanPreferences
import app.nudroidlabs.waktusolat.data.PrayerDay
import app.nudroidlabs.waktusolat.data.PrayerTimeDisplayFormatter
import app.nudroidlabs.waktusolat.data.PrayerTimeEngine
import app.nudroidlabs.waktusolat.data.TimeFormatPreferences
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class PrayerAlertStyle(val label: String) {
    SOUND("Bunyi"),
    VIBRATE("Getar"),
    SILENT("Senyap")
}

data class NotificationScheduleReport(
    val scheduledCount: Int,
    val exact: Boolean
)

object PrayerAlarmScheduler {
    const val CHANNEL_ID_SOUND = "prayer_times_sound_v1"
    const val CHANNEL_ID_VIBRATE = "prayer_times_vibrate_v1"
    const val CHANNEL_ID_SILENT = "prayer_times_silent_v1"
    const val CHANNEL_ID_AZAN = "prayer_times_azan_v1"

    const val EXTRA_PRAYER_NAME = "prayer_name"
    const val EXTRA_PRAYER_TIME = "prayer_time"
    const val EXTRA_ZONE_CODE = "zone_code"
    const val EXTRA_KIND = "kind"
    const val EXTRA_LEAD_MINUTES = "lead_minutes"
    const val KIND_ENTRY = "entry"
    const val KIND_EARLY = "early"

    val prayerNames: List<String> = listOf("Subuh", "Zohor", "Asar", "Maghrib", "Isyak")
    val supportedLeadMinutes: List<Int> = listOf(0, 5, 10, 15, 20, 30)

    private val malaysiaZone = ZoneId.of("Asia/Kuala_Lumpur")
    private val apiTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    fun createNotificationChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val soundChannel = NotificationChannel(
            CHANNEL_ID_SOUND,
            "Waktu solat · bunyi",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Peringatan waktu solat dengan bunyi dan getaran"
            enableVibration(true)
        }

        val vibrateChannel = NotificationChannel(
            CHANNEL_ID_VIBRATE,
            "Waktu solat · getar",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Peringatan waktu solat tanpa bunyi, dengan getaran"
            setSound(null, null)
            enableVibration(true)
        }

        val silentChannel = NotificationChannel(
            CHANNEL_ID_SILENT,
            "Waktu solat · senyap",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Peringatan waktu solat tanpa bunyi atau getaran"
            setSound(null, null)
            enableVibration(false)
        }

        val azanChannel = NotificationChannel(
            CHANNEL_ID_AZAN,
            "Waktu solat dengan azan",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Peringatan masuk waktu apabila audio azan penuh dimainkan"
            setSound(null, null)
            enableVibration(false)
        }

        manager.createNotificationChannels(
            listOf(soundChannel, vibrateChannel, silentChannel, azanChannel)
        )
    }

    fun notificationsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_MASTER_ENABLED, false)

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_MASTER_ENABLED, enabled) }
    }

    fun prayerEnabled(context: Context, prayerName: String): Boolean =
        prefs(context).getBoolean("$KEY_PRAYER_PREFIX$prayerName", true)

    fun setPrayerEnabled(context: Context, prayerName: String, enabled: Boolean) {
        requirePrayer(prayerName)
        prefs(context).edit { putBoolean("$KEY_PRAYER_PREFIX$prayerName", enabled) }
    }

    fun leadMinutes(context: Context, prayerName: String): Int {
        requirePrayer(prayerName)
        val stored = prefs(context).getInt("$KEY_LEAD_PREFIX$prayerName", -1)
        if (stored in supportedLeadMinutes) return stored

        val legacy = prefs(context).getInt(KEY_LEGACY_LEAD_MINUTES, 0)
        return legacy.takeIf { it in supportedLeadMinutes } ?: 0
    }

    fun setLeadMinutes(context: Context, prayerName: String, minutes: Int) {
        requirePrayer(prayerName)
        require(minutes in supportedLeadMinutes) { "Unsupported lead minutes: $minutes" }
        prefs(context).edit { putInt("$KEY_LEAD_PREFIX$prayerName", minutes) }
    }

    fun alertStyle(context: Context): PrayerAlertStyle {
        val stored = prefs(context).getString(KEY_ALERT_STYLE, null)
        return PrayerAlertStyle.entries.firstOrNull { it.name == stored }
            ?: PrayerAlertStyle.SOUND
    }

    fun setAlertStyle(context: Context, style: PrayerAlertStyle) {
        prefs(context).edit { putString(KEY_ALERT_STYLE, style.name) }
    }

    fun channelFor(context: Context, azanPlaying: Boolean): String {
        if (azanPlaying) return CHANNEL_ID_AZAN
        return when (alertStyle(context)) {
            PrayerAlertStyle.SOUND -> CHANNEL_ID_SOUND
            PrayerAlertStyle.VIBRATE -> CHANNEL_ID_VIBRATE
            PrayerAlertStyle.SILENT -> CHANNEL_ID_SILENT
        }
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

    fun scheduleNeeded(context: Context): Boolean =
        notificationsEnabled(context) ||
            (AzanPreferences.enabled(context) && canScheduleExact(context))

    fun reschedule(
        context: Context,
        days: List<PrayerDay>,
        zoneCode: String
    ): NotificationScheduleReport {
        cancelAll(context)
        createNotificationChannels(context)

        if (!scheduleNeeded(context)) {
            return NotificationScheduleReport(0, canScheduleExact(context))
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        var exactAvailable = canScheduleExact(context)
        val nowMillis = System.currentTimeMillis()
        val scheduledIds = mutableSetOf<String>()
        var count = 0

        days.forEach { day ->
            val date = PrayerTimeEngine.apiDate(day.dateRaw) ?: return@forEach
            prayerSpecs(day).forEachIndexed { prayerIndex, (name, rawTime) ->
                val notificationEntry = notificationsEnabled(context) && prayerEnabled(context, name)
                val azanEntry = exactAvailable && AzanPreferences.enabledForPrayer(context, name)
                if (!notificationEntry && !azanEntry) return@forEachIndexed

                val time = runCatching { LocalTime.parse(rawTime, apiTimeFormatter) }
                    .getOrNull() ?: return@forEachIndexed
                val prayerTarget = LocalDateTime.of(date, time)

                val entryCode = requestCode(date, prayerIndex, 0)
                when (
                    scheduleOne(
                        context = context,
                        alarmManager = alarmManager,
                        exactPreferred = exactAvailable,
                        trigger = prayerTarget,
                        nowMillis = nowMillis,
                        requestCode = entryCode,
                        prayerName = name,
                        prayerTime = PrayerTimeDisplayFormatter.formatLocalTime(time, TimeFormatPreferences.mode(context)),
                        zoneCode = zoneCode,
                        kind = KIND_ENTRY,
                        leadMinutes = 0
                    )
                ) {
                    ScheduleOutcome.EXACT -> {
                        scheduledIds += entryCode.toString()
                        count++
                    }

                    ScheduleOutcome.INEXACT -> {
                        exactAvailable = false
                        scheduledIds += entryCode.toString()
                        count++
                    }

                    ScheduleOutcome.SKIPPED -> Unit
                }

                val earlyMinutes = leadMinutes(context, name)
                if (notificationEntry && earlyMinutes > 0) {
                    val earlyCode = requestCode(date, prayerIndex, 1)
                    when (
                        scheduleOne(
                            context = context,
                            alarmManager = alarmManager,
                            exactPreferred = exactAvailable,
                            trigger = prayerTarget.minusMinutes(earlyMinutes.toLong()),
                            nowMillis = nowMillis,
                            requestCode = earlyCode,
                            prayerName = name,
                            prayerTime = PrayerTimeDisplayFormatter.formatLocalTime(time, TimeFormatPreferences.mode(context)),
                            zoneCode = zoneCode,
                            kind = KIND_EARLY,
                            leadMinutes = earlyMinutes
                        )
                    ) {
                        ScheduleOutcome.EXACT -> {
                            scheduledIds += earlyCode.toString()
                            count++
                        }

                        ScheduleOutcome.INEXACT -> {
                            exactAvailable = false
                            scheduledIds += earlyCode.toString()
                            count++
                        }

                        ScheduleOutcome.SKIPPED -> Unit
                    }
                }
            }
        }

        prefs(context).edit { putStringSet(KEY_SCHEDULED_IDS, scheduledIds) }
        return NotificationScheduleReport(count, exactAvailable)
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
        exactPreferred: Boolean,
        trigger: LocalDateTime,
        nowMillis: Long,
        requestCode: Int,
        prayerName: String,
        prayerTime: String,
        zoneCode: String,
        kind: String,
        leadMinutes: Int
    ): ScheduleOutcome {
        val triggerAtMillis = trigger
            .atZone(malaysiaZone)
            .toInstant()
            .toEpochMilli()
        if (triggerAtMillis <= nowMillis + 30_000L) return ScheduleOutcome.SKIPPED

        val pendingIntent = alarmPendingIntent(
            context = context,
            requestCode = requestCode,
            prayerName = prayerName,
            prayerTime = prayerTime,
            zoneCode = zoneCode,
            kind = kind,
            leadMinutes = leadMinutes
        )

        if (exactPreferred) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                return ScheduleOutcome.EXACT
            } catch (_: SecurityException) {
                // Permission can change between the capability check and scheduling.
            }
        }

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
        return ScheduleOutcome.INEXACT
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

    private fun requirePrayer(prayerName: String) {
        require(prayerName in prayerNames) { "Unknown prayer name: $prayerName" }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private enum class ScheduleOutcome {
        EXACT,
        INEXACT,
        SKIPPED
    }

    private const val PREFS_NAME = "prayer_notification_settings"
    private const val KEY_MASTER_ENABLED = "master_enabled"
    private const val KEY_PRAYER_PREFIX = "prayer_"
    private const val KEY_LEAD_PREFIX = "lead_minutes_"
    private const val KEY_LEGACY_LEAD_MINUTES = "lead_minutes"
    private const val KEY_ALERT_STYLE = "alert_style"
    private const val KEY_SCHEDULED_IDS = "scheduled_alarm_ids"
    private const val ACTION_ALARM = "app.nudroidlabs.waktusolat.PRAYER_ALARM"
}
