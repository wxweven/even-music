package com.getmusic.hifiti.data

import android.content.Context
import org.json.JSONObject

class SongDetailCache(context: Context) {

    private val prefs = context.getSharedPreferences("song_detail_cache", Context.MODE_PRIVATE)

    private val memoryCache = object : LinkedHashMap<String, SongDetail>(MAX_MEMORY, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, SongDetail>?): Boolean {
            return size > MAX_MEMORY
        }
    }

    init {
        warmUp()
    }

    fun get(threadId: String): SongDetail? {
        memoryCache[threadId]?.let { return it }

        val json = prefs.getString(keyFor(threadId), null) ?: return null
        return try {
            val detail = fromJson(json)
            memoryCache[threadId] = detail
            detail
        } catch (_: Exception) {
            prefs.edit().remove(keyFor(threadId)).apply()
            null
        }
    }

    fun put(threadId: String, detail: SongDetail) {
        memoryCache[threadId] = detail

        val ids = getStoredIds().toMutableList()
        ids.remove(threadId)
        ids.add(0, threadId)

        if (ids.size > MAX_DISK) {
            val removed = ids.subList(MAX_DISK, ids.size)
            val editor = prefs.edit()
            removed.forEach { editor.remove(keyFor(it)) }
            editor.apply()
        }

        val trimmed = ids.take(MAX_DISK)
        prefs.edit()
            .putString(keyFor(threadId), toJson(detail))
            .putString(KEY_IDS, trimmed.joinToString(","))
            .apply()
    }

    private fun warmUp() {
        val ids = getStoredIds().take(WARM_UP_COUNT)
        for (id in ids) {
            val json = prefs.getString(keyFor(id), null) ?: continue
            try {
                memoryCache[id] = fromJson(json)
            } catch (_: Exception) { }
        }
    }

    private fun getStoredIds(): List<String> {
        val raw = prefs.getString(KEY_IDS, null) ?: return emptyList()
        return raw.split(",").filter { it.isNotEmpty() }
    }

    private fun keyFor(threadId: String) = "detail_$threadId"

    private fun toJson(detail: SongDetail): String {
        return JSONObject().apply {
            put("songName", detail.songName)
            put("artist", detail.artist)
            put("audioUrl", detail.audioUrl)
            put("coverUrl", detail.coverUrl)
            put("lyrics", detail.lyrics ?: "")
        }.toString()
    }

    private fun fromJson(json: String): SongDetail {
        val obj = JSONObject(json)
        return SongDetail(
            songName = obj.getString("songName"),
            artist = obj.getString("artist"),
            audioUrl = obj.getString("audioUrl"),
            coverUrl = obj.getString("coverUrl"),
            lyrics = obj.optString("lyrics", "").ifEmpty { null },
            realAudioUrl = null
        )
    }

    fun clear() {
        memoryCache.clear()
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_IDS = "cached_ids"
        private const val MAX_MEMORY = 50
        private const val MAX_DISK = 200
        private const val WARM_UP_COUNT = 20
    }
}
