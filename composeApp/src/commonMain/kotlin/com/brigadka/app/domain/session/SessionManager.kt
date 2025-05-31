package com.brigadka.app.domain.session

import com.brigadka.app.data.api.BrigadkaApiService
import com.brigadka.app.data.api.BrigadkaApiServiceAuthorized
import com.brigadka.app.data.api.BrigadkaApiServiceUnauthorized
import com.brigadka.app.data.api.models.LoginRequest
import com.brigadka.app.data.api.models.RegisterRequest
import com.brigadka.app.data.repository.AuthTokenRepository
import com.brigadka.app.data.repository.Token
import com.brigadka.app.data.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


sealed class LoggingState {
    object LoggedIn : LoggingState()
    object LoggedOut : LoggingState()
}

typealias LogoutObserver = suspend () -> Unit

interface SessionManager {
    fun registerLogoutObserver(observer: LogoutObserver)
    suspend fun login(email: String, password: String): AuthResult
    suspend fun register(
        email: String,
        password: String,
    ): AuthResult
    fun logout()
}

class SessionManagerImpl(
    private val coroutineScope: CoroutineScope,
    private val apiServiceAuthorized: BrigadkaApiServiceAuthorized,
    private val apiServiceUnauthorized: BrigadkaApiServiceUnauthorized,
    private val authTokenRepository: AuthTokenRepository,
    private val userRepository: UserRepository,
) : SessionManager {
    private val logoutObservers = mutableListOf<LogoutObserver>()

    override fun registerLogoutObserver(observer: LogoutObserver) {
        logoutObservers += observer
    }

    override suspend fun login(email: String, password: String): AuthResult {
        return try {
            val response = apiServiceUnauthorized.login(LoginRequest(email, password))
            val token = Token(
                accessToken = response.token,
                refreshToken = response.refresh_token
            )
            authTokenRepository.saveToken(token)
            userRepository.setCurrentUserId(response.user_id)
            userRepository.setIsVerified(response.email_verified)

            AuthResult(
                success = true,
                token = response.token,
                userId = response.user_id
            )
        } catch (e: Exception) {
            AuthResult(
                success = false,
                error = e.message ?: "Login failed"
            )
        }
    }

    // Similar update for register method
    override suspend fun register(
        email: String,
        password: String,
    ): AuthResult {
        return try {
            val response = apiServiceUnauthorized.register(RegisterRequest(email = email, password = password))
            val token = Token(
                accessToken = response.token,
                refreshToken = response.refresh_token
            )
            authTokenRepository.saveToken(token)
            userRepository.setCurrentUserId(response.user_id)

            AuthResult(
                success = true,
                token = response.token,
                userId = response.user_id
            )
        } catch (e: Exception) {
            AuthResult(
                success = false,
                error = e.message ?: "Registration failed"
            )
        }
    }

    // Also update logout to unregister the push token
    override fun logout() {
        coroutineScope.launch {
            // Call all observers in parallel
            logoutObservers.map { observer ->
                async { observer.invoke() }
            }.awaitAll()

            // Now it's safe to clear token and session
            authTokenRepository.clearToken()
            userRepository.clearUser()
            apiServiceAuthorized.clearTokens()
        }
    }
}

data class AuthResult(
    val success: Boolean,
    val token: String? = null,
    val userId: Int? = null,
    val error: String? = null
)