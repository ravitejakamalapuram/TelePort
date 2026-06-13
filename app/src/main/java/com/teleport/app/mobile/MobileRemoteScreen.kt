package com.teleport.app.mobile

import android.widget.Toast
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.semantics.Role
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.delay

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
        when (connState) {
            is ConnectionState.Disconnected -> {
                NoConnectionScreen(
                    connectionManager = connectionManager,
                    nsdHelper = nsdHelper,
                    scanQr = scanQr,
                    onShowHelp = { showOnboarding = true }
                )
            }
            is ConnectionState.Connecting -> {
                ConnectingScreen(connectionManager)
            }
            is ConnectionState.Error -> {
                ConnectionFailedScreen(
                    connectionManager = connectionManager,
                    errorState = connState as ConnectionState.Error,
                    onShowHelp = { showOnboarding = true }
                )
            }
            is ConnectionState.Connected -> {
                ControllerScreen(
                    connectionManager = connectionManager,
                    gyroTracker = gyroTracker,
                    startMirroring = startMirroring,
                    stopMirroring = stopMirroring,
                    billingManager = billingManager,
                    onShowHelp = { showOnboarding = true }
                )
            }
        }
    }
}

@Composable
fun NoConnectionBottomNavBar(activeItem: String = "Devices") {
    NavigationBar(
        containerColor = ThemeTokens.CardBg,
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            Triple("Remote", Icons.Filled.Gamepad, "Remote"),
            Triple("Media", Icons.Filled.Movie, "Media"),
            Triple("Devices", Icons.Filled.Tv, "Devices"),
            Triple("Settings", Icons.Filled.Settings, "Settings")
        )
        items.forEach { (label, icon, value) ->
            val isActive = value == activeItem
            NavigationBarItem(
                selected = isActive,
                onClick = { /* Disabled when disconnected */ },
                icon = { Icon(icon, contentDescription = label, tint = if (isActive) ThemeTokens.Primary else ThemeTokens.TextSub.copy(alpha = 0.4f)) },
                label = { Text(label, color = if (isActive) ThemeTokens.Primary else ThemeTokens.TextSub.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = ThemeTokens.Primary.copy(alpha = 0.15f)
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoConnectionScreen(
    connectionManager: TvConnectionManager,
    nsdHelper: NsdHelper,
    scanQr: () -> Unit,
    onShowHelp: () -> Unit
) {
    var showDiscovery by remember { mutableStateOf(false) }
    val discoveredTvs by nsdHelper.discoveredTvs.collectAsState()
    var manualIp by remember { mutableStateOf("") }
    val context = LocalContext.current

    DisposableEffect(showDiscovery) {
        if (showDiscovery) {
            nsdHelper.startDiscovery()
        } else {
            nsdHelper.stopDiscovery()
        }
        onDispose {
            nsdHelper.stopDiscovery()
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(64.dp)
                    .background(ThemeTokens.Background)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Filled.SettingsRemote,
                        contentDescription = "Remote",
                        tint = ThemeTokens.Primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "TelePort",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = ThemeTokens.Primary
                    )
                }
                IconButton(onClick = onShowHelp) {
                    Icon(
                        Icons.Filled.CastConnected,
                        contentDescription = "Cast Connected",
                        tint = ThemeTokens.TextSub
                    )
                }
            }
        },
        bottomBar = {
            NoConnectionBottomNavBar(activeItem = "Devices")
        },
        containerColor = ThemeTokens.Background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 40.dp)
                        .size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(2.dp, ThemeTokens.TextSub.copy(alpha = 0.2f), CircleShape)
                    )
                    Icon(
                        Icons.Filled.TvOff,
                        contentDescription = "TV Off",
                        tint = ThemeTokens.TextSub.copy(alpha = 0.4f),
                        modifier = Modifier.size(84.dp)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 8.dp, y = 8.dp)
                            .background(ThemeTokens.CardBg, RoundedCornerShape(16.dp))
                            .border(1.dp, ThemeTokens.TextSub.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Icon(
                            Icons.Filled.LinkOff,
                            contentDescription = "Disconnected",
                            tint = ThemeTokens.Error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Text(
                    text = "No Device Connected",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = ThemeTokens.TextMain,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = "Pair a device to start controlling your media. Connect to the same Wi-Fi network to discover TVs and consoles.",
                    fontSize = 15.sp,
                    color = ThemeTokens.TextSub,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(bottom = 40.dp)
                )

                Button(
                    onClick = { showDiscovery = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeTokens.Primary),
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
                    shape = RoundedCornerShape(9999.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = ThemeTokens.Background)
                        Text(
                            text = "Find Devices",
                            color = ThemeTokens.Background,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ThemeTokens.CardBg.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .border(1.dp, ThemeTokens.Border, RoundedCornerShape(16.dp))
                        .clickable { onShowHelp() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = "Info",
                        tint = ThemeTokens.Accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Make sure your TV's Remote Desktop or Cast settings are enabled.",
                        fontSize = 12.sp,
                        color = ThemeTokens.TextSub,
                        lineHeight = 18.sp
                    )
                }
            }

            if (showDiscovery) {
                ModalBottomSheet(
                    onDismissRequest = { showDiscovery = false },
                    containerColor = ThemeTokens.Background,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = ThemeTokens.TextSub.copy(alpha = 0.4f)) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .navigationBarsPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Pair New Device",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemeTokens.TextMain,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Button(
                            onClick = {
                                showDiscovery = false
                                scanQr()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ThemeTokens.Primary)
                        ) {
                            Text("Scan TV QR Code", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ThemeTokens.Background)
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "DISCOVERED DEVICES",
                            fontSize = 12.sp,
                            color = ThemeTokens.TextSub,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
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
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("Searching on Wi-Fi...", color = ThemeTokens.TextSub, fontSize = 13.sp)
                                    }
                                }
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    itemsIndexed(discoveredTvs) { _, tv ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(role = Role.Button) {
                                                    showDiscovery = false
                                                    connectionManager.connect(tv.ipAddress, tv.port)
                                                }
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(tv.name, color = ThemeTokens.TextMain, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text("${tv.ipAddress}:${tv.port}", color = ThemeTokens.TextSub, fontSize = 12.sp)
                                            }
                                            Text("Connect", color = ThemeTokens.Accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = manualIp,
                                onValueChange = { manualIp = it },
                                label = { Text("Manual TV IP") },
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
                                            showDiscovery = false
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
                                        showDiscovery = false
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
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectingScreen(connectionManager: TvConnectionManager) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")

    val radarScale1 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale1"
    )
    val radarOpacity1 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "opacity1"
    )

    val radarScale2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale2"
    )
    val radarOpacity2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "opacity2"
    )

    val spinnerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val tvBounce by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    var ellipsis by remember { mutableStateOf(".") }
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            ellipsis = when (ellipsis) {
                "." -> ".."
                ".." -> "..."
                else -> "."
            }
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(64.dp)
                    .background(ThemeTokens.Background)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Filled.SettingsRemote,
                        contentDescription = "Remote",
                        tint = ThemeTokens.Primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "TelePort",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = ThemeTokens.Primary
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .background(ThemeTokens.Primary.copy(alpha = 0.15f), RoundedCornerShape(9999.dp))
                        .border(1.dp, ThemeTokens.Primary.copy(alpha = 0.3f), RoundedCornerShape(9999.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(ThemeTokens.Primary, CircleShape)
                    )
                    Text(
                        text = "Connecting...",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ThemeTokens.Primary
                    )
                }
                IconButton(onClick = {}) {
                    Icon(
                        Icons.Filled.CastConnected,
                        contentDescription = "Cast Connected",
                        tint = ThemeTokens.TextSub
                    )
                }
            }
        },
        bottomBar = {
            NoConnectionBottomNavBar(activeItem = "Remote")
        },
        containerColor = ThemeTokens.Background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(ThemeTokens.Primary.copy(alpha = 0.08f), Color.Transparent),
                            radius = 600f
                        )
                    )
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .scale(radarScale1)
                            .border(1.5.dp, ThemeTokens.Primary.copy(alpha = radarOpacity1), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .scale(radarScale2)
                            .border(1.5.dp, ThemeTokens.Primary.copy(alpha = radarOpacity2), CircleShape)
                    )

                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(ThemeTokens.CardBg, CircleShape)
                            .border(1.dp, ThemeTokens.Border, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = ThemeTokens.Primary,
                            strokeWidth = 3.dp,
                            modifier = Modifier
                                .size(96.dp)
                                .scale(spinnerRotation / 360f)
                        )
                        Icon(
                            Icons.Filled.Tv,
                            contentDescription = "TV",
                            tint = ThemeTokens.Primary,
                            modifier = Modifier
                                .size(40.dp)
                                .offset(y = tvBounce.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = "Connecting to TV$ellipsis",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = ThemeTokens.TextMain,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = "Establishing a low-latency secure tunnel via your local network...",
                    fontSize = 14.sp,
                    color = ThemeTokens.TextSub,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier
                        .width(260.dp)
                        .padding(bottom = 32.dp)
                )
            }
        }
    }
}

@Composable
fun ConnectionFailedScreen(
    connectionManager: TvConnectionManager,
    errorState: ConnectionState.Error,
    onShowHelp: () -> Unit
) {
    val activeIp = connectionManager.activeIp

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(64.dp)
                    .background(ThemeTokens.Background)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Filled.SettingsRemote,
                        contentDescription = "Remote",
                        tint = ThemeTokens.Primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "TelePort",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = ThemeTokens.Primary
                    )
                }
                IconButton(onClick = {}) {
                    Icon(
                        Icons.Filled.CastConnected,
                        contentDescription = "Cast Connected",
                        tint = ThemeTokens.Primary
                    )
                }
            }
        },
        bottomBar = {
            NoConnectionBottomNavBar(activeItem = "Devices")
        },
        containerColor = ThemeTokens.Background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ThemeTokens.Border, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = ThemeTokens.CardBg),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(bottom = 24.dp)
                                .size(96.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(ThemeTokens.Error.copy(alpha = 0.15f), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(ThemeTokens.CardBg, CircleShape)
                                    .border(2.dp, ThemeTokens.Error.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.WifiOff,
                                    contentDescription = "Wifi Off",
                                    tint = ThemeTokens.Error,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(32.dp)
                                    .background(ThemeTokens.CardBg, CircleShape)
                                    .border(1.dp, ThemeTokens.Border, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.LinkOff,
                                    contentDescription = "Disconnected",
                                    tint = ThemeTokens.Error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Text(
                            text = "Connection Failed",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemeTokens.TextMain,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            text = "We couldn't reach the Living Room TV. Make sure it's turned on and connected to the same Wi-Fi.",
                            fontSize = 14.sp,
                            color = ThemeTokens.TextSub,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 20.sp,
                            modifier = Modifier
                                .width(280.dp)
                                .padding(bottom = 32.dp)
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (activeIp != null) {
                                        connectionManager.connect(activeIp, ThemeTokens.PORT)
                                    } else {
                                        connectionManager.disconnect()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ThemeTokens.Primary)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = "Retry", tint = ThemeTokens.Background)
                                    Text("Try Again", color = ThemeTokens.Background, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = onShowHelp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .border(1.dp, ThemeTokens.Border, RoundedCornerShape(16.dp)),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ThemeTokens.CardBg)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.Info, contentDescription = "Help", tint = ThemeTokens.TextSub)
                                    Text("Troubleshoot", color = ThemeTokens.TextMain, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ThemeTokens.CardBg.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .border(1.dp, ThemeTokens.Border, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = "Pro Tip",
                        tint = ThemeTokens.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Pro tip: Try restarting your router if problems persist across multiple devices.",
                        fontSize = 12.sp,
                        color = ThemeTokens.TextSub,
                        lineHeight = 18.sp
                    )
                }
            }
        }
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

    if (showAdUnlockPrompt) {
        val context = LocalContext.current
        AlertDialog(
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

    if (showPaywall) {
        PaywallScreen(
            billingManager = billingManager,
            onDismiss = { showPaywall = false }
        )
        return
    }

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
        var selectedBottomTab by remember { mutableStateOf("Remote") }

        Scaffold(
            topBar = {
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.SettingsRemote,
                                contentDescription = "Remote icon",
                                tint = ThemeTokens.Primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Connected to TV",
                                    color = ThemeTokens.TextMain,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = connectionManager.activeIp ?: "Local Network",
                                    color = ThemeTokens.TextSub,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isProUnlocked && FeatureFlags.ENABLE_PAID_SUBSCRIPTIONS) {
                                Button(
                                    onClick = { showPaywall = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = ThemeTokens.Primary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "⭐ Pro",
                                        color = ThemeTokens.Background,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                            IconButton(
                                onClick = { connectionManager.disconnect() }
                            ) {
                                Icon(Icons.Filled.LinkOff, contentDescription = "Disconnect", tint = ThemeTokens.Error)
                            }
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = ThemeTokens.CardBg,
                    tonalElevation = 8.dp
                ) {
                    val tabs = listOf(
                        Triple("Remote", Icons.Filled.Gamepad, "Remote"),
                        Triple("Trackpad", Icons.Filled.TouchApp, "Trackpad"),
                        Triple("Tabs", Icons.Filled.Apps, "Tabs"),
                        Triple("Settings", Icons.Filled.Settings, "Settings")
                    )
                    tabs.forEach { (label, icon, value) ->
                        val isActive = selectedBottomTab == value
                        NavigationBarItem(
                            selected = isActive,
                            onClick = { selectedBottomTab = value },
                            icon = { Icon(icon, contentDescription = label, tint = if (isActive) ThemeTokens.Primary else ThemeTokens.TextSub.copy(alpha = 0.6f)) },
                            label = { Text(label, color = if (isActive) ThemeTokens.Primary else ThemeTokens.TextSub.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = ThemeTokens.Primary.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            },
            containerColor = ThemeTokens.Background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                tvState?.detectedStreamUrl?.let { streamUrl ->
                    val context = LocalContext.current
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable(role = Role.Button) {
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
                                Text("📺 Media stream detected!", color = ThemeTokens.Background, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Tap to play cleanly in Native Player", color = ThemeTokens.Background.copy(alpha = 0.8f), fontSize = 11.sp)
                            }
                            Text("PLAY", color = ThemeTokens.Background, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedBottomTab) {
                        "Remote" -> RemoteTab(connectionManager)
                        "Trackpad" -> TrackpadTab(
                            connectionManager = connectionManager,
                            gyroTracker = gyroTracker,
                            startMirroring = startMirroring,
                            stopMirroring = stopMirroring,
                            onShowPaywall = {
                                if (FeatureFlags.ENABLE_PAID_SUBSCRIPTIONS) {
                                    showPaywall = true
                                } else {
                                    showAdUnlockPrompt = true
                                }
                            }
                        )
                        "Tabs" -> TabsManagerTab(
                            connectionManager = connectionManager,
                            tvState = tvState,
                            onShowPaywall = {
                                if (FeatureFlags.ENABLE_PAID_SUBSCRIPTIONS) {
                                    showPaywall = true
                                } else {
                                    showAdUnlockPrompt = true
                                }
                            }
                        )
                        "Settings" -> SettingsTab(
                            connectionManager = connectionManager,
                            billingManager = billingManager,
                            onShowHelp = onShowHelp,
                            onShowTipJar = { showTipJar = true },
                            onShowPaywall = { showPaywall = true }
                        )
                    }
                }

                if (selectedBottomTab != "Settings") {
                    QuickInputBar(connectionManager)
                }

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
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("🎬 Watch ad to remove ads for 1 hour", color = ThemeTokens.Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                BannerAd()
            }
        }
    }
}

@Composable
fun RemoteTab(connectionManager: TvConnectionManager) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, ThemeTokens.Primary.copy(alpha = 0.2f), CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(ThemeTokens.Primary.copy(alpha = 0.05f), Color.Transparent),
                            radius = 350f
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF252B2D), Color(0xFF171D1E)),
                            radius = 300f
                        ),
                        CircleShape
                    )
                    .border(1.dp, ThemeTokens.Border, CircleShape)
                    .padding(8.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.align(Alignment.TopCenter).width(1.dp).height(20.dp).background(ThemeTokens.Primary.copy(alpha = 0.3f)))
                    Box(modifier = Modifier.align(Alignment.BottomCenter).width(1.dp).height(20.dp).background(ThemeTokens.Primary.copy(alpha = 0.3f)))
                    Box(modifier = Modifier.align(Alignment.CenterStart).width(20.dp).height(1.dp).background(ThemeTokens.Primary.copy(alpha = 0.3f)))
                    Box(modifier = Modifier.align(Alignment.CenterEnd).width(20.dp).height(1.dp).background(ThemeTokens.Primary.copy(alpha = 0.3f)))
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            connectionManager.sendCommand(Command.Scroll(0f, -150f))
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Up", tint = ThemeTokens.TextMain, modifier = Modifier.size(32.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                connectionManager.sendCommand(Command.Scroll(-150f, 0f))
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Left", tint = ThemeTokens.TextMain, modifier = Modifier.size(32.dp))
                        }

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(ThemeTokens.CardBg, CircleShape)
                                .border(2.dp, ThemeTokens.Primary.copy(alpha = 0.5f), CircleShape)
                                .clickable(role = Role.Button) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    connectionManager.sendCommand(Command.Click)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(ThemeTokens.Primary, CircleShape)
                            )
                        }

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                connectionManager.sendCommand(Command.Scroll(150f, 0f))
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Right", tint = ThemeTokens.TextMain, modifier = Modifier.size(32.dp))
                        }
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            connectionManager.sendCommand(Command.Scroll(0f, 150f))
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Down", tint = ThemeTokens.TextMain, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    connectionManager.sendCommand(Command.Scroll(-100f, 0f))
                },
                modifier = Modifier
                    .size(56.dp)
                    .background(ThemeTokens.CardBg, CircleShape)
                    .border(1.dp, ThemeTokens.Border, CircleShape)
            ) {
                Icon(Icons.Filled.FastRewind, contentDescription = "Rewind", tint = ThemeTokens.TextMain, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(24.dp))

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(ThemeTokens.Primary, CircleShape)
                    .clickable(role = Role.Button) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        connectionManager.sendCommand(Command.PlayPause)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Play Pause",
                    tint = ThemeTokens.Background,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    connectionManager.sendCommand(Command.Scroll(100f, 0f))
                },
                modifier = Modifier
                    .size(56.dp)
                    .background(ThemeTokens.CardBg, CircleShape)
                    .border(1.dp, ThemeTokens.Border, CircleShape)
            ) {
                Icon(Icons.Filled.FastForward, contentDescription = "Forward", tint = ThemeTokens.TextMain, modifier = Modifier.size(24.dp))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, ThemeTokens.Border, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = ThemeTokens.CardBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.VolumeMute, contentDescription = "Volume Down", tint = ThemeTokens.TextSub, modifier = Modifier.size(14.dp))
                        Text(
                            text = "VOLUME",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemeTokens.Primary,
                            letterSpacing = 1.sp
                        )
                        Icon(Icons.Filled.VolumeUp, contentDescription = "Volume Up", tint = ThemeTokens.TextSub, modifier = Modifier.size(14.dp))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(ThemeTokens.Border, RoundedCornerShape(2.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.65f)
                                .background(ThemeTokens.Primary, RoundedCornerShape(2.dp))
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                connectionManager.sendCommand(Command.VolumeDown)
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .background(ThemeTokens.Border, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "Decrease", tint = ThemeTokens.TextMain)
                        }
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                connectionManager.sendCommand(Command.VolumeUp)
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .background(ThemeTokens.Border, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Increase", tint = ThemeTokens.TextMain)
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, ThemeTokens.Border, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = ThemeTokens.CardBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Channel Down", tint = ThemeTokens.TextSub, modifier = Modifier.size(14.dp))
                        Text(
                            text = "CHANNEL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemeTokens.Primary,
                            letterSpacing = 1.sp
                        )
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Channel Up", tint = ThemeTokens.TextSub, modifier = Modifier.size(14.dp))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(ThemeTokens.Border, RoundedCornerShape(2.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.3f)
                                .background(ThemeTokens.Primary, RoundedCornerShape(2.dp))
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                connectionManager.sendCommand(Command.ChannelDown)
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .background(ThemeTokens.Border, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Filled.ExpandMore, contentDescription = "Decrease", tint = ThemeTokens.TextMain)
                        }
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                connectionManager.sendCommand(Command.ChannelUp)
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .background(ThemeTokens.Border, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Filled.ExpandLess, contentDescription = "Increase", tint = ThemeTokens.TextMain)
                        }
                    }
                }
            }
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
                    checkedThumbColor = ThemeTokens.Primary,
                    checkedTrackColor = ThemeTokens.Primary.copy(alpha = 0.5f)
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

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
                            modifier = Modifier.background(ThemeTokens.Primary, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
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
                    checkedThumbColor = ThemeTokens.Primary,
                    checkedTrackColor = ThemeTokens.Primary.copy(alpha = 0.5f)
                )
            )
        }

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
                    color = ThemeTokens.Primary,
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
fun TabsManagerTab(
    connectionManager: TvConnectionManager,
    tvState: com.teleport.app.protocol.TvState?,
    onShowPaywall: () -> Unit = {}
) {
    var newUrl by remember { mutableStateOf("") }

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
                    containerColor = ThemeTokens.Primary,
                    contentColor = ThemeTokens.Background,
                    disabledContainerColor = ThemeTokens.Primary.copy(alpha = 0.5f),
                    disabledContentColor = ThemeTokens.Background.copy(alpha = 0.5f)
                )
            ) {
                Text("Cast Video 📺", fontWeight = FontWeight.Bold, color = ThemeTokens.Background)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = {
                    if (newUrl.isNotBlank()) {
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
                        border = if (isActive) androidx.compose.foundation.BorderStroke(1.5.dp, ThemeTokens.Primary) else null
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
                                            color = ThemeTokens.Primary,
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
fun SettingsTab(
    connectionManager: TvConnectionManager,
    billingManager: BillingManager,
    onShowHelp: () -> Unit,
    onShowTipJar: () -> Unit,
    onShowPaywall: () -> Unit
) {
    var isDarkModeEnabled by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = ThemeTokens.TextMain,
            modifier = Modifier.align(Alignment.Start)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ThemeTokens.Border, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = ThemeTokens.CardBg),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Connected Device", fontSize = 12.sp, color = ThemeTokens.TextSub, fontWeight = FontWeight.Bold)
                Text(
                    text = connectionManager.activeIp?.let { "Living Room TV ($it)" } ?: "Connected TV",
                    color = ThemeTokens.TextMain,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ThemeTokens.Border, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = ThemeTokens.CardBg),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        isDarkModeEnabled = !isDarkModeEnabled
                        connectionManager.sendCommand(Command.ToggleDarkMode(isDarkModeEnabled))
                    }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("TV Dark Mode", color = ThemeTokens.TextMain, fontWeight = FontWeight.Bold)
                    Text("Invert page styling on TV client", color = ThemeTokens.TextSub, fontSize = 12.sp)
                }
                Switch(
                    checked = isDarkModeEnabled,
                    onCheckedChange = {
                        isDarkModeEnabled = it
                        connectionManager.sendCommand(Command.ToggleDarkMode(it))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ThemeTokens.Primary,
                        checkedTrackColor = ThemeTokens.Primary.copy(alpha = 0.5f)
                    )
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ThemeTokens.Border, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = ThemeTokens.CardBg),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShowHelp() }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Interactive Help Guide ❔", color = ThemeTokens.TextMain, fontWeight = FontWeight.Bold)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Open", tint = ThemeTokens.TextSub)
                }
                HorizontalDivider(color = ThemeTokens.Border)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShowTipJar() }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Support the Creator ☕", color = ThemeTokens.TextMain, fontWeight = FontWeight.Bold)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Open", tint = ThemeTokens.TextSub)
                }
                if (FeatureFlags.ENABLE_PAID_SUBSCRIPTIONS) {
                    HorizontalDivider(color = ThemeTokens.Border)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onShowPaywall() }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Upgrade to Pro ⭐", color = ThemeTokens.TextMain, fontWeight = FontWeight.Bold)
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Open", tint = ThemeTokens.TextSub)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { connectionManager.disconnect() },
            colors = ButtonDefaults.buttonColors(containerColor = ThemeTokens.Error),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Disconnect Remote", color = ThemeTokens.TextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun QuickInputBar(connectionManager: TvConnectionManager) {
    var textInput by remember { mutableStateOf("") }
    val context = LocalContext.current
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
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Text("Paste", color = ThemeTokens.Background, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = {
                if (textInput.isNotBlank()) {
                    connectionManager.sendCommand(Command.SendText(textInput))
                    textInput = ""
                }
            },
            enabled = textInput.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = ThemeTokens.Primary,
                contentColor = ThemeTokens.Background,
                disabledContainerColor = ThemeTokens.Primary.copy(alpha = 0.5f),
                disabledContentColor = ThemeTokens.Background.copy(alpha = 0.5f)
            ),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Text("Send", fontWeight = FontWeight.Bold, color = ThemeTokens.Background)
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
            color = ThemeTokens.Primary,
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
            colors = ButtonDefaults.buttonColors(containerColor = ThemeTokens.Error),
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
                color = ThemeTokens.Primary,
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

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
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

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(ThemeTokens.Primary, CircleShape)
                        .clickable(role = Role.Button) { connectionManager.sendCommand(Command.PlayPause) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("PLAY\nPAUSE", color = ThemeTokens.Background, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }

                Spacer(modifier = Modifier.width(24.dp))

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

        Button(
            onClick = { connectionManager.sendCommand(Command.GoBack) },
            colors = ButtonDefaults.buttonColors(containerColor = ThemeTokens.Error),
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
                            .clickable { onDismiss() }
                            .padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = steps[currentSlide].emoji,
                    fontSize = 72.sp,
                    modifier = Modifier.padding(16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = steps[currentSlide].title,
                    color = ThemeTokens.TextMain,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
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
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Text("Open Chrome Web Store 🌐", color = ThemeTokens.Background, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

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
                                    if (index == currentSlide) ThemeTokens.Primary else ThemeTokens.Border
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

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
                        Spacer(modifier = Modifier.width(80.dp))
                    }

                    Button(
                        onClick = {
                            if (currentSlide < steps.size - 1) {
                                onCurrentSlideChange(currentSlide + 1)
                            } else {
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeTokens.Primary),
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
