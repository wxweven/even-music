package com.getmusic.hifiti.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class FavoriteSong(
    val threadId: String,
    val title: String,
    val artist: String,
    val coverUrl: String,
    val audioUrl: String,
    val addedAt: Long = System.currentTimeMillis()
)

class FavoritesManager(context: Context) {

    private val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)

    fun getAll(): List<FavoriteSong> {
        val json = prefs.getString(KEY_FAVORITES, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                FavoriteSong(
                    threadId = obj.getString("threadId"),
                    title = obj.getString("title"),
                    artist = obj.getString("artist"),
                    coverUrl = obj.optString("coverUrl", ""),
                    audioUrl = obj.optString("audioUrl", ""),
                    addedAt = obj.optLong("addedAt", 0L)
                )
            }.sortedByDescending { it.addedAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun add(song: FavoriteSong) {
        val list = getAll().toMutableList()
        list.removeAll { it.threadId == song.threadId }
        list.add(0, song.copy(addedAt = System.currentTimeMillis()))
        save(list)
    }

    fun remove(threadId: String) {
        val list = getAll().toMutableList()
        list.removeAll { it.threadId == threadId }
        save(list)
    }

    fun isFavorite(threadId: String): Boolean {
        return getAll().any { it.threadId == threadId }
    }

    fun updateAudioUrl(threadId: String, newUrl: String) {
        val list = getAll().toMutableList()
        val index = list.indexOfFirst { it.threadId == threadId }
        if (index >= 0) {
            list[index] = list[index].copy(audioUrl = newUrl)
            save(list)
        }
    }

    fun toggle(song: FavoriteSong): Boolean {
        return if (isFavorite(song.threadId)) {
            remove(song.threadId)
            false
        } else {
            add(song)
            true
        }
    }

    private fun save(list: List<FavoriteSong>) {
        val array = JSONArray()
        list.forEach { song ->
            array.put(JSONObject().apply {
                put("threadId", song.threadId)
                put("title", song.title)
                put("artist", song.artist)
                put("coverUrl", song.coverUrl)
                put("audioUrl", song.audioUrl)
                put("addedAt", song.addedAt)
            })
        }
        prefs.edit().putString(KEY_FAVORITES, array.toString()).apply()
    }

    fun exportToJson(): String {
        val songs = getAll()
        val songsArray = JSONArray()
        songs.forEach { song ->
            songsArray.put(JSONObject().apply {
                put("threadId", song.threadId)
                put("title", song.title)
                put("artist", song.artist)
                put("coverUrl", song.coverUrl)
                put("audioUrl", song.audioUrl)
                put("addedAt", song.addedAt)
            })
        }
        return JSONObject().apply {
            put("version", 1)
            put("exportTime", System.currentTimeMillis())
            put("songs", songsArray)
        }.toString(2)
    }

    fun parseFromJson(json: String): List<FavoriteSong> {
        val root = JSONObject(json)
        val songsArray = root.getJSONArray("songs")
        return (0 until songsArray.length()).map { i ->
            val obj = songsArray.getJSONObject(i)
            FavoriteSong(
                threadId = obj.getString("threadId"),
                title = obj.getString("title"),
                artist = obj.getString("artist"),
                coverUrl = obj.optString("coverUrl", ""),
                audioUrl = obj.optString("audioUrl", ""),
                addedAt = obj.optLong("addedAt", 0L)
            )
        }
    }

    /**
     * @return 实际新增的歌曲数量
     */
    fun importSongs(songs: List<FavoriteSong>, replace: Boolean): Int {
        if (replace) {
            save(songs.sortedByDescending { it.addedAt })
            return songs.size
        }
        val existing = getAll()
        val existingIds = existing.map { it.threadId }.toSet()
        val newSongs = songs.filter { it.threadId !in existingIds }
        if (newSongs.isNotEmpty()) {
            save((existing + newSongs).sortedByDescending { it.addedAt })
        }
        return newSongs.size
    }

    companion object {
        private const val KEY_FAVORITES = "favorites_list"
    }
}
