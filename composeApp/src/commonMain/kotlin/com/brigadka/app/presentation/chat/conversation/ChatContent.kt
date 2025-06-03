package com.brigadka.app.presentation.chat.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.brigadka.app.common.formatInstantTo24HourTime
import com.brigadka.app.presentation.profile.common.Avatar
import com.brigadka.app.presentation.profile.edit.EditProfileScreen
import com.brigadka.app.presentation.profile.view.ProfileViewComponent
import com.brigadka.app.presentation.profile.view.ProfileViewContent
import com.brigadka.app.presentation.profile.view.ProfileViewScreen
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

@Composable
fun ChatContent(component: ChatComponent) {
    val childStack by component.childStack.subscribeAsState()

    Children(
        stack = childStack,
        animation = stackAnimation(fade() + slide())
    ) { child ->
        when (val instance = child.instance) {
            is ChatComponent.Child.Chat -> {
                val uiState by component.chatState.collectAsState()
                LaunchedEffect(Unit) {
                    component.showTopBar()
                }
                ChatContent(uiState = uiState, onBackClick = component::onBack, onSendMessage = component::sendMessage)
            }
            is ChatComponent.Child.Profile -> {
                ProfileViewContent(instance.component)
            }
        }
    }
}

@Composable
fun ChatContentPreview() {
    val uiState = ChatComponent.ChatState(
        chatName = "Jack Sparrow",
        isOnline = true,
        messages = listOf(
            ChatComponent.Message(
                message_id = "1",
                sender_id = 1,
                content = "привет) увидел у тебя видео с выступлением — очень круто сыграл\n" +
                        "сцена с преподавателем в лаборатории прям зашла \uD83D\uDD25",
                sent_at = Instant.parse("2023-10-01T12:00:00Z")
            ),
            ChatComponent.Message(
                message_id = "2",
                sender_id = 2,
                content = "о, спасибо)) это мы на ночной импровке играли, вообще без подготовки\n" +
                        "рад что понравилось",
                sent_at = Instant.parse("2023-10-01T12:01:00Z")
            ),
            ChatComponent.Message(
                message_id = "1",
                sender_id = 1,
                content = "ну прям классно было\n" +
                        "я сейчас собираю команду под длинную форму, типа сторителлинга с персонажами и отношениями\n" +
                        "не думал попробовать что-то такое?",
                sent_at = Instant.parse("2023-10-01T12:03:00Z")
            ),
            ChatComponent.Message(
                message_id = "2",
                sender_id = 2,
                content = "слушай да, давно хотелось пойти в сторону чего-то посерьёзнее\n" +
                        "шортформы кайф конечно, но хочется поглубже копнуть\n" +
                        "что за формат у вас?",
                sent_at = Instant.parse("2023-10-01T12:04:00Z")
            ),

        ),
        currentUserId = 1,
        isConnected = true
    )

    ChatContent(uiState, onBackClick = {}, onSendMessage = {})
}
@Composable
fun ChatContent(uiState: ChatComponent.ChatState, onBackClick: () -> Unit, onSendMessage: suspend (String) -> Unit) {
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var messageText by remember { mutableStateOf("") }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Messages list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                state = lazyListState,
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.messages) { message ->
                    MessageBubble(
                        message = message,
                        isFromCurrentUser = message.sender_id == uiState.currentUserId,
                        onReactionClick = {}
                    )
                }
            }

            // Typing indicator
            if (uiState.typingUsers.isNotEmpty()) {
                Text(
                    text = "${uiState.typingUsers.joinToString(", ")} typing...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Message input
            ChatInput(
                value = messageText,
                onValueChange = { messageText = it },
                onSendClick = {
                    if (messageText.isNotEmpty()) {
                        coroutineScope.launch {
                            onSendMessage(messageText)
                            messageText = ""
                        }
                    }
                }
            )
        }

        // Connection status
        if (!uiState.isConnected) {
            ConnectionStatusBanner()
        }
    }
}


@Composable
private fun MessageBubble(
    message: ChatComponent.Message,
    isFromCurrentUser: Boolean,
    onReactionClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(
            start = if (isFromCurrentUser) 16.dp else 4.dp,
            end = if (isFromCurrentUser) 4.dp else 16.dp,
        ),
        horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
    ) {
        val shape = RoundedCornerShape(
            bottomStart = 16.dp,
            bottomEnd = 16.dp,
            topStart = if (isFromCurrentUser) 16.dp else 0.dp,
            topEnd = if (isFromCurrentUser) 0.dp else 16.dp
        )

        Column(
            modifier = Modifier
                .shadow(elevation = 1.dp, shape = shape)
                .background(color = if (isFromCurrentUser) MaterialTheme.colorScheme.secondaryContainer else Color.White, shape = shape)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = message.sent_at?.let {
                formatInstantTo24HourTime(it)
            } ?: "Sending...",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                placeholder = { Text("Введите сообщение...") },
                singleLine = false,
                maxLines = 5,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
            )

            IconButton(
                onClick = onSendClick,
                enabled = value.isNotEmpty(),
                modifier = Modifier
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun ConnectionStatusBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Text(
            text = "Reconnecting...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp)
        )
    }
}


@Composable
fun ChatTopBarOnlinePreview() {
    ChatTopBar(
        state = ChatTopBarState(
            chatName = "Jack Sparrow",
            isOnline = true,
            onTitleClick = {},
            onBackClick = {}
        )
    )
}

@Composable
fun ChatTopBarOfflinePreview() {
    ChatTopBar(
        state = ChatTopBarState(
            chatName = "Jack Sparrow",
            isOnline = false,
            onTitleClick = {},
            onBackClick = {}
        )
    )
}

// Add to ChatContent.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(state: ChatTopBarState) {
    CenterAlignedTopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize().clickable {
                    state.onTitleClick()
                }
            ) {
                Avatar(mediaItem = state.image, modifier = Modifier.padding(8.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = state.chatName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    // TODO: implement online/offline status
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.Center
//                    ) {
//                        Box(
//                            modifier = Modifier
//                                .size(8.dp)
//                                .background(
//                                    color = if (state.isOnline)
//                                        MaterialTheme.colorScheme.primary
//                                    else
//                                        Color.Transparent,
//                                    shape = CircleShape
//                                ).border(
//                                    width = 1.dp,
//                                    color = if (state.isOnline)
//                                        Color.Transparent
//                                    else
//                                        MaterialTheme.colorScheme.onSurfaceVariant,
//                                    shape = CircleShape
//                                )
//                        )
//                        Spacer(modifier = Modifier.width(4.dp))
//                        Text(
//                            text = if (state.isOnline) "Online" else "Offline",
//                            style = MaterialTheme.typography.bodySmall,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
//                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = state.onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }
    )
}