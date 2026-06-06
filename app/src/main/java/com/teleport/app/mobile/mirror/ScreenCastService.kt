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
            showDefaultNotificationAndStop()
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
            showDefaultNotificationAndStop()
            return START_NOT_STICKY
        }

        // Start Foreground Service with MEDIA_PROJECTION type immediately before starting casting operations
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(100, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(100, notification)
        }

        startCast(resultCode, resultData, tvIp)
        return START_NOT_STICKY
    }

    private fun showDefaultNotificationAndStop() {
        val notification = createNotification()
        try {
            startForeground(100, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error calling startForeground in error fallback", e)
        }
        stopSelf()
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
            
            // Attempt to configure Constant Bitrate (CBR) and ultra low latency
            setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setInteger(MediaFormat.KEY_LATENCY, 1)
            }
        }

        var isConfigured = false
        encoder = MediaCodec.createEncoderByType("video/avc").apply {
            try {
                configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                isConfigured = true
            } catch (e: Exception) {
                Log.w(TAG, "Failed to configure encoder with low-latency CBR, falling back to default configuration", e)
            }

            if (!isConfigured) {
                val fallbackFormat = MediaFormat.createVideoFormat("video/avc", width, height).apply {
                    setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                    setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                    setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                }
                configure(fallbackFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            }

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

    private fun updateEncoderBitrate(newBitrate: Int) {
        try {
            val params = android.os.Bundle().apply {
                putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, newBitrate)
            }
            encoder?.setParameters(params)
            Log.d(TAG, "Adaptive Bitrate changed encoder target to ${newBitrate / 1000} Kbps")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to dynamically adjust encoder bitrate", e)
        }
    }

    private fun requestSyncFrame() {
        try {
            val params = android.os.Bundle().apply {
                putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0)
            }
            encoder?.setParameters(params)
            Log.d(TAG, "Requested sync frame (I-Frame) from encoder")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to request sync frame", e)
        }
    }

    private fun performEncodingLoop() {
        val codec = encoder ?: return
        val bufferInfo = MediaCodec.BufferInfo()

        var frameCount = 0
        var currentBitrate = 1_500_000
        val minBitrate = 300_000
        val maxBitrate = 3_500_000
        var highQueueCount = 0

        while (isRunning) {
            val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000L) // 10ms timeout
            if (outputBufferIndex >= 0) {
                val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                if (outputBuffer != null && bufferInfo.size > 0) {
                    val queueSize = webSocket?.queueSize() ?: 0

                    // Evaluate adaptive bitrate every 24 frames (~1s of video)
                    frameCount++
                    if (frameCount >= 24) {
                        frameCount = 0

                        if (queueSize > 800_000) { // > 800 KB queued
                            currentBitrate = (currentBitrate * 0.6f).toInt().coerceAtLeast(minBitrate)
                            updateEncoderBitrate(currentBitrate)
                            requestSyncFrame()
                        } else if (queueSize > 300_000) { // > 300 KB queued
                            currentBitrate = (currentBitrate * 0.85f).toInt().coerceAtLeast(minBitrate)
                            updateEncoderBitrate(currentBitrate)
                        } else if (queueSize < 50_000) { // < 50 KB queued (healthy)
                            if (currentBitrate < maxBitrate) {
                                currentBitrate = (currentBitrate + 150_000).coerceAtMost(maxBitrate)
                                updateEncoderBitrate(currentBitrate)
                            }
                        }
                    }

                    // WebSocket Frame Dropping / Skipping to prevent infinite lag
                    if (queueSize > 1_500_000) { // > 1.5 MB queue size
                        highQueueCount++
                        val isKeyframe = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0
                        if (!isKeyframe || highQueueCount > 10) {
                            // Drop this output frame to avoid congesting the queue further
                            codec.releaseOutputBuffer(outputBufferIndex, false)
                            requestSyncFrame() // Request sync frame to recover immediately
                            continue
                        }
                    } else {
                        highQueueCount = 0
                    }

                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)

                    // Bolt: Avoid copying data into an intermediate ByteArray to reduce redundant
                    // memory allocations, GC pressure, and CPU copying overhead in this high-frequency loop.
                    // Send NAL frames as binary WebSocket frames
                    webSocket?.send(outputBuffer.toByteString())
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
