package com.brigadka.app.domain.session

import com.brigadka.app.data.api.BrigadkaApiServiceUnauthorized
import com.brigadka.app.data.api.models.LoginRequest
import com.brigadka.app.data.api.models.RegisterRequest
import com.brigadka.app.data.repository.AuthTokenRepository
import com.brigadka.app.data.repository.Token
import com.brigadka.app.data.repository.UserDataRepository

interface SessionManager {
    suspend fun login(email: String, password: String): AuthResult
    suspend fun register(
        email: String,
        password: String,
    ): AuthResult
    suspend fun logout()
}

class SessionManagerImpl(
    private val apiService: BrigadkaApiServiceUnauthorized,
    private val authTokenRepository: AuthTokenRepository,
    private val userDataRepository: UserDataRepository,
) : SessionManager {
    override suspend fun login(email: String, password: String): AuthResult {
        return try {
            val response = apiService.login(LoginRequest(email, password))
            val token = Token(
                accessToken = response.token,
                refreshToken = response.refresh_token
            )
            authTokenRepository.saveToken(token)
            userDataRepository.setCurrentUserId(response.user_id)

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
            val response = apiService.register(RegisterRequest(email = email, password = password))
            val token = Token(
                accessToken = response.token,
                refreshToken = response.refresh_token
            )
            authTokenRepository.saveToken(token)
            userDataRepository.setCurrentUserId(response.user_id)

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
    override suspend fun logout() {
        authTokenRepository.clearToken()
    }
}

data class AuthResult(
    val success: Boolean,
    val token: String? = null,
    val userId: Int? = null,
    val error: String? = null
)