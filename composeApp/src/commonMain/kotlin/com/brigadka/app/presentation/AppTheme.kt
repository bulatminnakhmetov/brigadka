package com.brigadka.app.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

// A small theme wrapper for consistency
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme {
        content()
    }
}