package com.getmusic.hifiti.data

import android.content.Context
import org.json.JSONArray

class SearchHistoryManager(context: Context) {

    private val prefs = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)

    fun getHistory(): List<String> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { array.getString(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        val history = getHistory().toMutableList()
        history.remove(trimmed)
        history.add(0, trimmed)

        val limited = history.take(MAX_SIZE)
        val array = JSONArray(limited)
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    companion object {
        private const val KEY_HISTORY = "history"
        private const val MAX_SIZE = 15
    }
}
