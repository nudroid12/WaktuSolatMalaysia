package app.nudroidlabs.waktusolat.qibla

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import app.nudroidlabs.waktusolat.location.SavedLocation
import kotlin.math.abs

class QiblaSensor(
    context: Context,
    private val location: SavedLocation,
    private val onHeadingChanged: (Float) -> Unit
) : SensorEventListener {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val rotationMatrix = FloatArray(9)
    private val adjustedMatrix = FloatArray(9)
    private val orientation = FloatArray(3)
    private var lastHeading: Float? = null

    val available: Boolean get() = rotationSensor != null

    fun start(): Boolean {
        val sensor = rotationSensor ?: return false
        return sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        remapForDisplay(rotationMatrix, adjustedMatrix)
        SensorManager.getOrientation(adjustedMatrix, orientation)

        val magneticHeading = Math.toDegrees(orientation[0].toDouble()).toFloat()
        val declination = GeomagneticField(
            location.latitude.toFloat(),
            location.longitude.toFloat(),
            location.altitudeMetres.toFloat(),
            System.currentTimeMillis()
        ).declination

        val trueHeading = QiblaCalculator.normalise((magneticHeading + declination).toDouble()).toFloat()
        val smoothed = smoothHeading(lastHeading, trueHeading)
        lastHeading = smoothed
        onHeadingChanged(smoothed)
    }

    @Suppress("DEPRECATION")
    private fun currentRotation(): Int {
        val manager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return manager.defaultDisplay.rotation
    }

    private fun remapForDisplay(source: FloatArray, destination: FloatArray) {
        val (axisX, axisY) = when (currentRotation()) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }
        SensorManager.remapCoordinateSystem(source, axisX, axisY, destination)
    }

    private fun smoothHeading(previous: Float?, current: Float): Float {
        if (previous == null) return current
        var delta = current - previous
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f

        if (abs(delta) < 0.15f) return previous
        return QiblaCalculator.normalise((previous + delta * SMOOTHING).toDouble()).toFloat()
    }

    companion object {
        private const val SMOOTHING = 0.22f
    }
}
