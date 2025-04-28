package com.brigadka.app.presentation.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.brigadka.app.data.storage.TokenStorage
import com.brigadka.app.presentation.auth.AuthComponent
import com.brigadka.app.presentation.profile.ProfileComponent
import com.brigadka.app.presentation.search.SearchComponent
import com.brigadka.app.presentation.chat.list.ChatListComponent
import com.brigadka.app.presentation.chat.conversation.ChatComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable

class RootComponent(
    componentContext: ComponentContext,
    private val tokenStorage: TokenStorage,
    private val createAuthComponent: (ComponentContext, (String) -> Unit) -> AuthComponent,
    private val createProfileComponent: (ComponentContext) -> ProfileComponent,
    private val createSearchComponent: (ComponentContext) -> SearchComponent,
    private val createChatListComponent: (ComponentContext, (String) -> Unit) -> ChatListComponent,
    private val createChatComponent: (ComponentContext, String) -> ChatComponent
) : ComponentContext by componentContext {

    private val navigation = StackNavigation<Configuration>()

    private val stack = childStack(
        source = navigation,
        initialConfiguration = getInitialConfiguration(),
        handleBackButton = true,
        childFactory = ::createChild
    )

    val childStack: Value<ChildStack<*, Child>> = stack

    private fun getInitialConfiguration(): Configuration {
        // Check if user is logged in by checking token
        return runBlocking {
            val token = tokenStorage.token.first()
            if (token.accessToken != null) {
                Configuration.Main
            } else {
                Configuration.Auth
            }
        }
    }

    private fun createChild(
        configuration: Configuration,
        componentContext: ComponentContext
    ): Child = when (configuration) {
        is Configuration.Auth -> Child.Auth(
            createAuthComponent(
                componentContext,
                { onAuthSuccess() }
            )
        )
        is Configuration.Main -> Child.Main(
            MainComponent(
                componentContext = componentContext,
                navigation = navigation
            )
        )
    }

    private fun onAuthSuccess() {
        navigation.push(Configuration.Main)
    }

    fun logout() {
        runBlocking {
            tokenStorage.clearToken()
        }
        // Clear back stack and go to auth screen
        while (navigation.pop()) {
            // Pop until empty
        }
        navigation.push(Configuration.Auth)
    }

    @Serializable
    sealed class Configuration {
        @Serializable
        object Auth : Configuration()

        @Serializable
        object Main : Configuration()
    }

    sealed class Child {
        data class Auth(val component: AuthComponent) : Child()
        data class Main(val component: MainComponent) : Child()
    }

    inner class MainComponent(
        componentContext: ComponentContext,
        private val navigation: StackNavigation<Configuration>
    ) : ComponentContext by componentContext {

        private val mainNavigation = StackNavigation<MainConfiguration>()
        private val mainStack = childStack(
            source = mainNavigation,
            initialConfiguration = MainConfiguration.Profile,
            handleBackButton = true,
            childFactory = ::createChild
        )

        val childStack: Value<ChildStack<*, MainChild>> = mainStack

        private fun createChild(
            configuration: MainConfiguration,
            componentContext: ComponentContext
        ): MainChild = when (configuration) {
            is MainConfiguration.Profile -> MainChild.Profile(
                createProfileComponent(componentContext)
            )
            is MainConfiguration.Search -> MainChild.Search(
                createSearchComponent(componentContext)
            )
            is MainConfiguration.ChatList -> MainChild.ChatList(
                createChatListComponent(
                    componentContext,
                    { chatId -> navigateToChat(chatId) }
                )
            )
            is MainConfiguration.Chat -> MainChild.Chat(
                createChatComponent(componentContext, configuration.chatId)
            )
        }

        fun navigateToProfile() {
            mainNavigation.push(MainConfiguration.Profile)
        }

        fun navigateToSearch() {
            mainNavigation.push(MainConfiguration.Search)
        }

        fun navigateToChatList() {
            mainNavigation.push(MainConfiguration.ChatList)
        }

        fun navigateToChat(chatId: String) {
            mainNavigation.push(MainConfiguration.Chat(chatId))
        }

        fun logout() {
            this@RootComponent.logout()
        }

        @Serializable
        sealed class MainConfiguration {
            @Serializable
            object Profile : MainConfiguration()

            @Serializable
            object Search : MainConfiguration()

            @Serializable
            object ChatList : MainConfiguration()

            @Serializable
            data class Chat(val chatId: String) : MainConfiguration()
        }

        sealed class MainChild {
            data class Profile(val component: ProfileComponent) : MainChild()
            data class Search(val component: SearchComponent) : MainChild()
            data class ChatList(val component: ChatListComponent) : MainChild()
            data class Chat(val component: ChatComponent) : MainChild()
        }
    }
}