package com.brigadka.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

interface PushTokenRepository {
    val token: StateFlow<String?>
    fun saveToken(token: String)
    fun clearToken()
}

class PushTokenRepositoryImpl(
    private val settings: Settings
) : PushTokenRepository {
    private val tokenKey: String = "push_token"

    private val json = Json { ignoreUnknownKeys = true }

    private val _token = MutableStateFlow(getStoredToken())

    override val token: StateFlow<String?> = _token.asStateFlow()

    override fun saveToken(token: String) {
        val tokenJson = json.encodeToString(token)
        settings[tokenKey] = tokenJson
        _token.value = token
    }

    override fun clearToken() {
        settings.remove(tokenKey)
        _token.value = null
    }

    private fun getStoredToken(): String? {
        return settings.getStringOrNull(tokenKey)
    }
}