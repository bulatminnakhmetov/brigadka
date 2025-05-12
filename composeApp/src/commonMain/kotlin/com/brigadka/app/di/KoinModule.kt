package com.brigadka.app.di

import MainComponent
import com.arkivanov.decompose.ComponentContext
import com.brigadka.app.BASE_URL
import com.brigadka.app.data.api.BrigadkaApiService
import com.brigadka.app.data.api.BrigadkaApiServiceAuthorized
import com.brigadka.app.data.api.BrigadkaApiServiceAuthorizedImpl
import com.brigadka.app.data.api.BrigadkaApiServiceImpl
import com.brigadka.app.data.api.BrigadkaApiServiceUnauthorized
import com.brigadka.app.data.api.BrigadkaApiServiceUnauthorizedImpl
import com.brigadka.app.data.api.createAuthorizedKtorClient
import com.brigadka.app.data.api.createUnauthorizedKtorClient
import com.brigadka.app.data.api.models.Profile
import com.brigadka.app.data.api.websocket.ChatWebSocketClient
import com.brigadka.app.data.repository.AuthRepository
import com.brigadka.app.data.repository.AuthRepositoryImpl
import com.brigadka.app.data.repository.MediaRepository
import com.brigadka.app.data.repository.MediaRepositoryImpl
import com.brigadka.app.data.repository.ProfileRepository
import com.brigadka.app.data.repository.ProfileRepositoryImpl
import com.brigadka.app.data.repository.UserDataRepository
import com.brigadka.app.data.repository.UserDataRepositoryImpl
import com.brigadka.app.data.repository.Token
import com.brigadka.app.data.repository.TokenRepository
import com.brigadka.app.data.repository.TokenRepositoryImpl
import com.brigadka.app.presentation.auth.AuthComponent
import com.brigadka.app.presentation.chat.conversation.ChatComponent
import com.brigadka.app.presentation.chat.list.ChatListComponent
import com.brigadka.app.presentation.onboarding.OnboardingComponent
import com.brigadka.app.presentation.profile.view.ProfileViewComponent
import com.brigadka.app.presentation.root.RootComponent
import com.brigadka.app.presentation.search.SearchComponent
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.module


typealias CreateProfileViewComponent = (
    context: ComponentContext,
    userID: Int?,
    onEditProfile: () -> Unit,
    onContactClick: (String) -> Unit
        ) -> ProfileViewComponent

typealias CreateChatComponent = (
    context: ComponentContext,
    chatID: String,
    onBackClick: () -> Unit
        ) -> ChatComponent

fun initKoin(appModule: Module = module { }, additionalConfig: KoinApplication.() -> Unit = {}): KoinApplication {
    val koinApplication = startKoin {
        additionalConfig()
        modules(
            commonModule,
            platformModule,
            appModule
        )
    }

    // Return the configured KoinApplication
    return koinApplication
}

val commonModule = module {
    single<TokenRepository> { TokenRepositoryImpl(get()) }

    single<UserDataRepository> { UserDataRepositoryImpl(get()) }

    // HTTP clients with named qualifiers
    single(named(HttpClientType.UNAUTHORIZED)) {
        createUnauthorizedKtorClient()
    }

    // Define unauthorized API service first
    single<BrigadkaApiServiceUnauthorized> {
        BrigadkaApiServiceUnauthorizedImpl(
            client = get(named(HttpClientType.UNAUTHORIZED)),
            baseUrl = BASE_URL
        )
    }

    // Now the authorized client with the token refresher
    single(named(HttpClientType.AUTHORIZED)) {
        val tokenRepository: TokenRepository = get()
        val refreshToken: suspend (String) -> Token? = { refreshToken: String ->
            try {
                val service: BrigadkaApiServiceUnauthorized = get()
                val response = service.refreshToken(refreshToken)
                Token(response.token, response.token)
            } catch (e: Exception) {
                null
            }
        }
        createAuthorizedKtorClient(tokenRepository, refreshToken)
    }

    // Define authorized API service
    single<BrigadkaApiServiceAuthorized> {
        BrigadkaApiServiceAuthorizedImpl(
            client = get(named(HttpClientType.AUTHORIZED)),
            baseUrl = BASE_URL
        )
    }

    // Finally, combine them into the main API service
    single<BrigadkaApiService> {
        BrigadkaApiServiceImpl(
            unauthorizedService = get(),
            authorizedService = get()
        )
    }

    // Web socket client
    single<ChatWebSocketClient> {
        ChatWebSocketClient(
            tokenRepository = get(),
            httpClient = get(named(HttpClientType.AUTHORIZED)),
            baseUrl = BASE_URL
        )
    }

    // Repositories
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get()) }

    // Component factories
    factory { (context: ComponentContext, onAuthSuccess: (String) -> Unit) ->
        AuthComponent(
            componentContext = context,
            authRepository = get(),
            onAuthSuccess = onAuthSuccess
        )
    }

    factory { (
                  context: ComponentContext,
                  userID: Int?,
                  onEditProfile: () -> Unit,
                  onContactClick: (String) -> Unit,
    ) ->
        ProfileViewComponent(
            componentContext = context,
            brigadkaApiService = get(),
            profileRepository = get(),
            userDataRepository = get(),
            userID = userID,
            onEditProfile = onEditProfile,
            onContactClick = onContactClick,
        )
    }

    factory { (context: ComponentContext, onProfileClick: (Int) -> Unit) ->
        SearchComponent(
            componentContext = context,
            profileRepository = get(),
            onProfileClickCallback = onProfileClick
        )
    }

    factory { (context: ComponentContext, onChatSelected: (String) -> Unit) ->
        ChatListComponent(
            componentContext = context,
            onChatSelected = onChatSelected
            // Add your chat list component dependencies here
        )
    }

    factory { (context: ComponentContext, chatID: String, onBackClick: () -> Unit) ->
        ChatComponent(
            componentContext = context,
            userDataRepository = get(),
            api = get(),
            webSocketClient = get(),
            chatID = chatID,
            onBackClick = onBackClick
        )
    }

    factory { (mainContext: ComponentContext) ->
        MainComponent(
            componentContext = mainContext,
            createProfileViewComponent = { context, userID, onEditProfile, onContactClick ->
                get<ProfileViewComponent> { parametersOf(context, userID, onEditProfile, onContactClick) }
            },
            createSearchComponent = { context, onProfileClick ->
                get<SearchComponent> { parametersOf(context, onProfileClick) }
            },
            createChatListComponent = { context, onChatSelected ->
                get<ChatListComponent> { parametersOf(context, onChatSelected) }
            },
            createChatComponent = { context, chatID, onBackClick ->
                get<ChatComponent> { parametersOf(context, chatID, onBackClick) }
            },
        )
    }

    // Repositories
    single<ProfileRepository> { ProfileRepositoryImpl(get(), get()) }
    single<MediaRepository> { MediaRepositoryImpl(get()) }

    // Component factories
    factory { (context: ComponentContext, onFinished: (Profile) -> Unit) ->
        OnboardingComponent(
            componentContext = context,
            mediaRepository = get(),
            profileRepository = get(),
            userDataRepository = get(),
            onFinished = onFinished
        )
    }

    // Root component factory
    factory { (rootContext: ComponentContext) ->
        RootComponent(
            componentContext = rootContext,
            authRepository = get(),
            userDataRepository = get(),
            profileRepository = get(),
            createAuthComponent = { context, onSuccess ->
                get<AuthComponent> { parametersOf(context, onSuccess) }
            },
            createOnboardingComponent = { context, onFinished ->
                get<OnboardingComponent> { parametersOf(context, onFinished) }
            },
            createMainComponent = { context ->
                get<MainComponent> { parametersOf(context) }
            }
        )
    }
}

enum class HttpClientType {
    AUTHORIZED, UNAUTHORIZED
}

expect val platformModule: Module