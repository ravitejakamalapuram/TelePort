package com.teleport.app.mobile

import android.widget.Toast
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

@Composable
fun MobileRemoteScreen(
    connectionManager: TvConnectionManager,
    nsdHelper: NsdHelper,
    gyroTracker: GyroSensorTracker,
    scanQr: () -> Unit
) {
    val connState by connectionManager.connectionState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212)
    ) {
        if (connState == ConnectionState.Connected) {
            ControllerScreen(connectionManager, gyroTracker)
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
            text = "TelePort Remote",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Connect to your TV client to start controlling",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(48.dp))

        // QR Code Scanner Button
        Button(
            onClick = scanQr,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
        ) {
            Text("Scan TV QR Code", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("OR CONNECT VIA DISCOVERY", fontSize = 12.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Discovered TVs List
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            if (discoveredTvs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Searching for TVs on Wi-Fi...", color = Color.Gray, fontSize = 14.sp)
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
                                Text(tv.name, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("${tv.ipAddress}:${tv.port}", color = Color.Gray, fontSize = 12.sp)
                            }
                            Text("Connect", color = Color(0xFF03DAC6), fontWeight = FontWeight.Bold)
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
                    focusedBorderColor = Color(0xFF03DAC6),
                    focusedLabelColor = Color(0xFF03DAC6),
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = {
                    if (manualIp.isNotBlank()) {
                        connectionManager.connect(manualIp.trim(), 8080)
                    }
                },
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC6))
            ) {
                Text("Go", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        if (connState is ConnectionState.Connecting) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Connecting to TV...", color = Color(0xFF03DAC6))
        } else if (connState is ConnectionState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Error: ${(connState as ConnectionState.Error).message}", color = Color.Red, fontSize = 12.sp)
        }
    }
}

@Composable
fun ControllerScreen(connectionManager: TvConnectionManager, gyroTracker: GyroSensorTracker) {
    val tvState by connectionManager.tvState.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = remember { listOf("Trackpad", "D-Pad", "Tabs") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TelePort Remote",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Button(
                onClick = { connectionManager.disconnect() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
            ) {
                Text("Disconnect", color = Color.White, fontSize = 12.sp)
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFF6200EE)),
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
                        Text("📺 Media stream detected!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Tap to play cleanly in Native Player", color = Color.LightGray, fontSize = 11.sp)
                    }
                    Text("PLAY", color = Color(0xFF03DAC6), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        // Tab Selection
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color(0xFF1E1E1E),
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = Color(0xFF03DAC6)
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
                .background(Color(0xFF121212))
        ) {
            when (selectedTabIndex) {
                0 -> TrackpadTab(connectionManager, gyroTracker)
                1 -> DpadTab(connectionManager)
                2 -> TabsManagerTab(connectionManager, tvState)
            }
        }

        // Keyboard/Input Bar (Sticky at bottom)
        QuickInputBar(connectionManager)
    }
}

@Composable
fun TrackpadTab(connectionManager: TvConnectionManager, gyroTracker: GyroSensorTracker) {
    var isAirMouseOn by remember { mutableStateOf(false) }

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
        // Air Mouse Switch Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Air Mouse Mode", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Point phone to move TV cursor", color = Color.Gray, fontSize = 12.sp)
            }
            Switch(
                checked = isAirMouseOn,
                onCheckedChange = { isAirMouseOn = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF03DAC6),
                    checkedTrackColor = Color(0xFF03DAC6).copy(alpha = 0.5f)
                )
            )
        }

        // Trackpad Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .background(Color(0xFF1E1E1E), RoundedCornerShape(24.dp))
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
                    color = Color(0xFF03DAC6),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            } else {
                Text(
                    "TRACKPAD\nDrag to move. Tap to click.",
                    color = Color.DarkGray,
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2E2E)),
                modifier = Modifier.weight(1f)
            ) {
                Text("Scroll Up", color = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = { connectionManager.sendCommand(Command.Scroll(0f, 300f)) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2E2E)),
                modifier = Modifier.weight(1f)
            ) {
                Text("Scroll Down", color = Color.White)
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2E2E))
            ) {
                Text("Back", color = Color.White)
            }

            Button(
                onClick = { connectionManager.sendCommand(Command.PlayPause) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC6))
            ) {
                Text("Play / Pause", color = Color.Black, fontWeight = FontWeight.Bold)
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
                    .background(Color(0xFF1E1E1E), CircleShape)
            ) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Scroll Up", tint = Color.White, modifier = Modifier.size(40.dp))
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
                        .background(Color(0xFF1E1E1E), CircleShape)
                ) {
                    Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Scroll Left", tint = Color.White, modifier = Modifier.size(40.dp))
                }

                // OK / CLICK
                Box(
                    modifier = Modifier
                        .padding(20.dp)
                        .size(90.dp)
                        .background(Color(0xFF6200EE), CircleShape)
                        .clickable { connectionManager.sendCommand(Command.Click) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }

                // RIGHT
                IconButton(
                    onClick = { connectionManager.sendCommand(Command.Scroll(150f, 0f)) },
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFF1E1E1E), CircleShape)
                ) {
                    Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Scroll Right", tint = Color.White, modifier = Modifier.size(40.dp))
                }
            }

            // DOWN
            IconButton(
                onClick = { connectionManager.sendCommand(Command.Scroll(0f, 150f)) },
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFF1E1E1E), CircleShape)
            ) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Scroll Down", tint = Color.White, modifier = Modifier.size(40.dp))
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
                    focusedBorderColor = Color(0xFF03DAC6),
                    focusedLabelColor = Color(0xFF03DAC6),
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
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
                    .background(Color(0xFF03DAC6), RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Open Tab", tint = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("OPEN TABS", fontSize = 12.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        if (tvState == null || tvState.tabs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No tabs open. Open a URL above!", color = Color.Gray)
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
                            containerColor = if (isActive) Color(0xFF2E2E2E) else Color(0xFF1E1E1E)
                        ),
                        border = if (isActive) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF03DAC6)) else null
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
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = tab.url,
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { connectionManager.sendCommand(Command.CloseTab(index)) }
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Close Tab", tint = Color.LightGray)
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = textInput,
            onValueChange = { textInput = it },
            placeholder = { Text("Type text on TV...", color = Color.Gray, fontSize = 14.sp) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF121212),
                unfocusedContainerColor = Color(0xFF121212),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Button(
            onClick = {
                if (textInput.isNotBlank()) {
                    connectionManager.sendCommand(Command.SendText(textInput))
                    textInput = ""
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC6))
        ) {
            Text("Send", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}
