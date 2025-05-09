package com.brigadka.app.presentation.profile.view

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.brigadka.app.data.repository.ProfileRepository
import com.brigadka.app.presentation.profile.common.LoadableValue
import com.brigadka.app.data.repository.ProfileView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfileViewComponent(
    componentContext: ComponentContext,
    private val profileRepository: ProfileRepository,
    private val userId: Int? = null,
    val onEditProfile: () -> Unit = {},
) : ComponentContext by componentContext {

    private val _profileView = MutableValue<LoadableValue<ProfileView>>(LoadableValue(isLoading = true))
    val profileView: Value<LoadableValue<ProfileView>> = _profileView

    init {
        // TODO: change to scope tied to component
        CoroutineScope(Dispatchers.Default).launch {
            val view = profileRepository.getProfileView(userId)
            _profileView.update { it.copy(isLoading = false, value = view) }
        }
    }

    val isEditable: Boolean
        get() = userId == null
}