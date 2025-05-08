package com.brigadka.app.presentation.auth

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import com.brigadka.app.data.repository.AuthRepository
import com.brigadka.app.presentation.auth.login.LoginComponent
import com.brigadka.app.presentation.auth.register.RegisterComponent
import kotlinx.serialization.Serializable

class AuthComponent(
    componentContext: ComponentContext,
    private val authRepository: AuthRepository,
    private val onAuthSuccess: (String) -> Unit
) : ComponentContext by componentContext {

    private val navigation = StackNavigation<Configuration>()

    private val stack = childStack(
        source = navigation,
        serializer = Configuration.serializer(),
        initialConfiguration = Configuration.Login,
        handleBackButton = true,
        childFactory = ::createChild
    )

    val childStack: Value<ChildStack<*, Child>> = stack

    private fun createChild(
        configuration: Configuration,
        componentContext: ComponentContext
    ): Child = when (configuration) {
        is Configuration.Login -> Child.Login(
            LoginComponent(
                componentContext = componentContext,
                navigateToRegister = { navigation.pushNew(Configuration.Register) },
                onLoginSuccess = { token -> onAuthSuccess(token) },
                authRepository = authRepository
            )
        )
        is Configuration.Register -> Child.Register(
            RegisterComponent(
                componentContext = componentContext,
                onBackClickCallback = { navigation.pop() },
                onRegisterSuccess = { token -> onAuthSuccess(token) },
                authRepository = authRepository
            )
        )
    }

    @Serializable
    sealed class Configuration {
        @Serializable
        object Login : Configuration()

        @Serializable
        object Register : Configuration()
    }

    sealed class Child {
        data class Login(val component: LoginComponent) : Child()
        data class Register(val component: RegisterComponent) : Child()
    }
}