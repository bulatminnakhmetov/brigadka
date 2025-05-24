package com.brigadka.app.presentation.profile.edit

import co.touchlab.kermit.Logger
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.brigadka.app.common.coroutineScope
import com.brigadka.app.data.api.models.ProfileUpdateRequest
import com.brigadka.app.data.repository.MediaRepository
import com.brigadka.app.data.repository.ProfileRepository
import com.brigadka.app.data.repository.UserRepository
import com.brigadka.app.presentation.common.TopBarState
import com.brigadka.app.presentation.common.UIEvent
import com.brigadka.app.presentation.common.UIEventEmitter
import com.brigadka.app.presentation.common.profile.MediaUploader
import com.brigadka.app.presentation.onboarding.basic.BasicInfoEditor
import com.brigadka.app.presentation.onboarding.improv.ImprovInfoEditor
import com.brigadka.app.presentation.profile.common.LoadableValue
import com.brigadka.app.presentation.profile.common.ProfileState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val logger = Logger.withTag("EditProfileComponent")

data class EditProfileTopBarState(
    val onBackClick: () -> Unit,
    val onSaveClick: () -> Unit,
) : TopBarState

class EditProfileComponent(
    componentContext: ComponentContext,
    private val uiEventEmitter: UIEventEmitter,
    private val profileRepository: ProfileRepository,
    mediaRepository: MediaRepository,
    private val onFinished: () -> Unit,
    private val onBack: () -> Unit
) : ComponentContext by componentContext {

    private val _profileState = MutableValue(ProfileState())
    val profileState: Value<ProfileState> = _profileState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val scope = coroutineScope()

    // Editors for different sections
    private val basicInfoEditor = BasicInfoEditor(scope, _profileState, profileRepository)
    private val improvInfoEditor = ImprovInfoEditor(scope, _profileState, profileRepository)
    private val mediaUploader = MediaUploader(scope, mediaRepository, _profileState)

    // Expose the needed methods from editors
    val cities = basicInfoEditor.cities
    val genders = basicInfoEditor.genders
    val improvGoals = improvInfoEditor.improvGoals
    val improvStyles = improvInfoEditor.improvStyles

    init {
        loadCurrentProfile()
    }

    private fun loadCurrentProfile() {
        scope.launch {
            try {
                _isLoading.update { true }
                val profile = profileRepository.getProfile(null)

                _profileState.update {
                    ProfileState(
                        fullName = profile.full_name,
                        bio = profile.bio,
                        birthday = profile.birthday,
                        cityId = profile.city_id,
                        gender = profile.gender,
                        goal = profile.goal,
                        improvStyles = profile.improv_styles,
                        lookingForTeam = profile.looking_for_team,
                        avatar = LoadableValue(value = profile.avatar),
                        videos = profile.videos.map { LoadableValue(value = it)}
                    )
                }
            } catch (e: Exception) {
                logger.e(e) { "Failed to load profile" }
                _errorMessage.update { "Failed to load profile: ${e.message}" }
            } finally {
                _isLoading.update { false }
            }
        }
    }

    // Forward basic info methods
    fun updateFullName(fullName: String) = basicInfoEditor.updateFullName(fullName)
    fun updateBirthday(birthday: kotlinx.datetime.LocalDate?) = basicInfoEditor.updateBirthday(birthday)
    fun updateGender(gender: String) = basicInfoEditor.updateGender(gender)
    fun updateCityId(cityId: Int) = basicInfoEditor.updateCityId(cityId)

    // Forward improv info methods
    fun updateBio(bio: String) = improvInfoEditor.updateBio(bio)
    fun updateGoal(goal: String) = improvInfoEditor.updateGoal(goal)
    fun toggleStyle(styleCode: String) = improvInfoEditor.toggleStyle(styleCode)
    fun updateLookingForTeam(lookingForTeam: Boolean) = improvInfoEditor.updateLookingForTeam(lookingForTeam)

    // Forward media methods
    fun uploadAvatar(fileBytes: ByteArray, fileName: String) = mediaUploader.uploadAvatar(fileBytes, fileName)
    fun uploadVideo(fileBytes: ByteArray, fileName: String) = mediaUploader.uploadVideo(fileBytes, fileName)
    fun removeAvatar() = mediaUploader.removeAvatar()
    fun removeVideo(id: Int?) = mediaUploader.removeVideo(id)

    fun save() {
        scope.launch {
            _isLoading.update { true }
            try {
                val request = ProfileUpdateRequest(
                    full_name = profileState.value.fullName,
                    bio = profileState.value.bio,
                    birthday = profileState.value.birthday,
                    city_id = profileState.value.cityId,
                    gender = profileState.value.gender,
                    goal = profileState.value.goal,
                    improv_styles = profileState.value.improvStyles,
                    looking_for_team = profileState.value.lookingForTeam,
                    avatar = profileState.value.avatar.value?.id,
                    videos = profileState.value.videos.mapNotNull { it.value?.id }
                )
                profileRepository.updateProfile(request)
                logger.d { "Profile updated successfully" }
                onFinished()
            } catch (e: Exception) {
                logger.e(e) { "Failed to update profile" }
                _errorMessage.update { "Failed to update profile: ${e.message}" }
            } finally {
                _isLoading.update { false }
            }
        }
    }

    suspend fun showTopBar() {
        uiEventEmitter.emit(UIEvent.TopBarUpdate(
            EditProfileTopBarState(
                onBackClick = ::back,
                onSaveClick = ::save
            )
        ))
    }

    fun back() {
        onBack()
    }
}