package com.teleport.app.mobile.connection

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class QrScanActivity : CaptureActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun initializeContent(): DecoratedBarcodeView {
        val density = resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density).toInt()

        // Root View Container
        val rootLayout = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#0D0D11")) // Background color matching ThemeTokens
        }

        // DecoratedBarcodeView (Camera preview + scanner frame)
        val barcodeScannerView = DecoratedBarcodeView(this).apply {
            id = View.generateViewId()
            // Clear default status text to prevent duplicates
            setStatusText("")
        }
        rootLayout.addView(
            barcodeScannerView,
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        // Semi-transparent UI Overlay
        val overlayLayout = RelativeLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Header Bar Container
        val headerBar = LinearLayout(this).apply {
            id = View.generateViewId()
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#B30D0D11")) // 70% opacity dark background
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        val headerParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            addRule(RelativeLayout.ALIGN_PARENT_TOP)
        }

        // Cancel / Back Button (an X icon/text)
        val cancelButton = Button(this).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).apply {
                rightMargin = dp(12)
            }
            setOnClickListener {
                finish()
            }
        }
        headerBar.addView(cancelButton)

        // Screen Title
        val titleText = TextView(this).apply {
            text = "Scan TV QR Code"
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        headerBar.addView(titleText)
        overlayLayout.addView(headerBar, headerParams)

        // Styled Bottom Panel
        val bottomPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#E61A1A24")) // 90% opacity CardBg
            setPadding(dp(24), dp(20), dp(24), dp(20))
            
            val shape = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#E61A1A24"))
                cornerRadius = dp(16).toFloat()
            }
            background = shape
        }
        
        val promptText = TextView(this).apply {
            text = "Point your camera at the QR code displayed on your Android TV screen to connect."
            setTextColor(Color.parseColor("#FFFFFF"))
            textSize = 14f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        bottomPanel.addView(promptText)

        val subPromptText = TextView(this).apply {
            text = "Make sure both devices are on the same Wi-Fi network."
            setTextColor(Color.parseColor("#9EA2B0")) // TextSub
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        }
        bottomPanel.addView(subPromptText)

        val bottomParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            bottomMargin = dp(40)
            leftMargin = dp(24)
            rightMargin = dp(24)
        }
        overlayLayout.addView(bottomPanel, bottomParams)

        rootLayout.addView(overlayLayout)
        setContentView(rootLayout)

        return barcodeScannerView
    }
}
