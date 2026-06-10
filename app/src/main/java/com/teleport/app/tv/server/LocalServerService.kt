package com.teleport.app.tv.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.teleport.app.protocol.Command
import com.teleport.app.protocol.TvState
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.install
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.routing.routing
import io.ktor.server.routing.get
import io.ktor.server.response.respondText
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.readBytes
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.teleport.app.ui.theme.ThemeTokens
import java.util.UUID

class LocalServerService : Service() {
    private val TAG = "LocalServerService"
    private val PORT = ThemeTokens.PORT

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private var nsdPublisher: NsdPublisher? = null

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
        startServer()
        registerNsd()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "teleport_server_channel"
        val channelName = "TelePort Server Background Service"
        
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("TelePort Server Active")
            .setContentText("Listening for mobile controllers on port $PORT...")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(1, notification)
        }
    }

    private fun startServer() {
        serviceScope.launch {
            try {
                server = embeddedServer(Netty, port = PORT) {
                    install(WebSockets) {
                        contentConverter = KotlinxWebsocketSerializationConverter(Json)
                    }
                    routing {
                        get("/remote") {
                            call.respondText(REMOTE_HTML, io.ktor.http.ContentType.Text.Html)
                        }
                        
                        webSocket("/control") {
                            val origin = call.request.headers["Origin"]
                            val host = call.request.headers["Host"]
                            if (origin != null && host != null && origin != "http://$host" && origin != "https://$host") {
                                Log.w(TAG, "CSWSH prevented: Rejected control connection from invalid Origin: $origin for Host: $host")
                                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "CSWSH Check Failed"))
                                return@webSocket
                            }

                            val clientId = UUID.randomUUID().toString()
                            Log.d(TAG, "Client connected via WebSocket: $clientId")

                            val deviceName = call.request.queryParameters["device"] ?: "Mobile Remote"
                            TvEventBus.postPendingConnection(clientId, deviceName)

                            // Bring MainActivity to the foreground automatically on client connection
                            try {
                                val launchIntent = Intent(this@LocalServerService, com.teleport.app.MainActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                startActivity(launchIntent)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to start MainActivity on connection", e)
                            }

                            // Wait for TV user confirmation (approval or denial)
                            var isApproved = false
                            try {
                                combine(
                                    TvEventBus.approvedClientIds,
                                    TvEventBus.pendingRequests
                                ) { approvedIds, pendingReqs ->
                                    if (clientId in approvedIds) {
                                        isApproved = true
                                        true
                                    } else if (pendingReqs.none { it.clientId == clientId }) {
                                        isApproved = false
                                        true
                                    } else {
                                        false
                                    }
                                }.first { it }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error waiting for connection confirmation", e)
                            }

                            if (!isApproved) {
                                Log.w(TAG, "Client connection denied by TV user: $clientId")
                                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Connection Denied"))
                                return@webSocket
                            }

                            TvEventBus.registerClient(clientId)

                            // Launch a separate coroutine to push state updates to this connection
                            val stateJob = launch {
                                TvEventBus.tvState.collectLatest { state ->
                                    if (state != null) {
                                        try {
                                            val jsonString = json.encodeToString(state)
                                            send(Frame.Text(jsonString))
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Error sending state update to client", e)
                                        }
                                    }
                                }
                            }

                            try {
                                for (frame in incoming) {
                                    if (frame is Frame.Text) {
                                        val text = frame.readText()
                                        try {
                                            val command = json.decodeFromString<Command>(text)
                                            TvEventBus.postCommand(clientId, command)
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Error parsing command: $text", e)
                                        }
                                    } else if (frame is Frame.Binary) {
                                        val data = frame.readBytes()
                                        if (data.size >= 9) {
                                            val type = data[0].toInt()
                                            val buffer = java.nio.ByteBuffer.wrap(data, 1, 8)
                                            val dx = buffer.float
                                            val dy = buffer.float
                                            val command = when (type) {
                                                0x01 -> Command.MoveCursor(dx, dy)
                                                0x02 -> Command.Scroll(dx, dy)
                                                else -> null
                                            }
                                            if (command != null) {
                                                TvEventBus.postCommand(clientId, command)
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "WebSocket exception in active connection", e)
                            } finally {
                                stateJob.cancel()
                                TvEventBus.unregisterClient(clientId)
                                Log.d(TAG, "Client disconnected: $clientId")
                            }
                        }
                        
                        webSocket("/mirror") {
                            val origin = call.request.headers["Origin"]
                            val host = call.request.headers["Host"]
                            if (origin != null && host != null && origin != "http://$host" && origin != "https://$host") {
                                Log.w(TAG, "CSWSH prevented: Rejected mirror connection from invalid Origin: $origin for Host: $host")
                                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "CSWSH Check Failed"))
                                return@webSocket
                            }

                            Log.d(TAG, "Mirror socket connected")
                            try {
                                for (frame in incoming) {
                                    if (frame is Frame.Binary) {
                                        val data = frame.readBytes()
                                        TvEventBus.postMirrorFrame(data)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in mirror WebSocket session", e)
                            } finally {
                                Log.d(TAG, "Mirror socket disconnected")
                            }
                        }
                    }
                }
                server?.start(wait = true)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start Ktor server", e)
            }
        }
    }

    private fun registerNsd() {
        nsdPublisher = NsdPublisher(this)
        nsdPublisher?.registerService(PORT)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Stopping server and unregistering NSD service...")
        nsdPublisher?.unregisterService()
        serviceScope.launch {
            server?.stop(1000, 2000)
        }
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

private val REMOTE_HTML = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="Content-Security-Policy" content="default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; connect-src 'self' ws: wss:;">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>TelePort Web Remote</title>
    <style>
        /* CSS_THEME_TOKENS_START */
        :root {
            --bg-color: #0d0d11;
            --primary-color: #7928ca;
            --accent-color: #00dfd8;
            --card-bg: #1a1a24;
            --border-color: #1f1f2a;
        }
        /* CSS_THEME_TOKENS_END */
        body {
            margin: 0;
            padding: 0;
            background-color: var(--bg-color);
            color: #ffffff;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            overflow: hidden;
            user-select: none;
            -webkit-user-select: none;
        }
        .container {
            display: flex;
            flex-direction: column;
            height: 100vh;
            justify-content: space-between;
        }
        .header {
            background-color: var(--card-bg);
            padding: 16px;
            text-align: center;
            font-weight: bold;
            font-size: 18px;
            border-bottom: 1px solid var(--border-color);
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.3);
        }
        .tab-bar {
            display: flex;
            background-color: var(--card-bg);
            border-bottom: 1px solid var(--border-color);
        }
        .tab {
            flex: 1;
            padding: 12px;
            text-align: center;
            font-weight: bold;
            font-size: 14px;
            color: #888888;
            cursor: pointer;
            border-bottom: 2px solid transparent;
            transition: all 0.2s ease;
        }
        .tab.active {
            color: var(--accent-color);
            border-bottom: 2px solid var(--accent-color);
        }
        .tab-content {
            flex: 1;
            display: none;
            flex-direction: column;
            padding: 16px;
            box-sizing: border-box;
            height: calc(100vh - 180px);
        }
        .tab-content.active {
            display: flex;
        }
        /* Trackpad styling */
        #trackpad {
            flex: 1;
            background: radial-gradient(circle, #252525 0%, #1e1e1e 100%);
            border-radius: 24px;
            border: 2px dashed #3d3d3d;
            display: flex;
            align-items: center;
            justify-content: center;
            text-align: center;
            color: #888888;
            font-weight: bold;
            font-size: 16px;
            touch-action: none;
            margin-bottom: 16px;
            box-shadow: inset 0 4px 10px rgba(0,0,0,0.5);
        }
        .scroll-row {
            display: flex;
            gap: 16px;
        }
        .btn-scroll {
            flex: 1;
            background-color: #2e2e2e;
            color: white;
            border: none;
            padding: 14px;
            border-radius: 12px;
            font-weight: bold;
            font-size: 14px;
            cursor: pointer;
        }
        .btn-scroll:active {
            background-color: #3e3e3e;
        }
        /* D-Pad styling */
        .dpad-container {
            flex: 1;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
        }
        .dpad-row {
            display: flex;
            justify-content: center;
            align-items: center;
        }
        .btn-dpad {
            width: 80px;
            height: 80px;
            background-color: #1e1e1e;
            border: none;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            margin: 10px;
            cursor: pointer;
            box-shadow: 0 4px 8px rgba(0,0,0,0.4);
        }
        .btn-dpad:active {
            background-color: #2d2d2d;
        }
        .btn-dpad svg {
            fill: #ffffff;
            width: 32px;
            height: 32px;
        }
        .btn-center {
            width: 90px;
            height: 90px;
            background-color: var(--primary-color);
            color: white;
            font-weight: bold;
            font-size: 18px;
            border-radius: 50%;
            border: none;
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            box-shadow: 0 4px 12px rgba(121, 40, 202, 0.4);
        }
        .btn-center:active {
            background-color: #903dfa;
        }
        /* Input bar styling */
        .input-bar {
            background-color: var(--card-bg);
            padding: 12px 16px;
            display: flex;
            align-items: center;
            border-top: 1px solid var(--border-color);
        }
        .input-field {
            flex: 1;
            background-color: var(--bg-color);
            border: 1px solid var(--border-color);
            border-radius: 8px;
            color: white;
            padding: 12px;
            font-size: 14px;
            outline: none;
        }
        .btn-send {
            background-color: var(--accent-color);
            color: #000000;
            border: none;
            border-radius: 8px;
            padding: 12px 18px;
            margin-left: 12px;
            font-weight: bold;
            cursor: pointer;
        }
        /* Tabs panel */
        .url-box {
            display: flex;
            margin-bottom: 16px;
        }
        .url-input {
            flex: 1;
            background-color: var(--card-bg);
            border: 1px solid var(--border-color);
            border-radius: 12px;
            color: white;
            padding: 14px;
            font-size: 14px;
            outline: none;
        }
        .btn-add {
            background-color: var(--accent-color);
            color: black;
            border: none;
            border-radius: 12px;
            width: 50px;
            height: 50px;
            margin-left: 12px;
            font-size: 24px;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .tabs-list {
            flex: 1;
            overflow-y: auto;
        }
        .tab-item {
            background-color: var(--card-bg);
            border-radius: 12px;
            padding: 12px 16px;
            margin-bottom: 8px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            border: 1.5px solid transparent;
        }
        .tab-item.active {
            border: 1.5px solid var(--accent-color);
            background-color: #2e2e2e;
        }
        .tab-info {
            flex: 1;
            cursor: pointer;
        }
        .tab-title {
            font-weight: bold;
            font-size: 14px;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }
        .tab-url {
            color: #888888;
            font-size: 11px;
            margin-top: 4px;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }
        .btn-close {
            background: none;
            border: none;
            color: #888888;
            cursor: pointer;
            font-size: 16px;
        }
        .btn-close:hover {
            color: #ff3b30;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">TelePort Web Remote</div>
        
        <div class="tab-bar">
            <div class="tab active" onclick="switchTab(0)">Trackpad</div>
            <div class="tab" onclick="switchTab(1)">D-Pad</div>
            <div class="tab" onclick="switchTab(2)">Tabs</div>
        </div>

        <!-- Trackpad Panel -->
        <div id="content-trackpad" class="tab-content active">
            <div id="trackpad">
                TRACKPAD<br><span style="font-size: 12px; color: #666; font-weight: normal; margin-top: 8px; display: inline-block;">Drag to move. Tap to click.<br>Drag two fingers to scroll.</span>
            </div>
            <div class="scroll-row">
                <button class="btn-scroll" onclick="sendScroll(0, -300)">Scroll Up</button>
                <button class="btn-scroll" onclick="sendScroll(0, 300)">Scroll Down</button>
            </div>
        </div>

        <!-- D-Pad Panel -->
        <div id="content-dpad" class="tab-content">
            <div style="display: flex; justify-content: space-between; width: 100%; margin-bottom: 24px;">
                <button class="btn-scroll" onclick="sendCommand({type:'com.teleport.app.protocol.Command.GoBack'})">Back</button>
                <button class="btn-scroll" style="background-color: #03dac6; color: black;" onclick="sendCommand({type:'com.teleport.app.protocol.Command.PlayPause'})">Play/Pause</button>
            </div>
            <div class="dpad-container">
                <div class="dpad-row">
                    <button class="btn-dpad" onclick="sendScroll(0, -150)">
                        <svg viewBox="0 0 24 24"><path d="M7.41 15.41L12 10.83l4.59 4.58L18 14l-6-6-6 6z"/></svg>
                    </button>
                </div>
                <div class="dpad-row">
                    <button class="btn-dpad" onclick="sendScroll(-150, 0)">
                        <svg viewBox="0 0 24 24"><path d="M15.41 16.59L10.83 12l4.58-4.59L14 6l-6 6 6 6z"/></svg>
                    </button>
                    <button class="btn-center" onclick="sendClick()">OK</button>
                    <button class="btn-dpad" onclick="sendScroll(150, 0)">
                        <svg viewBox="0 0 24 24"><path d="M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6z"/></svg>
                    </button>
                </div>
                <div class="dpad-row">
                    <button class="btn-dpad" onclick="sendScroll(0, 150)">
                        <svg viewBox="0 0 24 24"><path d="M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6z"/></svg>
                    </button>
                </div>
            </div>
        </div>

        <!-- Tabs Panel -->
        <div id="content-tabs" class="tab-content">
            <div class="url-box">
                <input id="urlInput" class="url-input" type="url" placeholder="Enter URL (https://...)" onkeydown="if(event.key==='Enter') openTab()">
                <button class="btn-add" onclick="openTab()">+</button>
            </div>
            <div id="tabsList" class="tabs-list">
                <!-- Discovered tabs load here -->
            </div>
        </div>

        <!-- Keyboard input bar -->
        <div class="input-bar">
            <input id="keyboardInput" class="input-field" type="text" placeholder="Type text on TV..." onkeydown="if(event.key==='Enter') sendKeyboardText()">
            <button class="btn-send" onclick="sendKeyboardText()">Send</button>
        </div>
    </div>

    <script>
        let ws;
        let tvState = null;
        const trackpad = document.getElementById('trackpad');
        
        let lastTouches = [];
        let isTwoFingerScroll = false;
        
        function connect() {
            const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
            const host = window.location.host;
            ws = new WebSocket(proto + '//' + host + '/control');
            
            ws.onopen = () => {
                console.log('Connected to TelePort TV');
            };
            
            ws.onmessage = (event) => {
                try {
                    const state = JSON.parse(event.data);
                    if (state && state.tabs) {
                        tvState = state;
                        renderTabs();
                    }
                } catch (e) {
                    console.error('Error parsing TV state:', e);
                }
            };
            
            ws.onclose = () => {
                console.log('Disconnected. Reconnecting in 2s...');
                setTimeout(connect, 2000);
            };
        }

        function sendCommand(cmd) {
            if (ws && ws.readyState === WebSocket.OPEN) {
                ws.send(JSON.stringify(cmd));
            }
        }

        // Gesture handling
        trackpad.addEventListener('touchstart', (e) => {
            lastTouches = Array.from(e.touches).map(t => ({ id: t.identifier, x: t.clientX, y: t.clientY }));
            isTwoFingerScroll = e.touches.length === 2;
        });

        trackpad.addEventListener('touchmove', (e) => {
            e.preventDefault();
            const currentTouches = Array.from(e.touches).map(t => ({ id: t.identifier, x: t.clientX, y: t.clientY }));
            
            if (e.touches.length === 1 && !isTwoFingerScroll) {
                // Single finger drag -> Move cursor
                const prev = lastTouches.find(t => t.id === currentTouches[0].id);
                if (prev) {
                    const dx = currentTouches[0].x - prev.x;
                    const dy = currentTouches[0].y - prev.y;
                    sendCommand({
                        type: 'com.teleport.app.protocol.Command.MoveCursor',
                        dx: dx,
                        dy: dy
                    });
                }
            } else if (e.touches.length === 2 && isTwoFingerScroll) {
                // Two fingers scroll -> Natural Scroll
                if (lastTouches.length >= 2) {
                    const prev1 = lastTouches.find(t => t.id === currentTouches[0].id);
                    const prev2 = lastTouches.find(t => t.id === currentTouches[1].id);
                    if (prev1 && prev2) {
                        const dy1 = currentTouches[0].y - prev1.y;
                        const dy2 = currentTouches[1].y - prev2.y;
                        const averageDy = (dy1 + dy2) / 2;
                        
                        const dx1 = currentTouches[0].x - prev1.x;
                        const dx2 = currentTouches[1].x - prev2.x;
                        const averageDx = (dx1 + dx2) / 2;
                        
                        sendCommand({
                            type: 'com.teleport.app.protocol.Command.Scroll',
                            dx: -averageDx * 2,
                            dy: -averageDy * 2
                        });
                    }
                }
            }
            lastTouches = currentTouches;
        }, { passive: false });

        trackpad.addEventListener('touchend', (e) => {
            if (e.touches.length === 0) {
                // Check for tap
                if (lastTouches.length === 1 && !isTwoFingerScroll) {
                    sendClick();
                }
                isTwoFingerScroll = false;
            }
            lastTouches = Array.from(e.touches).map(t => ({ id: t.identifier, x: t.clientX, y: t.clientY }));
        });

        function sendClick() {
            sendCommand({ type: 'com.teleport.app.protocol.Command.Click' });
        }

        function sendScroll(dx, dy) {
            sendCommand({ type: 'com.teleport.app.protocol.Command.Scroll', dx: dx, dy: dy });
        }

        function sendKeyboardText() {
            const input = document.getElementById('keyboardInput');
            if (input.value.trim() !== '') {
                sendCommand({
                    type: 'com.teleport.app.protocol.Command.SendText',
                    text: input.value
                });
                input.value = '';
            }
        }

        function openTab() {
            const input = document.getElementById('urlInput');
            let url = input.value.trim();
            if (url !== '') {
                if (!url.startsWith('http://') && !url.startsWith('https://')) {
                    url = 'https://' + url;
                }
                sendCommand({
                    type: 'com.teleport.app.protocol.Command.OpenUrl',
                    url: url
                });
                input.value = '';
            }
        }

        function switchTab(index) {
            document.querySelectorAll('.tab').forEach((tab, idx) => {
                if (idx === index) tab.classList.add('active');
                else tab.classList.remove('active');
            });
            document.querySelectorAll('.tab-content').forEach((content, idx) => {
                if (idx === index) content.classList.add('active');
                else content.classList.remove('active');
            });
        }

        function escapeHtml(unsafe) {
            if (!unsafe) return '';
            return String(unsafe)
                 .replace(/&/g, "&amp;")
                 .replace(/</g, "&lt;")
                 .replace(/>/g, "&gt;")
                 .replace(/"/g, "&quot;")
                 .replace(/'/g, "&#039;");
        }

        function renderTabs() {
            const list = document.getElementById('tabsList');
            list.innerHTML = '';
            if (tvState && tvState.tabs) {
                tvState.tabs.forEach((tab, index) => {
                    const isActive = index === tvState.activeTabIndex;
                    const item = document.createElement('div');
                    item.className = 'tab-item' + (isActive ? ' active' : '');
                    
                    item.innerHTML = `
                        <div class="tab-info" onclick="sendCommand({type:'com.teleport.app.protocol.Command.SelectTab', index:${'$'}{index}})">
                            <div class="tab-title">${'$'}{escapeHtml(tab.title || 'Loading...')}</div>
                            <div class="tab-url">${'$'}{escapeHtml(tab.url)}</div>
                        </div>
                        <button class="btn-close" onclick="sendCommand({type:'com.teleport.app.protocol.Command.CloseTab', index:${'$'}{index}})">×</button>
                    `;
                    list.appendChild(item);
                });
            }
        }

        connect();
    </script>
</body>
</html>
"""
