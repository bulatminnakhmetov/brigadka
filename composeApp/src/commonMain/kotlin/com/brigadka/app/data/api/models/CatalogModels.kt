package com.brigadka.app.data.api.models

import kotlinx.serialization.Serializable

@Serializable
data class TranslatedItem(
    val key: String,
    val value: String,
)