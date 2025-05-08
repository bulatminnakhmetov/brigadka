package com.brigadka.app.di

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module
    get() = module {
        single<Settings> {
            val sharedPreferences = get<Context>().getSharedPreferences(
                "brigadka_app_preferences",
                Context.MODE_PRIVATE
            )
            SharedPreferencesSettings(sharedPreferences)
        }
    }