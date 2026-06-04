package com.teleport.app // Note: Adjust this package to match your target project's package directory

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.robolectric.Robolectric
import org.robolectric.shadows.ShadowLooper
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * ScreenshotEngine: A reusable, framework-agnostic screenshot generator for Jetpack Compose UIs.
 * Uses Robolectric NATIVE graphics mode to render layouts to target bitmaps headlessly.
 */
object ScreenshotEngine {

    enum class DeviceType {
        MOBILE, TABLET, TV
    }

    data class DeviceConfig(
        val width: Int,
        val height: Int,
        val densityDpi: Int,
        val type: DeviceType
    ) {
        companion object {
            val Phone = DeviceConfig(1080, 2400, 480, DeviceType.MOBILE)
            val Tablet = DeviceConfig(2560, 1600, 320, DeviceType.TABLET)
            val Tv = DeviceConfig(1920, 1080, 240, DeviceType.TV)
        }
    }

    data class DecorationConfig(
        val title: String,
        val description: String,
        val backgroundGradientColors: List<Int> = listOf(0xFF0F0C20.toInt(), 0xFF15102A.toInt()),
        val textColor: Int = 0xFFFFFFFF.toInt(),
        val descColor: Int = 0xFF9EA2B0.toInt()
    )

    /**
     * Captures a Compose UI layout headlessly, scales it down, and saves it to a PNG file.
     * Optionally wraps it inside a gorgeous device bezel/canvas with customizable titles.
     */
    fun capture(
        name: String,
        device: DeviceConfig,
        isDarkMode: Boolean = true,
        localeCode: String = "en",
        decoration: DecorationConfig? = null,
        content: @Composable () -> Unit
    ) {
        // Apply locale configurations to runtime context
        val locale = Locale(localeCode)
        Locale.setDefault(locale)
        val resources = org.robolectric.RuntimeEnvironment.getApplication().resources
        val config = resources.configuration
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)

        // Build Activity and set content
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            MaterialTheme {
                Surface(color = if (isDarkMode) Color(0xFF0D0D11) else Color.White) {
                    content()
                }
            }
        }

        val contentView = activity.findViewById<ViewGroup>(android.R.id.content)
        
        // Measure and layout screen
        contentView.measure(
            View.MeasureSpec.makeMeasureSpec(device.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(device.height, View.MeasureSpec.EXACTLY)
        )
        contentView.layout(0, 0, device.width, device.height)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        val rawBitmap = Bitmap.createBitmap(device.width, device.height, Bitmap.Config.ARGB_8888)
        val rawCanvas = Canvas(rawBitmap)
        contentView.draw(rawCanvas)

        val finalBitmap = if (decoration != null) {
            decorate(rawBitmap, device, decoration)
        } else {
            rawBitmap
        }

        // Scale down to prevent git repository bloat
        // TV is 1920x1080 -> scaled to 0.5 = 960x540
        // Phone is 1080x2400 (or decorated 1440x2560) -> scaled to 0.333 = 480x853
        val scale = if (device.width > device.height) 0.5f else 0.333f
        val targetWidth = (finalBitmap.width * scale).toInt()
        val targetHeight = (finalBitmap.height * scale).toInt()
        val scaledBitmap = Bitmap.createScaledBitmap(finalBitmap, targetWidth, targetHeight, true)

        // Save image to output directory
        val projectRoot = findProjectRoot(File("."))
        val screenshotsDir = File(projectRoot, "docs/screenshots/current")
        screenshotsDir.mkdirs()
        
        val file = File(screenshotsDir, name)
        FileOutputStream(file).use { out ->
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
        }
        println("Generated screenshot: ${file.absolutePath}")
    }

    private fun findProjectRoot(current: File): File {
        var dir = current.absoluteFile
        while (true) {
            if (File(dir, "settings.gradle").exists() || File(dir, "settings.gradle.kts").exists()) {
                return dir
            }
            val parent = dir.parentFile ?: break
            dir = parent
        }
        return current
    }

    /**
     * Places the raw screenshot onto a styled device mockup frame with background and marketing text.
     */
    private fun decorate(
        rawBitmap: Bitmap,
        device: DeviceConfig,
        decoration: DecorationConfig
    ): Bitmap {
        val canvasWidth = if (device.type == DeviceType.TV) 1920 else 1440
        val canvasHeight = if (device.type == DeviceType.TV) 1080 else 2560
        val decorated = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(decorated)

        // Draw background gradient / solid fill
        val backgroundPaint = Paint().apply {
            color = decoration.backgroundGradientColors[0]
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat(), backgroundPaint)

        // Draw Title text
        val titlePaint = Paint().apply {
            color = decoration.textColor
            textSize = if (device.type == DeviceType.TV) 48f else 72f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        
        // Draw Description text
        val descPaint = Paint().apply {
            color = decoration.descColor
            textSize = if (device.type == DeviceType.TV) 28f else 40f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val textYOffset = if (device.type == DeviceType.TV) 120f else 200f
        canvas.drawText(decoration.title, (canvasWidth / 2).toFloat(), textYOffset, titlePaint)
        canvas.drawText(decoration.description, (canvasWidth / 2).toFloat(), textYOffset + (if (device.type == DeviceType.TV) 50f else 80f), descPaint)

        // Calculate device boundaries
        val frameRect = if (device.type == DeviceType.TV) {
            RectF(240f, 250f, 1680f, 1000f) // TV widescreen frame
        } else {
            RectF(180f, 380f, 1260f, 2400f) // Phone vertical frame
        }

        // Draw sleek metallic/dark bezels
        val borderPaint = Paint().apply {
            color = AndroidColor.DKGRAY
            style = Paint.Style.STROKE
            strokeWidth = if (device.type == DeviceType.TV) 16f else 28f
            isAntiAlias = true
        }
        val innerBorderPaint = Paint().apply {
            color = AndroidColor.BLACK
            style = Paint.Style.STROKE
            strokeWidth = if (device.type == DeviceType.TV) 4f else 8f
            isAntiAlias = true
        }

        val rx = if (device.type == DeviceType.TV) 30f else 80f
        val ry = if (device.type == DeviceType.TV) 30f else 80f

        canvas.drawRoundRect(frameRect, rx, ry, borderPaint)
        canvas.drawRoundRect(frameRect, rx, ry, innerBorderPaint)

        // Clip and draw screen content image
        canvas.save()
        val clipPath = android.graphics.Path().apply {
            addRoundRect(frameRect, rx, ry, android.graphics.Path.Direction.CW)
        }
        canvas.clipPath(clipPath)
        canvas.drawBitmap(rawBitmap, null, frameRect, Paint(Paint.FILTER_BITMAP_FLAG))
        canvas.restore()

        return decorated
    }
}
