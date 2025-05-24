package com.brigadka.app.presentation.profile.view

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.brigadka.app.common.coroutineScope
import com.brigadka.app.data.api.BrigadkaApiService
import com.brigadka.app.data.repository.ProfileRepository
import com.brigadka.app.presentation.profile.common.LoadableValue
import com.brigadka.app.data.repository.ProfileView
import com.brigadka.app.data.repository.UserRepository
import com.brigadka.app.di.CreateChatComponent
import com.brigadka.app.di.CreateEditProfileComponent
import com.brigadka.app.domain.session.SessionManager
import com.brigadka.app.presentation.chat.conversation.ChatComponent
import com.brigadka.app.presentation.common.TopBarState
import com.brigadka.app.presentation.common.UIEvent
import com.brigadka.app.presentation.common.UIEventEmitter
import com.brigadka.app.presentation.profile.edit.EditProfileComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

data class ProfileViewTopBarState(
    val isCurrentUser: Boolean,
    val onBackClick: () -> Unit,
    val onEditProfile: () -> Unit,
    val onLogout: () -> Unit
): TopBarState

class ProfileViewComponent(
    componentContext: ComponentContext,
    private val uiEventEmitter: UIEventEmitter,
    private val brigadkaApiService: BrigadkaApiService,
    private val userRepository: UserRepository,
    private val profileRepository: ProfileRepository,
    private val sessionManager: SessionManager,
    private val userID: Int? = null,
    val onBackClick: () -> Unit,
    val createChatComponent: CreateChatComponent,
    val createEditProfileComponent: CreateEditProfileComponent
) : ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    private val _stack = childStack(
        source = navigation,
        initialConfiguration = Config.Profile,
        handleBackButton = true,
        childFactory = ::createChild,
        serializer = Config.serializer()
    )

    val childStack: Value<ChildStack<Config, Child>> = _stack

    private val _profileView = MutableValue<LoadableValue<ProfileView>>(LoadableValue(isLoading = true))
    val profileView: Value<LoadableValue<ProfileView>> = _profileView

    private val coroutineScope = coroutineScope()

    val topBarState: ProfileViewTopBarState
        get() = ProfileViewTopBarState(
            isCurrentUser = isCurrentUser,
            onBackClick = onBackClick,
            onEditProfile = this::onEditProfile,
            onLogout = { coroutineScope.launch { sessionManager.logout() } }
        )

    init {
        loadProfileView()
    }

    private fun loadProfileView() {
        coroutineScope.launch {
            val view = profileRepository.getProfileView(userID)
            _profileView.update { it.copy(isLoading = false, value = view) }
        }
    }

    private fun createChild(config: Config, componentContext: ComponentContext): Child {
        return when (config) {
            is Config.Profile -> Child.Profile
            is Config.EditProfile -> Child.EditProfile(
                createEditProfileComponent(
                    componentContext,
                    this::onNavigateBack
                )
            )
            is Config.Chat -> Child.Chat(
                createChatComponent(
                    componentContext,
                    config.chatId,
                    this::onNavigateBack
                )
            )
        }
    }

    private fun onNavigateBack() {
        navigation.pop()

        // Refresh profile data when returning from edit
        if (_stack.value.active.configuration is Config.Profile) {
            loadProfileView()
        }
    }

    suspend fun showTopBar() {
        uiEventEmitter.emit(UIEvent.TopBarUpdate(topBarState))
    }

    fun onEditProfile() {
        if (!isCurrentUser) {
            return
        }
        navigation.push(Config.EditProfile)
    }

    fun onContactClick() {
        if (userID == null) {
            return
        }
        coroutineScope.launch {
            try {
                val chatId = brigadkaApiService.getOrCreateDirectChat(userID).chat_id
                withContext(Dispatchers.Main) {
                    navigation.push(Config.Chat(chatId))
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private val isCurrentUser: Boolean
        get() = userID == null || userID == userRepository.requireUserId()

    val isEditable: Boolean
        get() = isCurrentUser

    val isContactable: Boolean
        get() = !isCurrentUser

    @Serializable
    sealed class Config {
        @Serializable
        object Profile : Config()

        @Serializable
        object EditProfile : Config()

        @Serializable
        data class Chat(val chatId: String) : Config()
    }

    sealed class Child {
        object Profile : Child()
        data class EditProfile(val component: EditProfileComponent) : Child()
        data class Chat(val component: ChatComponent) : Child()
    }
}