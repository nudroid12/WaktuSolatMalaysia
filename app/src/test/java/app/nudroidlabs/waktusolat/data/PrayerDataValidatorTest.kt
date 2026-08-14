package app.nudroidlabs.waktusolat.data

import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerDataValidatorTest {
    @Test
    fun validWeekPasses() {
        val response = response(
            listOf(
                day("15-Ogos-2026"),
                day("16-Ogos-2026")
            )
        )
        PrayerDataValidator.validate(response, "WLY01")
    }

    @Test
    fun wrongZoneFails() {
        val error = expectRejected {
            PrayerDataValidator.validate(response(listOf(day("15-Ogos-2026"))), "SGR01")
        }
        assertTrue(error.message.orEmpty().contains("zone mismatch", ignoreCase = true))
    }

    @Test
    fun unorderedTimesFail() {
        // 07:00 is before Dhuha 07:35, so the sequence is genuinely invalid.
        val broken = day("15-Ogos-2026").copy(zohor = "07:00:00")
        val error = expectRejected {
            PrayerDataValidator.validate(response(listOf(broken)), "WLY01")
        }
        assertTrue(error.message.orEmpty().contains("dhuha >= zohor", ignoreCase = true))
    }

    @Test
    fun invalidTimeFails() {
        val broken = day("15-Ogos-2026").copy(subuh = "25:00:00")
        val error = expectRejected {
            PrayerDataValidator.validate(response(listOf(broken)), "WLY01")
        }
        assertTrue(error.message.orEmpty().contains("Invalid subuh time"))
    }

    @Test
    fun duplicateDatesFail() {
        val error = expectRejected {
            PrayerDataValidator.validate(
                response(listOf(day("15-Ogos-2026"), day("15-Ogos-2026"))),
                "WLY01"
            )
        }
        assertTrue(error.message.orEmpty().contains("duplicated or out of order"))
    }

    @Test
    fun badStatusFails() {
        val broken = response(listOf(day("15-Ogos-2026"))).copy(status = "ERROR")
        val error = expectRejected {
            PrayerDataValidator.validate(broken, "WLY01")
        }
        assertTrue(error.message.orEmpty().contains("JAKIM status"))
    }

    @Test
    fun emptyDaysFail() {
        val error = expectRejected {
            PrayerDataValidator.validate(response(emptyList()), "WLY01")
        }
        assertTrue(error.message.orEmpty().contains("no prayer times"))
    }

    @Test
    fun fallbackMustCoverToday() {
        val response = response(
            listOf(day("15-Ogos-2026"), day("16-Ogos-2026"))
        )
        assertTrue(PrayerDataValidator.isUsableForToday(response, LocalDate.of(2026, 8, 15)))
        assertTrue(PrayerDataValidator.isUsableForToday(response, LocalDate.of(2026, 8, 16)))
        assertFalse(PrayerDataValidator.isUsableForToday(response, LocalDate.of(2026, 8, 14)))
        assertFalse(PrayerDataValidator.isUsableForToday(response, LocalDate.of(2026, 8, 17)))
    }

    private fun expectRejected(block: () -> Unit): IllegalArgumentException {
        return try {
            block()
            throw AssertionError("Expected PrayerDataValidator to reject malformed data")
        } catch (error: IllegalArgumentException) {
            error
        }
    }

    private fun response(days: List<PrayerDay>) = PrayerResponse(
        days = days,
        status = "OK!",
        serverTime = "2026-08-15 01:00:00",
        zone = "WLY01",
        bearing = "292°"
    )

    private fun day(date: String) = PrayerDay(
        hijri = "01-01-1448",
        dateRaw = date,
        dayRaw = "Sabtu",
        imsak = "05:50:00",
        subuh = "06:00:00",
        syuruk = "07:10:00",
        dhuha = "07:35:00",
        zohor = "13:15:00",
        asar = "16:35:00",
        maghrib = "19:25:00",
        isyak = "20:35:00"
    )
}
