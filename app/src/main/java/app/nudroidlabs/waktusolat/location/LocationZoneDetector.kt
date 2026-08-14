package app.nudroidlabs.waktusolat.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.edit
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import app.nudroidlabs.waktusolat.data.PrayerZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ZoneSuggestion(
    val zone: PrayerZone,
    val addressText: String,
    val accuracyMetres: Float,
    val latitude: Double,
    val longitude: Double,
    val altitudeMetres: Double,
    val locationTimeMillis: Long
)

data class SavedLocation(
    val latitude: Double,
    val longitude: Double,
    val altitudeMetres: Double,
    val accuracyMetres: Float,
    val capturedAtMillis: Long,
    val addressText: String?
)

class LocationZoneDetector(private val context: Context) {
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    suspend fun detect(): Result<ZoneSuggestion> = runCatching {
        val location = withTimeout(20_000L) { obtainLocation() }
        saveLocation(location, null)

        val address = reverseGeocode(location)
            ?: error(
                "Koordinat lokasi berjaya dikesan, tetapi alamat pentadbiran tidak dapat " +
                    "dikenal pasti. Kiblat masih boleh menggunakan lokasi ini."
            )

        if (!address.countryCode.isNullOrBlank() &&
            !address.countryCode.equals("MY", ignoreCase = true)
        ) {
            error("Lokasi yang dikesan bukan di Malaysia.")
        }

        val parts = listOf(
            address.subLocality,
            address.locality,
            address.subAdminArea,
            address.featureName
        )

        val zone = JakimZoneResolver.resolve(address.adminArea, parts)
            ?: error(
                "Lokasi berjaya dikesan, tetapi zon JAKIM tidak dapat dipastikan dengan yakin. " +
                    "Sila pilih zon secara manual."
            )

        val label = addressLabel(address)
        saveLocation(location, label)

        ZoneSuggestion(
            zone = zone,
            addressText = label,
            accuracyMetres = location.accuracy,
            latitude = location.latitude,
            longitude = location.longitude,
            altitudeMetres = location.altitude,
            locationTimeMillis = location.time
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun obtainLocation(): Location {
        val enabledProviders = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        ).filter { provider ->
            runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)
        }

        if (enabledProviders.isEmpty()) {
            error("Perkhidmatan lokasi tidak aktif. Hidupkan Location/GPS dan cuba lagi.")
        }

        val recent = enabledProviders
            .mapNotNull { provider ->
                runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
            }
            .filter { location ->
                val ageMs = System.currentTimeMillis() - location.time
                ageMs in 0..RECENT_LOCATION_MAX_AGE_MS
            }
            .minWithOrNull(
                compareBy<Location> { it.accuracy }
                    .thenByDescending { it.time }
            )

        if (recent != null) return recent

        val provider = when {
            LocationManager.NETWORK_PROVIDER in enabledProviders -> LocationManager.NETWORK_PROVIDER
            else -> LocationManager.GPS_PROVIDER
        }

        return suspendCancellableCoroutine { continuation ->
            val signal = CancellationSignal()
            LocationManagerCompat.getCurrentLocation(
                locationManager,
                provider,
                signal,
                ContextCompat.getMainExecutor(context)
            ) { location ->
                if (!continuation.isActive) return@getCurrentLocation
                if (location != null) {
                    continuation.resume(location)
                } else {
                    continuation.resumeWithException(
                        IllegalStateException("Lokasi semasa tidak dapat diperoleh.")
                    )
                }
            }
            continuation.invokeOnCancellation { signal.cancel() }
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun reverseGeocode(location: Location): Address? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        val geocoder = Geocoder(context, Locale.forLanguageTag("ms-MY"))
        geocoder.getFromLocation(location.latitude, location.longitude, 1)
            ?.firstOrNull()
    }

    private fun addressLabel(address: Address): String {
        val parts = listOf(
            address.subLocality,
            address.locality,
            address.subAdminArea,
            address.adminArea
        ).mapNotNull { it?.takeIf(String::isNotBlank) }
            .distinct()

        return parts.joinToString(", ").ifBlank { "Lokasi semasa" }
    }

    private fun saveLocation(location: Location, addressText: String?) {
        prefs(context).edit {
            putString(KEY_LATITUDE, location.latitude.toString())
            putString(KEY_LONGITUDE, location.longitude.toString())
            putString(KEY_ALTITUDE, location.altitude.toString())
            putFloat(KEY_ACCURACY, location.accuracy)
            putLong(
                KEY_CAPTURED_AT,
                location.time.takeIf { it > 0L } ?: System.currentTimeMillis()
            )
            if (addressText != null) putString(KEY_ADDRESS, addressText)
        }
    }

    companion object {
        private const val RECENT_LOCATION_MAX_AGE_MS = 15 * 60 * 1000L
        private const val PREFS_NAME = "saved_location"
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"
        private const val KEY_ALTITUDE = "altitude"
        private const val KEY_ACCURACY = "accuracy"
        private const val KEY_CAPTURED_AT = "captured_at"
        private const val KEY_ADDRESS = "address"

        fun savedLocation(context: Context): SavedLocation? {
            val prefs = prefs(context)
            val latitude = prefs.getString(KEY_LATITUDE, null)?.toDoubleOrNull() ?: return null
            val longitude = prefs.getString(KEY_LONGITUDE, null)?.toDoubleOrNull() ?: return null
            val altitude = prefs.getString(KEY_ALTITUDE, null)?.toDoubleOrNull() ?: 0.0

            if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null

            return SavedLocation(
                latitude = latitude,
                longitude = longitude,
                altitudeMetres = altitude,
                accuracyMetres = prefs.getFloat(KEY_ACCURACY, 0f),
                capturedAtMillis = prefs.getLong(KEY_CAPTURED_AT, 0L),
                addressText = prefs.getString(KEY_ADDRESS, null)
            )
        }

        private fun prefs(context: Context) =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
