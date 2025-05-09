package com.brigadka.app

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.defaultComponentContext
import com.brigadka.app.di.initKoin
import com.brigadka.app.presentation.root.RootComponent
import com.brigadka.app.presentation.root.RootContent
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.logger.Level
import org.koin.core.parameter.parametersOf

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initKoin {
            androidLogger(Level.ERROR)
            androidContext(applicationContext)
        }

        val koin = GlobalContext.get()

        val rootComponent: RootComponent by koin.inject { parametersOf(defaultComponentContext()) }

        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(WindowInsets.safeDrawing.asPaddingValues())
                    ) {
                        RootContent(rootComponent)
                    }
                }

            }
        }

        setLightStatusBar(window)
    }
}

// A small theme wrapper for consistency
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme {
        content()
    }
}

fun setLightStatusBar(window: Window) {
    // Set a light background color for the status bar

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // API 30+
        window.insetsController?.setSystemBarsAppearance(
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
        )
    } else {
        // TODO: check if it works
        // API 28 to 29

        @Suppress("DEPRECATION")
        window.statusBarColor = Color.WHITE

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }
}