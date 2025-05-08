package com.brigadka.app.presentation.onboarding

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.brigadka.app.data.api.models.Profile
import com.brigadka.app.data.api.models.ProfileCreateRequest
import com.brigadka.app.data.repository.MediaRepository
import com.brigadka.app.data.repository.ProfileRepository
import com.brigadka.app.data.repository.UserDataRepository
import com.brigadka.app.presentation.onboarding.improv.ImprovInfoComponent
import com.brigadka.app.presentation.onboarding.basic.BasicInfoComponent
import com.brigadka.app.presentation.onboarding.media.MediaUploadComponent
import com.brigadka.app.presentation.profile.common.ProfileData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

class OnboardingComponent(
    componentContext: ComponentContext,
    private val mediaRepository: MediaRepository,
    private val profileRepository: ProfileRepository,
    private val userDataRepository: UserDataRepository,
    private val onFinished: (Profile) -> Unit
) : ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()
    private val _profileData = MutableValue(ProfileData())
    val profileData: Value<ProfileData> = _profileData

    private val componentScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _stack = childStack(
        source = navigation,
        initialConfiguration = Config.BasicInfo,
        handleBackButton = true,
        serializer = Config.serializer(),
        childFactory = ::createChild
    )

    val childStack: Value<ChildStack<Config, Child>> = _stack

    private fun createChild(config: Config, componentContext: ComponentContext): Child {
        return when (config) {
            is Config.BasicInfo -> Child.BasicInfo(
                BasicInfoComponent(
                    componentContext = componentContext,
                    _profileData = _profileData,
                    profileRepository = profileRepository,
                    onNext = ::onBasicInfoCompleted,
                )
            )
            is Config.ImprovInfo -> Child.ImprovInfo(
                ImprovInfoComponent(
                    componentContext = componentContext,
                    profileRepository = profileRepository,
                    _profileData = _profileData,
                    onNext = ::onImprovInfoCompleted,
                    onBack = navigation::pop
                )
            )
            is Config.MediaUpload -> Child.MediaUpload(
                MediaUploadComponent(
                    componentContext = componentContext,
                    mediaRepository = mediaRepository,
                    _profileData = _profileData,
                    onFinish = ::onMediaUploadCompleted,
                    onBack = navigation::pop
                )
            )
        }
    }

    private fun onBasicInfoCompleted() {
        navigation.pushNew(Config.ImprovInfo)
    }

    private fun onImprovInfoCompleted() {
        navigation.pushNew(Config.MediaUpload)
    }

    private fun onMediaUploadCompleted() {
        completeOnboarding()
    }

    private fun completeOnboarding() {
        componentScope.launch {
            val userId = userDataRepository.currentUserId.value ?: return@launch

            val request = ProfileCreateRequest(
                user_id = userId,
                full_name = profileData.value.fullName,
                bio = profileData.value.bio,
                birthday = profileData.value.birthday ?: LocalDate(2000, 1, 1),
                city_id = profileData.value.cityId ?: 1,
                gender = profileData.value.gender ?: "other",
                goal = profileData.value.goal ?: "",
                improv_styles = profileData.value.improvStyles,
                looking_for_team = profileData.value.lookingForTeam,
                avatar = profileData.value.avatar.value?.id,
                videos = profileData.value.videos.mapNotNull { it.value?.id }
            )

            try {
                val profile = profileRepository.createProfile(request)
                onFinished(profile)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    @Serializable
    sealed class Config {
        @Serializable
        object BasicInfo : Config()

        @Serializable
        object ImprovInfo : Config()

        @Serializable
        object MediaUpload : Config()
    }

    sealed class Child {
        data class BasicInfo(val component: BasicInfoComponent) : Child()
        data class ImprovInfo(val component: ImprovInfoComponent) : Child()
        data class MediaUpload(val component: MediaUploadComponent) : Child()
    }
}