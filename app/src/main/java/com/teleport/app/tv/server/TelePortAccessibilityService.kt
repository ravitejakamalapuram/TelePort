package com.teleport.app.tv.server

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.teleport.app.protocol.Command
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class TelePortAccessibilityService : AccessibilityService() {

    private val TAG = "TelePortAccessService"

    private var windowManager: WindowManager? = null
    private var cursorView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var cursorX = 960f
    private var cursorY = 540f
    private var screenWidth = 1920
    private var screenHeight = 1080

    companion object {
        @Volatile
        var isRunning = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        Log.d(TAG, "Accessibility Service created")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        // Fetch screen dimensions to constrain cursor movement
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            this.display
        } else {
            @Suppress("DEPRECATION")
            windowManager?.defaultDisplay
        }
        val metrics = android.util.DisplayMetrics()
        @Suppress("DEPRECATION")
        display?.getRealMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels

        cursorX = screenWidth / 2f
        cursorY = screenHeight / 2f

        createCursorOverlay()
        listenForCommands()
        listenForConnectionState()
        Log.d(TAG, "Accessibility Service connected. Screen: ${screenWidth}x${screenHeight}")
    }

    private fun createCursorOverlay() {
        if (cursorView != null) return
        cursorView = CursorOverlayView(this)

        // Performance Optimization: Create a full-screen overlay window.
        // Instead of calling windowManager.updateViewLayout (which triggers expensive system-level IPC transactions)
        // at 100-200Hz, we draw the cursor inside a local full-screen Canvas and invalidate locally.
        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }

        try {
            windowManager?.addView(cursorView, layoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add cursor view to WindowManager", e)
        }
    }

    private fun listenForConnectionState() {
        serviceScope.launch {
            // Using collect instead of collectLatest to avoid GC overhead and frame drops
            // when handling high-frequency network events.
            TvEventBus.clientConnected.collect { connected ->
                updateCursorVisibility(connected)
            }
        }
    }

    private fun updateCursorVisibility(visible: Boolean) {
        cursorView?.post {
            cursorView?.visibility = if (visible) View.VISIBLE else View.GONE
        }
    }

    private fun listenForCommands() {
        serviceScope.launch {
            // Using collect instead of collectLatest to avoid GC overhead and coroutine
            // cancellation overhead on high-frequency events like cursor movement.
            TvEventBus.commands.collect { clientCommand ->
                val command = clientCommand.command
                // When Accessibility Service is active, handle control events globally
                when (command) {
                    is Command.MoveCursor -> updateCursorPosition(command.dx, command.dy)
                    is Command.Click -> performClick()
                    is Command.Scroll -> performScroll(command.dx, command.dy)
                    is Command.GoBack -> performGlobalAction(GLOBAL_ACTION_BACK)
                    is Command.SendText -> performTextInsertion(command.text)
                    is Command.PlayPause -> {
                        // Global media controls could trigger key codes here
                    }
                    else -> { /* Other commands handled by visible TV UI */ }
                }
            }
        }
    }

    private fun updateCursorPosition(dx: Float, dy: Float) {
        cursorX = (cursorX + dx).coerceIn(0f, screenWidth.toFloat())
        cursorY = (cursorY + dy).coerceIn(0f, screenHeight.toFloat())

        cursorView?.post {
            (cursorView as? CursorOverlayView)?.updatePosition(cursorX, cursorY)
        }
    }

    private fun performClick() {
        val path = Path().apply {
            moveTo(cursorX, cursorY)
        }
        val gestureBuilder = GestureDescription.Builder()
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        gestureBuilder.addStroke(stroke)
        dispatchGesture(gestureBuilder.build(), null, null)
    }

    private fun performScroll(dx: Float, dy: Float) {
        val path = Path().apply {
            moveTo(cursorX, cursorY)
            lineTo(cursorX - dx, cursorY - dy)
        }
        val gestureBuilder = GestureDescription.Builder()
        val stroke = GestureDescription.StrokeDescription(path, 0, 150)
        gestureBuilder.addStroke(stroke)
        dispatchGesture(gestureBuilder.build(), null, null)
    }

    private fun performTextInsertion(text: String) {
        val rootNode = rootInActiveWindow ?: return
        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focusedNode != null) {
            val arguments = android.os.Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            focusedNode.recycle()
        }
        rootNode.recycle()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not capturing window changes
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility Service interrupted")
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        isRunning = false
        removeCursorOverlay()
        serviceJob.cancel()
        Log.d(TAG, "Accessibility Service unbound")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        isRunning = false
        removeCursorOverlay()
        serviceJob.cancel()
        super.onDestroy()
        Log.d(TAG, "Accessibility Service destroyed")
    }

    private fun removeCursorOverlay() {
        cursorView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove cursor view", e)
            }
        }
        cursorView = null
        layoutParams = null
    }

    // Programmatically drawn cursor that updates local position coordinates
    // and invalidates locally to bypass WindowManager layout IPC transactions
    private class CursorOverlayView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val path = Path()
        private var cx = 960f
        private var cy = 540f

        init {
            // Define standard mouse pointer shape coordinates
            path.moveTo(0f, 0f)
            path.lineTo(0f, 24f)
            path.lineTo(7f, 17f)
            path.lineTo(13f, 30f)
            path.lineTo(17f, 28f)
            path.lineTo(11f, 15f)
            path.lineTo(18f, 15f)
            path.close()
        }

        fun updatePosition(x: Float, y: Float) {
            cx = x
            cy = y
            invalidate() // Local redraw loop
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.save()
            // Translate canvas origin to the current cursor position
            canvas.translate(cx, cy)
            // Scale cursor rendering for TV screen readability
            canvas.scale(2.2f, 2.2f)

            // Draw black outline
            paint.color = android.graphics.Color.BLACK
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.5f
            canvas.drawPath(path, paint)

            // Fill with white
            paint.color = android.graphics.Color.WHITE
            paint.style = Paint.Style.FILL
            canvas.drawPath(path, paint)

            canvas.restore()
        }
    }
}
