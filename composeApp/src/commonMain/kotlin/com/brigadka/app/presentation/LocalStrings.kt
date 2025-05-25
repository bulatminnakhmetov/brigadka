package com.brigadka.app.presentation

import androidx.compose.runtime.compositionLocalOf
import com.brigadka.app.presentation.common.strings.RussianStrings
import com.brigadka.app.presentation.common.strings.Strings

val LocalStrings = compositionLocalOf<Strings> { RussianStrings }