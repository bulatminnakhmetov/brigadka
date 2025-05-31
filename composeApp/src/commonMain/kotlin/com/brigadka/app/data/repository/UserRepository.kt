// UserDataRepository.kt
package com.brigadka.app.data.repository

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


interface UserRepository {
    val isLoggedIn: StateFlow<Boolean>
    val isVerified: StateFlow<Boolean>

    fun setCurrentUserId(userId: Int)
    fun clearUser()
    fun requireUserId(): Int // TODO: Handle null case gracefully
    fun setIsVerified(isVerified: Boolean)
}

class UserRepositoryImpl(
    private val settings: Settings,
) : UserRepository {
    private val userIdKey = "user_id"
    private val emailVerifiedKey = "email_verified"

    private var _isLoggedIn: MutableStateFlow<Boolean> = MutableStateFlow(
        getStoredUserId() != null
    )
    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private var _isVerified: MutableStateFlow<Boolean> = MutableStateFlow(getStoredIsVerified())
    override val isVerified: StateFlow<Boolean> = _isVerified

    override fun requireUserId(): Int {
        return getStoredUserId()!! // TODO: Handle null case
    }

    override fun setIsVerified(isVerified: Boolean) {
        settings[emailVerifiedKey] = isVerified
        _isVerified.value = isVerified
    }

    override fun setCurrentUserId(userId: Int) {
        settings[userIdKey] = userId
        _isLoggedIn.value = true
    }

    override fun clearUser() {
        settings.remove(userIdKey)
        settings.remove(emailVerifiedKey)
        _isLoggedIn.value = false
        _isVerified.value = false
    }

    private fun getStoredUserId(): Int? {
        return settings.getIntOrNull(userIdKey)
    }

    private fun getStoredIsVerified(): Boolean{
        return settings.getBoolean(emailVerifiedKey, false)
    }
}