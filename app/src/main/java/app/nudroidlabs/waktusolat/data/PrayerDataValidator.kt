package app.nudroidlabs.waktusolat.data

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object PrayerDataValidator {
    private val apiTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val malaysiaZone = ZoneId.of("Asia/Kuala_Lumpur")

    fun validate(response: PrayerResponse, expectedZone: String) {
        require(response.status == "OK!") { "JAKIM status: ${response.status}" }
        require(response.zone.equals(expectedZone, ignoreCase = true)) {
            "JAKIM zone mismatch: expected $expectedZone, got ${response.zone}"
        }
        require(response.days.isNotEmpty()) { "JAKIM returned no prayer times" }
        require(response.days.size <= 14) { "Unexpected prayer-day count: ${response.days.size}" }

        var previousDate: LocalDate? = null
        response.days.forEach { day ->
            val date = PrayerTimeEngine.apiDate(day.dateRaw)
                ?: error("Invalid JAKIM date: ${day.dateRaw}")
            previousDate?.let { previous ->
                require(date.isAfter(previous)) {
                    "Prayer dates are duplicated or out of order: ${day.dateRaw}"
                }
            }
            previousDate = date

            val times = listOf(
                "imsak" to day.imsak,
                "subuh" to day.subuh,
                "syuruk" to day.syuruk,
                "dhuha" to day.dhuha,
                "zohor" to day.zohor,
                "asar" to day.asar,
                "maghrib" to day.maghrib,
                "isyak" to day.isyak
            ).map { (name, raw) ->
                name to runCatching { LocalTime.parse(raw, apiTimeFormatter) }
                    .getOrElse { throw IllegalArgumentException("Invalid $name time: $raw", it) }
            }

            times.zipWithNext().forEach { (first, second) ->
                require(second.second.isAfter(first.second)) {
                    "Prayer times out of order on ${day.dateRaw}: ${first.first} >= ${second.first}"
                }
            }
        }
    }

    fun isUsableForToday(
        response: PrayerResponse,
        today: LocalDate = LocalDate.now(malaysiaZone)
    ): Boolean {
        val dates = response.days.mapNotNull { PrayerTimeEngine.apiDate(it.dateRaw) }
        if (dates.size != response.days.size || dates.isEmpty()) return false
        return !today.isBefore(dates.first()) && !today.isAfter(dates.last())
    }
}
