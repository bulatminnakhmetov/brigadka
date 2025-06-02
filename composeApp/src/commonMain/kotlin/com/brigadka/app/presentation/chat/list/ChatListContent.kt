package com.brigadka.app.presentation.chat.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.brigadka.app.presentation.chat.conversation.ChatContent
import com.brigadka.app.presentation.profile.common.Avatar

@Composable
fun ChatListContent(component: ChatListComponent) {
    val childStack by component.childStack.subscribeAsState()

    Children(
        stack = childStack,
        animation = stackAnimation(fade() + com.arkivanov.decompose.extensions.compose.stack.animation.scale())
    ) { child ->
        when (val instance = child.instance) {
            is ChatListComponent.Child.ChatList -> {
                val uiState by component.chatListState.collectAsState()
                LaunchedEffect(Unit) {
                    component.showTopBar()
                }
                ChatListContent(
                    chatListState = uiState,
                    onChatClick = component::onChatClick,
                    onError = component::onError
                )
            }
            is ChatListComponent.Child.Chat -> {
                ChatContent(instance.component)
            }
        }
    }


}

@Composable
fun ChatListContentPreview() {
    ChatListContent(
        chatListState = ChatListComponent.ChatListState(
            isLoading = false,
            chats = listOf(
                ChatListComponent.ChatPreview(
                    chatId = "1",
                    name = "John Doe",
                    lastMessage = "Hello!",
                    lastMessageTime = "10:00 AM",
                    unreadCount = 2,
                    avatar = null
                ),
                ChatListComponent.ChatPreview(
                    chatId = "2",
                    name = "Jane Smith",
                    lastMessage = null,
                    lastMessageTime = null,
                    unreadCount = 0,
                    avatar = null
                )
            )
        ),
        onChatClick = {_, _ -> },
        onError = {}
    )
}

@Composable
fun ChatListContent(
    chatListState: ChatListComponent.ChatListState,
    onChatClick: (String, Int?) -> Unit,
    onError: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (chatListState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (chatListState.chats.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Нет чатов пока что",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Зайдите в профиль другого пользователя и напишите ему сообщение",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(chatListState.chats) { chat ->
                        ChatItem(
                            chat = chat,
                            onClick = { onChatClick(chat.chatId, chat.userID) },
                            onError = onError
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatItem(
    chat: ChatListComponent.ChatPreview,
    onClick: () -> Unit,
    onError: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (chat.avatar != null) {
            Avatar(
                mediaItem = chat.avatar,
                isUploading = false,
                onError = onError,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )
        } else {
            // Fallback avatar
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = chat.name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chat.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            if (chat.lastMessage != null) {
                Text(
                    text = chat.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {
            if (chat.lastMessageTime != null) {
                Text(
                    text = chat.lastMessageTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (chat.unreadCount > 0) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListTopBar() {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "Чаты",
                style = MaterialTheme.typography.titleLarge
            )
        },
    )
}