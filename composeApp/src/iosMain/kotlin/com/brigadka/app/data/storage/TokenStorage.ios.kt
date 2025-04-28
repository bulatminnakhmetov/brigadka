package com.brigadka.app.data.storage

import com.russhwolf.settings.NSUserDefaultsSettings
import platform.Foundation.NSUserDefaults

actual fun createTokenStorage(context: Any?): TokenStorage {
    // Create Settings implementation using NSUserDefaults
    val userDefaults = NSUserDefaults.standardUserDefaults
    val settings = NSUserDefaultsSettings(userDefaults)

    return TokenStorageImpl(settings)
}