package app.nudroidlabs.waktusolat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import app.nudroidlabs.waktusolat.audio.AzanPlaybackService
import app.nudroidlabs.waktusolat.ui.WaktuSolatApp

class MainActivity : ComponentActivity() {
    private var resumeToken by mutableIntStateOf(0)
    private var hasResumedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { WaktuSolatApp(resumeToken = resumeToken) }
    }

    override fun onResume() {
        super.onResume()
        if (hasResumedOnce) {
            resumeToken++
        } else {
            hasResumedOnce = true
        }
    }

    override fun onStop() {
        AzanPlaybackService.stopPreview(applicationContext)
        super.onStop()
    }
}
