package com.teleport.app.tv.player

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@OptIn(UnstableApi::class)
object VideoCacheManager {
    private const val TAG = "VideoCacheManager"
    private var cache: SimpleCache? = null
    private var isFailed = false

    @Synchronized
    fun getCache(context: Context): SimpleCache? {
        if (isFailed) return null
        if (cache == null) {
            try {
                val cacheDir = File(context.cacheDir, "video_cache")
                val cacheSize = 100 * 1024 * 1024L // 100 MB max cache size
                val evictor = LeastRecentlyUsedCacheEvictor(cacheSize)
                val databaseProvider = StandaloneDatabaseProvider(context)
                cache = SimpleCache(cacheDir, evictor, databaseProvider)
                Log.d(TAG, "Video cache initialized successfully at ${cacheDir.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize video cache. Defaulting to network-only playback.", e)
                isFailed = true
                cache = null
            }
        }
        return cache
    }
}
