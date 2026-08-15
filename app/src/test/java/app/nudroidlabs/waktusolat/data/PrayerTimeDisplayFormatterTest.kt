package app.nudroidlabs.waktusolat.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class PrayerTimeDisplayFormatterTest {
    @Test
    fun twentyFourHourFormat() {
        assertEquals(
            "18:45",
            PrayerTimeDisplayFormatter.formatLocalTime(
                LocalTime.of(18, 45),
                TimeFormatMode.HOUR_24
            )
        )
    }

    @Test
    fun twelveHourFormat() {
        assertEquals(
            "6:45 PM",
            PrayerTimeDisplayFormatter.formatLocalTime(
                LocalTime.of(18, 45),
                TimeFormatMode.HOUR_12
            )
        )
    }

    @Test
    fun midnightTwelveHourFormat() {
        assertEquals(
            "12:05 AM",
            PrayerTimeDisplayFormatter.formatApiTime("00:05:00", TimeFormatMode.HOUR_12)
        )
    }

    @Test
    fun noonTwelveHourFormat() {
        assertEquals(
            "12:00 PM",
            PrayerTimeDisplayFormatter.formatApiTime("12:00:00", TimeFormatMode.HOUR_12)
        )
    }
}
