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
import com.teleport.app.mobile.connection.ConnectionState
import com.teleport.app.mobile.connection.TvConnectionManager
import com.teleport.app.mobile.nsd.DiscoveredTv
import com.teleport.app.mobile.nsd.NsdHelper
import com.teleport.app.mobile.sensors.GyroSensorTracker
import com.teleport.app.protocol.TabInfo
import com.teleport.app.protocol.TvState
import com.teleport.app.tv.PairingScreen as TvPairingScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
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

    private fun setConnectionState(connectionManager: TvConnectionManager, state: ConnectionState) {
        val field = TvConnectionManager::class.java.getDeclaredField("_connectionState")
        field.isAccessible = true
        val flow = field.get(connectionManager) as MutableStateFlow<ConnectionState>
        flow.value = state
    }

    private fun setTvState(connectionManager: TvConnectionManager, state: TvState?) {
        val field = TvConnectionManager::class.java.getDeclaredField("_tvState")
        field.isAccessible = true
        val flow = field.get(connectionManager) as MutableStateFlow<TvState?>
        flow.value = state
    }

    private fun setDiscoveredTvs(nsdHelper: NsdHelper, tvs: List<DiscoveredTv>) {
        val field = NsdHelper::class.java.getDeclaredField("_discoveredTvs")
        field.isAccessible = true
        val flow = field.get(nsdHelper) as MutableStateFlow<List<DiscoveredTv>>
        flow.value = tvs
    }

    @Test
    @Config(qualifiers = "w1920dp-h1080dp-mdpi")
    fun captureTvPairingScreen() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            MaterialTheme {
                Surface(color = Color(0xFF121212)) {
                    TvPairingScreen(
                        connectionUrl = "ws://192.168.1.100:8080/remote",
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
        
        // Mock two discovered TV nodes for a richer, more professional screen
        setDiscoveredTvs(nsdHelper, listOf(
            DiscoveredTv("Living Room Android TV", "192.168.1.102", 8080),
            DiscoveredTv("Master Bedroom TV", "192.168.1.105", 8080)
        ))
        
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
    fun captureMobileControllerTrackpad() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val coroutineScope = CoroutineScope(Dispatchers.Main)
        val connectionManager = TvConnectionManager(coroutineScope)
        val gyroTracker = GyroSensorTracker(context) { _, _ -> }
        
        setConnectionState(connectionManager, ConnectionState.Connected)
        
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
        captureActivity(activity, 1080, 2400, "mobile_controller_trackpad.png")
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun captureMobileControllerDpad() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val coroutineScope = CoroutineScope(Dispatchers.Main)
        val connectionManager = TvConnectionManager(coroutineScope)
        val gyroTracker = GyroSensorTracker(context) { _, _ -> }
        
        setConnectionState(connectionManager, ConnectionState.Connected)
        
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            MaterialTheme {
                Surface(color = Color(0xFF121212)) {
                    // Manually switch tab via selectedTabIndex field reflection is not needed 
                    // if we just load the ControllerScreen or invoke the sub-tab composable.
                    // However, we can use the top-level ControllerScreen and trigger tab selection via simulated UI click
                    // or just render the DpadTab directly inside the Surface!
                    // Let's render the DpadTab directly to guarantee D-Pad is displayed.
                    com.teleport.app.mobile.DpadTab(connectionManager)
                }
            }
        }
        captureActivity(activity, 1080, 2400, "mobile_controller_dpad.png")
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun captureMobileControllerTabs() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val coroutineScope = CoroutineScope(Dispatchers.Main)
        val connectionManager = TvConnectionManager(coroutineScope)
        
        setConnectionState(connectionManager, ConnectionState.Connected)
        
        // Mock a TV state with 3 active tabs and a detected media stream
        val mockTvState = TvState(
            tabs = listOf(
                TabInfo(url = "https://www.youtube.com", title = "YouTube", isLoading = false),
                TabInfo(url = "https://en.wikipedia.org", title = "Wikipedia", isLoading = false),
                TabInfo(url = "https://www.google.com", title = "Google Search", isLoading = false)
            ),
            activeTabIndex = 0,
            detectedStreamUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        )
        setTvState(connectionManager, mockTvState)
        
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            MaterialTheme {
                Surface(color = Color(0xFF121212)) {
                    com.teleport.app.mobile.TabsManagerTab(connectionManager, mockTvState)
                }
            }
        }
        captureActivity(activity, 1080, 2400, "mobile_controller_tabs.png")
    }
}
