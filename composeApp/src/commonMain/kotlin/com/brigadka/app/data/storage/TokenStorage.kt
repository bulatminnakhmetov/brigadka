package com.brigadka.app.data.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import com.russhwolf.settings.Settings
import com.russhwolf.settings.contains
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

@Serializable
data class Token(
    val accessToken: String? = null,
    val refreshToken: String? = null,
)

interface TokenStorage {
    val token: Flow<Token>
    suspend fun saveToken(token: Token)
    suspend fun clearToken()
}

class TokenStorageImpl(private val settings: Settings) : TokenStorage {
    private val json = Json { ignoreUnknownKeys = true }
    private val tokenKey = "auth_token"
    private val _token = MutableStateFlow(getStoredToken())

    override val token: Flow<Token> = _token.asStateFlow()

    override suspend fun saveToken(token: Token) {
        val tokenJson = json.encodeToString(token)
        settings[tokenKey] = tokenJson
        _token.value = token
    }

    override suspend fun clearToken() {
        settings.remove(tokenKey)
        _token.value = Token()
    }

    private fun getStoredToken(): Token {
        val tokenJson = settings.getStringOrNull(tokenKey)
        return if (tokenJson != null) {
            try {
                json.decodeFromString<Token>(tokenJson)
            } catch (e: Exception) {
                Token()
            }
        } else {
            Token()
        }
    }
}

expect fun createTokenStorage(context: Any? = null): TokenStorage