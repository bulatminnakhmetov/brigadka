package com.brigadka.app.presentation.search

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SearchComponent(
    componentContext: ComponentContext
) : ComponentContext by componentContext {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    // Stub implementation
    fun search(query: String) {
        // Stub: Would perform search
    }

    data class SearchState(
        val isLoading: Boolean = false,
        val query: String = "",
        val results: List<String> = listOf("Sample Result 1", "Sample Result 2"),
        val error: String? = null
    )
}