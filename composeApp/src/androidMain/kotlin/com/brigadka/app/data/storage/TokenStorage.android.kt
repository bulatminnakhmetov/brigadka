package com.brigadka.app.data.storage

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings


actual fun createTokenStorage(context: Any?): TokenStorage {
    // Require a Context for Android implementation
    requireNotNull(context) { "Android implementation requires a Context instance" }
    require(context is Context) { "Android implementation requires a Context instance" }

    // Create Settings implementation using Android SharedPreferences
    val sharedPreferences = (context as Context).getSharedPreferences(
        "brigadka_app_preferences",
        Context.MODE_PRIVATE
    )

    val settings = SharedPreferencesSettings(sharedPreferences)

    return TokenStorageImpl(settings)
}