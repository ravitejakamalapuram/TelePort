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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teleport.app.mobile.connection.ConnectionState
import com.teleport.app.mobile.connection.TvConnectionManager
import com.teleport.app.mobile.nsd.NsdHelper
import com.teleport.app.mobile.sensors.GyroSensorTracker
import com.teleport.app.protocol.Command
import com.teleport.app.ui.theme.ThemeTokens

@Composable
fun MobileRemoteScreen(
    connectionManager: TvConnectionManager,
    nsdHelper: NsdHelper,
    gyroTracker: GyroSensorTracker,
    scanQr: () -> Unit,
    startMirroring: () -> Unit,
    stopMirroring: () -> Unit
) {
    val connState by connectionManager.connectionState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ThemeTokens.Background
    ) {
        if (connState == ConnectionState.Connected) {
            ControllerScreen(connectionManager, gyroTracker, startMirroring, stopMirroring)
        } else {
            PairingScreen(connectionManager, nsdHelper, scanQr)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingScreen(
    connectionManager: TvConnectionManager,
    nsdHelper: NsdHelper,
    scanQr: () -> Unit
) {
    val discoveredTvs by nsdHelper.discoveredTvs.collectAsState()
    val connState by connectionManager.connectionState.collectAsState()
    var manualIp by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        nsdHelper.startDiscovery()
        onDispose {
            nsdHelper.stopDiscovery()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "${ThemeTokens.APP_NAME} Remote",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = ThemeTokens.TextMain
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Connect to your TV client to start controlling",
            fontSize = 14.sp,
            color = ThemeTokens.TextSub
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
                                .clickable { connectionManager.connect(tv.ipAddress, tv.port) }
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
                        connectionManager.connect(manualIp.trim(), ThemeTokens.PORT)
                    }
                },
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ThemeTokens.Accent)
            ) {
                Text("Go", color = ThemeTokens.Background, fontWeight = FontWeight.Bold)
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
    }
}

@Composable
fun ControllerScreen(
    connectionManager: TvConnectionManager,
    gyroTracker: GyroSensorTracker,
    startMirroring: () -> Unit,
    stopMirroring: () -> Unit
) {
    val tvState by connectionManager.tvState.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = remember { listOf("Trackpad", "D-Pad", "Tabs") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ThemeTokens.CardBg)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    var isDarkModeEnabled by remember { mutableStateOf(false) }
                    Text("Dark Mode", color = ThemeTokens.TextSub, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = isDarkModeEnabled,
                        onCheckedChange = {
                            isDarkModeEnabled = it
                            connectionManager.sendCommand(Command.ToggleDarkMode(it))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ThemeTokens.Accent,
                            checkedTrackColor = ThemeTokens.Accent.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.scale(0.7f)
                    )
                }
            }
            Button(
                onClick = { connectionManager.disconnect() },
                colors = ButtonDefaults.buttonColors(containerColor = ThemeTokens.Error.copy(alpha = 0.8f))
            ) {
                Text("Disconnect", color = ThemeTokens.TextMain, fontSize = 12.sp)
            }
        }

        // GLOWING NATIVE STREAM DETECTED BANNER
        tvState?.detectedStreamUrl?.let { streamUrl ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable {
                        connectionManager.sendCommand(Command.PlayStreamNatively(streamUrl))
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
                TabRowDefaults.Indicator(
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
                0 -> TrackpadTab(connectionManager, gyroTracker, startMirroring, stopMirroring)
                1 -> DpadTab(connectionManager)
                2 -> TabsManagerTab(connectionManager, tvState)
            }
        }

        // Keyboard/Input Bar (Sticky at bottom)
        QuickInputBar(connectionManager)
    }
}

@Composable
fun TrackpadTab(
    connectionManager: TvConnectionManager,
    gyroTracker: GyroSensorTracker,
    startMirroring: () -> Unit,
    stopMirroring: () -> Unit
) {
    var isAirMouseOn by remember { mutableStateOf(false) }
    val isCasting by com.teleport.app.mobile.mirror.ScreenCastService.isCasting.collectAsState()

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
        // Mirror Screen Switch Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ThemeTokens.CardBg, RoundedCornerShape(12.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Mirror Phone Screen", color = ThemeTokens.TextMain, fontWeight = FontWeight.Bold)
                Text("Cast phone display to TV", color = ThemeTokens.TextSub, fontSize = 12.sp)
            }
            Switch(
                checked = isCasting,
                onCheckedChange = { checked ->
                    if (checked) {
                        startMirroring()
                    } else {
                        stopMirroring()
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ThemeTokens.Accent,
                    checkedTrackColor = ThemeTokens.Accent.copy(alpha = 0.5f)
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Air Mouse Switch Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ThemeTokens.CardBg, RoundedCornerShape(12.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Air Mouse Mode", color = ThemeTokens.TextMain, fontWeight = FontWeight.Bold)
                Text("Point phone to move TV cursor", color = ThemeTokens.TextSub, fontSize = 12.sp)
            }
            Switch(
                checked = isAirMouseOn,
                onCheckedChange = { isAirMouseOn = it },
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
                        .background(ThemeTokens.CardBg, CircleShape)
                ) {
                    Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Scroll Left", tint = ThemeTokens.TextMain, modifier = Modifier.size(40.dp))
                }

                // OK / CLICK
                Box(
                    modifier = Modifier
                        .padding(20.dp)
                        .size(90.dp)
                        .background(ThemeTokens.Primary, CircleShape)
                        .clickable { connectionManager.sendCommand(Command.Click) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("OK", color = ThemeTokens.TextMain, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                // RIGHT
                IconButton(
                    onClick = { connectionManager.sendCommand(Command.Scroll(150f, 0f)) },
                    modifier = Modifier
                        .size(80.dp)
                        .background(ThemeTokens.CardBg, CircleShape)
                ) {
                    Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Scroll Right", tint = ThemeTokens.TextMain, modifier = Modifier.size(40.dp))
                }
            }

            // DOWN
            IconButton(
                onClick = { connectionManager.sendCommand(Command.Scroll(0f, 150f)) },
                modifier = Modifier
                    .size(80.dp)
                    .background(ThemeTokens.CardBg, CircleShape)
            ) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Scroll Down", tint = ThemeTokens.TextMain, modifier = Modifier.size(40.dp))
            }
        }
    }
}

@Composable
fun TabsManagerTab(connectionManager: TvConnectionManager, tvState: com.teleport.app.protocol.TvState?) {
    var newUrl by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newUrl,
                onValueChange = { newUrl = it },
                label = { Text("Open URL on TV") },
                placeholder = { Text("https://...") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ThemeTokens.Accent,
                    focusedLabelColor = ThemeTokens.Accent,
                    unfocusedBorderColor = ThemeTokens.Border,
                    focusedTextColor = ThemeTokens.TextMain,
                    unfocusedTextColor = ThemeTokens.TextMain
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(
                onClick = {
                    if (newUrl.isNotBlank()) {
                        var formattedUrl = newUrl.trim()
                        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                            formattedUrl = "https://$formattedUrl"
                        }
                        connectionManager.sendCommand(Command.OpenUrl(formattedUrl))
                        newUrl = ""
                    }
                },
                modifier = Modifier
                    .size(56.dp)
                    .background(ThemeTokens.Accent, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Open Tab", tint = ThemeTokens.Background)
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
                            .clickable { connectionManager.sendCommand(Command.SelectTab(index)) },
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
                                Text(
                                    text = tab.title.ifBlank { "Loading page..." },
                                    color = ThemeTokens.TextMain,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                               )
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
                    connectionManager.sendCommand(Command.SendText(textInput))
                    textInput = ""
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = ThemeTokens.Accent),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
        ) {
            Text("Send", color = ThemeTokens.Background, fontWeight = FontWeight.Bold)
        }
    }
}
