package com.brigadka.app.presentation.common

import androidx.compose.runtime.Composable

interface FilePickerLauncher {
    fun launch()
}

enum class FileType {
    IMAGE,
    VIDEO
}

@Composable
expect fun rememberFilePickerLauncher(
    fileType: FileType,
    onFilePicked: (ByteArray, String) -> Unit,
    onError: (String) -> Unit
): FilePickerLauncher