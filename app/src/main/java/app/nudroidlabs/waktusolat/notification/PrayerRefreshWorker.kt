package app.nudroidlabs.waktusolat.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.nudroidlabs.waktusolat.data.JakimPrayerRepository
import java.util.concurrent.TimeUnit

class PrayerRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!PrayerAlarmScheduler.notificationsEnabled(applicationContext)) {
            return Result.success()
        }

        val repository = JakimPrayerRepository(applicationContext)
        val zoneCode = repository.savedZone()

        return repository.loadWeek(zoneCode, forceRefresh = true).fold(
            onSuccess = { response ->
                PrayerAlarmScheduler.reschedule(
                    context = applicationContext,
                    days = response.days,
                    zoneCode = zoneCode
                )
                Result.success()
            },
            onFailure = {
                Result.retry()
            }
        )
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "prayer_schedule_refresh"

        fun ensureScheduled(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequest.Builder(
                PrayerRefreshWorker::class.java,
                24,
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}
