package com.brigadka.app.data.repository

import com.brigadka.app.data.api.BrigadkaApiService
import com.brigadka.app.data.api.models.LoginRequest
import com.brigadka.app.data.api.models.RegisterRequest
import com.brigadka.app.data.storage.Token
import com.brigadka.app.data.storage.TokenStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface AuthRepository {
//    val isAuthenticated: Flow<Boolean>
    suspend fun login(email: String, password: String): AuthResult
    suspend fun register(
        email: String,
        password: String,
        fullName: String,
        age: Int,
        cityId: Int,
        gender: String
    ): AuthResult
    suspend fun logout()
    suspend fun verifyToken(): Boolean
    suspend fun refreshToken(refreshToken: String?): Token?
}

class AuthRepositoryImpl(
    private val apiService: BrigadkaApiService,
    private val tokenStorage: TokenStorage
) : AuthRepository {

//    override val isAuthenticated: Flow<Boolean> = tokenStorage.isAuthenticated

    override suspend fun login(email: String, password: String): AuthResult {
        return try {
            val response = apiService.login(LoginRequest(email, password))
            val token = Token(
                accessToken = response.token,
                refreshToken = response.token // Using same token as refresh for simplicity
            )
            tokenStorage.saveToken(token)
            AuthResult(
                success = true,
                token = response.token,
                userId = response.user.id
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
        fullName: String,
        age: Int,
        cityId: Int,
        gender: String
    ): AuthResult {
        return try {
            val request = RegisterRequest(
                email = email,
                password = password,
                full_name = fullName,
                age = age,
                city_id = cityId,
                gender = gender
            )
            val response = apiService.register(request)
            val token = Token(
                accessToken = response.token,
                refreshToken = response.token // Using same token as refresh for simplicity
            )
            tokenStorage.saveToken(token)
            AuthResult(
                success = true,
                token = response.token,
                userId = response.user.id
            )
        } catch (e: Exception) {
            AuthResult(
                success = false,
                error = e.message ?: "Registration failed"
            )
        }
    }

    override suspend fun logout() {
        tokenStorage.clearToken()
    }

    override suspend fun verifyToken(): Boolean {
        return try {
            val currentToken = tokenStorage.token.first().accessToken
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
                refreshToken = response.token // In a real app with separate tokens, you'd use response.refreshToken
            )

            // Save the new token
            tokenStorage.saveToken(token)

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