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
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LocalServerService : Service() {
    private val TAG = "LocalServerService"
    private val PORT = 8080

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private var server: NettyApplicationEngine? = null
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

        startForeground(1, notification)
    }

    private fun startServer() {
        serviceScope.launch {
            try {
                server = embeddedServer(Netty, port = PORT) {
                    install(WebSockets) {
                        contentConverter = KotlinxWebsocketSerializationConverter(Json)
                    }
                    routing {
                        webSocket("/control") {
                            Log.d(TAG, "Client connected via WebSocket")
                            TvEventBus.setClientConnected(true)

                            // Bring MainActivity to the foreground automatically on client connection
                            try {
                                val launchIntent = Intent(this@LocalServerService, com.teleport.app.MainActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                startActivity(launchIntent)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to start MainActivity on connection", e)
                            }

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
                                            TvEventBus.postCommand(command)
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Error parsing command: $text", e)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "WebSocket exception in active connection", e)
                            } finally {
                                stateJob.cancel()
                                TvEventBus.setClientConnected(false)
                                Log.d(TAG, "Client disconnected")
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
