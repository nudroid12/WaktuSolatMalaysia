package app.nudroidlabs.waktusolat.audio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import app.nudroidlabs.waktusolat.MainActivity
import app.nudroidlabs.waktusolat.R

class AzanPlaybackService : Service() {
    private var player: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private val handler = Handler(Looper.getMainLooper())
    private val stopSafety = Runnable { stopPlayback() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopPlayback()
            ACTION_PLAY -> {
                val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME)
                    ?.takeIf(String::isNotBlank)
                    ?: "Waktu solat"
                val preview = intent.getBooleanExtra(EXTRA_PREVIEW, false)
                val uri = AzanPreferences.audioUri(this)
                if (uri.isNullOrBlank()) {
                    stopSelf()
                } else {
                    startPlayback(prayerName, uri, preview)
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopPlayback(stopService = false)
        super.onDestroy()
    }

    private fun startPlayback(prayerName: String, uriText: String, preview: Boolean) {
        stopPlayback(stopService = false)
        createChannel()
        startForeground(NOTIFICATION_ID, playbackNotification(prayerName, preview))

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener { change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS ||
                    change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                    change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
                ) {
                    stopPlayback()
                }
            }
            .build()
        focusRequest = request

        if (audioManager.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            stopPlayback()
            return
        }

        val mediaPlayer = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(attributes)
                setDataSource(this@AzanPlaybackService, uriText.toUri())
                setOnPreparedListener {
                    it.start()
                    handler.removeCallbacks(stopSafety)
                    handler.postDelayed(stopSafety, MAX_PLAYBACK_MS)
                }
                setOnCompletionListener { stopPlayback() }
                setOnErrorListener { _, _, _ ->
                    stopPlayback()
                    true
                }
            }
        }.getOrElse {
            stopPlayback()
            return
        }
        player = mediaPlayer

        runCatching { mediaPlayer.prepareAsync() }
            .onFailure { stopPlayback() }
    }

    private fun stopPlayback(stopService: Boolean = true) {
        handler.removeCallbacks(stopSafety)

        player?.let { mediaPlayer ->
            runCatching {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
            }
            runCatching { mediaPlayer.reset() }
            runCatching { mediaPlayer.release() }
        }
        player = null

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        focusRequest?.let { request ->
            runCatching { audioManager.abandonAudioFocusRequest(request) }
        }
        focusRequest = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        if (stopService) stopSelf()
    }

    private fun playbackNotification(
        prayerName: String,
        preview: Boolean
    ): android.app.Notification {
        val openApp = PendingIntent.getActivity(
            this,
            3101,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = stopPendingIntent(this)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (preview) "Ujian azan" else "Azan $prayerName")
            .setContentText(if (preview) "Pratonton audio pilihan" else "Audio azan sedang dimainkan")
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "Henti", stopIntent)
            .build()
    }

    private fun createChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Main balik azan",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notifikasi semasa audio azan dimainkan"
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "azan_playback"
        private const val NOTIFICATION_ID = 3100
        private const val ACTION_PLAY = "app.nudroidlabs.waktusolat.AZAN_PLAY"
        private const val ACTION_STOP = "app.nudroidlabs.waktusolat.AZAN_STOP"
        private const val EXTRA_PRAYER_NAME = "prayer_name"
        private const val EXTRA_PREVIEW = "preview"
        private const val MAX_PLAYBACK_MS = 10 * 60 * 1000L

        fun start(context: Context, prayerName: String) {
            val intent = Intent(context, AzanPlaybackService::class.java)
                .setAction(ACTION_PLAY)
                .putExtra(EXTRA_PRAYER_NAME, prayerName)
                .putExtra(EXTRA_PREVIEW, false)
            ContextCompat.startForegroundService(context, intent)
        }

        fun preview(context: Context) {
            val intent = Intent(context, AzanPlaybackService::class.java)
                .setAction(ACTION_PLAY)
                .putExtra(EXTRA_PRAYER_NAME, "Ujian")
                .putExtra(EXTRA_PREVIEW, true)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            runCatching { context.startService(stopIntent(context)) }
        }

        fun stopPendingIntent(context: Context): PendingIntent = PendingIntent.getService(
            context,
            3102,
            stopIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        private fun stopIntent(context: Context): Intent =
            Intent(context, AzanPlaybackService::class.java).setAction(ACTION_STOP)
    }
}
