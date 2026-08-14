package app.nudroidlabs.waktusolat.qibla

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

object QiblaCalculator {
    private const val KAABA_LATITUDE = 21.422487
    private const val KAABA_LONGITUDE = 39.826206

    fun bearingDegrees(latitude: Double, longitude: Double): Double {
        require(latitude in -90.0..90.0) { "Invalid latitude" }
        require(longitude in -180.0..180.0) { "Invalid longitude" }

        val phi = Math.toRadians(latitude)
        val kaabaPhi = Math.toRadians(KAABA_LATITUDE)
        val deltaLambda = Math.toRadians(KAABA_LONGITUDE - longitude)

        val y = sin(deltaLambda)
        val x = cos(phi) * tan(kaabaPhi) - sin(phi) * cos(deltaLambda)
        return normalise(Math.toDegrees(atan2(y, x)))
    }

    fun relativeDegrees(qiblaBearing: Double, trueHeading: Double): Double =
        signedNormalise(qiblaBearing - trueHeading)

    fun parseJakimBearing(raw: String): Double? {
        if (raw.isBlank()) return null
        val values = Regex("(-?\\d+(?:\\.\\d+)?)")
            .findAll(raw)
            .mapNotNull { it.value.toDoubleOrNull() }
            .toList()

        if (values.isEmpty()) return null
        val degrees = values.getOrElse(0) { 0.0 }
        val minutes = values.getOrElse(1) { 0.0 }
        val seconds = values.getOrElse(2) { 0.0 }
        if (degrees !in -360.0..360.0 || minutes !in 0.0..59.999 || seconds !in 0.0..59.999) {
            return null
        }
        val sign = if (degrees < 0) -1 else 1
        return normalise(degrees + sign * (minutes / 60.0 + seconds / 3600.0))
    }

    fun normalise(value: Double): Double = ((value % 360.0) + 360.0) % 360.0

    private fun signedNormalise(value: Double): Double {
        val normal = normalise(value)
        return if (normal > 180.0) normal - 360.0 else normal
    }
}
