package app.nudroidlabs.waktusolat.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

class JakimPrayerRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("waktu_solat_cache", Context.MODE_PRIVATE)

    suspend fun loadWeek(zoneCode: String, forceRefresh: Boolean = false): Result<PrayerResponse> = withContext(Dispatchers.IO) {
        val cacheKey = "week_$zoneCode"
        if (!forceRefresh) {
            prefs.getString(cacheKey, null)?.let { cached ->
                runCatching { parse(cached) }.getOrNull()?.let { parsed ->
                    if (isCacheFresh(zoneCode)) return@withContext Result.success(parsed)
                }
            }
        }

        runCatching {
            val endpoint = "https://www.e-solat.gov.my/index.php?r=esolatApi/TakwimSolat&period=week&zone=$zoneCode"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "WaktuSolatMalaysia/0.4.0 (NudroidLabs)")
                useCaches = false
            }

            try {
                val code = connection.responseCode
                if (code !in 200..299) error("JAKIM HTTP $code")
                val body = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val parsed = parse(body)
                check(parsed.status == "OK!") { "JAKIM status: ${parsed.status}" }
                check(parsed.zone.equals(zoneCode, ignoreCase = true)) { "JAKIM zone mismatch" }
                check(parsed.days.isNotEmpty()) { "JAKIM returned no prayer times" }
                prefs.edit {
                    putString(cacheKey, body)
                    putLong("${cacheKey}_saved", System.currentTimeMillis())
                }
                parsed
            } finally {
                connection.disconnect()
            }
        }.recoverCatching { networkError ->
            val cached = prefs.getString(cacheKey, null) ?: throw networkError
            parse(cached)
        }
    }

    fun savedZone(): String = prefs.getString("selected_zone", "WLY01") ?: "WLY01"

    fun saveZone(zoneCode: String) {
        prefs.edit { putString("selected_zone", zoneCode) }
    }

    private fun isCacheFresh(zoneCode: String): Boolean {
        val savedAt = prefs.getLong("week_${zoneCode}_saved", 0L)
        val age = System.currentTimeMillis() - savedAt
        return savedAt > 0 && age in 0 until CACHE_MAX_AGE_MS
    }

    internal fun parse(raw: String): PrayerResponse {
        val root = JSONObject(raw)
        val array = root.optJSONArray("prayerTime") ?: error("Missing prayerTime")
        val days = buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(
                    PrayerDay(
                        hijri = item.requireText("hijri"),
                        dateRaw = item.requireText("date"),
                        dayRaw = item.requireText("day"),
                        imsak = item.requireTime("imsak"),
                        subuh = item.requireTime("fajr"),
                        syuruk = item.requireTime("syuruk"),
                        dhuha = item.requireTime("dhuha"),
                        zohor = item.requireTime("dhuhr"),
                        asar = item.requireTime("asr"),
                        maghrib = item.requireTime("maghrib"),
                        isyak = item.requireTime("isha")
                    )
                )
            }
        }
        return PrayerResponse(
            days = days,
            status = root.optString("status"),
            serverTime = root.optString("serverTime"),
            zone = root.optString("zone"),
            bearing = decodeBearing(root.optString("bearing"))
        )
    }

    private fun JSONObject.requireText(name: String): String =
        optString(name).takeIf { it.isNotBlank() } ?: error("Missing $name")

    private fun JSONObject.requireTime(name: String): String {
        val value = requireText(name)
        require(TIME_REGEX.matches(value)) { "Invalid $name time: $value" }
        return value
    }

    private fun decodeBearing(value: String): String = value
        .replace("&#176;", "°")
        .replace("&#8242;", "′")
        .replace("&#8243;", "″")
        .replace("&deg;", "°")

    companion object {
        private const val CACHE_MAX_AGE_MS = 12 * 60 * 60 * 1000L
        private val TIME_REGEX = Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d$")
    }
}
