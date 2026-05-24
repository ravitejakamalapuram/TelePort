package com.teleport.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.teleport.app.mobile.ControllerScreen
import com.teleport.app.mobile.PairingScreen as MobilePairingScreen
import com.teleport.app.mobile.connection.TvConnectionManager
import com.teleport.app.mobile.nsd.NsdHelper
import com.teleport.app.mobile.sensors.GyroSensorTracker
import com.teleport.app.tv.PairingScreen as TvPairingScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ScreenshotCapturerTest {

    private fun captureActivity(activity: ComponentActivity, width: Int, height: Int, filename: String) {
        val contentView = activity.findViewById<ViewGroup>(android.R.id.content)
        
        // Force layout pass
        contentView.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        )
        contentView.layout(0, 0, width, height)
        
        // Idle looper for Compose layouts
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        contentView.draw(canvas)
        
        // Scale down to prevent git repository bloat
        val scale = if (width > height) 0.5f else 0.333f // scale TV (1920x1080 -> 960x540), mobile (1080x2400 -> 360x800)
        val targetWidth = (width * scale).toInt()
        val targetHeight = (height * scale).toInt()
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        
        // Save to docs/screenshots in project root
        // Since test CWD is under 'app' folder, project root is parent
        val screenshotsDir = File("../docs/screenshots")
        screenshotsDir.mkdirs()
        
        val file = File(screenshotsDir, filename)
        FileOutputStream(file).use { out ->
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
        }
        println("Generated scaled asset screenshot: ${file.absolutePath}")
    }

    @Test
    @Config(qualifiers = "w1920dp-h1080dp-xhdpi")
    fun captureTvPairingScreen() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            MaterialTheme {
                Surface(color = Color(0xFF121212)) {
                    TvPairingScreen(
                        connectionUrl = "ws://192.168.1.100:8080/control",
                        localIp = "192.168.1.100"
                    )
                }
            }
        }
        captureActivity(activity, 1920, 1080, "tv_pairing_screen.png")
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun captureMobilePairingScreen() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val coroutineScope = CoroutineScope(Dispatchers.Main)
        val connectionManager = TvConnectionManager(coroutineScope)
        val nsdHelper = NsdHelper(context)
        
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            MaterialTheme {
                Surface(color = Color(0xFF121212)) {
                    MobilePairingScreen(
                        connectionManager = connectionManager,
                        nsdHelper = nsdHelper,
                        scanQr = {}
                    )
                }
            }
        }
        captureActivity(activity, 1080, 2400, "mobile_pairing_screen.png")
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun captureMobileControllerScreen() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val coroutineScope = CoroutineScope(Dispatchers.Main)
        val connectionManager = TvConnectionManager(coroutineScope)
        val gyroTracker = GyroSensorTracker(context) { _, _ -> }
        
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            MaterialTheme {
                Surface(color = Color(0xFF121212)) {
                    ControllerScreen(
                        connectionManager = connectionManager,
                        gyroTracker = gyroTracker,
                        startMirroring = {},
                        stopMirroring = {}
                    )
                }
            }
        }
        captureActivity(activity, 1080, 2400, "mobile_controller_screen.png")
    }
}
