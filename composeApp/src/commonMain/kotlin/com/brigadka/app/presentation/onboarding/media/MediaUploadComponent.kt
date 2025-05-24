package com.brigadka.app.presentation.onboarding.media

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.brigadka.app.common.coroutineScope
import com.brigadka.app.data.repository.MediaRepository
import com.brigadka.app.presentation.common.profile.MediaUploader
import com.brigadka.app.presentation.profile.common.ProfileState

class MediaUploadComponent(
    componentContext: ComponentContext,
    mediaRepository: MediaRepository,
    profileState: MutableValue<ProfileState>,
    private val onFinish: () -> Unit,
    private val onBack: () -> Unit,
) : MediaUploader(componentContext.coroutineScope(), mediaRepository, profileState) {

    val profileState: Value<ProfileState> = profileState

    fun finish() {
        onFinish()
    }

    fun back() {
        onBack()
    }
}
