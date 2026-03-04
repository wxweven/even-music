package com.getmusic.hifiti.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.getmusic.hifiti.data.HiFiTiApi
import com.getmusic.hifiti.data.SearchHistoryManager
import com.getmusic.hifiti.data.SearchItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<SearchItem> = emptyList(),
    val currentPage: Int = 0,
    val hasMore: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val hasSearched: Boolean = false
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val api = HiFiTiApi()
    private val historyManager = SearchHistoryManager(application)

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    init {
        _searchHistory.value = historyManager.getHistory()
    }

    fun updateQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    fun search() {
        val query = _uiState.value.query.trim()
        if (query.isEmpty()) return

        historyManager.addQuery(query)
        _searchHistory.value = historyManager.getHistory()

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                results = emptyList(),
                hasSearched = true
            )

            try {
                val result = api.search(query, page = 1)
                _uiState.value = _uiState.value.copy(
                    results = result.items,
                    currentPage = 1,
                    hasMore = result.hasMore,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "搜索失败"
                )
            }
        }
    }

    fun searchFromHistory(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        search()
    }

    fun clearHistory() {
        historyManager.clear()
        _searchHistory.value = emptyList()
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return

        viewModelScope.launch {
            _uiState.value = state.copy(isLoadingMore = true)

            try {
                val nextPage = state.currentPage + 1
                val result = api.search(state.query, page = nextPage)
                _uiState.value = _uiState.value.copy(
                    results = state.results + result.items,
                    currentPage = nextPage,
                    hasMore = result.hasMore,
                    isLoadingMore = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    error = e.message ?: "加载更多失败"
                )
            }
        }
    }
}
