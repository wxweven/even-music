package com.getmusic.hifiti.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@OptIn(UnstableApi::class)
object AudioCache {

    private const val MAX_CACHE_BYTES = 2L * 1024 * 1024 * 1024 // 2 GB

    @Volatile
    private var cache: SimpleCache? = null

    fun getInstance(context: Context): SimpleCache {
        return cache ?: synchronized(this) {
            cache ?: createCache(context.applicationContext).also { cache = it }
        }
    }

    private fun createCache(context: Context): SimpleCache {
        val cacheDir = File(context.cacheDir, "audio_cache")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES)
        val databaseProvider = StandaloneDatabaseProvider(context)
        return SimpleCache(cacheDir, evictor, databaseProvider)
    }

    fun release() {
        synchronized(this) {
            cache?.release()
            cache = null
        }
    }

    fun clear(context: Context) {
        synchronized(this) {
            cache?.release()
            cache = null
            val cacheDir = File(context.cacheDir, "audio_cache")
            cacheDir.deleteRecursively()
        }
    }

    fun getCacheSize(context: Context): Long {
        val cacheDir = File(context.cacheDir, "audio_cache")
        if (!cacheDir.exists()) return 0L
        return cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
}
