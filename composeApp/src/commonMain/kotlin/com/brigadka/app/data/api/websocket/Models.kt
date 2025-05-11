package com.brigadka.app.data.api.websocket

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


// Base interface for all WebSocket messages
interface WebSocketMessage {
    val type: MessageType
    val chat_id: String
}

enum class MessageType {
    CHAT,
    JOIN,
    LEAVE,
    REACTION,
    REACTION_REMOVED,
    TYPING,
    READ_RECEIPT
}

@Serializable
data class ChatMessage(
    @SerialName("type")
    override val type: MessageType,
    @SerialName("chat_id")
    override val chat_id: String,
    @SerialName("message_id")
    val message_id: String,
    @SerialName("sender_id")
    val sender_id: Int? = null,
    @SerialName("content")
    val content: String,
    @SerialName("sent_at")
    val sent_at: String? = null
) : WebSocketMessage

@Serializable
data class JoinChatMessage(
    @SerialName("type")
    override val type: MessageType,
    @SerialName("chat_id")
    override val chat_id: String,
    @SerialName("user_id")
    val user_id: Int,
    @SerialName("joined_at")
    val joined_at: String? = null
) : WebSocketMessage

@Serializable
data class LeaveChatMessage(
    @SerialName("type")
    override val type: MessageType,
    @SerialName("chat_id")
    override val chat_id: String,
    @SerialName("user_id")
    val user_id: Int,
    @SerialName("left_at")
    val left_at: String? = null
) : WebSocketMessage

@Serializable
data class ReactionMessage(
    @SerialName("type")
    override val type: MessageType,
    @SerialName("chat_id")
    override val chat_id: String,
    @SerialName("reaction_id")
    val reaction_id: String,
    @SerialName("message_id")
    val message_id: String,
    @SerialName("user_id")
    val user_id: Int? = null,
    @SerialName("reaction_code")
    val reaction_code: String,
    @SerialName("reacted_at")
    val reacted_at: String? = null
) : WebSocketMessage

@Serializable
data class ReactionRemovedMessage(
    @SerialName("type")
    override val type: MessageType,
    @SerialName("chat_id")
    override val chat_id: String,
    @SerialName("reaction_id")
    val reaction_id: String,
    @SerialName("message_id")
    val message_id: String,
    @SerialName("user_id")
    val user_id: Int? = null,
    @SerialName("reaction_code")
    val reaction_code: String,
    @SerialName("removed_at")
    val removed_at: String? = null
) : WebSocketMessage

@Serializable
data class TypingMessage(
    @SerialName("type")
    override val type: MessageType,
    @SerialName("chat_id")
    override val chat_id: String,
    @SerialName("user_id")
    val user_id: Int? = null,
    @SerialName("is_typing")
    val is_typing: Boolean,
    @SerialName("timestamp")
    val timestamp: String? = null
) : WebSocketMessage

@Serializable
data class ReadReceiptMessage(
    @SerialName("type")
    override val type: MessageType,
    @SerialName("chat_id")
    override val chat_id: String,
    @SerialName("user_id")
    val user_id: Int? = null,
    @SerialName("message_id")
    val message_id: String,
    @SerialName("read_at")
    val read_at: String? = null
) : WebSocketMessage
