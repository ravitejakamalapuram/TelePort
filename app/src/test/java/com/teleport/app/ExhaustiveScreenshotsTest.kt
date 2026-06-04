package com.teleport.app

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.teleport.app.mobile.ControllerScreen
import com.teleport.app.mobile.PairingScreen as MobilePairingScreen
import com.teleport.app.mobile.OnboardingModal
import com.teleport.app.mobile.OnboardingContent
import com.teleport.app.mobile.DpadTab
import com.teleport.app.mobile.TabsManagerTab
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExhaustiveScreenshotsTest {

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

    // ========================================================================
    // 🚫 OPTION A: Visual Regression Testing (Raw Component Screenshots)
    // ========================================================================

    @Test
    @Config(qualifiers = "w1920dp-h1080dp-mdpi")
    fun captureTvPairingDashboard_Raw() {
        ScreenshotEngine.capture(
            name = "tv_pairing_dashboard.png",
            device = ScreenshotEngine.DeviceConfig.Tv,
            isDarkMode = true
        ) {
            TvPairingScreen(
                connectionUrl = "ws://192.168.1.100:8080/remote",
                localIp = "192.168.1.100"
            )
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun captureMobileConnectionScreen_Raw() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val coroutineScope = CoroutineScope(Dispatchers.Main)
        val connectionManager = TvConnectionManager(coroutineScope)
        val nsdHelper = NsdHelper(context)
        
        setDiscoveredTvs(nsdHelper, listOf(
            DiscoveredTv("Living Room TV", "192.168.1.102", 8080),
            DiscoveredTv("Bedroom TV", "192.168.1.105", 8080)
        ))

        ScreenshotEngine.capture(
            name = "mobile_pairing_screen.png",
            device = ScreenshotEngine.DeviceConfig.Phone,
            isDarkMode = true
        ) {
            MobilePairingScreen(
                connectionManager = connectionManager,
                nsdHelper = nsdHelper,
                scanQr = {},
                onShowHelp = {}
            )
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun captureMobileControllerTrackpad_Raw() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val coroutineScope = CoroutineScope(Dispatchers.Main)
        val connectionManager = TvConnectionManager(coroutineScope)
        val gyroTracker = GyroSensorTracker(context) { _, _ -> }
        val billingManager = com.teleport.app.billing.BillingManager(context, coroutineScope)
        
        setConnectionState(connectionManager, ConnectionState.Connected)
        
        ScreenshotEngine.capture(
            name = "mobile_controller_trackpad.png",
            device = ScreenshotEngine.DeviceConfig.Phone,
            isDarkMode = true
        ) {
            ControllerScreen(
                connectionManager = connectionManager,
                gyroTracker = gyroTracker,
                startMirroring = {},
                stopMirroring = {},
                billingManager = billingManager,
                onShowHelp = {}
            )
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun captureMobileControllerDpad_Raw() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val coroutineScope = CoroutineScope(Dispatchers.Main)
        val connectionManager = TvConnectionManager(coroutineScope)
        
        setConnectionState(connectionManager, ConnectionState.Connected)
        
        ScreenshotEngine.capture(
            name = "mobile_controller_dpad.png",
            device = ScreenshotEngine.DeviceConfig.Phone,
            isDarkMode = true
        ) {
            DpadTab(connectionManager)
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun captureMobileControllerTabs_Raw() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val coroutineScope = CoroutineScope(Dispatchers.Main)
        val connectionManager = TvConnectionManager(coroutineScope)
        
        setConnectionState(connectionManager, ConnectionState.Connected)
        
        val mockTvState = TvState(
            tabs = listOf(
                TabInfo(url = "https://www.youtube.com", title = "YouTube Media", isLoading = false),
                TabInfo(url = "https://en.wikipedia.org", title = "Wikipedia", isLoading = false)
            ),
            activeTabIndex = 0,
            detectedStreamUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        )
        setTvState(connectionManager, mockTvState)

        ScreenshotEngine.capture(
            name = "mobile_controller_tabs.png",
            device = ScreenshotEngine.DeviceConfig.Phone,
            isDarkMode = true
        ) {
            TabsManagerTab(connectionManager, mockTvState)
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun captureMobileOnboardingPairingStep_Raw() {
        ScreenshotEngine.capture(
            name = "mobile_onboarding_slide2.png",
            device = ScreenshotEngine.DeviceConfig.Phone,
            isDarkMode = true
        ) {
            OnboardingContent(currentSlide = 1, onCurrentSlideChange = {}, onDismiss = {})
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun captureMobileOnboardingChromeStep_Raw() {
        ScreenshotEngine.capture(
            name = "mobile_onboarding_slide6.png",
            device = ScreenshotEngine.DeviceConfig.Phone,
            isDarkMode = true
        ) {
            OnboardingContent(currentSlide = 5, onCurrentSlideChange = {}, onDismiss = {})
        }
    }

    // ========================================================================
    // 🔄 OPTION B: Automated App Store Assets (Google Play & Chrome Web Store)
    // ========================================================================

    @Test
    @Config(qualifiers = "w1920dp-h1080dp-mdpi")
    fun captureTvPlayStoreAsset() {
        ScreenshotEngine.capture(
            name = "store_asset_tv_pairing.png",
            device = ScreenshotEngine.DeviceConfig.Tv,
            isDarkMode = true,
            decoration = ScreenshotEngine.DecorationConfig(
                title = "Polished TV Pairing Dashboard",
                description = "Scan the QR code or type the local URL to instantly pair your phone remote."
            )
        ) {
            TvPairingScreen(
                connectionUrl = "ws://192.168.1.100:8080/remote",
                localIp = "192.168.1.100"
            )
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun captureMobilePlayStoreTrackpadAsset() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val coroutineScope = CoroutineScope(Dispatchers.Main)
        val connectionManager = TvConnectionManager(coroutineScope)
        val gyroTracker = GyroSensorTracker(context) { _, _ -> }
        val billingManager = com.teleport.app.billing.BillingManager(context, coroutineScope)
        
        setConnectionState(connectionManager, ConnectionState.Connected)

        ScreenshotEngine.capture(
            name = "store_asset_mobile_trackpad.png",
            device = ScreenshotEngine.DeviceConfig.Phone,
            isDarkMode = true,
            decoration = ScreenshotEngine.DecorationConfig(
                title = "Seamless Precision Trackpad",
                description = "Control your Android TV cursor using standard swipe inputs or gyroscopic sensors."
            )
        ) {
            ControllerScreen(
                connectionManager = connectionManager,
                gyroTracker = gyroTracker,
                startMirroring = {},
                stopMirroring = {},
                billingManager = billingManager,
                onShowHelp = {}
            )
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun captureMobilePlayStoreDpadAsset() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val coroutineScope = CoroutineScope(Dispatchers.Main)
        val connectionManager = TvConnectionManager(coroutineScope)
        
        setConnectionState(connectionManager, ConnectionState.Connected)

        ScreenshotEngine.capture(
            name = "store_asset_mobile_dpad.png",
            device = ScreenshotEngine.DeviceConfig.Phone,
            isDarkMode = true,
            decoration = ScreenshotEngine.DecorationConfig(
                title = "Hardware D-Pad Controls",
                description = "Navigate system menus, scroll lists, and input keyboard queries with absolute ease."
            )
        ) {
            DpadTab(connectionManager)
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun captureMobilePlayStoreTabsAsset() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val coroutineScope = CoroutineScope(Dispatchers.Main)
        val connectionManager = TvConnectionManager(coroutineScope)
        
        setConnectionState(connectionManager, ConnectionState.Connected)
        
        val mockTvState = TvState(
            tabs = listOf(
                TabInfo(url = "https://www.youtube.com", title = "YouTube Media Stream", isLoading = false),
                TabInfo(url = "https://en.wikipedia.org", title = "Wikipedia", isLoading = false)
            ),
            activeTabIndex = 0,
            detectedStreamUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        )
        setTvState(connectionManager, mockTvState)

        ScreenshotEngine.capture(
            name = "store_asset_mobile_tabs.png",
            device = ScreenshotEngine.DeviceConfig.Phone,
            isDarkMode = true,
            decoration = ScreenshotEngine.DecorationConfig(
                title = "Cast Media Direct to TV",
                description = "Launch search queries or cast detected video links to play in TV's video player."
            )
        ) {
            TabsManagerTab(connectionManager, mockTvState)
        }
    }

    // ========================================================================
    // 📱 OPTION D: Cross-Device, Theme & Locale Validation
    // ========================================================================

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun captureMobilePairingScreen_LocaleSpanish() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val coroutineScope = CoroutineScope(Dispatchers.Main)
        val connectionManager = TvConnectionManager(coroutineScope)
        val nsdHelper = NsdHelper(context)
        
        setDiscoveredTvs(nsdHelper, listOf(
            DiscoveredTv("Sala de estar TV", "192.168.1.102", 8080)
        ))

        ScreenshotEngine.capture(
            name = "validation_locale_spanish.png",
            device = ScreenshotEngine.DeviceConfig.Phone,
            isDarkMode = true,
            localeCode = "es"
        ) {
            MobilePairingScreen(
                connectionManager = connectionManager,
                nsdHelper = nsdHelper,
                scanQr = {},
                onShowHelp = {}
            )
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun captureMobilePairingScreen_LocaleArabic_RTL() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val coroutineScope = CoroutineScope(Dispatchers.Main)
        val connectionManager = TvConnectionManager(coroutineScope)
        val nsdHelper = NsdHelper(context)
        
        setDiscoveredTvs(nsdHelper, listOf(
            DiscoveredTv("تلفزيون غرفة المعيشة", "192.168.1.102", 8080)
        ))

        ScreenshotEngine.capture(
            name = "validation_locale_arabic_rtl.png",
            device = ScreenshotEngine.DeviceConfig.Phone,
            isDarkMode = true,
            localeCode = "ar"
        ) {
            MobilePairingScreen(
                connectionManager = connectionManager,
                nsdHelper = nsdHelper,
                scanQr = {},
                onShowHelp = {}
            )
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-xxhdpi")
    fun captureMobileOnboardingSlide2_LightTheme() {
        ScreenshotEngine.capture(
            name = "validation_theme_light_onboarding.png",
            device = ScreenshotEngine.DeviceConfig.Phone,
            isDarkMode = false
        ) {
            OnboardingContent(currentSlide = 1, onCurrentSlideChange = {}, onDismiss = {})
        }
    }

    @Test
    @Config(qualifiers = "w1024dp-h768dp-mdpi")
    fun captureTabletController_TabletLandscape() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val coroutineScope = CoroutineScope(Dispatchers.Main)
        val connectionManager = TvConnectionManager(coroutineScope)
        val gyroTracker = GyroSensorTracker(context) { _, _ -> }
        val billingManager = com.teleport.app.billing.BillingManager(context, coroutineScope)
        
        setConnectionState(connectionManager, ConnectionState.Connected)

        ScreenshotEngine.capture(
            name = "validation_tablet_landscape.png",
            device = ScreenshotEngine.DeviceConfig.Tablet,
            isDarkMode = true
        ) {
            ControllerScreen(
                connectionManager = connectionManager,
                gyroTracker = gyroTracker,
                startMirroring = {},
                stopMirroring = {},
                billingManager = billingManager,
                onShowHelp = {}
            )
        }
    }
}
