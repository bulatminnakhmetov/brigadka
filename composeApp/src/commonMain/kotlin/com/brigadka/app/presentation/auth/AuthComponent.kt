package com.brigadka.app.presentation.auth

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.brigadka.app.common.coroutineScope
import com.brigadka.app.data.repository.UserRepository
import com.brigadka.app.di.RegisterComponentFactory
import com.brigadka.app.domain.session.SessionManager
import com.brigadka.app.presentation.auth.login.LoginComponent
import com.brigadka.app.presentation.auth.register.RegisterComponent
import com.brigadka.app.presentation.common.UIEventFlowProvider
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class AuthComponent(
    componentContext: ComponentContext,
    private val uiEventFlowProvider: UIEventFlowProvider,
    private val sessionManager: SessionManager,
    private val userRepository: UserRepository,
    private val registerComponentFactory: RegisterComponentFactory,
) : ComponentContext by componentContext, UIEventFlowProvider by uiEventFlowProvider {

    private val scope = coroutineScope()
    private val navigation = StackNavigation<Configuration>()

    private val stack = childStack(
        source = navigation,
        serializer = Configuration.serializer(),
        initialConfiguration = if (userRepository.isLoggedIn.value) Configuration.Register else Configuration.Login,
        handleBackButton = true,
        childFactory = ::createChild
    )

    init {
        // Observe user login state
        scope.launch {
            userRepository.isLoggedIn.collect{ isLoggedIn ->
                if (isLoggedIn) {
                    navigation.replaceAll(Configuration.Register)
                } else {
                    navigation.replaceAll(Configuration.Login)
                }
            }
        }
    }

    val childStack: Value<ChildStack<*, Child>> = stack

    private fun createChild(
        configuration: Configuration,
        componentContext: ComponentContext
    ): Child = when (configuration) {
        is Configuration.Login -> Child.Login(
            LoginComponent(
                componentContext = componentContext,
                navigateToRegister = { navigation.replaceAll(Configuration.Register) },
                sessionManager = sessionManager
            )
        )
        is Configuration.Register -> Child.Register(
            registerComponentFactory.create(
                componentContext, { navigation.replaceAll(Configuration.Login) }
            )
        )
    }
//
//    // TODO: same fuctionality in MainComponent, consider moving to base class
//    fun navigateTo(screen: Configuration) {
//        val stackItems = childStack.value.items
//        val existingIndex = stackItems.indexOfFirst { it.configuration == screen }
//
//        if (childStack.value.active.configuration == screen) {
//            // Already on this screen, do nothing
//            return
//        }
//
//        if (existingIndex != -1) {
//            // Screen is in stack, bring to front
//            navigation.bringToFront(screen)
//        } else {
//            // Not in stack, push it
//            navigation.pushNew(screen)
//        }
//    }

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