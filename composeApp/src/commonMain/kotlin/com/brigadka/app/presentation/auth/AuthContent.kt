package com.brigadka.app.presentation.auth

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.brigadka.app.presentation.auth.login.LoginScreen
import com.brigadka.app.presentation.auth.register.RegisterScreen
import com.brigadka.app.presentation.common.TopBarState
import com.brigadka.app.presentation.common.UIEvent

@Composable
fun AuthContent(component: AuthComponent) {
    val childStack by component.childStack.subscribeAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    // Listen for top bar update events
    LaunchedEffect(Unit) {
        component.events.collect { event ->
            when (event) {
                is UIEvent.Message -> {
                    snackbarHostState.showSnackbar(message = event.text)
                }
                else -> {}
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) {
        Children(
            stack = childStack,
            animation = stackAnimation(fade() + slide()),
        ) { child ->
            when (val instance = child.instance) {
                is AuthComponent.Child.Login -> LoginScreen(instance.component)
                is AuthComponent.Child.Register -> RegisterScreen(instance.component)
            }
        }
    }
}