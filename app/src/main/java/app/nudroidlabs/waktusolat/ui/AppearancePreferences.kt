package app.nudroidlabs.waktusolat.ui

import android.content.Context
import androidx.core.content.edit

enum class AppearanceMode(val storageValue: String, val label: String) {
    SYSTEM("system", "Ikut sistem"),
    LIGHT("light", "Cerah"),
    DARK("dark", "Gelap");

    companion object {
        fun fromStorage(value: String?): AppearanceMode = entries.firstOrNull {
            it.storageValue == value
        } ?: SYSTEM
    }
}

object AppearancePreferences {
    private const val PREFS = "waktu_solat_appearance"
    private const val KEY_MODE = "appearance_mode"

    fun mode(context: Context): AppearanceMode = AppearanceMode.fromStorage(
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_MODE, AppearanceMode.SYSTEM.storageValue)
    )

    fun setMode(context: Context, mode: AppearanceMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString(KEY_MODE, mode.storageValue)
        }
    }
}
