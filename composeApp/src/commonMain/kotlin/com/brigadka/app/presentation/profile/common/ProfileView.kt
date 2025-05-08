package com.brigadka.app.presentation.profile.common

import com.brigadka.app.data.api.models.MediaItem
import kotlinx.datetime.LocalDate

data class ProfileView(
    val fullName: String,
    val age: Int?,
    val genderLabel: String?,
    val cityLabel: String?,
    val bio: String,
    val goalLabel: String?,
    val improvStylesLabels: List<String> = emptyList(),
    val lookingForTeam: Boolean = false,
    val avatar: MediaItem?,
    val videos: List<MediaItem> = emptyList()
)