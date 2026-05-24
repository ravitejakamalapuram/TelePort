package com.teleport.app.mobile.mirror

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import java.nio.ByteBuffer

class ScreenCastService : Service() {
    companion object {
        val isCasting = kotlinx.coroutines.flow.MutableStateFlow(false)
    }

    private val TAG = "ScreenCastService"
    private val CHANNEL_ID = "screen_cast_channel"

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var encoder: MediaCodec? = null
    private var webSocket: WebSocket? = null
    private val okHttpClient = OkHttpClient()

    private var isRunning = false
    private var encodeThread: Thread? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val action = intent.action
        if (action == "STOP") {
            stopCast()
            stopSelf()
            return START_NOT_STICKY
        }

        val resultCode = intent.getIntExtra("RESULT_CODE", -1)
        val resultData = intent.getParcelableExtra<Intent>("RESULT_DATA")
        val tvIp = intent.getStringExtra("TV_IP") ?: ""

        if (resultCode == -1 || resultData == null || tvIp.isBlank()) {
            Log.e(TAG, "Invalid parameters for ScreenCastService")
            stopSelf()
            return START_NOT_STICKY
        }

        // Start Foreground Service
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(100, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(100, notification)
        }

        startCast(resultCode, resultData, tvIp)
        return START_STICKY
    }

    private fun startCast(resultCode: Int, resultData: Intent, tvIp: String) {
        if (isRunning) return
        isRunning = true
        isCasting.value = true

        // Initialize WebSocket connection
        val wsUrl = "ws://$tvIp:8080/mirror"
        Log.d(TAG, "Connecting mirror stream to $wsUrl")
        val request = Request.Builder().url(wsUrl).build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                Log.d(TAG, "Mirror socket connected successfully")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                Log.e(TAG, "Mirror socket connection failure: ${t.message}", t)
                stopSelf()
            }
        })

        // Initialize MediaProjection
        val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mpManager.getMediaProjection(resultCode, resultData)

        // Setup Encoder and Thread
        encodeThread = Thread {
            try {
                setupEncoderAndVirtualDisplay()
                performEncodingLoop()
            } catch (e: Exception) {
                Log.e(TAG, "Exception in casting thread", e)
            } finally {
                cleanup()
            }
        }.apply { start() }
    }

    private fun setupEncoderAndVirtualDisplay() {
        val width = 960
        val height = 540
        val fps = 24
        val bitrate = 1_500_000 // 1.5 Mbps

        val format = MediaFormat.createVideoFormat("video/avc", width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // 1 second keyframes
        }

        encoder = MediaCodec.createEncoderByType("video/avc").apply {
            configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = createInputSurface()
            start()

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "TelePortMirror",
                width,
                height,
                160, // DPI
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                inputSurface,
                null,
                null
            )
        }
    }

    private fun performEncodingLoop() {
        val codec = encoder ?: return
        val bufferInfo = MediaCodec.BufferInfo()

        while (isRunning) {
            val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000L) // 10ms timeout
            if (outputBufferIndex >= 0) {
                val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                if (outputBuffer != null && bufferInfo.size > 0) {
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)

                    val data = ByteArray(bufferInfo.size)
                    outputBuffer.get(data)

                    // Send NAL frames as binary WebSocket frames
                    webSocket?.send(data.toByteString(0, bufferInfo.size))
                }
                codec.releaseOutputBuffer(outputBufferIndex, false)
            } else if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // No frame available yet
            }
        }
    }

    private fun stopCast() {
        isRunning = false
        encodeThread?.interrupt()
        encodeThread = null
    }

    private fun cleanup() {
        isCasting.value = false
        try {
            virtualDisplay?.release()
        } catch (e: Exception) {}
        try {
            encoder?.stop()
            encoder?.release()
        } catch (e: Exception) {}
        try {
            mediaProjection?.stop()
        } catch (e: Exception) {}
        try {
            webSocket?.close(1000, "Casting stopped")
        } catch (e: Exception) {}

        virtualDisplay = null
        encoder = null
        mediaProjection = null
        webSocket = null
        Log.d(TAG, "Casting resources cleaned up successfully")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Mirroring Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Casting Phone Screen")
            .setContentText("TelePort is mirroring your phone screen to the TV.")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCast()
        cleanup()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
