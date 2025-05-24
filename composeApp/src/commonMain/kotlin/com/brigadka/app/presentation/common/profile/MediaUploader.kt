package com.brigadka.app.presentation.common.profile

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.brigadka.app.data.repository.MediaRepository
import com.brigadka.app.presentation.profile.common.ProfileState
import com.brigadka.app.presentation.profile.common.LoadableValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

open class MediaUploader(
    private val scope: CoroutineScope,
    private val mediaRepository: MediaRepository,
    private val profileState: MutableValue<ProfileState>,
) {

    fun removeVideo(id: Int?) {
        if (id == null) {
            // TODO: when removing uploading video coroutine should be cancelled
            profileState.update {
                val idx = it.videos.indexOfFirst { video -> video.isLoading }
                it.copy(videos = it.videos.filterIndexed { i, _ -> i != idx })
            }
        } else {
            profileState.update {
                it.copy(videos = it.videos.filter { video -> video.value?.id != id })
            }
        }
    }

    fun removeAvatar() {
        profileState.update {
            it.copy(avatar = LoadableValue())
        }
    }

    fun uploadAvatar(fileBytes: ByteArray, fileName: String) {
        profileState.update { it.copy(avatar = it.avatar.copy(isLoading = true)) }

        scope.launch {
            try {
                val mediaItem = mediaRepository.uploadMedia(fileBytes, fileName)
                profileState.update { it.copy(avatar = LoadableValue(mediaItem)) }
            } catch (e: Exception) {
                profileState.update { it.copy(avatar = it.avatar.copy(isLoading = false)) }
                // TODO: log exception
            }
        }
    }

    fun uploadVideo(fileBytes: ByteArray, fileName: String) {
        profileState.update { it.copy(videos = it.videos + LoadableValue(isLoading = true)) }

        scope.launch {
            try {
                val mediaItem = mediaRepository.uploadMedia(fileBytes, fileName)

                profileState.update { it ->
                    val idx = it.videos.indexOfFirst { it.isLoading }
                    it.copy(videos = it.videos.mapIndexed() { i, item ->
                        if (i == idx) LoadableValue(mediaItem) else item
                    })
                }

            } catch (e: Exception) {
                // TODO: log exception
                profileState.update {
                    val idx = it.videos.indexOfFirst { it.isLoading }
                    it.copy(videos = it.videos.filterIndexed { i, _ -> i != idx})
                }
            }
        }
    }
}
