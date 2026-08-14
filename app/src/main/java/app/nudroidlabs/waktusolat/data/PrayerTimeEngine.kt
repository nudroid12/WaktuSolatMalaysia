package app.nudroidlabs.waktusolat.data

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class UpcomingPrayer(
    val name: String,
    val time: String,
    val target: LocalDateTime
)

object PrayerTimeEngine {
    private val apiTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val displayTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private val malayMonths = mapOf(
        "januari" to 1,
        "februari" to 2,
        "mac" to 3,
        "april" to 4,
        "mei" to 5,
        "jun" to 6,
        "julai" to 7,
        "ogos" to 8,
        "september" to 9,
        "oktober" to 10,
        "november" to 11,
        "disember" to 12
    )

    fun apiDate(raw: String): LocalDate? {
        val parts = raw.trim().split("-")
        if (parts.size != 3) return null
        val day = parts[0].toIntOrNull() ?: return null
        val month = malayMonths[parts[1].lowercase(Locale.ROOT)] ?: return null
        val year = parts[2].toIntOrNull() ?: return null
        return runCatching { LocalDate.of(year, month, day) }.getOrNull()
    }

    fun displayTime(raw: String): String = LocalTime.parse(raw, apiTimeFormatter).format(displayTimeFormatter)

    fun findUpcoming(days: List<PrayerDay>, now: LocalDateTime): UpcomingPrayer? {
        val candidates = buildList {
            days.forEach { day ->
                val date = apiDate(day.dateRaw) ?: return@forEach
                addCandidate("Subuh", day.subuh, date)
                addCandidate("Zohor", day.zohor, date)
                addCandidate("Asar", day.asar, date)
                addCandidate("Maghrib", day.maghrib, date)
                addCandidate("Isyak", day.isyak, date)
            }
        }
        return candidates.filter { it.target.isAfter(now) }.minByOrNull { it.target }
    }

    fun countdown(target: LocalDateTime, now: LocalDateTime): String {
        val seconds = Duration.between(now, target).seconds.coerceAtLeast(0)
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return "%02d:%02d:%02d".format(hours, minutes, secs)
    }

    private fun MutableList<UpcomingPrayer>.addCandidate(name: String, rawTime: String, date: LocalDate) {
        val time = runCatching { LocalTime.parse(rawTime, apiTimeFormatter) }.getOrNull() ?: return
        add(UpcomingPrayer(name, time.format(displayTimeFormatter), LocalDateTime.of(date, time)))
    }
}
