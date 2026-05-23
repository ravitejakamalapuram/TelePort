package com.teleport.app.tv.server

import android.app.UiModeManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    private val TAG = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (isTvDevice(context)) {
                Log.d(TAG, "Starting LocalServerService on TV boot")
                val serviceIntent = Intent(context, LocalServerService::class.java)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start LocalServerService on boot", e)
                }
            } else {
                Log.d(TAG, "Not a TV device, skipping boot startup")
            }
        }
    }

    private fun isTvDevice(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isTelevisionMode = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        val hasLeanbackFeature = context.packageManager.hasSystemFeature("android.software.leanback")
        return isTelevisionMode || hasLeanbackFeature
    }
}
