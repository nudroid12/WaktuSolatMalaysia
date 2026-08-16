package app.nudroidlabs.waktusolat.update

import android.content.Context
import androidx.core.content.edit

object UpdatePreferences {
    private const val PREFS = "self_update"
    private const val KEY_LAST_AUTO_CHECK = "last_auto_check"
    private const val AUTO_CHECK_INTERVAL_MS = 24L * 60L * 60L * 1000L

    fun shouldAutoCheck(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        val last = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_AUTO_CHECK, 0L)
        return last <= 0L || now - last >= AUTO_CHECK_INTERVAL_MS
    }

    fun markAutoCheckAttempt(context: Context, now: Long = System.currentTimeMillis()) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putLong(KEY_LAST_AUTO_CHECK, now)
        }
    }
}
