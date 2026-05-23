package com.teleport.app

import android.app.UiModeManager
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.teleport.app.mobile.MobileRemoteScreen
import com.teleport.app.mobile.connection.ConnectionState
import com.teleport.app.mobile.connection.TvConnectionManager
import com.teleport.app.mobile.nsd.NsdHelper
import com.teleport.app.mobile.sensors.GyroSensorTracker
import com.teleport.app.protocol.Command
import com.teleport.app.tv.TvActivityContent
import com.teleport.app.tv.browser.TabManager
import androidx.lifecycle.lifecycleScope
import android.os.Build
import com.teleport.app.tv.server.LocalServerService
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException
import java.net.URI

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"

    // Mobile specific properties
    private lateinit var nsdHelper: NsdHelper
    private lateinit var connectionManager: TvConnectionManager
    private var gyroTracker: GyroSensorTracker? = null
    private val pendingSharedUrl = mutableStateOf<String?>(null)

    // TV specific properties
    private lateinit var tabManager: TabManager

    // QR scanner launcher for Mobile pairing
    private val qrCodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            parseAndConnect(result.contents)
        } else {
            Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isTvDevice = checkIsTvDevice()
        Log.d(TAG, "Device check: isTvDevice = $isTvDevice")

        if (isTvDevice) {
            // Android TV flow
            startService(Intent(this, LocalServerService::class.java))
        } else {
            // Android Mobile flow
            nsdHelper = NsdHelper(this)
            connectionManager = TvConnectionManager(lifecycleScope)
            gyroTracker = GyroSensorTracker(this) { dx, dy ->
                connectionManager.sendCommand(Command.MoveCursor(dx, dy))
            }
            handleShareIntent(intent)
            handleDeepLinkIntent(intent)
        }

        setContent {
            val coroutineScope = rememberCoroutineScope()

            MaterialTheme {
                Surface(color = Color(0xFF121212)) {
                    if (isTvDevice) {
                        tabManager = remember { TabManager(this@MainActivity, coroutineScope) }
                        val localIp = remember { getLocalIpAddress() }
                        TvActivityContent(tabManager, localIp)
                    } else {
                        val connState by connectionManager.connectionState.collectAsState()
                        val sharedUrl by pendingSharedUrl

                        // Automatically connect to 127.0.0.1:8080 if running on an emulator and disconnected
                        LaunchedEffect(Unit) {
                            if (isEmulator() && connectionManager.connectionState.value == ConnectionState.Disconnected) {
                                Log.i(TAG, "Emulator detected, auto-connecting to TV at 127.0.0.1:8080")
                                connectionManager.connect("127.0.0.1", 8080)
                            }
                        }

                        // Automatically open shared URL if connected
                        LaunchedEffect(connState, sharedUrl) {
                            val url = sharedUrl
                            if (connState == ConnectionState.Connected && url != null) {
                                connectionManager.sendCommand(Command.OpenUrl(url))
                                pendingSharedUrl.value = null
                                Toast.makeText(this@MainActivity, "Opened shared URL on TV", Toast.LENGTH_SHORT).show()
                            }
                        }

                        MobileRemoteScreen(
                            connectionManager = connectionManager,
                            nsdHelper = nsdHelper,
                            gyroTracker = gyroTracker!!,
                            scanQr = {
                                val options = ScanOptions().apply {
                                    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                    setPrompt("Scan the QR code shown on your TV")
                                    setCameraId(0)
                                    setBeepEnabled(false)
                                    setBarcodeImageEnabled(false)
                                }
                                qrCodeLauncher.launch(options)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (!checkIsTvDevice()) {
            handleShareIntent(intent)
            handleDeepLinkIntent(intent)
        }
    }

    private fun checkIsTvDevice(): Boolean {
        val uiModeManager = getSystemService(UI_MODE_SERVICE) as UiModeManager
        val isTelevisionMode = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        val hasLeanbackFeature = packageManager.hasSystemFeature(Intent.CATEGORY_LEANBACK_LAUNCHER) ||
                packageManager.hasSystemFeature("android.software.leanback")
        return isTelevisionMode || hasLeanbackFeature
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!text.isNullOrBlank()) {
                val url = extractUrl(text)
                if (url != null) {
                    pendingSharedUrl.value = url
                    Log.d(TAG, "Captured shared URL: $url")
                    if (::connectionManager.isInitialized && connectionManager.connectionState.value == ConnectionState.Connected) {
                        connectionManager.sendCommand(Command.OpenUrl(url))
                        pendingSharedUrl.value = null
                    }
                }
            }
        }
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val data = intent.data ?: return
            Log.d(TAG, "Deep link intent received: $data")
            if (data.path == "/remote" && data.port == 8080) {
                val ip = data.host
                if (!ip.isNullOrBlank()) {
                    Log.d(TAG, "Deep link connecting to TV at $ip:8080")
                    Toast.makeText(this, "Connecting to TV at $ip...", Toast.LENGTH_SHORT).show()
                    connectionManager.connect(ip, 8080)
                }
            }
        }
    }

    private fun extractUrl(text: String): String? {
        val words = text.split("\\s+".toRegex())
        for (word in words) {
            try {
                val uri = URI(word)
                if (uri.scheme == "http" || uri.scheme == "https") {
                    return word
                }
            } catch (e: Exception) {
                // Try next
            }
        }
        if (text.startsWith("http://") || text.startsWith("https://")) {
            return text
        }
        return null
    }

    private fun parseAndConnect(qrContent: String) {
        try {
            val uri = URI(qrContent)
            val ip = uri.host ?: throw Exception("Invalid host")
            val port = if (uri.port != -1) uri.port else 8080
            connectionManager.connect(ip, port)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing QR Code: $qrContent", e)
            Toast.makeText(this, "Invalid QR Code format", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getLocalIpAddress(): String {
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val enumIpAddr = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        val ip = inetAddress.hostAddress ?: ""
                        if (!ip.startsWith("127.")) {
                            return ip
                        }
                    }
                }
            }
        } catch (ex: SocketException) {
            Log.e(TAG, "SocketException getting IP", ex)
        }
        return "127.0.0.1"
    }

    override fun onDestroy() {
        super.onDestroy()
        if (checkIsTvDevice()) {
            stopService(Intent(this, LocalServerService::class.java))
        } else {
            if (::connectionManager.isInitialized) {
                connectionManager.disconnect()
            }
            gyroTracker?.stop()
        }
    }

    private fun isEmulator(): Boolean {
        val brand = Build.BRAND
        val device = Build.DEVICE
        return (brand.startsWith("generic") && device.startsWith("generic")) ||
                Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                (brand.startsWith("google") && device.startsWith("google") && Build.PRODUCT.startsWith("sdk_gphone"))
    }
}
