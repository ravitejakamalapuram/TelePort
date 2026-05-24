package com.teleport.app.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class UpdateInfo(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val updateUrl: String,
    val releaseNotes: String,
    val isForceUpdate: Boolean
)

@Serializable
data class UpdateConfig(
    val production: UpdateInfo,
    val beta: UpdateInfo
)

class UpdateManager(private val context: Context) {
    private val TAG = "UpdateManager"
    
    // We fetch from the GitHub Pages URL of the project repository
    private val CONFIG_URL = "https://ravitejakamalapuram.github.io/TelePort/app-update.json"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client = HttpClient(OkHttp)

    /**
     * Determines the active update channel for the application.
     * 1. If the package name ends with ".beta" -> "beta"
     * 2. If it is a debuggable/development build -> "beta" (to allow developer testing)
     * 3. Otherwise -> "production"
     */
    fun getUpdateChannel(): String {
        val packageName = context.packageName
        if (packageName.endsWith(".beta")) {
            return "beta"
        }
        
        // Check if debuggable build
        val isDebuggable = try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: Exception) {
            false
        }
        
        if (isDebuggable) {
            return "beta" // Fallback to beta channel for local debug builds so we can test it
        }
        
        return "production"
    }

    /**
     * Fetches the update configuration from the remote URL and checks if a newer version is available.
     * Returns UpdateInfo if an update is available, null otherwise.
     */
    suspend fun checkForUpdates(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Checking for updates from $CONFIG_URL")
            val response = client.get(CONFIG_URL)
            val responseText = response.bodyAsText()
            Log.d(TAG, "Update config response received: $responseText")
            
            val config = json.decodeFromString<UpdateConfig>(responseText)
            val channel = getUpdateChannel()
            val remoteInfo = if (channel == "beta") config.beta else config.production
            
            val currentVersionCode = getCurrentVersionCode()
            Log.d(TAG, "Channel: $channel, Current Version Code: $currentVersionCode, Remote Version Code: ${remoteInfo.latestVersionCode}")
            
            if (remoteInfo.latestVersionCode > currentVersionCode) {
                Log.i(TAG, "New version available: ${remoteInfo.latestVersionName} (Code: ${remoteInfo.latestVersionCode})")
                return@withContext remoteInfo
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for updates", e)
        } finally {
            try {
                client.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing HttpClient", e)
            }
        }
        return@withContext null
    }

    private fun getCurrentVersionCode(): Long {
        return try {
            val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting package version code", e)
            1L
        }
    }
}
