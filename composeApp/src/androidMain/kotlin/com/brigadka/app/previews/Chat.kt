package com.brigadka.app.previews

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.brigadka.app.presentation.AppTheme
import com.brigadka.app.presentation.chat.conversation.ChatContentPreview
import com.brigadka.app.presentation.chat.conversation.ChatTopBarOfflinePreview
import com.brigadka.app.presentation.chat.conversation.ChatTopBarOnlinePreview
import com.brigadka.app.presentation.chat.list.ChatListContentPreview

@Preview
@Composable
fun ChatListContentPreviewPreview() {
    AppTheme {
        Surface {
            ChatListContentPreview()
        }
    }
}

@Preview
@Composable
fun ChatTopBarOnlinePreviewPreview() {
    AppTheme {
        Surface {
            ChatTopBarOnlinePreview()
        }
    }
}

@Preview
@Composable
fun ChatTopBarOfflinePreviewPreview() {
    AppTheme {
        Surface {
            ChatTopBarOfflinePreview()
        }
    }
}

@Preview
@Composable
fun ChatContentPreviewPreview() {
    AppTheme {
        Surface {
            ChatContentPreview()
        }
    }
}

