package com.brigadka.app.presentation.chat.list

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import com.brigadka.app.common.coroutineScope
import com.brigadka.app.data.api.BrigadkaApiService
import com.brigadka.app.data.api.models.MediaItem
import com.brigadka.app.data.api.websocket.ChatWebSocketClient
import com.brigadka.app.data.repository.ProfileRepository
import com.brigadka.app.data.repository.UserRepository
import com.brigadka.app.di.ChatComponentFactory
import com.brigadka.app.presentation.chat.conversation.ChatComponent
import com.brigadka.app.presentation.common.TopBarState
import com.brigadka.app.presentation.common.UIEvent
import com.brigadka.app.presentation.common.UIEventEmitter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

data object ChatListTopBarState: TopBarState

class ChatListComponent(
    componentContext: ComponentContext,
    private val uiEventEmitter: UIEventEmitter,
    private val api: BrigadkaApiService,
    private val profileRepository: ProfileRepository,
    private val userRepository: UserRepository,
    private val webSocketClient: ChatWebSocketClient,
    private val chatComponentFactory: ChatComponentFactory,
) : ComponentContext by componentContext {

    private val scope = coroutineScope()

    private val _chatListState = MutableStateFlow(ChatListState())
    val chatListState: StateFlow<ChatListState> = _chatListState.asStateFlow()

    private val navigation = StackNavigation<Config>()

    private val _stack = childStack(
        source = navigation,
        initialConfiguration = Config.ChatList,
        handleBackButton = true,
        childFactory = ::createChild,
        serializer = Config.serializer()
    )

    val childStack: Value<ChildStack<Config, Child>> = _stack

    private fun createChild(config: Config, componentContext: ComponentContext): Child {
        return when (config) {
            is Config.Chat -> Child.Chat(
                chatComponentFactory.create(
                    componentContext,
                    config.chatId,
                    config.otherUserID!!, // TODO: create chat repository and pass only chat id
                    { navigation.pop() }
                )
            )
            is Config.ChatList -> Child.ChatList
        }
    }


    init {
        loadChats()

        // Listen for new messages
        scope.launch {
            webSocketClient.chatMessages.collect { message ->
                // Refresh chat list when new message arrives
                loadChats()
            }
        }
    }

    private fun loadChats() {
        _chatListState.update { it.copy(isLoading = true) }

        scope.launch {
            try {
                val chats = api.getChats()
                val chatPreviews = mutableListOf<ChatPreview>()

                for (chat in chats) {
                    // Get last message
                    val messages = api.getChatMessages(chat.chat_id, 1, 0)
                    val lastMessage = messages.firstOrNull()

                    // Get other participant profile for direct chats
                    var name = chat.chat_name
                    var avatar: MediaItem? = null
                    var userID: Int? = null

                    if (!chat.is_group && chat.participants.isNotEmpty()) {
                        val currentUserId = userRepository.requireUserId()
                        val otherParticipants = chat.participants.filter { it != currentUserId }
                        if (otherParticipants.isNotEmpty()) {
                            try {
                                val otherProfile = profileRepository.getProfileView(otherParticipants.first())
                                name = otherProfile.fullName
                                avatar = otherProfile.avatar
                                userID = otherProfile.userID
                            } catch (e: Exception) {
                                // Use default name if profile fetch fails
                            }
                        }
                    }

                    chatPreviews.add(
                        ChatPreview(
                            chatId = chat.chat_id,
                            userID = userID,
                            name = name,
                            avatar = avatar,
                            lastMessage = lastMessage?.content,
                            lastMessageTime = lastMessage?.sent_at?.let { formatMessageTime(it) },
                            unreadCount = 0 // We'll implement unread count later
                        )
                    )
                }

                _chatListState.update { it.copy(
                    isLoading = false,
                    chats = chatPreviews.sortedByDescending {
                        it.lastMessageTime // Sort by last message time
                    }
                ) }

            } catch (e: Exception) {
                _chatListState.update { it.copy(
                    isLoading = false,
                    error = e
                ) }
            }
        }
    }

    suspend fun showTopBar() {
        uiEventEmitter.emit(UIEvent.TopBarUpdate(ChatListTopBarState))
    }

    fun onChatClick(chatId: String, userID: Int? = null) {
        navigation.pushNew(Config.Chat(chatId, userID))
    }

    fun onError(error: String) {
        // TODO: Log or handle error
    }

    private fun formatMessageTime(instant: Instant): String {
        try {
            val timezone = TimeZone.currentSystemDefault()
            val timestamp = instant.toLocalDateTime(timezone)
            val now = Clock.System.now().toLocalDateTime(timezone)

            return when {
                // Today
                timestamp.date == now.date -> {
                    "${timestamp.hour.toString().padStart(2, '0')}:${timestamp.minute.toString().padStart(2, '0')}"
                }
                // Yesterday
                timestamp.date.daysUntil(now.date) == 1 -> {
                    "Yesterday"
                }
                // This week
                timestamp.date.daysUntil(now.date) < 7 -> {
                    when (timestamp.dayOfWeek) {
                        DayOfWeek.MONDAY -> "Mon"
                        DayOfWeek.TUESDAY -> "Tue"
                        DayOfWeek.WEDNESDAY -> "Wed"
                        DayOfWeek.THURSDAY -> "Thu"
                        DayOfWeek.FRIDAY -> "Fri"
                        DayOfWeek.SATURDAY -> "Sat"
                        DayOfWeek.SUNDAY -> "Sun"
                        else -> ""
                    }
                }
                // Older
                else -> {
                    "${timestamp.monthNumber}/${timestamp.dayOfMonth}"
                }
            }
        } catch (e: Exception) {
            return ""
        }
    }

    @Serializable
    sealed class Config {
        @Serializable
        data class Chat(val chatId: String, val otherUserID: Int?) : Config()
        @Serializable
        data object ChatList: Config()
    }

    sealed class Child {
        data class Chat(val component: ChatComponent) : Child()
        data object ChatList : Child()
    }

    data class ChatListState(
        val isLoading: Boolean = false,
        val chats: List<ChatPreview> = emptyList(),
        val error: Throwable? = null
    )

    data class ChatPreview(
        val chatId: String,
        val name: String,
        val userID: Int? = null,
        val avatar: MediaItem?,
        val lastMessage: String?,
        val lastMessageTime: String?,
        val unreadCount: Int = 0
    )
}