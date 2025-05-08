package com.brigadka.app.presentation.chat.conversation

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChatComponent(
    componentContext: ComponentContext,
    val chatId: String
) : ComponentContext by componentContext {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    // Stub implementation
    fun loadMessages() {
        // Stub: Would load messages
    }

    fun sendMessage(text: String) {
        // Stub: Would send a message
    }

    data class ChatState(
        val isLoading: Boolean = false,
        val messages: List<Message> = listOf(
            Message("1", "Hello", "Sample User", 1000),
            Message("2", "Hi there!", "Other User", 2000)
        ),
        val chatName: String = "Sample Chat",
        val error: String? = null
    )

    data class Message(
        val id: String,
        val text: String,
        val sender: String,
        val timestamp: Long
    )
}