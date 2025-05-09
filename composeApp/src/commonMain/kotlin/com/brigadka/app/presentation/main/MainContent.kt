package com.brigadka.app.presentation.main

import MainComponent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import brigadka.composeapp.generated.resources.Res
import brigadka.composeapp.generated.resources.chat_24px
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.brigadka.app.presentation.profile.view.ProfileViewScreen
import com.brigadka.app.presentation.search.SearchScreen
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.vectorResource

@Composable
fun MainContent(component: MainComponent) {
    val childStack by component.childStack.subscribeAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BrigadkaBottomBar(
                onProfileClick = component::navigateToProfile,
                onSearchClick = component::navigateToSearch,
                onChatListClick = component::navigateToChatList,
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Children(
                stack = childStack,
                animation = stackAnimation(fade() + scale()),
            ) { child ->
                when (val instance = child.instance) {
                    is Child.Profile ->
                        ProfileViewScreen(instance.component) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(it)
                            }
                        }
                    is Child.Search ->
                        SearchScreen(instance.component)
                    is Child.ChatList ->
                        Text("this is chat list")
//                        ChatListContent(instance.component)
                    is Child.Chat ->
                        Text("this is chat")
//                        ChatContent(instance.component)
                    else -> {}
                }
            }
        }
    }
}


@Composable
fun BrigadkaBottomBar(
    onProfileClick: () -> Unit,
    onSearchClick: () -> Unit,
    onChatListClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }

    NavigationBar(
        modifier = modifier.fillMaxWidth()
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = {
                selectedTab = 0
                onProfileClick()
            },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile") }
        )

        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = {
                selectedTab = 1
                onSearchClick()
            },
            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            label = { Text("Search") }
        )

        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = {
                selectedTab = 2
                onChatListClick()
            },
            icon = { Icon(Icons.Default.Email, contentDescription = "Chat") }, // TODO: replace with normal icon
            label = { Text("Chat") }
        )
    }
}