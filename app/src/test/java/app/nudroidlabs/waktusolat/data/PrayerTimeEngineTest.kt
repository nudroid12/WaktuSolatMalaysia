package app.nudroidlabs.waktusolat.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class PrayerTimeEngineTest {
    @Test
    fun parsesMalayJakimDate() {
        assertEquals(LocalDate.of(2026, 8, 12), PrayerTimeEngine.apiDate("12-Ogos-2026"))
    }

    @Test
    fun findsNextPrayer() {
        val day = PrayerDay(
            hijri = "1448-02-28",
            dateRaw = "12-Ogos-2026",
            dayRaw = "Wednesday",
            imsak = "05:51:00",
            subuh = "06:01:00",
            syuruk = "07:11:00",
            dhuha = "07:36:00",
            zohor = "13:21:00",
            asar = "16:39:00",
            maghrib = "19:28:00",
            isyak = "20:39:00"
        )
        val next = PrayerTimeEngine.findUpcoming(listOf(day), LocalDateTime.of(2026, 8, 12, 13, 30))
        assertNotNull(next)
        assertEquals("Asar", next?.name)
        assertEquals("16:39", next?.time)
    }

    @Test
    fun hasExactlySixtyUniqueZones() {
        assertEquals(60, JakimZones.all.size)
        assertEquals(60, JakimZones.all.map { it.code }.distinct().size)
    }
}
