package com.brigadka.app.presentation.chat.conversation

import co.touchlab.kermit.Logger
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import com.brigadka.app.common.coroutineScope
import com.brigadka.app.data.api.BrigadkaApiService
import com.brigadka.app.data.api.models.MediaItem
import com.brigadka.app.data.api.models.ChatMessage as ChatMessageApi
import com.brigadka.app.data.api.websocket.ChatMessage as ChatMessageWS
import com.brigadka.app.data.api.websocket.ChatWebSocketClient
import com.brigadka.app.data.repository.ProfileRepository
import com.brigadka.app.data.repository.UserRepository
import com.brigadka.app.di.ProfileViewComponentFactory
import com.brigadka.app.presentation.common.TopBarState
import com.brigadka.app.presentation.common.UIEvent
import com.brigadka.app.presentation.common.UIEventEmitter
import com.brigadka.app.presentation.profile.view.ProfileViewComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

private val logger = Logger.withTag("ChatComponent")

data class ChatTopBarState(
    val chatName: String,
    val image: MediaItem? = null,
    val isOnline: Boolean,
    val onTitleClick: () -> Unit,
    val onBackClick: () -> Unit
): TopBarState


class ChatComponent(
    componentContext: ComponentContext,
    private val uiEventEmitter: UIEventEmitter,
    private val userRepository: UserRepository,
    private val profileRepository: ProfileRepository,
    private val api: BrigadkaApiService,
    private val webSocketClient: ChatWebSocketClient,
    private val profileViewComponentFactory: ProfileViewComponentFactory,
    private val chatID: String,
    private val otherUserID: Int,
    private val onBackClick: () -> Unit
) : ComponentContext by componentContext {

    private val scope = coroutineScope()

    private val _chatState = MutableStateFlow(ChatState(currentUserId = userRepository.requireUserId()))
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()

    private val onTitleClick: () -> Unit = {
        // Navigate to profile of the other user
        navigateTo(Config.Profile(otherUserID))
    }

    private val topBarState = MutableStateFlow(
        ChatTopBarState(
            chatName = _chatState.value.chatName,
            isOnline = _chatState.value.isOnline,
            onTitleClick = onTitleClick,
            onBackClick = onBackClick,
        )
    )

    private val navigation = StackNavigation<Config>()
    private val _childStack = childStack(
        source = navigation,
        initialConfiguration = Config.Chat,
        serializer = Config.serializer(),
        handleBackButton = true,
        childFactory = ::createChild
    )

    val childStack: Value<ChildStack<Config, Child>> = _childStack

    private fun createChild(
        configuration: Config,
        componentContext: ComponentContext
    ): Child = when (configuration) {
        is Config.Profile -> Child.Profile(
            profileViewComponentFactory.create(context = componentContext, userID = otherUserID, onBackClick = { navigation.pop() })
        )
        is Config.Chat -> Child.Chat
    }

    fun navigateTo(screen: Config) {
        val stackItems = childStack.value.items
        val existingIndex = stackItems.indexOfFirst { it.configuration == screen }

        if (childStack.value.active.configuration == screen) {
            // Already on this screen, do nothing
            return
        }

        if (existingIndex != -1) {
            // Screen is in stack, bring to front
            navigation.bringToFront(screen)
        } else {
            // Not in stack, push it
            navigation.pushNew(screen)
        }
    }

    // Keep track of pending messages
    private val pendingMessages = mutableMapOf<String, Message>()

    init {

        scope.launch {
            val otherUserProfile = profileRepository.getProfileView(otherUserID)
            topBarState.update { it.copy(image = otherUserProfile.avatar, chatName = otherUserProfile.fullName) }
        }

        scope.launch {
            chatState.collect {
                // Update top bar state when chat state changes
                topBarState.update {
                    it.copy(isOnline = it.isOnline)
                }
            }
        }

        scope.launch {

            // Get chat
            try {
                val chat = api.getChat(chatID)
                _chatState.update { it.copy(chatName = chat.chat_name) }
            } catch (e: Exception) {
                // TODO: handler error
            }

            // Load messages history
            try {
                val messages = api.getChatMessages(chatID, 50, 0)
                _chatState.update { it.copy(messages = messages.map { it.toUiModel() }) }
            } catch (e: Exception) {
                // TODO: Handle error
            }

            // Connect WebSocket
            webSocketClient.connect()

            // Update connection state
            launch {
                webSocketClient.connectionState.collect { state ->
                    _chatState.update { it.copy(
                        isConnected = state == ChatWebSocketClient.ConnectionState.CONNECTED
                    ) }
                }
            }

            launch {
                webSocketClient.incomingMessages.collect { msg ->
                    logger.d("Received incoming message: $msg")
                }
            }


            launch {
                webSocketClient.chatMessages.collect { wsMessage ->
                    if (wsMessage.chat_id == chatID) {
                        // Convert websocket message to app model

                        val message = wsMessage.toUiModel()

                        _chatState.update { state ->
                            val updatedMessages = state.messages.toMutableList()

                            // If this was a pending message that we sent, replace it
                            val pendingIndex = updatedMessages.indexOfFirst {
                                it.message_id == message.message_id
                            }

                            if (pendingIndex >= 0) {
                                updatedMessages[pendingIndex] = message
                            } else {
                                updatedMessages.add(message)
                            }

                            state.copy(messages = updatedMessages)
                        }

                        // Remove from pending
                        pendingMessages.remove(message.message_id)

                        // Mark message as read
                        webSocketClient.sendReadReceipt(chatID, message.message_id)
                    }
                }
            }
        }
    }

    suspend fun showTopBar() {
        topBarState.collect { state ->
            uiEventEmitter.emit(UIEvent.TopBarUpdate(state))
        }
    }

    fun onBack() {
        onBackClick.invoke()
    }

    suspend fun sendMessage(content: String) {
        // TODO: handle
        val chatId = chatID ?: return

        try {
            val senderID = userRepository.requireUserId()
            val messageId = webSocketClient.sendChatMessage(senderID, chatId, content)

            // Add as pending message
            val pendingMessage = Message(
                message_id = messageId,
                content = content,
                sender_id = senderID,
                sent_at = null
            )

            pendingMessages[messageId] = pendingMessage

            // Add to UI immediately with "pending" state
            _chatState.update { state ->
                val updatedMessages = state.messages.toMutableList()
                updatedMessages.add(pendingMessage)
                state.copy(messages = updatedMessages)
            }



        } catch (e: Exception) {
            // TODO: Handle sending error, try to send with http
        }
    }

    data class ChatState(
        val isConnected: Boolean = false,
        val isOnline: Boolean = false,
        val chatName: String = "",
        val participants: List<Int> = emptyList(),
        val currentUserId: Int = 0,  // This should be set from your auth state
        val messages: List<Message> = emptyList(),
        val typingUsers: Set<Int> = emptySet(),
        val isBroken: Boolean = false,
    )

    data class Message(
        val sender_id: Int,
        val message_id: String,
        val content: String,
        val sent_at: Instant?,
    )

    @Serializable
    sealed class Config {
        @Serializable
        data class Profile(val userID: Int? = null) : Config()
        @Serializable
        data object Chat : Config()
    }

    sealed class Child {
        data class Profile(val component: ProfileViewComponent) : Child()
        data object Chat : Child()
    }
}

fun ChatMessageWS.toUiModel() = ChatComponent.Message(
    message_id = message_id,
    content = content,
    sent_at = sent_at,
    sender_id = sender_id
)

fun ChatMessageApi.toUiModel() = ChatComponent.Message(
    message_id = message_id,
    content = content,
    sent_at = sent_at,
    sender_id = sender_id
)
