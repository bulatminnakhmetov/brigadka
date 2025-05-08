package com.brigadka.app.presentation.chat.list

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChatListComponent(
    componentContext: ComponentContext,
    private val onChatSelected: (String) -> Unit
) : ComponentContext by componentContext {

    private val _state = MutableStateFlow(ChatListState())
    val state: StateFlow<ChatListState> = _state.asStateFlow()

    // Stub implementation
    fun loadChats() {
        // Stub: Would load list of chats
    }

    fun onChatClicked(chatId: String) {
        onChatSelected(chatId)
    }

    data class ChatListState(
        val isLoading: Boolean = false,
        val chats: List<ChatPreview> = listOf(
            ChatPreview("1", "Sample Chat 1", "Last message 1"),
            ChatPreview("2", "Sample Chat 2", "Last message 2")
        ),
        val error: String? = null
    )

    data class ChatPreview(
        val id: String,
        val name: String,
        val lastMessage: String
    )
}