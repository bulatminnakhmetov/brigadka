// UserDataRepository.kt
package com.brigadka.app.data.repository

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set


interface UserRepository {
    val isLoggedIn: Boolean
    suspend fun setCurrentUserId(userId: Int)
    suspend fun clearCurrentUserId()
    fun requireUserId(): Int
}

class UserRepositoryImpl(
    private val settings: Settings,
) : UserRepository {
    private val userIdKey = "user_id"

    override val isLoggedIn: Boolean = getStoredUserId() != null

    override fun requireUserId(): Int {
        return getStoredUserId()!! // TODO: Handle null case
    }

    override suspend fun setCurrentUserId(userId: Int) {
        settings[userIdKey] = userId
    }

    override suspend fun clearCurrentUserId() {
        settings.remove(userIdKey)
    }

    private fun getStoredUserId(): Int? {
        return settings.getIntOrNull(userIdKey)
    }
}