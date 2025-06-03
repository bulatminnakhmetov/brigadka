import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import com.brigadka.app.data.repository.UserRepository
import com.brigadka.app.di.ChatComponentFactory
import com.brigadka.app.di.ChatListComponentFactory
import com.brigadka.app.di.ProfileViewComponentFactory
import com.brigadka.app.di.SearchComponentFactory
import com.brigadka.app.presentation.chat.conversation.ChatComponent
import com.brigadka.app.presentation.chat.list.ChatListComponent
import com.brigadka.app.presentation.common.UIEventFlowProvider
import com.brigadka.app.presentation.profile.view.ProfileViewComponent
import com.brigadka.app.presentation.search.SearchComponent
import kotlinx.serialization.Serializable

class MainComponent(
    componentContext: ComponentContext,
    uiEventFlowProvider: UIEventFlowProvider,
    private val userRepository: UserRepository,
    private val profileViewComponentFactory: ProfileViewComponentFactory,
    private val searchComponentFactory: SearchComponentFactory,
    private val chatListComponentFactory: ChatListComponentFactory,
) : ComponentContext by componentContext, UIEventFlowProvider by uiEventFlowProvider {

    private val mainNavigation = StackNavigation<Config>()
    private val mainStack = childStack(
        source = mainNavigation,
        initialConfiguration = Config.Profile(userRepository.requireUserId()),
        serializer = Config.serializer(),
        handleBackButton = true,
        childFactory = ::createChild
    )

    val childStack: Value<ChildStack<Config, Child>> = mainStack

    private fun createChild(
        configuration: Config,
        componentContext: ComponentContext
    ): Child = when (configuration) {
        is Config.Profile -> Child.Profile(
            profileViewComponentFactory.create(
                context = componentContext,
                userID = configuration.userID,
            )
        )
        is Config.Search -> Child.Search(
            searchComponentFactory.create(componentContext)
        )
        is Config.ChatList -> Child.ChatList(
            chatListComponentFactory.create(componentContext)
        )
        else -> throw IllegalArgumentException("Unknown configuration: $configuration")
    }

    fun navigateTo(screen: Config) {
        val stackItems = childStack.value.items
        val existingIndex = stackItems.indexOfFirst { it.configuration == screen }

        if (childStack.value.active.configuration == screen) {
            // Already on this screen, do nothing
            return
        }

        if (existingIndex != -1) {
            // Screen is in stack, bring to front
            mainNavigation.bringToFront(screen)
        } else {
            // Not in stack, push it
            mainNavigation.pushNew(screen)
        }
    }

    fun navigateToProfile() {
        navigateTo(Config.Profile(userRepository.requireUserId()))
    }

    fun navigateToSearch() {
        navigateTo(Config.Search)
    }

    fun navigateToChatList() {
        navigateTo(Config.ChatList)
    }
}

// These need to be outside the inner class to be properly serializable
@Serializable
sealed class Config {
    @Serializable
    data class Profile(val userID: Int) : Config()

    @Serializable
    object Search : Config()

    @Serializable
    object ChatList : Config()
}

sealed class Child {
    data class Profile(val component: ProfileViewComponent) : Child()
    data class Search(val component: SearchComponent) : Child()
    data class ChatList(val component: ChatListComponent) : Child()
}