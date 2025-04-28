package com.brigadka.app.presentation.auth.register

import com.arkivanov.decompose.ComponentContext
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

class RegisterComponent(
    componentContext: ComponentContext,
    private val onBackClick: () -> Unit,
    private val onRegisterSuccess: (String) -> Unit,
    private val authRepository: AuthRepository
) : ComponentContext by componentContext {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(RegisterState())
    val state: StateFlow<RegisterState> = _state.asStateFlow()

    init {
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

    fun onFullNameChanged(fullName: String) {
        _state.update { it.copy(fullName = fullName) }
    }

    fun onAgeChanged(age: String) {
        val ageInt = age.toIntOrNull()
        _state.update { it.copy(age = ageInt) }
    }

    fun onCityIdChanged(cityId: String) {
        val cityIdInt = cityId.toIntOrNull()
        _state.update { it.copy(cityId = cityIdInt) }
    }

    fun onGenderChanged(gender: String) {
        _state.update { it.copy(gender = gender) }
    }

    fun onRegisterClick() {
        if (!validateInput()) return

        _state.update { it.copy(isLoading = true, error = null) }

        scope.launch {
            try {
                val result = authRepository.register(
                    email = _state.value.email,
                    password = _state.value.password,
                    fullName = _state.value.fullName,
                    age = _state.value.age ?: 0,
                    cityId = _state.value.cityId ?: 0,
                    gender = _state.value.gender
                )
                _state.update { it.copy(isLoading = false) }
                result.token?.let { token -> onRegisterSuccess(token) }
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

    fun onBackClick() {
        onBackClick()
    }

    private fun validateInput(): Boolean {
        val emailError = when {
            _state.value.email.isBlank() -> "Email cannot be empty"
            !_state.value.email.contains("@") -> "Invalid email format"
            else -> null
        }

        val passwordError = when {
            _state.value.password.isBlank() -> "Password cannot be empty"
            _state.value.password.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }

        val fullNameError = if (_state.value.fullName.isBlank()) "Full name cannot be empty" else null
        val ageError = when (_state.value.age) {
            null -> "Age must be at least 18"
            in 0..17 -> "Age must be at least 18"
            else -> null
        }
        val cityIdError = if (_state.value.cityId == null) "City must be selected" else null
        val genderError = if (_state.value.gender.isBlank()) "Gender must be selected" else null

        _state.update {
            it.copy(
                emailError = emailError,
                passwordError = passwordError,
                fullNameError = fullNameError,
                ageError = ageError,
                cityIdError = cityIdError,
                genderError = genderError
            )
        }

        return emailError == null && passwordError == null &&
                fullNameError == null && ageError == null &&
                cityIdError == null && genderError == null
    }

    data class RegisterState(
        val email: String = "",
        val password: String = "",
        val fullName: String = "",
        val age: Int? = null,
        val cityId: Int? = null,
        val gender: String = "",
        val emailError: String? = null,
        val passwordError: String? = null,
        val fullNameError: String? = null,
        val ageError: String? = null,
        val cityIdError: String? = null,
        val genderError: String? = null,
        val isLoading: Boolean = false,
        val error: String? = null
    )
}