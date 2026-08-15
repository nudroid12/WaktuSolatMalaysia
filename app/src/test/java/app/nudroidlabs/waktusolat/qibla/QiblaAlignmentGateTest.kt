package app.nudroidlabs.waktusolat.qibla

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QiblaAlignmentGateTest {
    @Test
    fun notifiesOnlyOnceWhileAligned() {
        val gate = QiblaAlignmentGate()

        assertTrue(gate.shouldNotify(2.5))
        assertFalse(gate.shouldNotify(1.0))
        assertFalse(gate.shouldNotify(-2.0))
    }

    @Test
    fun smallSensorJitterDoesNotRetrigger() {
        val gate = QiblaAlignmentGate()

        assertTrue(gate.shouldNotify(2.0))
        assertFalse(gate.shouldNotify(4.0))
        assertFalse(gate.shouldNotify(2.0))
    }

    @Test
    fun rearmsAfterMovingClearlyAway() {
        val gate = QiblaAlignmentGate()

        assertTrue(gate.shouldNotify(1.0))
        assertFalse(gate.shouldNotify(8.0))
        assertTrue(gate.shouldNotify(-2.0))
    }

    @Test
    fun invalidReadingsNeverNotify() {
        val gate = QiblaAlignmentGate()

        assertFalse(gate.shouldNotify(null))
        assertFalse(gate.shouldNotify(Double.NaN))
        assertFalse(gate.shouldNotify(Double.POSITIVE_INFINITY))
    }
}
