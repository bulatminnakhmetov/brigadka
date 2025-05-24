package com.brigadka.app.presentation.onboarding.basic

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.brigadka.app.common.coroutineScope
import com.brigadka.app.data.repository.ProfileRepository
import com.brigadka.app.presentation.profile.common.ProfileState

class BasicInfoComponent(
    componentContext: ComponentContext,
    profileState: MutableValue<ProfileState>,
    profileRepository: ProfileRepository,
    private val onNext: () -> Unit
) : BasicInfoEditor(componentContext.coroutineScope(), profileState, profileRepository) {

    val profileState: Value<ProfileState> = profileState

    fun next() {
        onNext()
    }
}