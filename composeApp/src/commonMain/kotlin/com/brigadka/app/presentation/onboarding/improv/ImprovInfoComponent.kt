package com.brigadka.app.presentation.onboarding.improv

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.brigadka.app.common.coroutineScope
import com.brigadka.app.data.repository.ProfileRepository
import com.brigadka.app.presentation.profile.common.ProfileState

class ImprovInfoComponent(
    componentContext: ComponentContext,
    profileRepository: ProfileRepository,
    profileState: MutableValue<ProfileState>,
    private val onNext: () -> Unit,
    private val onBack: () -> Unit
) : ImprovInfoEditor(componentContext.coroutineScope(), profileState, profileRepository) {

    val profileState: Value<ProfileState> = profileState

    fun next() {
        onNext()
    }

    fun back() {
        onBack()
    }
}