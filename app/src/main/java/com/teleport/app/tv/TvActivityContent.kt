package com.teleport.app.tv

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
    val connectionUrl = "http://$localIp:8080/remote"

    // Listen for incoming commands in the event bus and route them to tabManager
    LaunchedEffect(Unit) {
        TvEventBus.commands.collectLatest { clientCommand ->
            val clientId = clientCommand.clientId
            val command = clientCommand.command
            Log.d(TAG, "Executing command from $clientId: $command")
            when (command) {
                is Command.OpenUrl -> tabManager.openTab(command.url)
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212)
    ) {
        if (isMirroring) {
            MirrorPlayerScreen(tabManager)
        } else if (clientConnected) {
            BrowserScreen(tabManager)
        } else {
            PairingScreen(connectionUrl, localIp)
        }
    }
}

@Composable
fun PairingScreen(connectionUrl: String, localIp: String) {
    val qrBitmap = remember(connectionUrl) { generateQrCodeBitmap(connectionUrl) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "TelePort TV",
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Connect your mobile remote to start browsing",
            fontSize = 18.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(32.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "Pairing QR Code",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(Color.White)
                        .padding(8.dp)
                )
            } else {
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }

            Spacer(modifier = Modifier.width(32.dp))

            Column {
                Text(
                    text = "1. Connect your phone to the same Wi-Fi network",
                    fontSize = 16.sp,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "2. Open the TelePort Mobile App",
                    fontSize = 16.sp,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "3. Scan the QR code or connect to IP:",
                    fontSize = 16.sp,
                    color = Color.LightGray
                )
                Text(
                    text = localIp,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E676)
                )
            }
        }
    }
}

@Composable
fun BrowserScreen(tabManager: TabManager) {
    val tabs by tabManager.tabs.collectAsState()
    val activeIndex by tabManager.activeTabIndex.collectAsState()
    val cursors by tabManager.cursors.collectAsState()

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
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
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
                TvEventBus.mirrorFrames.collect { frameData ->
                    // Feed input buffer
                    val inputBufferIndex = codec.dequeueInputBuffer(10000L) // 10ms timeout
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
                    }

                    // Render output buffer
                    var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000L)
                    while (outputBufferIndex >= 0) {
                        codec.releaseOutputBuffer(outputBufferIndex, true)
                        outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0L)
                    }
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
