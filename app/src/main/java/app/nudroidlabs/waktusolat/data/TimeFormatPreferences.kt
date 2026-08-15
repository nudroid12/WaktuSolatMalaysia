package app.nudroidlabs.waktusolat.data

import android.content.Context
import androidx.core.content.edit
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class TimeFormatMode(val label: String) {
    HOUR_24("24 jam"),
    HOUR_12("12 jam")
}

object TimeFormatPreferences {
    private const val PREFS_NAME = "display_preferences"
    private const val KEY_TIME_FORMAT = "time_format"

    fun mode(context: Context): TimeFormatMode {
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TIME_FORMAT, null)
        return TimeFormatMode.entries.firstOrNull { it.name == stored }
            ?: TimeFormatMode.HOUR_24
    }

    fun setMode(context: Context, mode: TimeFormatMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(KEY_TIME_FORMAT, mode.name) }
    }
}

object PrayerTimeDisplayFormatter {
    private val apiFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val twentyFourHourFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val twelveHourFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
    private val malaysiaZone = ZoneId.of("Asia/Kuala_Lumpur")

    fun formatApiTime(raw: String, mode: TimeFormatMode): String {
        val time = runCatching { LocalTime.parse(raw, apiFormatter) }.getOrNull()
            ?: return raw
        return formatLocalTime(time, mode)
    }

    fun formatLocalTime(time: LocalTime, mode: TimeFormatMode): String =
        time.format(
            when (mode) {
                TimeFormatMode.HOUR_24 -> twentyFourHourFormatter
                TimeFormatMode.HOUR_12 -> twelveHourFormatter
            }
        )

    fun formatShortDateTime(dateTime: LocalDateTime, mode: TimeFormatMode): String {
        val datePart = dateTime.format(DateTimeFormatter.ofPattern("dd/MM"))
        return "$datePart, ${formatLocalTime(dateTime.toLocalTime(), mode)}"
    }

    fun formatFullMalaysiaDateTime(millis: Long, mode: TimeFormatMode): String {
        val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), malaysiaZone)
        val datePart = dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        return "$datePart ${formatLocalTime(dateTime.toLocalTime(), mode)}"
    }
}
