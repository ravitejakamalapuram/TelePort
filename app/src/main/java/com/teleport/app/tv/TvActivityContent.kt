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

@Composable
fun TvActivityContent(tabManager: TabManager, localIp: String) {
    val TAG = "TvActivityContent"
    val clientConnected by TvEventBus.clientConnected.collectAsState()
    val connectionUrl = "ws://$localIp:8080/control"

    // Listen for incoming commands in the event bus and route them to tabManager
    LaunchedEffect(Unit) {
        TvEventBus.commands.collectLatest { command ->
            Log.d(TAG, "Executing command: $command")
            when (command) {
                is Command.OpenUrl -> tabManager.openTab(command.url)
                is Command.CloseTab -> tabManager.closeTab(command.index)
                is Command.SelectTab -> tabManager.selectTab(command.index)
                is Command.Scroll -> tabManager.scrollActive(command.dx, command.dy)
                is Command.MoveCursor -> tabManager.moveCursor(command.dx, command.dy)
                is Command.Click -> tabManager.clickActive()
                is Command.SendText -> tabManager.sendTextActive(command.text)
                is Command.PlayPause -> tabManager.playPauseActive()
                is Command.GoBack -> tabManager.goBackActive()
                is Command.PlayStreamNatively -> tabManager.playNatively(command.streamUrl)
                is Command.SetAirRemoteMode -> { /* Handled on sensor stream side */ }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212)
    ) {
        if (clientConnected) {
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
    val cursorX by tabManager.cursorX.collectAsState()
    val cursorY by tabManager.cursorY.collectAsState()

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

        // Draw Virtual Cursor
        Box(
            modifier = Modifier
                .offset { IntOffset(cursorX.toInt(), cursorY.toInt()) }
                .size(16.dp)
                .background(Color.Red.copy(alpha = 0.8f), CircleShape)
                .align(Alignment.TopStart)
        )
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
