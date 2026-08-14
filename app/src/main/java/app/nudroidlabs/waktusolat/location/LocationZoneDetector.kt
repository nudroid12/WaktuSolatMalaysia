package app.nudroidlabs.waktusolat.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
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
    val accuracyMetres: Float
)

class LocationZoneDetector(private val context: Context) {
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    suspend fun detect(): Result<ZoneSuggestion> = runCatching {
        val location = withTimeout(20_000L) { obtainLocation() }
        val address = reverseGeocode(location)
            ?: error("Alamat pentadbiran tidak dapat dikenal pasti daripada lokasi ini.")

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
                "Zon JAKIM tidak dapat dipastikan dengan yakin daripada alamat ini. " +
                    "Sila pilih zon secara manual."
            )

        ZoneSuggestion(
            zone = zone,
            addressText = addressLabel(address),
            accuracyMetres = location.accuracy
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
            .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
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

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            suspendCancellableCoroutine { continuation ->
                val signal = CancellationSignal()
                locationManager.getCurrentLocation(
                    provider,
                    signal,
                    context.mainExecutor
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
        } else {
            suspendCancellableCoroutine { continuation ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        locationManager.removeUpdates(this)
                        if (continuation.isActive) continuation.resume(location)
                    }

                    @Deprecated("Deprecated in Android")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

                    override fun onProviderEnabled(provider: String) = Unit

                    override fun onProviderDisabled(provider: String) {
                        if (continuation.isActive) {
                            locationManager.removeUpdates(this)
                            continuation.resumeWithException(
                                IllegalStateException("Perkhidmatan lokasi telah dimatikan.")
                            )
                        }
                    }
                }

                locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                continuation.invokeOnCancellation {
                    runCatching { locationManager.removeUpdates(listener) }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun reverseGeocode(location: Location): Address? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null

        val geocoder = Geocoder(context, Locale("ms", "MY"))
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

    companion object {
        private const val RECENT_LOCATION_MAX_AGE_MS = 15 * 60 * 1000L
    }
}
