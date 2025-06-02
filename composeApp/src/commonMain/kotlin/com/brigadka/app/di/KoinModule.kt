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
import com.brigadka.app.data.api.websocket.ChatWebSocketClient
import com.brigadka.app.domain.session.SessionManager
import com.brigadka.app.domain.session.SessionManagerImpl
import com.brigadka.app.data.repository.MediaRepository
import com.brigadka.app.data.repository.MediaRepositoryImpl
import com.brigadka.app.data.repository.ProfileRepository
import com.brigadka.app.data.repository.ProfileRepositoryImpl
import com.brigadka.app.data.repository.UserRepository
import com.brigadka.app.data.repository.UserRepositoryImpl
import com.brigadka.app.data.repository.Token
import com.brigadka.app.data.repository.AuthTokenRepository
import com.brigadka.app.data.repository.AuthTokenRepositoryImpl
import com.brigadka.app.data.repository.PushTokenRepository
import com.brigadka.app.data.repository.PushTokenRepositoryImpl
import com.brigadka.app.presentation.auth.AuthComponent
import com.brigadka.app.presentation.chat.conversation.ChatComponent
import com.brigadka.app.presentation.chat.list.ChatListComponent
import com.brigadka.app.presentation.onboarding.OnboardingComponent
import com.brigadka.app.presentation.profile.view.ProfileViewComponent
import com.brigadka.app.presentation.root.RootComponent
import com.brigadka.app.presentation.search.SearchComponent
import com.brigadka.app.domain.push.PushTokenRegistrationManager
import com.brigadka.app.domain.push.PushTokenRegistrationManagerImpl
import com.brigadka.app.domain.verification.VerificationManager
import com.brigadka.app.domain.verification.VerificationManagerImpl
import com.brigadka.app.presentation.auth.register.RegisterComponent
import com.brigadka.app.presentation.auth.register.verification.VerificationComponent
import com.brigadka.app.presentation.common.UIEventBus
import com.brigadka.app.presentation.profile.edit.EditProfileComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.module


interface ProfileViewComponentFactory {
    fun create(
        context: ComponentContext,
        userID: Int? = null,
        onBackClick: () -> Unit = {},
        onContactClick: (() -> Unit)? = null
    ): ProfileViewComponent
}

interface ChatListComponentFactory {
    fun create(context: ComponentContext): ChatListComponent
}

interface MainComponentFactory {
    fun create(context: ComponentContext): MainComponent
}

interface ChatComponentFactory {
    fun create(
        context: ComponentContext,
        chatID: String,
        otherUserID: Int,
        onBackClick: () -> Unit
    ): ChatComponent
}

interface EditProfileComponentFactory {
    fun create(
        context: ComponentContext,
        onBackClick: () -> Unit
    ): EditProfileComponent
}

interface AuthComponentFactory {
    fun create(context: ComponentContext): AuthComponent
}

interface SearchComponentFactory {
    fun create(context: ComponentContext): SearchComponent
}

interface OnboardingComponentFactory {
    fun create(context: ComponentContext, onFinished: () -> Unit): OnboardingComponent
}

interface RootComponentFactory {
    fun create(rootContext: ComponentContext): RootComponent
}

interface VerificationComponentFactory {
    fun create(context: ComponentContext): VerificationComponent
}

interface VerificationManagerFactory {
    fun create(scope: CoroutineScope): VerificationManager
}

interface RegisterComponentFactory {
    fun create(context: ComponentContext, onLoginClick: () -> Unit): RegisterComponent
}

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
    single {
        // Provide a CoroutineScope for the Koin module
        CoroutineScope(Dispatchers.Main + SupervisorJob())
    }

    single<UIEventBus> {
        UIEventBus()
    }

    single<AuthTokenRepository> { AuthTokenRepositoryImpl(get()) }
    single<PushTokenRepository> { PushTokenRepositoryImpl(get()) }

    single<UserRepository> { UserRepositoryImpl(get()) }

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
        val authTokenRepository: AuthTokenRepository = get()
        val refreshToken: suspend (String) -> Token? = { refreshToken: String ->
            try {
                val service: BrigadkaApiServiceUnauthorized = get()
                val response = service.refreshToken(refreshToken)
                Token(response.token, response.token)
            } catch (e: Exception) {
                null
            }
        }
        createAuthorizedKtorClient(authTokenRepository, refreshToken)
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

    single<VerificationManagerFactory> {
        object : VerificationManagerFactory {
            override fun create(scope: CoroutineScope): VerificationManager {
                return VerificationManagerImpl(
                    coroutineScope = scope,
                    apiService = get(),
                    sessionManager = get(),
                    uiEventEmitter = get<UIEventBus>()
                )
            }
        }
    }

    // Web socket client
    single<ChatWebSocketClient>(createdAtStart = true) {
        ChatWebSocketClient(
            coroutineScope = get(),
            authTokenRepository = get(),
            httpClient = get(named(HttpClientType.AUTHORIZED)),
            baseUrl = BASE_URL,
            uiEventEmitter = get<UIEventBus>()
        )
    }


    single<SessionManager> { SessionManagerImpl(get(), get(), get(), get(), get()) }

    single<PushTokenRegistrationManager>(createdAtStart = true) {
        PushTokenRegistrationManagerImpl(
            coroutineScope = get(),
            userRepository = get(),
            sessionManager = get(),
            pushTokenRepository = get(),
            apiService = get(),
            deviceIdProvider = get()
        )
    }



    // Component factories
    single<AuthComponentFactory> {
        object : AuthComponentFactory {
            override fun create(context: ComponentContext): AuthComponent {
                return AuthComponent(
                    componentContext = context,
                    uiEventFlowProvider = get<UIEventBus>(),
                    sessionManager = get(),
                    registerComponentFactory = get(),
                    userRepository = get(),
                )
            }
        }
    }

    factory<EditProfileComponentFactory> {
        object: EditProfileComponentFactory {
            override fun create(
                context: ComponentContext,
                onBackClick: () -> Unit
            ): EditProfileComponent {
                return EditProfileComponent(
                    componentContext = context,
                    uiEventEmitter = get<UIEventBus>(),
                    profileRepository = get(),
                    mediaRepository = get(),
                    onFinished = onBackClick,
                    onBack = onBackClick,
                )
            }
        }
    }

    single<ProfileViewComponentFactory> {
        object: ProfileViewComponentFactory {
            override fun create(
                context: ComponentContext,
                userID: Int?,
                onBackClick: () -> Unit,
                onContactClick: (() -> Unit)?
            ): ProfileViewComponent {
                return ProfileViewComponent(
                    componentContext = context,
                    uiEventEmitter = get<UIEventBus>(),
                    brigadkaApiService = get(),
                    profileRepository = get(),
                    userRepository = get(),
                    sessionManager = get(),
                    userID = userID,
                    onBackClick = onBackClick,
                    chatComponentFactory = get(),
                    editProfileComponentFactory = get(),
                    onContactClick = onContactClick,
                )
            }
        }
    }

    single<VerificationComponentFactory> {
        object : VerificationComponentFactory {
            override fun create(context: ComponentContext): VerificationComponent {
                return VerificationComponent(
                    componentContext = context,
                    verificationManagerFactory = get(),
                    sessionManager = get()
                )
            }
        }
    }

    single<RegisterComponentFactory> {
        object : RegisterComponentFactory {
            override fun create(context: ComponentContext, onLoginClick: () -> Unit): RegisterComponent {
                return RegisterComponent(
                    componentContext = context,
                    onLoginClick = onLoginClick,
                    verificationComponentFactory = get(),
                    sessionManager = get(),
                    userRepository = get(),
                )
            }
        }
    }

    single<SearchComponentFactory> {
        object : SearchComponentFactory {
            override fun create(context: ComponentContext): SearchComponent {
                return SearchComponent(
                    componentContext = context,
                    uiEventEmitter = get<UIEventBus>(),
                    profileRepository = get(),
                    profileViewComponentFactory = get()
                )
            }
        }
    }

    // Replace ChatComponentFactory class with interface
    single<ChatComponentFactory> {
        object : ChatComponentFactory {
            override fun create(
                context: ComponentContext,
                chatID: String,
                otherUserID: Int,
                onBackClick: () -> Unit
            ): ChatComponent {
                return ChatComponent(
                    componentContext = context,
                    uiEventEmitter = get<UIEventBus>(),
                    userRepository = get(),
                    profileRepository = get(),
                    api = get(),
                    webSocketClient = get(),
                    profileViewComponentFactory = get(),
                    chatID = chatID,
                    otherUserID = otherUserID,
                    onBackClick = onBackClick
                )
            }
        }
    }

    single<OnboardingComponentFactory> {
        object : OnboardingComponentFactory {
            override fun create(context: ComponentContext, onFinished: () -> Unit): OnboardingComponent {
                return OnboardingComponent(
                    componentContext = context,
                    mediaRepository = get(),
                    profileRepository = get(),
                    userRepository = get(),
                    onFinished = onFinished
                )
            }
        }
    }

    single<ChatListComponentFactory> {
        object : ChatListComponentFactory {
            override fun create(context: ComponentContext): ChatListComponent {
                return ChatListComponent(
                    componentContext = context,
                    uiEventEmitter = get<UIEventBus>(),
                    userRepository = get(),
                    webSocketClient = get(),
                    profileRepository = get(),
                    api = get(),
                    chatComponentFactory = get()
                )
            }
        }
    }

    single<MainComponentFactory> {
        object : MainComponentFactory {
            override fun create(context: ComponentContext): MainComponent {
                return MainComponent(
                    componentContext = context,
                    userRepository = get(),
                    chatListComponentFactory = get(),
                    profileViewComponentFactory = get(),
                    uiEventFlowProvider = get<UIEventBus>(),
                    searchComponentFactory = get()
                )
            }
        }
    }

    single<RootComponentFactory> {
        object : RootComponentFactory {
            override fun create(rootContext: ComponentContext): RootComponent {
                return RootComponent(
                    componentContext = rootContext,
                    userRepository = get(),
                    profileRepository = get(),
                    createAuthComponent = { context ->
                        get<AuthComponentFactory>().create(context)
                    },
                    createOnboardingComponent = { context, onFinished ->
                        get<OnboardingComponentFactory>().create(context, onFinished)
                    },
                    mainComponentFactory = get()
                )
            }
        }
    }

    // Repositories
    single<ProfileRepository> { ProfileRepositoryImpl(get(), get(), get()) }
    single<MediaRepository> { MediaRepositoryImpl(get()) }
}

enum class HttpClientType {
    AUTHORIZED, UNAUTHORIZED
}

expect val platformModule: Module