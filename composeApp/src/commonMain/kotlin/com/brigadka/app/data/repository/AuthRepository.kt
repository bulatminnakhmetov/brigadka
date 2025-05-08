package com.brigadka.app.data.repository

import com.brigadka.app.data.api.BrigadkaApiServiceUnauthorized
import com.brigadka.app.data.api.models.LoginRequest
import com.brigadka.app.data.api.models.RegisterRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface AuthRepository {
    val isAuthenticated: Flow<Boolean>
    suspend fun login(email: String, password: String): AuthResult
    suspend fun register(
        email: String,
        password: String,
    ): AuthResult
    suspend fun logout()
    suspend fun verifyToken(): Boolean
    suspend fun refreshToken(refreshToken: String?): Token?
}

class AuthRepositoryImpl(
    private val apiService: BrigadkaApiServiceUnauthorized,
    private val tokenRepository: TokenRepository,
    private val userDataRepository: UserDataRepository
) : AuthRepository {

    override val isAuthenticated: Flow<Boolean> = tokenRepository.token.map { it.accessToken != null }

    override suspend fun login(email: String, password: String): AuthResult {
        return try {
            val response = apiService.login(LoginRequest(email, password))
            val token = Token(
                accessToken = response.token,
                refreshToken = response.refresh_token
            )
            tokenRepository.saveToken(token)
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

    override suspend fun register(
        email: String,
        password: String,
    ): AuthResult {
        return try {
            val request = RegisterRequest(
                email = email,
                password = password,
            )
            val response = apiService.register(request)
            userDataRepository.setCurrentUserId(response.user_id)
            val token = Token(
                accessToken = response.token,
                refreshToken = response.refresh_token
            )
            tokenRepository.saveToken(token)
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

    override suspend fun logout() {
        tokenRepository.clearToken()
    }

    override suspend fun verifyToken(): Boolean {
        return try {
            val currentToken = tokenRepository.token.first().accessToken
            if (currentToken != null) {
                apiService.verifyToken(currentToken)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun refreshToken(refreshToken: String?): Token? {
        return try {
            if (refreshToken == null) return null

            // Use the dedicated refresh token endpoint
            val response = apiService.refreshToken(refreshToken)

            // Create a new token from the response
            val token = Token(
                accessToken = response.token,
                refreshToken = response.refresh_token
            )

            // Save the new token
            tokenRepository.saveToken(token)

            return token
        } catch (e: Exception) {
            null
        }
    }
}

data class AuthResult(
    val success: Boolean,
    val token: String? = null,
    val userId: Int? = null,
    val error: String? = null
)