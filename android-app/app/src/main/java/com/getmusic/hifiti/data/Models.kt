package com.getmusic.hifiti.data

data class SearchItem(
    val threadId: String,
    val title: String,
    val artist: String,
    val songName: String,
    val format: String,
    val category: String
)

data class SongDetail(
    val songName: String,
    val artist: String,
    val audioUrl: String,
    val coverUrl: String,
    val lyrics: String?,
    val realAudioUrl: String?
)

data class SearchResult(
    val items: List<SearchItem>,
    val currentPage: Int,
    val hasMore: Boolean
)

data class DownloadState(
    val songName: String = "",
    val artist: String = "",
    val progress: Float = 0f,
    val isDownloading: Boolean = false,
    val isCompleted: Boolean = false,
    val filePath: String? = null,
    val error: String? = null
)
