package com.brigadka.app.presentation.onboarding.improv

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.brigadka.app.data.api.models.StringItem
import com.brigadka.app.data.repository.ProfileRepository
import com.brigadka.app.presentation.profile.common.ProfileState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

open class ImprovInfoEditor(
    private val coroutineScope: CoroutineScope,
    private val profileState: MutableValue<ProfileState>,
    private val profileRepository: ProfileRepository,
) {

    private val _improvGoals = MutableStateFlow<List<StringItem>>(emptyList())
    val improvGoals: StateFlow<List<StringItem>> = _improvGoals.asStateFlow()

    private val _improvStyles = MutableStateFlow<List<StringItem>>(emptyList())
    val improvStyles: StateFlow<List<StringItem>> = _improvStyles.asStateFlow()

    val isCompleted: Boolean
        get() = profileState.value.improvStyles.isNotEmpty() && profileState.value.bio.isNotEmpty() && !profileState.value.goal.isNullOrBlank()

    init {
        loadCatalogData()
    }

    private fun loadCatalogData() {
        coroutineScope.launch {
            try {
                _improvGoals.update { profileRepository.getImprovGoals() }
                _improvStyles.update { profileRepository.getImprovStyles() }
            } catch (e: Exception) {
                // TODO: Handle error
            }
        }
    }

    fun updateBio(bio: String) {
        profileState.update { it.copy(bio = bio) }
    }

    fun updateGoal(goal: String) {
        profileState.update { it.copy(goal = goal) }
    }

    fun updateImprovStyles(styles: List<String>) {
        profileState.update { it.copy(improvStyles = styles) }
    }

    fun toggleStyle(styleCode: String) {
        val currentStyles = profileState.value.improvStyles
        val updatedStyles = if (styleCode in currentStyles) {
            currentStyles - styleCode
        } else {
            currentStyles + styleCode
        }
        updateImprovStyles(updatedStyles)
    }

    fun updateLookingForTeam(lookingForTeam: Boolean) {
        profileState.update { it.copy(lookingForTeam = lookingForTeam) }
    }
}