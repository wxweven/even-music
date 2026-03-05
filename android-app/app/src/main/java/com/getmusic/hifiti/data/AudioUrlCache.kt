package com.getmusic.hifiti.data

/**
 * In-memory cache for audio URLs with a 10-minute TTL.
 *
 * Audio URLs from hifiti.com contain time-limited tokens that expire
 * after ~1-2 hours. This cache avoids redundant network requests when
 * the same song is played multiple times within a short period.
 */
object AudioUrlCache {

    private const val TTL_MS = 10 * 60 * 1000L

    private data class Entry(val audioUrl: String, val fetchedAt: Long)

    private val cache = mutableMapOf<String, Entry>()

    fun get(threadId: String): String? {
        val entry = cache[threadId] ?: return null
        if (System.currentTimeMillis() - entry.fetchedAt > TTL_MS) {
            cache.remove(threadId)
            return null
        }
        return entry.audioUrl
    }

    fun put(threadId: String, audioUrl: String) {
        cache[threadId] = Entry(audioUrl, System.currentTimeMillis())
    }

    fun clear() {
        cache.clear()
    }
}
