# Waktu Solat & Kiblat release rules.
# Keep Android and WorkManager entry points that are instantiated outside
# normal direct Kotlin call sites.

-keep class app.nudroidlabs.waktusolat.audio.AzanPlaybackService {
    public <init>();
    *;
}

-keep class app.nudroidlabs.waktusolat.notification.PrayerAlarmReceiver {
    public <init>();
    *;
}

-keep class app.nudroidlabs.waktusolat.notification.RescheduleReceiver {
    public <init>();
    *;
}

-keep class app.nudroidlabs.waktusolat.notification.PrayerRefreshWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
