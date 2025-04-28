package com.brigadka.app.presentation.auth.login

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.brigadka.app.data.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginComponent(
    componentContext: ComponentContext,
    private val onRegisterClick: () -> Unit,
    private val onLoginSuccess: (String) -> Unit,
    private val authRepository: AuthRepository
) : ComponentContext by componentContext {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    init {
        lifecycle.doOnCreate {
            // Any initialization logic
        }

        lifecycle.doOnDestroy {
            scope.cancel()
        }
    }

    fun onEmailChanged(email: String) {
        _state.update { it.copy(email = email) }
    }

    fun onPasswordChanged(password: String) {
        _state.update { it.copy(password = password) }
    }

    fun onLoginClick() {
        if (!validateInput()) return

        _state.update { it.copy(isLoading = true, error = null) }

        scope.launch {
            try {
                val result = authRepository.login(_state.value.email, _state.value.password)
                _state.update { it.copy(isLoading = false) }
                result.token?.let { token -> onLoginSuccess(token) }
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

    fun onRegisterClick() {
        onRegisterClick()
    }

    private fun validateInput(): Boolean {
        val emailError = if (_state.value.email.isBlank()) "Email cannot be empty" else null
        val passwordError = if (_state.value.password.isBlank()) "Password cannot be empty" else null

        _state.update {
            it.copy(
                emailError = emailError,
                passwordError = passwordError
            )
        }

        return emailError == null && passwordError == null
    }

    data class LoginState(
        val email: String = "",
        val password: String = "",
        val emailError: String? = null,
        val passwordError: String? = null,
        val isLoading: Boolean = false,
        val error: String? = null
    )
}