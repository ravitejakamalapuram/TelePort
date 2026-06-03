package com.teleport.app

import android.app.Application
import android.os.Build
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.teleport.app.ads.AdManager

class TelePortApp : Application() {
    private val TAG = "TelePortApp"

    override fun onCreate() {
        super.onCreate()
        initCrashlytics()
        initAds()
    }

    private fun initCrashlytics() {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()

            // Set custom keys for better crash context
            crashlytics.setCustomKey("device_model", Build.MODEL)
            crashlytics.setCustomKey("device_manufacturer", Build.MANUFACTURER)
            crashlytics.setCustomKey("os_version", Build.VERSION.RELEASE)
            crashlytics.setCustomKey("sdk_int", Build.VERSION.SDK_INT)
            crashlytics.setCustomKey("device_brand", Build.BRAND)
            crashlytics.setCustomKey("device_product", Build.PRODUCT)

            // Detect if running on TV or Mobile
            val isTv = packageManager.hasSystemFeature("android.software.leanback")
            crashlytics.setCustomKey("form_factor", if (isTv) "TV" else "Mobile")

            // Set up a global uncaught exception handler as a safety net
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                Log.e(TAG, "Uncaught exception on thread ${thread.name}", throwable)
                // Record to Crashlytics before passing to default handler
                try {
                    crashlytics.recordException(throwable)
                } catch (e: Exception) {
                    // Crashlytics itself failed, nothing more we can do
                }
                // Pass to the default handler (which includes Crashlytics' own handler)
                defaultHandler?.uncaughtException(thread, throwable)
            }

            Log.i(TAG, "Firebase Crashlytics initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase Crashlytics", e)
        }
    }

    private fun initAds() {
        // AdMob is initialized later via ConsentHelper, after consent is obtained.
        // Pre-warm by just logging readiness here.
        Log.i(TAG, "Ad infrastructure ready, consent will be requested on MainActivity launch")
    }
}
