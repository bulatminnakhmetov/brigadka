package com.brigadka.app.presentation.main

import MainComponent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.brigadka.app.presentation.profile.view.ProfileScreen
import kotlinx.coroutines.launch

@Composable
fun MainContent(component: MainComponent) {
    val childStack by component.childStack.subscribeAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
//            BrigadkaBottomBar(
//                onProfileClick = component::navigateToProfile,
//                onSearchClick = component::navigateToSearch,
//                onChatListClick = component::navigateToChatList,
//                onLogoutClick = component::logout
//            )
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
                        ProfileScreen(instance.component) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(it)
                            }
                        }
                    else -> { Text("MAIN CONTENT BUT NOT PROFILE ")}
//                    is RootComponent.MainComponent.MainChild.Search ->
//                        SearchContent(instance.component)
//                    is RootComponent.MainComponent.MainChild.ChatList ->
//                        ChatListContent(instance.component)
//                    is RootComponent.MainComponent.MainChild.Chat ->
//                        ChatContent(instance.component)
                }
            }
        }
    }
}