package com.teleport.app.mobile

import android.widget.Toast
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.teleport.app.mobile.connection.ConnectionState
import com.teleport.app.mobile.connection.TvConnectionManager
import com.teleport.app.mobile.nsd.NsdHelper
import com.teleport.app.mobile.sensors.GyroSensorTracker
import com.teleport.app.protocol.Command
import com.teleport.app.ui.theme.ThemeTokens
import com.teleport.app.ads.BannerAd
import com.teleport.app.ads.AdManager
import com.teleport.app.billing.BillingManager
import com.teleport.app.billing.PremiumState
import com.teleport.app.config.FeatureFlags
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalUriHandler

private val isProUnlocked: Boolean
    get() = PremiumState.isPremium || AdManager.isTemporaryProActive || !FeatureFlags.ENABLE_ADVERTISEMENTS

@Composable
fun MobileRemoteScreen(
    connectionManager: TvConnectionManager,
    nsdHelper: NsdHelper,
    gyroTracker: GyroSensorTracker,
    scanQr: () -> Unit,
    startMirroring: () -> Unit,
    stopMirroring: () -> Unit,
    billingManager: BillingManager
) {
    val connState by connectionManager.connectionState.collectAsState()
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("teleport_prefs", Context.MODE_PRIVATE) }
    var showOnboarding by remember {
        mutableStateOf(!sharedPrefs.getBoolean("has_seen_onboarding", false))
    }

    if (showOnboarding) {
        OnboardingModal(
            onDismiss = {
                sharedPrefs.edit().putBoolean("has_seen_onboarding", true).apply()
                showOnboarding = false
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ThemeTokens.Background
    ) {
        if (connState == ConnectionState.Connected) {
            ControllerScreen(
                connectionManager = connectionManager,
                gyroTracker = gyroTracker,
                startMirroring = startMirroring,
                stopMirroring = stopMirroring,
                billingManager = billingManager,
                onShowHelp = { showOnboarding = true }
            )
        } else {
            PairingScreen(
                connectionManager = connectionManager,
                nsdHelper = nsdHelper,
                scanQr = scanQr,
                onShowHelp = { showOnboarding = true }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingScreen(
    connectionManager: TvConnectionManager,
    nsdHelper: NsdHelper,
    scanQr: () -> Unit,
    onShowHelp: () -> Unit
) {
    val discoveredTvs by nsdHelper.discoveredTvs.collectAsState()
    val connState by connectionManager.connectionState.collectAsState()
    var manualIp by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    DisposableEffect(Unit) {
        nsdHelper.startDiscovery()
        onDispose {
            nsdHelper.stopDiscovery()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${ThemeTokens.APP_NAME} Remote",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = ThemeTokens.TextMain
            )
            IconButton(
                onClick = onShowHelp,
                modifier = Modifier
                    .size(44.dp)
                    .background(ThemeTokens.CardBg, CircleShape)
                    .clearAndSetSemantics { contentDescription = "Help" }
            ) {
                Text("❔", fontSize = 20.sp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Connect to your TV client to start controlling",
            fontSize = 14.sp,
            color = ThemeTokens.TextSub,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // QR Code Scanner Button
        Button(
            onClick = scanQr,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ThemeTokens.Primary)
        ) {
            Text("Scan TV QR Code", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("OR CONNECT VIA DISCOVERY", fontSize = 12.sp, color = ThemeTokens.TextSub, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Discovered TVs List
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = ThemeTokens.CardBg)
        ) {
            if (discoveredTvs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = ThemeTokens.Accent,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Searching for TVs on Wi-Fi...", color = ThemeTokens.TextSub, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(discoveredTvs) { _, tv ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button) { connectionManager.connect(tv.ipAddress, tv.port) }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(tv.name, color = ThemeTokens.TextMain, fontWeight = FontWeight.Bold)
                                Text("${tv.ipAddress}:${tv.port}", color = ThemeTokens.TextSub, fontSize = 12.sp)
                            }
                            Text("Connect", color = ThemeTokens.Accent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Manual IP Fallback
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = manualIp,
                onValueChange = { manualIp = it },
                label = { Text("Manual TV IP Address") },
                placeholder = { Text("192.168.1.X") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        if (manualIp.isNotBlank()) {
                            focusManager.clearFocus()
                            connectionManager.connect(manualIp.trim(), ThemeTokens.PORT)
                        }
                    }
                ),
                trailingIcon = {
                    if (manualIp.isNotEmpty()) {
                        IconButton(onClick = { manualIp = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear IP", tint = ThemeTokens.TextSub)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ThemeTokens.Accent,
                    focusedLabelColor = ThemeTokens.Accent,
                    unfocusedBorderColor = ThemeTokens.Border,
                    focusedTextColor = ThemeTokens.TextMain,
                    unfocusedTextColor = ThemeTokens.TextMain
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = {
                    if (manualIp.isNotBlank()) {
                        focusManager.clearFocus()
                        connectionManager.connect(manualIp.trim(), ThemeTokens.PORT)
                    }
                },
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = manualIp.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemeTokens.Accent,
                    contentColor = ThemeTokens.Background,
                    disabledContainerColor = ThemeTokens.Accent.copy(alpha = 0.5f),
                    disabledContentColor = ThemeTokens.Background.copy(alpha = 0.5f)
                )
            ) {
                Text("Go", fontWeight = FontWeight.Bold)
            }
        }

        if (connState is ConnectionState.Connecting) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = ThemeTokens.Accent,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connecting to TV...", color = ThemeTokens.Accent)
            }
        } else if (connState is ConnectionState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Error: ${(connState as ConnectionState.Error).message}", color = ThemeTokens.Error, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
        val uriHandler = LocalUriHandler.current
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button) { uriHandler.openUri("https://chromewebstore.google.com") }
                .padding(8.dp)
        ) {
            Text("🌐 Get Chrome Extension for Computer", color = ThemeTokens.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        // Monetization: Banner ad at bottom of pairing screen
        Spacer(modifier = Modifier.height(8.dp))
        BannerAd()
    }
}

@Composable
fun ControllerScreen(
    connectionManager: TvConnectionManager,
    gyroTracker: GyroSensorTracker,
    startMirroring: () -> Unit,
    stopMirroring: () -> Unit,
    billingManager: BillingManager,
    onShowHelp: () -> Unit
) {
    val tvState by connectionManager.tvState.collectAsState()
    val isResolvingHeadlessly = tvState?.isResolvingHeadlessly ?: false
    val isNativePlaying = tvState?.isNativePlaying ?: false
    var showPaywall by remember { mutableStateOf(false) }
    var showTipJar by remember { mutableStateOf(false) }
    var showAdUnlockPrompt by remember { mutableStateOf(false) }

    // Ad unlock dialog
    if (showAdUnlockPrompt) {
        val context = LocalContext.current
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAdUnlockPrompt = false },
            title = { Text("Unlock Pro Features", fontWeight = FontWeight.Bold) },
            text = { Text("To cast more than 3 tabs or use screen mirroring, you can temporarily unlock all Pro features for 1 hour by watching a quick video ad.") },
            confirmButton = {
                Button(
                    onClick = {
                        showAdUnlockPrompt = false
                        AdManager.showRewarded(context as android.app.Activity) {
                            AdManager.grantTemporaryAdFree()
                            startMirroring()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeTokens.Accent)
                ) {
                    Text("Watch Ad 🎬", color = ThemeTokens.Background, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdUnlockPrompt = false }) {
                    Text("Cancel", color = ThemeTokens.TextSub)
                }
            },
            containerColor = ThemeTokens.CardBg,
            titleContentColor = ThemeTokens.TextMain,
            textContentColor = ThemeTokens.TextSub
        )
    }

    // Paywall overlay
    if (showPaywall) {
        PaywallScreen(
            billingManager = billingManager,
            onDismiss = { showPaywall = false }
        )
        return
    }

    // Tip jar overlay
    if (showTipJar) {
        TipJarSheet(
            billingManager = billingManager,
            onDismiss = { showTipJar = false }
        )
        return
    }

    if (isResolvingHeadlessly) {
        MobileCastingScreen(connectionManager, tvState)
    } else if (isNativePlaying) {
        MobileMediaRemoteScreen(connectionManager, tvState)
    } else {
        var selectedTabIndex by remember { mutableIntStateOf(0) }
        val tabTitles = remember { listOf("Trackpad", "D-Pad", "Tabs") }

        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ThemeTokens.CardBg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${ThemeTokens.APP_NAME} Remote",
                            color = ThemeTokens.TextMain,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        var isDarkModeEnabled by remember { mutableStateOf(false) }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .toggleable(
                                    value = isDarkModeEnabled,
                                    role = Role.Switch,
                                    onValueChange = {
                                        isDarkModeEnabled = it
                                        connectionManager.sendCommand(Command.ToggleDarkMode(it))
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Dark Mode", color = ThemeTokens.TextSub, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Switch(
                                checked = isDarkModeEnabled,
                                onCheckedChange = null,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = ThemeTokens.Accent,
                                    checkedTrackColor = ThemeTokens.Accent.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.scale(0.7f)
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Help button
                        IconButton(
                            onClick = onShowHelp,
                            modifier = Modifier
                                .size(36.dp)
                                .clearAndSetSemantics { contentDescription = "Help" }
                        ) {
                            Text("❔", fontSize = 18.sp)
                        }
                        // Tip jar button
                        IconButton(
                            onClick = { showTipJar = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clearAndSetSemantics { contentDescription = "Tip Jar" }
                        ) {
                            Text("☕", fontSize = 18.sp)
                        }
                        if (!isProUnlocked && FeatureFlags.ENABLE_PAID_SUBSCRIPTIONS) {
                            Button(
                                onClick = { showPaywall = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ThemeTokens.Primary),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "⭐ Pro",
                                    color = ThemeTokens.TextMain,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                        Button(
                            onClick = { connectionManager.disconnect() },
                            colors = ButtonDefaults.buttonColors(containerColor = ThemeTokens.Error.copy(alpha = 0.8f)),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Disconnect",
                                color = ThemeTokens.TextMain,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // GLOWING NATIVE STREAM DETECTED BANNER
            tvState?.detectedStreamUrl?.let { streamUrl ->
                val context = LocalContext.current
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable(role = Role.Button) {
                            // Monetization: Show interstitial before launching native player (free tier)
                            if (!isProUnlocked) {
                                AdManager.showInterstitial(context as android.app.Activity) {
                                    connectionManager.sendCommand(Command.PlayStreamNatively(streamUrl))
                                }
                            } else {
                                connectionManager.sendCommand(Command.PlayStreamNatively(streamUrl))
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = ThemeTokens.Primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("📺 Media stream detected!", color = ThemeTokens.TextMain, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Tap to play cleanly in Native Player", color = ThemeTokens.TextSub, fontSize = 11.sp)
                        }
                        Text("PLAY", color = ThemeTokens.Accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            // Tab Selection
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = ThemeTokens.CardBg,
                contentColor = ThemeTokens.TextMain,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = ThemeTokens.Accent
                    )
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // Active Tab Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(ThemeTokens.Background)
            ) {
                when (selectedTabIndex) {
                    0 -> TrackpadTab(connectionManager, gyroTracker, startMirroring, stopMirroring, onShowPaywall = {
                        if (FeatureFlags.ENABLE_PAID_SUBSCRIPTIONS) {
                            showPaywall = true
                        } else {
                            showAdUnlockPrompt = true
                        }
                    })
                    1 -> DpadTab(connectionManager)
                    2 -> TabsManagerTab(connectionManager, tvState, onShowPaywall = {
                        if (FeatureFlags.ENABLE_PAID_SUBSCRIPTIONS) {
                            showPaywall = true
                        } else {
                            showAdUnlockPrompt = true
                        }
                    })
                }
            }

            // Keyboard/Input Bar (Sticky at bottom)
            QuickInputBar(connectionManager)

            // Monetization: "Remove Ads" rewarded ad option
            if (AdManager.shouldShowAds()) {
                val context = LocalContext.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) {
                            AdManager.showRewarded(context as android.app.Activity) {
                                AdManager.grantTemporaryAdFree()
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("🎬 Watch ad to remove ads for 1 hour", color = ThemeTokens.Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Monetization: Banner ad at bottom of controller screen
            BannerAd()
        }
    }
}

@Composable
fun TrackpadTab(
    connectionManager: TvConnectionManager,
    gyroTracker: GyroSensorTracker,
    startMirroring: () -> Unit,
    stopMirroring: () -> Unit,
    onShowPaywall: () -> Unit
) {
    var isAirMouseOn by remember { mutableStateOf(false) }
    val isCasting by com.teleport.app.mobile.mirror.ScreenCastService.isCasting.collectAsState()
    val context = LocalContext.current

    DisposableEffect(isAirMouseOn) {
        if (isAirMouseOn) {
            gyroTracker.start()
            connectionManager.sendCommand(Command.SetAirRemoteMode(true))
        } else {
            gyroTracker.stop()
            connectionManager.sendCommand(Command.SetAirRemoteMode(false))
        }
        onDispose {
            gyroTracker.stop()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Mirror Screen Switch Header — PRO ONLY
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ThemeTokens.CardBg, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .toggleable(
                    value = isCasting,
                    role = Role.Switch,
                    onValueChange = { checked ->
                        if (!isProUnlocked) {
                            onShowPaywall()
                        } else {
                            if (checked) startMirroring() else stopMirroring()
                        }
                    }
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Mirror Phone Screen", color = ThemeTokens.TextMain, fontWeight = FontWeight.Bold)
                    if (!isProUnlocked) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PRO", color = ThemeTokens.Background, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.background(ThemeTokens.Primary, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Text("Cast phone display to TV", color = ThemeTokens.TextSub, fontSize = 12.sp)
            }
            Switch(
                checked = isCasting,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ThemeTokens.Accent,
                    checkedTrackColor = ThemeTokens.Accent.copy(alpha = 0.5f)
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Air Mouse Switch Header — Rewarded Ad unlock or PRO
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ThemeTokens.CardBg, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .toggleable(
                    value = isAirMouseOn,
                    role = Role.Switch,
                    onValueChange = { wantsOn ->
                        if (wantsOn && !isProUnlocked) {
                            // Free users: watch rewarded ad to unlock for this session
                            AdManager.showRewarded(context as android.app.Activity) {
                                AdManager.grantTemporaryAdFree()
                                isAirMouseOn = true
                            }
                        } else {
                            isAirMouseOn = wantsOn
                        }
                    }
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Air Mouse Mode", color = ThemeTokens.TextMain, fontWeight = FontWeight.Bold)
                    if (!isProUnlocked) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🎬 AD", color = ThemeTokens.Background, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.background(ThemeTokens.Accent, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Text(
                    if (!isProUnlocked) "Watch ad to unlock for this session" else "Point phone to move TV cursor",
                    color = ThemeTokens.TextSub, fontSize = 12.sp
                )
            }
            Switch(
                checked = isAirMouseOn,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ThemeTokens.Accent,
                    checkedTrackColor = ThemeTokens.Accent.copy(alpha = 0.5f)
                )
            )
        }

        // Trackpad Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .background(ThemeTokens.CardBg, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .pointerInput(isAirMouseOn) {
                    detectTapGestures(
                        onTap = { connectionManager.sendCommand(Command.Click) }
                    )
                }
                .pointerInput(isAirMouseOn) {
                    if (!isAirMouseOn) {
                        awaitPointerEventScope {
                            var isScrolling = false
                            while (true) {
                                val event = awaitPointerEvent()
                                val activePointers = event.changes.filter { it.pressed }

                                if (activePointers.isEmpty()) {
                                    isScrolling = false
                                } else if (activePointers.size >= 2) {
                                    isScrolling = true
                                }

                                if (isScrolling) {
                                    if (activePointers.size >= 2) {
                                        val change1 = activePointers[0]
                                        val change2 = activePointers[1]

                                        val dy1 = change1.position.y - change1.previousPosition.y
                                        val dy2 = change2.position.y - change2.previousPosition.y
                                        val averageDy = (dy1 + dy2) / 2f

                                        val dx1 = change1.position.x - change1.previousPosition.x
                                        val dx2 = change2.position.x - change2.previousPosition.x
                                        val averageDx = (dx1 + dx2) / 2f

                                        if (averageDy != 0f || averageDx != 0f) {
                                            // Send scroll command (invert dy/dx for natural scrolling)
                                            connectionManager.sendCommand(
                                                Command.Scroll(-averageDx * 2f, -averageDy * 2f)
                                            )
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                } else {
                                    if (activePointers.size == 1) {
                                        val change = activePointers.first()
                                        val dragAmount = change.position - change.previousPosition
                                        if (dragAmount.x != 0f || dragAmount.y != 0f) {
                                            connectionManager.sendCommand(
                                                Command.MoveCursor(dragAmount.x, dragAmount.y)
                                            )
                                            change.consume()
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isAirMouseOn) {
                Text(
                    "TAP ANYWHERE TO CLICK\n(Wave phone to move cursor)",
                    color = ThemeTokens.Accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            } else {
                Text(
                    "TRACKPAD\nDrag to move. Tap to click.",
                    color = ThemeTokens.TextSub,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // Scroll Helper Buttons (For easier page scrolling)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { connectionManager.sendCommand(Command.Scroll(0f, -300f)) },
                colors = ButtonDefaults.buttonColors(containerColor = ThemeTokens.Border),
                modifier = Modifier.weight(1f)
            ) {
                Text("Scroll Up", color = ThemeTokens.TextMain)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = { connectionManager.sendCommand(Command.Scroll(0f, 300f)) },
                colors = ButtonDefaults.buttonColors(containerColor = ThemeTokens.Border),
                modifier = Modifier.weight(1f)
            ) {
                Text("Scroll Down", color = ThemeTokens.TextMain)
            }
        }
    }
}

@Composable
fun DpadTab(connectionManager: TvConnectionManager) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { connectionManager.sendCommand(Command.GoBack) },
                colors = ButtonDefaults.buttonColors(containerColor = ThemeTokens.Border)
            ) {
                Text("Back", color = ThemeTokens.TextMain)
            }

            Button(
                onClick = { connectionManager.sendCommand(Command.PlayPause) },
                colors = ButtonDefaults.buttonColors(containerColor = ThemeTokens.Accent)
            ) {
                Text("Play / Pause", color = ThemeTokens.Background, fontWeight = FontWeight.Bold)
            }
        }

        // The D-pad layout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            // UP
            IconButton(
                onClick = { connectionManager.sendCommand(Command.Scroll(0f, -150f)) },
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(ThemeTokens.CardBg, CircleShape)
            ) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Scroll Up", tint = ThemeTokens.TextMain, modifier = Modifier.size(40.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // LEFT
                IconButton(
                    onClick = { connectionManager.sendCommand(Command.Scroll(-150f, 0f)) },
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(ThemeTokens.CardBg, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Scroll Left", tint = ThemeTokens.TextMain, modifier = Modifier.size(40.dp))
                }

                // OK / CLICK
                Box(
                    modifier = Modifier
                        .padding(20.dp)
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(ThemeTokens.Primary, CircleShape)
                        .clickable(role = Role.Button) { connectionManager.sendCommand(Command.Click) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("OK", color = ThemeTokens.TextMain, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                // RIGHT
                IconButton(
                    onClick = { connectionManager.sendCommand(Command.Scroll(150f, 0f)) },
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(ThemeTokens.CardBg, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Scroll Right", tint = ThemeTokens.TextMain, modifier = Modifier.size(40.dp))
                }
            }

            // DOWN
            IconButton(
                onClick = { connectionManager.sendCommand(Command.Scroll(0f, 150f)) },
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(ThemeTokens.CardBg, CircleShape)
            ) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Scroll Down", tint = ThemeTokens.TextMain, modifier = Modifier.size(40.dp))
            }
        }
    }
}

@Composable
fun TabsManagerTab(connectionManager: TvConnectionManager, tvState: com.teleport.app.protocol.TvState?, onShowPaywall: () -> Unit = {}) {
    var newUrl by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = newUrl,
            onValueChange = { newUrl = it },
            label = { Text("Web URL or Video Link") },
            placeholder = { Text("https://...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(
                onGo = {
                    if (newUrl.isNotBlank()) {
                        focusManager.clearFocus()
                        var formattedUrl = newUrl.trim()
                        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                            formattedUrl = "https://$formattedUrl"
                        }
                        connectionManager.sendCommand(Command.OpenUrl(formattedUrl, headless = true))
                        newUrl = ""
                    }
                }
            ),
            trailingIcon = {
                if (newUrl.isNotEmpty()) {
                    IconButton(onClick = { newUrl = "" }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear URL", tint = ThemeTokens.TextSub)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ThemeTokens.Accent,
                focusedLabelColor = ThemeTokens.Accent,
                unfocusedBorderColor = ThemeTokens.Border,
                focusedTextColor = ThemeTokens.TextMain,
                unfocusedTextColor = ThemeTokens.TextMain
            )
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    if (newUrl.isNotBlank()) {
                        focusManager.clearFocus()
                        // Free tier: limit to 3 tabs
                        val tabCount = tvState?.tabs?.size ?: 0
                        if (!isProUnlocked && tabCount >= 3) {
                            onShowPaywall()
                            return@Button
                        }
                        var formattedUrl = newUrl.trim()
                        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                            formattedUrl = "https://$formattedUrl"
                        }
                        connectionManager.sendCommand(Command.OpenUrl(formattedUrl, headless = true))
                        newUrl = ""
                    }
                },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = newUrl.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemeTokens.Accent,
                    contentColor = ThemeTokens.Background,
                    disabledContainerColor = ThemeTokens.Accent.copy(alpha = 0.5f),
                    disabledContentColor = ThemeTokens.Background.copy(alpha = 0.5f)
                )
            ) {
                Text("Cast Video 📺", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = {
                    if (newUrl.isNotBlank()) {
                        focusManager.clearFocus()
                        // Free tier: limit to 3 tabs
                        val tabCount = tvState?.tabs?.size ?: 0
                        if (!isProUnlocked && tabCount >= 3) {
                            onShowPaywall()
                            return@Button
                        }
                        var formattedUrl = newUrl.trim()
                        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                            formattedUrl = "https://$formattedUrl"
                        }
                        connectionManager.sendCommand(Command.OpenUrl(formattedUrl, headless = false))
                        newUrl = ""
                    }
                },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = newUrl.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemeTokens.Border,
                    contentColor = ThemeTokens.TextMain,
                    disabledContainerColor = ThemeTokens.Border.copy(alpha = 0.5f),
                    disabledContentColor = ThemeTokens.TextMain.copy(alpha = 0.5f)
                )
            ) {
                Text("Open Page 🌐")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("OPEN TABS", fontSize = 12.sp, color = ThemeTokens.TextSub, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        if (tvState == null || tvState.tabs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No tabs open. Open a URL above!", color = ThemeTokens.TextSub)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                itemsIndexed(tvState.tabs) { index, tab ->
                    val isActive = index == tvState.activeTabIndex
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable(role = Role.Button) { connectionManager.sendCommand(Command.SelectTab(index)) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) ThemeTokens.Border else ThemeTokens.CardBg
                        ),
                        border = if (isActive) androidx.compose.foundation.BorderStroke(1.5.dp, ThemeTokens.Accent) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = tab.title.ifBlank { "Loading page..." },
                                        color = ThemeTokens.TextMain,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (tab.isLoading) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            color = ThemeTokens.Accent,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                                Text(
                                    text = tab.url,
                                    color = ThemeTokens.TextSub,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { connectionManager.sendCommand(Command.CloseTab(index)) }
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Close Tab", tint = ThemeTokens.TextSub)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickInputBar(connectionManager: TvConnectionManager) {
    var textInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val clipboardManager = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ThemeTokens.CardBg)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = textInput,
            onValueChange = { textInput = it },
            placeholder = { Text("Type text on TV...", color = ThemeTokens.TextSub, fontSize = 14.sp) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (textInput.isNotBlank()) {
                        focusManager.clearFocus()
                        connectionManager.sendCommand(Command.SendText(textInput))
                        textInput = ""
                    }
                }
            ),
            trailingIcon = {
                if (textInput.isNotEmpty()) {
                    IconButton(onClick = { textInput = "" }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear input", tint = ThemeTokens.TextSub)
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = ThemeTokens.Background,
                unfocusedContainerColor = ThemeTokens.Background,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = ThemeTokens.TextMain,
                unfocusedTextColor = ThemeTokens.TextMain
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = {
                try {
                    val clipData = clipboardManager.primaryClip
                    if (clipData != null && clipData.itemCount > 0) {
                        val pastedText = clipData.getItemAt(0).text?.toString() ?: ""
                        if (pastedText.isNotBlank()) {
                            connectionManager.sendCommand(Command.SendText(pastedText))
                            Toast.makeText(context, "Pasted to TV!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to read clipboard", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = ThemeTokens.Primary),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
        ) {
            Text("Paste", color = ThemeTokens.TextMain, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = {
                if (textInput.isNotBlank()) {
                    focusManager.clearFocus()
                    connectionManager.sendCommand(Command.SendText(textInput))
                    textInput = ""
                }
            },
            enabled = textInput.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = ThemeTokens.Accent,
                contentColor = ThemeTokens.Background,
                disabledContainerColor = ThemeTokens.Accent.copy(alpha = 0.5f),
                disabledContentColor = ThemeTokens.Background.copy(alpha = 0.5f)
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
        ) {
            Text("Send", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MobileCastingScreen(
    connectionManager: TvConnectionManager,
    tvState: com.teleport.app.protocol.TvState?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = ThemeTokens.Accent,
            modifier = Modifier.size(64.dp),
            strokeWidth = 4.dp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Resolving Video Stream...",
            color = ThemeTokens.TextMain,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        tvState?.resolvingUrl?.let { url ->
            val host = remember(url) {
                try {
                    java.net.URI(url).host?.replace("www.", "") ?: url
                } catch (e: Exception) {
                    url
                }
            }
            Text(
                text = "Extracting video from $host",
                color = ThemeTokens.TextSub,
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = { connectionManager.sendCommand(Command.GoBack) },
            colors = ButtonDefaults.buttonColors(containerColor = ThemeTokens.Error.copy(alpha = 0.8f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Cancel Casting", color = ThemeTokens.TextMain, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MobileMediaRemoteScreen(
    connectionManager: TvConnectionManager,
    tvState: com.teleport.app.protocol.TvState?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 32.dp)
        ) {
            Text(
                text = "Playing on TV 📺",
                color = ThemeTokens.Accent,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = ThemeTokens.CardBg),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Active Video Stream",
                        color = ThemeTokens.TextSub,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = tvState?.detectedStreamUrl?.substringAfterLast("/")?.substringBefore("?") ?: "Direct Native Player",
                        color = ThemeTokens.TextMain,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // Playback Control D-Pad/Row
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Seek Backward Button (-10s)
                IconButton(
                    onClick = { connectionManager.sendCommand(Command.Scroll(-100f, 0f)) },
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(ThemeTokens.CardBg, CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Seek Back",
                        tint = ThemeTokens.TextMain,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Play / Pause Circle
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(ThemeTokens.Primary, CircleShape)
                        .clickable(role = Role.Button) { connectionManager.sendCommand(Command.PlayPause) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("PLAY\nPAUSE", color = ThemeTokens.TextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Seek Forward Button (+10s)
                IconButton(
                    onClick = { connectionManager.sendCommand(Command.Scroll(100f, 0f)) },
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(ThemeTokens.CardBg, CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Seek Forward",
                        tint = ThemeTokens.TextMain,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }

        // Stop / Go Back button
        Button(
            onClick = { connectionManager.sendCommand(Command.GoBack) },
            colors = ButtonDefaults.buttonColors(containerColor = ThemeTokens.Error.copy(alpha = 0.8f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Stop Playback / Exit", color = ThemeTokens.TextMain, fontWeight = FontWeight.Bold)
        }
    }
}

data class OnboardingStep(
    val emoji: String,
    val title: String,
    val description: String
)

@Composable
fun OnboardingContent(
    currentSlide: Int,
    onCurrentSlideChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val steps = remember {
        listOf(
            OnboardingStep(
                emoji = "📺📱",
                title = "Welcome to TelePort",
                description = "TelePort connects your Phone Remote directly to your Android TV browser. Navigate, scroll, cast video streams, and mirror screens with zero cloud dependencies."
            ),
            OnboardingStep(
                emoji = "🔗",
                title = "Pairing with TV",
                description = "First, install and open the TelePort TV app on your Android TV device. Ensure both TV and Phone are on the same Wi-Fi, then scan the TV's QR Code."
            ),
            OnboardingStep(
                emoji = "🖱️",
                title = "Trackpad & Gestures",
                description = "Move your finger on the Trackpad area to move the virtual cursor. Single-tap to Click. Swipe up or down with TWO fingers to Scroll page content smoothly."
            ),
            OnboardingStep(
                emoji = "🪄",
                title = "Air Mouse Control",
                description = "Turn on Air Mouse mode to point your phone like a laser pointer! Wave your phone around to move the cursor on the TV screen."
            ),
            OnboardingStep(
                emoji = "🍿",
                title = "Beam & Cast Videos",
                description = "Open any webpage to automatically detect video streams (like mp4 or m3u8) and cast them to the native ExoPlayer. You can also share links from other apps using the Android Share menu!"
            ),
            OnboardingStep(
                emoji = "🌐",
                title = "Chrome Extension",
                description = "Mirror Chrome browser tabs or beam links directly from your computer using the TelePort Cast & Remote extension."
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = ThemeTokens.CardBg),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, ThemeTokens.Primary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Skip button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Skip",
                        color = ThemeTokens.TextSub,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button) { onDismiss() }
                            .padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Large Emoji
                Text(
                    text = steps[currentSlide].emoji,
                    fontSize = 72.sp,
                    modifier = Modifier.padding(16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = steps[currentSlide].title,
                    color = ThemeTokens.TextMain,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description or custom content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp), // Slightly taller to accommodate button
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = steps[currentSlide].description,
                        color = ThemeTokens.TextSub,
                        fontSize = 15.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                    if (currentSlide == 5) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val uriHandler = LocalUriHandler.current
                        Button(
                            onClick = { uriHandler.openUri("https://chromewebstore.google.com") },
                            colors = ButtonDefaults.buttonColors(containerColor = ThemeTokens.Primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Text("Open Chrome Web Store 🌐", color = ThemeTokens.TextMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .size(if (index == currentSlide) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == currentSlide) ThemeTokens.Accent else ThemeTokens.Border
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Bottom Navigation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentSlide > 0) {
                        Button(
                            onClick = { onCurrentSlideChange(currentSlide - 1) },
                            colors = ButtonDefaults.buttonColors(containerColor = ThemeTokens.Border),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Back", color = ThemeTokens.TextMain)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(80.dp)) // Maintain alignment
                    }

                    Button(
                        onClick = {
                            if (currentSlide < steps.size - 1) {
                                onCurrentSlideChange(currentSlide + 1)
                            } else {
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeTokens.Accent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(
                            text = if (currentSlide == steps.size - 1) "Get Started 🚀" else "Next",
                            color = ThemeTokens.Background,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingModal(onDismiss: () -> Unit, initialSlide: Int = 0) {
    var currentSlide by remember { mutableStateOf(initialSlide) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        OnboardingContent(
            currentSlide = currentSlide,
            onCurrentSlideChange = { currentSlide = it },
            onDismiss = onDismiss
        )
    }
}

