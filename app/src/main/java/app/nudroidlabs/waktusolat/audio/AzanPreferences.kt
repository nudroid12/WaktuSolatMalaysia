package app.nudroidlabs.waktusolat.audio

import android.content.Context
import androidx.core.content.edit

object AzanPreferences {
    private val prayerNames = setOf("Subuh", "Zohor", "Asar", "Maghrib", "Isyak")

    fun enabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_ENABLED, enabled) }
    }

    fun audioUri(context: Context): String? =
        prefs(context).getString(KEY_AUDIO_URI, null)?.takeIf(String::isNotBlank)

    fun setAudioUri(context: Context, uri: String) {
        require(uri.isNotBlank())
        prefs(context).edit { putString(KEY_AUDIO_URI, uri) }
    }

    fun clearAudioUri(context: Context) {
        prefs(context).edit {
            remove(KEY_AUDIO_URI)
            putBoolean(KEY_ENABLED, false)
        }
    }

    fun prayerEnabled(context: Context, prayerName: String): Boolean {
        requirePrayer(prayerName)
        return prefs(context).getBoolean("$KEY_PRAYER_PREFIX$prayerName", true)
    }

    fun setPrayerEnabled(context: Context, prayerName: String, enabled: Boolean) {
        requirePrayer(prayerName)
        prefs(context).edit { putBoolean("$KEY_PRAYER_PREFIX$prayerName", enabled) }
    }

    fun enabledForPrayer(context: Context, prayerName: String): Boolean =
        enabled(context) &&
            prayerEnabled(context, prayerName) &&
            !audioUri(context).isNullOrBlank()

    private fun requirePrayer(prayerName: String) {
        require(prayerName in prayerNames) { "Unknown prayer name: $prayerName" }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private const val PREFS_NAME = "azan_settings"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_AUDIO_URI = "audio_uri"
    private const val KEY_PRAYER_PREFIX = "prayer_"
}
