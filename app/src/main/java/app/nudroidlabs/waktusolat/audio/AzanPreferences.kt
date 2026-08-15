package app.nudroidlabs.waktusolat.audio

import android.content.Context
import androidx.core.content.edit

enum class AzanAudioSource(val label: String) {
    BUILT_IN("Terbina dalam"),
    CUSTOM("Fail sendiri")
}

object AzanPreferences {
    private val prayerNames = setOf("Subuh", "Zohor", "Asar", "Maghrib", "Isyak")

    fun enabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_ENABLED, enabled) }
    }

    fun source(context: Context): AzanAudioSource {
        val stored = prefs(context).getString(KEY_SOURCE, null)
        AzanAudioSource.entries.firstOrNull { it.name == stored }?.let { return it }

        return if (!audioUri(context).isNullOrBlank()) {
            AzanAudioSource.CUSTOM
        } else {
            AzanAudioSource.BUILT_IN
        }
    }

    fun setSource(context: Context, source: AzanAudioSource) {
        prefs(context).edit { putString(KEY_SOURCE, source.name) }
    }

    fun audioUri(context: Context): String? =
        prefs(context).getString(KEY_AUDIO_URI, null)?.takeIf(String::isNotBlank)

    fun setAudioUri(context: Context, uri: String) {
        require(uri.isNotBlank())
        prefs(context).edit {
            putString(KEY_AUDIO_URI, uri)
            putString(KEY_SOURCE, AzanAudioSource.CUSTOM.name)
        }
    }

    fun clearAudioUri(context: Context) {
        prefs(context).edit {
            remove(KEY_AUDIO_URI)
            putString(KEY_SOURCE, AzanAudioSource.BUILT_IN.name)
        }
    }

    fun hasPlayableAudio(context: Context): Boolean =
        when (source(context)) {
            AzanAudioSource.BUILT_IN -> true
            AzanAudioSource.CUSTOM -> !audioUri(context).isNullOrBlank()
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
            hasPlayableAudio(context)

    private fun requirePrayer(prayerName: String) {
        require(prayerName in prayerNames) { "Unknown prayer name: $prayerName" }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private const val PREFS_NAME = "azan_settings"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_SOURCE = "audio_source"
    private const val KEY_AUDIO_URI = "audio_uri"
    private const val KEY_PRAYER_PREFIX = "prayer_"
}
