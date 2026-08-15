package app.nudroidlabs.waktusolat.qibla

import kotlin.math.abs

class QiblaAlignmentGate(
    private val alignedThresholdDegrees: Double = 3.0,
    private val rearmThresholdDegrees: Double = 7.0
) {
    private var armed = true

    init {
        require(alignedThresholdDegrees >= 0.0)
        require(rearmThresholdDegrees > alignedThresholdDegrees)
    }

    fun shouldNotify(relativeDegrees: Double?): Boolean {
        if (relativeDegrees == null || !relativeDegrees.isFinite()) return false

        val offset = abs(relativeDegrees)

        if (!armed && offset >= rearmThresholdDegrees) {
            armed = true
        }

        if (armed && offset <= alignedThresholdDegrees) {
            armed = false
            return true
        }

        return false
    }
}
