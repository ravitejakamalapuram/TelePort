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
import com.teleport.app.ui.theme.ThemeTokens
import com.teleport.app.mobile.connection.ConnectionState
import com.teleport.app.mobile.connection.TvConnectionManager
import com.teleport.app.mobile.connection.QrScanActivity
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
import android.media.projection.MediaProjectionManager
import android.content.Context
import kotlinx.coroutines.launch
import com.teleport.app.ads.AdManager
import com.teleport.app.ads.ConsentHelper
import com.teleport.app.billing.BillingManager
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"

    // Google Play In-App Updates
    private lateinit var appUpdateManager: AppUpdateManager
    private val installListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            popupInstallUpdate()
        }
    }
    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            Log.e(TAG, "Update flow failed! Result code: ${result.resultCode}")
        }
    }

    // Mobile specific properties
    private lateinit var nsdHelper: NsdHelper
    private lateinit var connectionManager: TvConnectionManager
    private var gyroTracker: GyroSensorTracker? = null
    private val pendingSharedUrl = mutableStateOf<String?>(null)

    private lateinit var billingManager: BillingManager

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

    // Launcher for Screen Mirroring MediaProjection permission
    private val projectionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val tvIp = connectionManager.activeIp ?: ""
            if (tvIp.isNotBlank()) {
                val intent = Intent(this, com.teleport.app.mobile.mirror.ScreenCastService::class.java).apply {
                    putExtra("RESULT_CODE", result.resultCode)
                    putExtra("RESULT_DATA", result.data)
                    putExtra("TV_IP", tvIp)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                // Notify TV to switch to Mirror player screen
                connectionManager.sendCommand(Command.StartMirroring)
            } else {
                Toast.makeText(this, "Not connected to any TV", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Google Play Update Manager
        appUpdateManager = AppUpdateManagerFactory.create(this)
        appUpdateManager.registerListener(installListener)
        checkPlayStoreUpdates()

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
            checkAndRequestNotificationPermission()

            // Initialize billing for premium subscriptions
            billingManager = BillingManager(this, lifecycleScope)
            billingManager.connect()

            // Initialize ad consent and preload ads
            if (!isEmulator()) {
                ConsentHelper.requestConsent(this, onConsentResult = { canRequestAds ->
                    if (canRequestAds) {
                        AdManager.initialize(this)
                        AdManager.preloadInterstitial(this)
                        AdManager.preloadRewarded(this)
                    }
                })
            }
        }

        setContent {
            val coroutineScope = rememberCoroutineScope()

            MaterialTheme {
                Surface(color = ThemeTokens.Background) {
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
                                Log.i(TAG, "Emulator detected, auto-connecting to TV at 127.0.0.1:${ThemeTokens.PORT}")
                                connectionManager.connect("127.0.0.1", ThemeTokens.PORT)
                            }
                        }

                        // Monetization: Show interstitial ad on successful connection
                        LaunchedEffect(connState) {
                            if (connState == ConnectionState.Connected) {
                                AdManager.showInterstitial(this@MainActivity)
                            }
                        }

                        // Automatically open shared URL if connected
                        LaunchedEffect(connState, sharedUrl) {
                            val url = sharedUrl
                            if (connState == ConnectionState.Connected && url != null) {
                                connectionManager.sendCommand(Command.OpenUrl(url, headless = true))
                                pendingSharedUrl.value = null
                                Toast.makeText(this@MainActivity, "Casting shared URL to TV...", Toast.LENGTH_SHORT).show()
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
                                    setOrientationLocked(false)
                                    setCaptureActivity(QrScanActivity::class.java)
                                }
                                qrCodeLauncher.launch(options)
                            },
                            startMirroring = {
                                val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                                projectionLauncher.launch(mpManager.createScreenCaptureIntent())
                            },
                            stopMirroring = {
                                val intent = Intent(this@MainActivity, com.teleport.app.mobile.mirror.ScreenCastService::class.java).apply {
                                    action = "STOP"
                                }
                                startService(intent)
                                connectionManager.sendCommand(Command.StopMirroring)
                            },
                            billingManager = billingManager
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
                        connectionManager.sendCommand(Command.OpenUrl(url, headless = true))
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
            if (data.path == "/remote" && data.port == ThemeTokens.PORT) {
                val ip = data.host
                if (!ip.isNullOrBlank()) {
                    Log.d(TAG, "Deep link connecting to TV at $ip:${ThemeTokens.PORT}")
                    Toast.makeText(this, "Connecting to TV at $ip...", Toast.LENGTH_SHORT).show()
                    connectionManager.connect(ip, ThemeTokens.PORT)
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
            val port = if (uri.port != -1) uri.port else ThemeTokens.PORT
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
        if (::appUpdateManager.isInitialized) {
            appUpdateManager.unregisterListener(installListener)
        }
        if (checkIsTvDevice()) {
            stopService(Intent(this, LocalServerService::class.java))
        } else {
            if (::connectionManager.isInitialized) {
                connectionManager.disconnect()
            }
            if (::billingManager.isInitialized) {
                billingManager.disconnect()
            }
            gyroTracker?.stop()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::appUpdateManager.isInitialized) {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            updateLauncher,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error resuming update flow", e)
                    }
                }
            }
        }
    }

    private fun checkPlayStoreUpdates() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                // If update priority is >= 4, trigger IMMEDIATE update, otherwise FLEXIBLE
                val updateType = if (appUpdateInfo.updatePriority() >= 4) {
                    AppUpdateType.IMMEDIATE
                } else {
                    AppUpdateType.FLEXIBLE
                }
                
                if (appUpdateInfo.isUpdateTypeAllowed(updateType)) {
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            updateLauncher,
                            AppUpdateOptions.newBuilder(updateType).build()
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error starting update flow", e)
                    }
                }
            }
        }
    }

    private fun popupInstallUpdate() {
        Toast.makeText(this, "An update has just been downloaded. Restarting app to complete installation...", Toast.LENGTH_LONG).show()
        lifecycleScope.launch {
            kotlinx.coroutines.delay(3000)
            appUpdateManager.completeUpdate()
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permission), 101)
            }
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
