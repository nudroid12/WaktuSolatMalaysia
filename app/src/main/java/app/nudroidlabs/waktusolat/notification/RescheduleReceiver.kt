package app.nudroidlabs.waktusolat.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.nudroidlabs.waktusolat.data.JakimPrayerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in SUPPORTED_ACTIONS) return
        if (!PrayerAlarmScheduler.notificationsEnabled(context)) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                PrayerRefreshWorker.ensureScheduled(appContext)

                val repository = JakimPrayerRepository(appContext)
                val zoneCode = repository.savedZone()
                repository.loadWeek(zoneCode).onSuccess { response ->
                    PrayerAlarmScheduler.reschedule(
                        context = appContext,
                        days = response.days,
                        zoneCode = zoneCode
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
        )
    }
}
