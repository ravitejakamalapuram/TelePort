package com.teleport.app.tv

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import com.teleport.app.ui.theme.ThemeTokens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.teleport.app.protocol.Command
import com.teleport.app.tv.browser.TabManager
import com.teleport.app.tv.server.TvEventBus
import kotlinx.coroutines.flow.collectLatest
import android.media.MediaCodec
import android.media.MediaFormat
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.os.Build
import android.content.Intent
import android.provider.Settings
import android.webkit.WebView
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun TvActivityContent(tabManager: TabManager, localIp: String) {
    val TAG = "TvActivityContent"
    val clientConnected by TvEventBus.clientConnected.collectAsState()
    val isMirroring by tabManager.isMirroring.collectAsState()
    val tabs by tabManager.tabs.collectAsState()
    val isResolvingHeadlessly by tabManager.isResolvingHeadlessly.collectAsState()
    val resolvingUrl by tabManager.resolvingUrl.collectAsState()
    val headlessWebView by tabManager.headlessWebView.collectAsState()
    val connectionUrl = "http://$localIp:${ThemeTokens.PORT}/remote"

    // Listen for incoming commands in the event bus and route them to tabManager
    LaunchedEffect(Unit) {
        TvEventBus.commands.collectLatest { clientCommand ->
            val clientId = clientCommand.clientId
            val command = clientCommand.command
            // Bolt: Filter out high-frequency sensor commands (Cursor, Scroll) to prevent logcat spam
            // and reduce IPC/String allocation overhead on the main thread during smooth continuous motions.
            if (command !is Command.MoveCursor && command !is Command.Scroll) {
                Log.d(TAG, "Executing command from $clientId: $command")
            }
            when (command) {
                is Command.OpenUrl -> tabManager.openTab(command.url, command.headless)
                is Command.CloseTab -> tabManager.closeTab(command.index)
                is Command.SelectTab -> tabManager.selectTab(command.index)
                is Command.Scroll -> tabManager.scrollActive(command.dx, command.dy)
                is Command.MoveCursor -> tabManager.moveCursor(clientId, command.dx, command.dy)
                is Command.Click -> tabManager.clickActive(clientId)
                is Command.SendText -> tabManager.sendTextActive(command.text)
                is Command.PlayPause -> tabManager.playPauseActive()
                is Command.GoBack -> tabManager.goBackActive()
                is Command.PlayStreamNatively -> tabManager.playNatively(command.streamUrl)
                is Command.SetAirRemoteMode -> { /* Handled on sensor stream side */ }
                is Command.ToggleDarkMode -> tabManager.toggleDarkMode(command.enabled)
                is Command.StartMirroring -> tabManager.startMirroring()
                is Command.StopMirroring -> tabManager.stopMirroring()
            }
        }
    }

    val pendingRequests by TvEventBus.pendingRequests.collectAsState()
    val request = pendingRequests.firstOrNull()
    if (request != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { /* Do not dismiss on click outside */ },
            title = {
                Text(
                    text = "Connection Request",
                    color = ThemeTokens.TextMain,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Allow device \"${request.deviceName}\" to connect and control this TV?",
                    color = ThemeTokens.TextSub
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = { TvEventBus.approveClient(request.clientId) },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = ThemeTokens.Accent,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Allow")
                }
            },
            dismissButton = {
                androidx.compose.material3.Button(
                    onClick = { TvEventBus.denyClient(request.clientId) },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = ThemeTokens.TextSub
                    )
                ) {
                    Text("Deny")
                }
            },
            containerColor = ThemeTokens.CardBg,
            textContentColor = ThemeTokens.TextSub
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ThemeTokens.Background
    ) {
        if (isMirroring) {
            MirrorPlayerScreen(tabManager)
        } else if (isResolvingHeadlessly) {
            CastingScreen(resolvingUrl ?: "")
        } else if (clientConnected || tabs.isNotEmpty()) {
            BrowserScreen(tabManager)
        } else {
            PairingScreen(connectionUrl, localIp)
        }
    }

    // Hidden container for headless WebView to ensure it is active and attached to Window
    if (headlessWebView != null) {
        Box(modifier = Modifier.size(1.dp).background(Color.Transparent)) {
            AndroidView(
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(1, 1)
                        headlessWebView?.let { hwv ->
                            (hwv.parent as? ViewGroup)?.removeView(hwv)
                            addView(hwv)
                        }
                    }
                },
                update = { container ->
                    val hwv = headlessWebView
                    if (hwv != null && container.getChildAt(0) != hwv) {
                        container.removeAllViews()
                        (hwv.parent as? ViewGroup)?.removeView(hwv)
                        container.addView(hwv)
                    }
                }
            )
        }
    }
}

@Composable
fun CastingScreen(url: String) {
    val host = remember(url) {
        try {
            java.net.URI(url).host?.replace("www.", "") ?: url
        } catch (e: Exception) {
            url
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(140.dp)
        ) {
            CircularProgressIndicator(
                color = ThemeTokens.Accent,
                strokeWidth = 6.dp,
                modifier = Modifier.size(100.dp)
            )
            Text(
                text = "📺",
                fontSize = 42.sp
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Connecting to Video Stream...",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = ThemeTokens.TextMain
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Extracting video from $host",
            fontSize = 16.sp,
            color = ThemeTokens.TextSub
        )
    }
}

@Composable
fun PairingScreen(connectionUrl: String, localIp: String) {
    val qrBitmap = remember(connectionUrl) { generateQrCodeBitmap(connectionUrl) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Pairing Connection Panel (40% width)
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${ThemeTokens.APP_NAME} TV",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = ThemeTokens.TextMain
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Scan QR or enter IP to connect your remote",
                fontSize = 14.sp,
                color = ThemeTokens.TextSub,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Check if Accessibility Service is active
            val context = LocalContext.current
            val isAccessRunning = com.teleport.app.tv.server.TelePortAccessibilityService.isRunning

            if (!isAccessRunning) {
                androidx.compose.material3.Button(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("TvActivityContent", "Failed to launch accessibility settings", e)
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = ThemeTokens.CardBg,
                        contentColor = ThemeTokens.Accent
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ThemeTokens.Border),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "🪄 Enable Global Air Mouse",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "✓ Global Air Mouse Active",
                    fontSize = 12.sp,
                    color = ThemeTokens.Accent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(ThemeTokens.CardBg, RoundedCornerShape(20.dp))
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Pairing QR Code",
                        modifier = Modifier
                            .size(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ThemeTokens.TextMain)
                            .padding(6.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier.size(130.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ThemeTokens.Accent)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "1. Install & open TelePort Remote on phone",
                        fontSize = 13.sp,
                        color = ThemeTokens.TextSub,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "2. Connect phone to same Wi-Fi",
                        fontSize = 13.sp,
                        color = ThemeTokens.TextSub,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "3. Scan QR or connect to IP:",
                        fontSize = 13.sp,
                        color = ThemeTokens.TextSub,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val isDisconnected = localIp == "127.0.0.1" || localIp.isBlank()
                    val displayIp = if (isDisconnected) "No Wi-Fi Connection" else localIp
                    val ipColor = if (isDisconnected) ThemeTokens.Error else ThemeTokens.TextMain
                    val borderColor = if (isDisconnected) ThemeTokens.Error else ThemeTokens.Accent
                    val emoji = if (isDisconnected) "⚠️" else "🌐"

                    Surface(
                        color = ThemeTokens.Background,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = emoji,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = displayIp,
                                fontSize = if (isDisconnected) 15.sp else 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = ipColor
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(48.dp))

        // Right Column: Feature Showcase Panel (60% width)
        Column(
            modifier = Modifier
                .weight(1.8f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Key Features",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = ThemeTokens.TextMain
            )
            Spacer(modifier = Modifier.height(20.dp))

            // 2x2 Grid of Feature Cards
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FeatureCard(
                        emoji = "🖱️",
                        title = "Touchpad & Gestures",
                        desc = "Navigate naturally using relative trackpad drag, single-tap to click, and two-finger scroll.",
                        modifier = Modifier.weight(1f)
                    )
                    FeatureCard(
                        emoji = "🪄",
                        title = "Air Mouse Control",
                        desc = "Wave your phone around like a real laser remote. Uses phone gyroscope sensors.",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FeatureCard(
                        emoji = "🍿",
                        title = "Native Media Player",
                        desc = "Bypasses web video ads completely. Streams automatically to full-screen ExoPlayer.",
                        modifier = Modifier.weight(1f)
                    )
                    FeatureCard(
                        emoji = "📱",
                        title = "Screen Mirroring",
                        desc = "Share your phone screen or computer browser tabs onto the TV natively and latency-free.",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureCard(
    emoji: String,
    title: String,
    desc: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(ThemeTokens.CardBg, RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = ThemeTokens.Border,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = emoji,
                    fontSize = 28.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ThemeTokens.TextMain
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = desc,
                fontSize = 12.sp,
                color = ThemeTokens.TextSub,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun BrowserScreen(tabManager: TabManager) {
    val tabs by tabManager.tabs.collectAsState()
    val activeIndex by tabManager.activeTabIndex.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                tabManager.updateDimensions(coordinates.size.width, coordinates.size.height)
            }
    ) {
        if (activeIndex in tabs.indices) {
            val activeWebView = tabs[activeIndex]
            AndroidView(
                factory = {
                    FrameLayout(it).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        (activeWebView.parent as? ViewGroup)?.removeView(activeWebView)
                        addView(activeWebView)
                    }
                },
                update = { container ->
                    val currentWebView = container.getChildAt(0)
                    if (currentWebView != activeWebView) {
                        container.removeAllViews()
                        (activeWebView.parent as? ViewGroup)?.removeView(activeWebView)
                        container.addView(activeWebView)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Open a tab on your phone to start browsing",
                    fontSize = 20.sp,
                    color = Color.Gray
                )
            }
        }

        CursorOverlay(tabManager)
    }
}

// Bolt: Extracted cursor rendering to a separate Composable to strictly confine recomposition and prevent the heavy BrowserScreen (with AndroidView) from recomposing at 60Hz.
@Composable
fun CursorOverlay(tabManager: TabManager) {
    val cursors by tabManager.cursors.collectAsState()
    Box(modifier = Modifier.fillMaxSize()) {
        // Draw multiple color-coded cursors
        cursors.values.forEach { cursor ->
            val color = remember(cursor.colorHex) {
                try {
                    Color(AndroidColor.parseColor(cursor.colorHex))
                } catch (e: Exception) {
                    Color.Red
                }
            }
            Box(
                modifier = Modifier
                    .offset { IntOffset(cursor.x.toInt(), cursor.y.toInt()) }
                    .size(16.dp)
                    .background(color.copy(alpha = 0.8f), CircleShape)
                    .align(Alignment.TopStart)
            )
        }
    }
}

private fun generateQrCodeBitmap(content: String): Bitmap? {
    try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        // Optimization: Use IntArray to batch pixel updates instead of
        // hundreds of thousands of individual JNI setPixel calls.
        val pixels = IntArray(width * height)
        var offset = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                pixels[offset++] = if (bitMatrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE
            }
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    } catch (e: Exception) {
        Log.e("TvActivityContent", "Error generating QR code", e)
    }
    return null
}

@Composable
fun MirrorPlayerScreen(tabManager: TabManager) {
    val context = LocalContext.current
    val surfaceState = remember { mutableStateOf<android.view.Surface?>(null) }

    val surface = surfaceState.value
    LaunchedEffect(surface) {
        if (surface == null) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            var decoder: MediaCodec? = null
            try {
                Log.d("MirrorPlayerScreen", "Starting MediaCodec H.264 decoder")
                val codec = MediaCodec.createDecoderByType("video/avc")
                val format = MediaFormat.createVideoFormat("video/avc", 960, 540)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    format.setInteger(MediaFormat.KEY_LOW_LATENCY, 1)
                }
                codec.configure(format, surface, null, 0)
                codec.start()
                decoder = codec

                val bufferInfo = MediaCodec.BufferInfo()

                fun drainOutput(codec: MediaCodec) {
                    var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0L)
                    while (outputBufferIndex >= 0) {
                        codec.releaseOutputBuffer(outputBufferIndex, true)
                        outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0L)
                    }
                }

                TvEventBus.mirrorFrames.collect { frameData ->
                    // 1. Drain output buffers before queuing to free up slots
                    drainOutput(codec)

                    // 2. Feed input buffer with retries if backlogged
                    var inputBufferIndex = codec.dequeueInputBuffer(10000L) // 10ms timeout
                    var attempts = 0
                    while (inputBufferIndex < 0 && attempts < 3) {
                        attempts++
                        drainOutput(codec)
                        inputBufferIndex = codec.dequeueInputBuffer(5000L) // 5ms timeout
                    }

                    if (inputBufferIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                        if (inputBuffer != null) {
                            inputBuffer.clear()
                            inputBuffer.put(frameData)
                            codec.queueInputBuffer(
                                inputBufferIndex,
                                0,
                                frameData.size,
                                System.nanoTime() / 1000L,
                                0
                            )
                        }
                    } else {
                        Log.w("MirrorPlayerScreen", "Dropped frame: Decoder input buffers remain full after retries")
                    }

                    // 3. Drain output buffers again to present the new frame immediately
                    drainOutput(codec)
                }
            } catch (e: Exception) {
                Log.e("MirrorPlayerScreen", "Decoder exception: ${e.message}", e)
            } finally {
                try {
                    decoder?.stop()
                } catch (e: Exception) {}
                try {
                    decoder?.release()
                } catch (e: Exception) {}
                Log.d("MirrorPlayerScreen", "Decoder released")
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { surfaceView ->
                val holder = surfaceView.holder
                val callback = object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        surfaceState.value = holder.surface
                    }

                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        surfaceState.value = null
                    }
                }
                holder.addCallback(callback)
                // Retain reference to callback to prevent garbage collection
                surfaceView.setTag(callback)
            }
        )
    }
}
