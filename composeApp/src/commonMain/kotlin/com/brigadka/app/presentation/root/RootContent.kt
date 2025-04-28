package com.brigadka.app.presentation.root

package com.brigadka.app.presentation.root

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.brigadka.app.presentation.auth.AuthContent

@Composable
fun RootContent(component: RootComponent) {
    val childStack by component.childStack.subscribeAsState()

    Children(
        stack = childStack,
        animation = stackAnimation(fade() + scale()),
    ) { child ->
        when (val instance = child.instance) {
            is RootComponent.Child.Auth -> AuthContent(instance.component)
            is RootComponent.Child.Main -> MainContent(instance.component)
        }
    }
}

@Composable
private fun MainContent(component: RootComponent.MainComponent) {
    val childStack by component.childStack.subscribeAsState()

    Scaffold(
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
//            Children(
//                stack = childStack,
//                animation = stackAnimation(fade() + scale()),
//            ) { child ->
//                when (val instance = child.instance) {
//                    is RootComponent.MainComponent.MainChild.Profile ->
//                        ProfileContent(instance.component)
//                    is RootComponent.MainComponent.MainChild.Search ->
//                        SearchContent(instance.component)
//                    is RootComponent.MainComponent.MainChild.ChatList ->
//                        ChatListContent(instance.component)
//                    is RootComponent.MainComponent.MainChild.Chat ->
//                        ChatContent(instance.component)
//                }
//            }
        }
    }
}