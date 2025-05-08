// UserDataRepository.kt
package com.brigadka.app.data.repository

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

interface UserDataRepository {
    val currentUserId: StateFlow<Int?>
    suspend fun setCurrentUserId(userId: Int)
    suspend fun clearCurrentUserId()
    fun requireUserId(): Int
}

class UserDataRepositoryImpl(
    private val settings: Settings,
) : UserDataRepository {
    private val userIdKey = "user_id"
    private val _currentUserId = MutableStateFlow(getStoredUserId())
    override val currentUserId = _currentUserId.asStateFlow()

    override fun requireUserId(): Int {
        return currentUserId.value
            ?: throw IllegalStateException("User ID is required but not available")
    }

    override suspend fun setCurrentUserId(userId: Int) {
        settings[userIdKey] = userId
        _currentUserId.value = userId
        // Profile ID will be updated via the flow collector in init
    }

    override suspend fun clearCurrentUserId() {
        settings.remove(userIdKey)
        _currentUserId.value = null
        // Profile ID will be cleared via the flow collector in init
    }

    private fun getStoredUserId(): Int? {
        return settings.getIntOrNull(userIdKey)
    }
}