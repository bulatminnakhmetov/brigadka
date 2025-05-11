package com.brigadka.app.presentation.root

import MainComponent
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.brigadka.app.data.api.models.Profile
import com.brigadka.app.data.repository.AuthRepository
import com.brigadka.app.data.repository.ProfileRepository
import com.brigadka.app.data.repository.UserDataRepository
import com.brigadka.app.presentation.auth.AuthComponent
import com.brigadka.app.presentation.chat.conversation.ChatComponent
import com.brigadka.app.presentation.chat.list.ChatListComponent
import com.brigadka.app.presentation.onboarding.OnboardingComponent
import com.brigadka.app.presentation.profile.view.ProfileViewComponent
import com.brigadka.app.presentation.search.SearchComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

class RootComponent(
    componentContext: ComponentContext,
    private val authRepository: AuthRepository,
    private val userDataRepository: UserDataRepository,
    private val profileRepository: ProfileRepository,
    private val createOnboardingComponent: (ComponentContext, (Profile) -> Unit) -> OnboardingComponent,
    private val createAuthComponent: (ComponentContext, (String) -> Unit) -> AuthComponent,
    private val createMainComponent: (ComponentContext) -> MainComponent,
) : ComponentContext by componentContext {

    private val navigation = StackNavigation<Configuration>()

    private val stack = childStack(
        source = navigation,
        initialConfiguration = Configuration.Auth,
        serializer = Configuration.serializer(),
        handleBackButton = true,
        childFactory = ::createChild
    )

    val childStack: Value<ChildStack<Configuration, Child>> = stack

    init {
        // Observe the current user ID and profile ID
        CoroutineScope(Dispatchers.Default).launch {
            // TODO: if profile is loading, onboarding will be shown, but it should be loading screen
            userDataRepository.currentUserId.combine(profileRepository.currentUserProfile) { userId, profile ->
                if (userId == null) {
                    Configuration.Auth
                } else {
                    if (profile == null) {
                        Configuration.Onboarding
                    } else {
                        Configuration.Main
                    }
                }
            }.collect { configuration ->
                withContext(Dispatchers.Main) {
                    navigation.replaceAll(configuration)
                }
            }
        }
    }

    private fun createChild(
        configuration: Configuration,
        componentContext: ComponentContext
    ): Child = when (configuration) {
        is Configuration.Auth -> Child.Auth(
            createAuthComponent(componentContext, {}) // TODO: why empty?
        )
        is Configuration.Main -> Child.Main(
            createMainComponent(componentContext)
        )
        is Configuration.Onboarding -> Child.Onboarding(
            createOnboardingComponent(
                componentContext,
                { navigation.replaceAll(Configuration.Main) },
            )
        )
    }

    fun logout() {
        runBlocking {
            authRepository.logout()
        }

        navigation.replaceAll(Configuration.Auth)
    }

    @Serializable
    sealed class Configuration {
        @Serializable
        data object Auth : Configuration()

        @Serializable
        data object Main : Configuration()

        @Serializable
        data object Onboarding : Configuration()
    }

    sealed class Child {
        data class Auth(val component: AuthComponent) : Child()
        data class Main(val component: MainComponent) : Child()
        data class Onboarding(val component: OnboardingComponent) : Child()
    }
}