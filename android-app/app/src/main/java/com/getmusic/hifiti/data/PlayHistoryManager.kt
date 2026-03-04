package com.getmusic.hifiti.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class PlayHistoryItem(
    val threadId: String,
    val title: String,
    val artist: String,
    val coverUrl: String,
    val audioUrl: String,
    val playedAt: Long = System.currentTimeMillis()
)

class PlayHistoryManager(context: Context) {

    private val prefs = context.getSharedPreferences("play_history", Context.MODE_PRIVATE)

    fun getAll(): List<PlayHistoryItem> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                PlayHistoryItem(
                    threadId = obj.getString("threadId"),
                    title = obj.getString("title"),
                    artist = obj.getString("artist"),
                    coverUrl = obj.optString("coverUrl", ""),
                    audioUrl = obj.optString("audioUrl", ""),
                    playedAt = obj.optLong("playedAt", 0L)
                )
            }.sortedByDescending { it.playedAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun record(item: PlayHistoryItem) {
        val list = getAll().toMutableList()
        list.removeAll { it.threadId == item.threadId }
        list.add(0, item.copy(playedAt = System.currentTimeMillis()))
        val limited = list.take(MAX_SIZE)
        save(limited)
    }

    fun clear() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    private fun save(list: List<PlayHistoryItem>) {
        val array = JSONArray()
        list.forEach { item ->
            array.put(JSONObject().apply {
                put("threadId", item.threadId)
                put("title", item.title)
                put("artist", item.artist)
                put("coverUrl", item.coverUrl)
                put("audioUrl", item.audioUrl)
                put("playedAt", item.playedAt)
            })
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    companion object {
        private const val KEY_HISTORY = "history_list"
        private const val MAX_SIZE = 200
    }
}
