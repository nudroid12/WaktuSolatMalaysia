package app.nudroidlabs.waktusolat.qibla

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QiblaCalculatorTest {
    @Test
    fun kualaLumpurBearingIsInExpectedRange() {
        val bearing = QiblaCalculator.bearingDegrees(3.1390, 101.6869)
        assertTrue(bearing in 292.0..293.1)
    }

    @Test
    fun relativeDirectionUsesShortestTurn() {
        assertEquals(20.0, QiblaCalculator.relativeDegrees(10.0, 350.0), 0.001)
        assertEquals(-20.0, QiblaCalculator.relativeDegrees(350.0, 10.0), 0.001)
    }

    @Test
    fun parsesJakimDegreeMinuteSecondText() {
        val bearing = QiblaCalculator.parseJakimBearing("292° 31′ 16″")
        assertTrue(bearing != null)
        assertEquals(292.521, bearing ?: 0.0, 0.01)
    }
}
