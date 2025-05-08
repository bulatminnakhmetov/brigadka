import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import com.brigadka.app.presentation.chat.conversation.ChatComponent
import com.brigadka.app.presentation.chat.list.ChatListComponent
import com.brigadka.app.presentation.profile.view.ProfileViewComponent
import com.brigadka.app.presentation.search.SearchComponent
import kotlinx.serialization.Serializable

class MainComponent(
    componentContext: ComponentContext,
    private val createProfileViewComponent: (ComponentContext) -> ProfileViewComponent,
    private val createSearchComponent: (ComponentContext) -> SearchComponent,
    private val createChatListComponent: (ComponentContext, (String) -> Unit) -> ChatListComponent,
    private val createChatComponent: (ComponentContext, String) -> ChatComponent
) : ComponentContext by componentContext {

    private val mainNavigation = StackNavigation<Config>()
    private val mainStack = childStack(
        source = mainNavigation,
        initialConfiguration = Config.Profile,
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
            createProfileViewComponent(componentContext)
        )
        is Config.Search -> Child.Search(
            createSearchComponent(componentContext)
        )
        is Config.ChatList -> Child.ChatList(
            createChatListComponent(
                componentContext,
                { chatId -> navigateToChat(chatId) }
            )
        )
        is Config.Chat -> Child.Chat(
            createChatComponent(componentContext, configuration.chatId)
        )
    }

    fun navigateToProfile() {
        mainNavigation.pushNew(Config.Profile)
    }

    fun navigateToSearch() {
        mainNavigation.pushNew(Config.Search)
    }

    fun navigateToChatList() {
        mainNavigation.pushNew(Config.ChatList)
    }

    fun navigateToChat(chatId: String) {
        mainNavigation.pushNew(Config.Chat(chatId))
    }
}

// These need to be outside the inner class to be properly serializable
@Serializable
sealed class Config {
    @Serializable
    object Profile : Config()

    @Serializable
    object Search : Config()

    @Serializable
    object ChatList : Config()

    @Serializable
    data class Chat(val chatId: String) : Config()
}

sealed class Child {
    data class Profile(val component: ProfileViewComponent) : Child()
    data class Search(val component: SearchComponent) : Child()
    data class ChatList(val component: ChatListComponent) : Child()
    data class Chat(val component: ChatComponent) : Child()
}