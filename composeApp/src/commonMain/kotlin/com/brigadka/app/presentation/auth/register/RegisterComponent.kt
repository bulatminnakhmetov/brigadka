package com.brigadka.app.presentation.auth.register

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.brigadka.app.common.coroutineScope
import com.brigadka.app.data.repository.UserRepository
import com.brigadka.app.di.VerificationComponentFactory
import com.brigadka.app.domain.session.SessionManager
import com.brigadka.app.presentation.auth.login.LoginComponent
import com.brigadka.app.presentation.auth.register.verification.VerificationComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class RegisterComponent(
    componentContext: ComponentContext,
    val onLoginClick: () -> Unit,
    private val sessionManager: SessionManager,
    private val userRepository: UserRepository,
    private val verificationComponentFactory: VerificationComponentFactory,
) : ComponentContext by componentContext {

    private val scope = coroutineScope()

    private val _state = MutableStateFlow(RegisterState())
    val state: StateFlow<RegisterState> = _state.asStateFlow()

    init {
        lifecycle.doOnDestroy {
            scope.cancel()
        }
        scope.launch {
            // Observe user registration state
            userRepository.isLoggedIn.collect { isLoggedIn ->
                if (isLoggedIn) {
                    navigateTo(Configuration.Verification)
                } else {
                    navigateTo(Configuration.Register)
                }
            }
        }
    }

    fun onEmailChanged(email: String) {
        _state.update { it.copy(email = email) }
    }

    fun onPasswordChanged(password: String) {
        _state.update { it.copy(password = password) }
    }

    fun onRegisterClick() {
        if (!validateInput()) return

        _state.update { it.copy(isLoading = true, error = null) }

        scope.launch {
            try {
                sessionManager.register(
                    email = _state.value.email,
                    password = _state.value.password
                )
                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }

    private fun validateInput(): Boolean {
        val emailError = when {
            _state.value.email.isBlank() -> "Email cannot be empty"
            !_state.value.email.contains("@") -> "Invalid email format"
            else -> null
        }

        val passwordError = when {
            _state.value.password.isBlank() -> "Password cannot be empty"
            _state.value.password.length < 8 -> "Password must be at least 8 characters"
            else -> null
        }

        _state.update {
            it.copy(
                emailError = emailError,
                passwordError = passwordError,
            )
        }

        return emailError == null && passwordError == null
    }

    data class RegisterState(
        val email: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
        val emailError: String? = null,
        val passwordError: String? = null
    )


    private val navigation = StackNavigation<Configuration>()

    private val stack = childStack(
        source = navigation,
        serializer = Configuration.serializer(),
        initialConfiguration = Configuration.Register,
        handleBackButton = true,
        childFactory = ::createChild
    )

    val childStack: Value<ChildStack<*, Child>> = stack

    private fun createChild(
        configuration: Configuration,
        componentContext: ComponentContext
    ): Child = when (configuration) {
        is Configuration.Verification -> Child.Verification(
            verificationComponentFactory.create(componentContext)
        )
        is Configuration.Register -> Child.Register
    }

    // TODO: same fuctionality in MainComponent, consider moving to base class
    fun navigateTo(screen: Configuration) {
        val stackItems = childStack.value.items
        val existingIndex = stackItems.indexOfFirst { it.configuration == screen }

        if (childStack.value.active.configuration == screen) {
            // Already on this screen, do nothing
            return
        }

        if (existingIndex != -1) {
            // Screen is in stack, bring to front
            navigation.bringToFront(screen)
        } else {
            // Not in stack, push it
            navigation.pushNew(screen)
        }
    }

    @Serializable
    sealed class Configuration {
        @Serializable
        object Register : Configuration()

        @Serializable
        object Verification : Configuration()
    }

    sealed class Child {
        data class Verification(val component: VerificationComponent) : Child()
        data object Register : Child()
    }
}