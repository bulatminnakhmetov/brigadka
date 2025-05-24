package com.brigadka.app.presentation.profile.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.brigadka.app.data.api.models.City
import com.brigadka.app.data.api.models.MediaItem
import com.brigadka.app.data.api.models.StringItem
import com.brigadka.app.data.repository.ProfileView
import com.brigadka.app.presentation.common.compose.CityPicker
import com.brigadka.app.presentation.common.compose.DatePickerField
import com.brigadka.app.presentation.common.rememberFilePickerLauncher
import com.brigadka.app.presentation.profile.common.Avatar
import com.brigadka.app.presentation.profile.common.LoadableValue
import com.brigadka.app.presentation.profile.common.ProfileState
import com.brigadka.app.presentation.profile.common.VideoSection
import kotlinx.datetime.LocalDate

@Composable
fun EditProfileScreen(component: EditProfileComponent) {
    val profileState by component.profileState.subscribeAsState()
    val cities by component.cities.subscribeAsState()
    val genders by component.genders.subscribeAsState()
    val improvGoals by component.improvGoals.collectAsState()
    val improvStyles by component.improvStyles.collectAsState()
    val isLoading by component.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        component.showTopBar()
    }

    EditProfileScreen(
        profileState = profileState,
        cities = cities,
        genders = genders,
        improvGoals = improvGoals,
        improvStyles = improvStyles,
        isLoading = isLoading,
        uploadAvatar = component::uploadAvatar,
        uploadVideo = component::uploadVideo,
        removeAvatar = component::removeAvatar,
        updateFullName = component::updateFullName,
        updateBirthday = component::updateBirthday,
        updateGender = component::updateGender,
        updateCityId = component::updateCityId,
        updateBio = component::updateBio,
        updateGoal = component::updateGoal,
        toggleStyle = component::toggleStyle,
        updateLookingForTeam = component::updateLookingForTeam,
        removeVideo = component::removeVideo,
    )
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditProfileScreen(
    profileState: ProfileState,
    cities: List<City>,
    genders: List<StringItem>,
    improvGoals: List<StringItem>,
    improvStyles: List<StringItem>,
    isLoading: Boolean,
    uploadAvatar: (ByteArray, String) -> Unit,
    uploadVideo: (ByteArray, String) -> Unit,
    removeAvatar: () -> Unit,
    updateFullName: (String) -> Unit,
    updateBirthday: (LocalDate?) -> Unit,
    updateGender: (String) -> Unit,
    updateCityId: (Int) -> Unit,
    updateBio: (String) -> Unit,
    updateGoal: (String) -> Unit,
    toggleStyle: (String) -> Unit,
    updateLookingForTeam: (Boolean) -> Unit,
    removeVideo: (Int?) -> Unit,
) {
    val scope = rememberCoroutineScope()

    val avatarPickerLauncher = rememberFilePickerLauncher(
        fileType = "image/*",
        onFilePicked = { bytes, fileName ->
            uploadAvatar(bytes, fileName)
        },
        onError = {} // TODO: handle error
    )

    val videoPickerLauncher = rememberFilePickerLauncher(
        fileType = "video/*",
        onFilePicked = { bytes, fileName ->
            uploadVideo(bytes, fileName)
        },
        onError = {} // TODO: handle error
    )

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }


    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "Фото",
            style = MaterialTheme.typography.titleMedium
        )

        Avatar(
            mediaItem = profileState.avatar.value,
            isUploading = profileState.avatar.isLoading,
            onError = {}, // TODO: handle error,
            onClick = { avatarPickerLauncher.launch() },
            onRemove = { removeAvatar() },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(160.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Видео",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Загрузите видео с джемов или выступлений, чтобы другие импровизаторы могли увидеть вашу игру",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Start,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        VideoSection(
            videos = profileState.videos,
            pickVideo = { videoPickerLauncher.launch() },
            removeVideo = { removeVideo(it) },
            onError = {}, // TODO: handle error
            modifier = Modifier.fillMaxWidth()
        )

        // Basic Info Section
        OutlinedTextField(
            value = profileState.fullName,
            onValueChange = { updateFullName(it) },
            label = { Text("Имя") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        )

        DatePickerField(
            label = "Дата рождения",
            selectedDate = profileState.birthday,
            onDateSelected = { updateBirthday(it) },
        )

        if (genders.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Пол")

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    genders.forEach { gender ->
                        FilterChip(
                            selected = profileState.gender == gender.code,
                            onClick = { updateGender(gender.code) },
                            label = { Text(gender.label) }
                        )
                    }
                }
            }
        }

        CityPicker(cities, profileState.cityId, onCitySelected = { cityId ->
            updateCityId(cityId)
        })

        Spacer(modifier = Modifier.height(16.dp))

        // Improv Info Section
        OutlinedTextField(
            value = profileState.bio,
            onValueChange = { updateBio(it) },
            label = { Text("О себе") },
            placeholder = { Text("Tell more about your experience...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
            shape = MaterialTheme.shapes.medium,
        )

        Text(
            text = "Your goal in improv",
            style = MaterialTheme.typography.titleMedium
        )

        if (improvGoals.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                improvGoals.forEach { goal ->
                    FilterChip(
                        selected = profileState.goal == goal.code,
                        onClick = { updateGoal(goal.code) },
                        label = { Text(goal.label) }
                    )
                }
            }
        }

        Text(
            text = "What you like in improv",
            style = MaterialTheme.typography.titleMedium
        )

        if (improvStyles.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                improvStyles.forEach { style ->
                    FilterChip(
                        selected = style.code in profileState.improvStyles,
                        onClick = { toggleStyle(style.code) },
                        label = { Text(style.label) }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Looking for team",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Let other improvisers know that you're looking for a team",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = profileState.lookingForTeam,
                onCheckedChange = { updateLookingForTeam(it) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileTopBar(
    state: EditProfileTopBarState,
) {
    TopAppBar(
        title = { Text("Edit Profile") },
        navigationIcon = {
            IconButton(onClick = state.onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = state.onSaveClick) {
                Icon(Icons.Default.Done, contentDescription = "Save")
            }
        }
    )
}

@Composable
fun EditProfileScreenPreview() {
    val profileState = ProfileState(
        fullName = "John Doe",
        birthday = LocalDate(2000, 1, 1),
        gender = "male",
        cityId = 1,
        goal = "hobby",
        improvStyles = listOf("shortform"),
        lookingForTeam = true,
        avatar = LoadableValue(
            value = MediaItem(
                id = 1,
                url = "https://example.com/avatar.jpg",
                thumbnail_url = "https://example.com/avatar_thumbnail.jpg"
            )
        ),
        videos = listOf(
            LoadableValue(
                value = MediaItem(
                    id = 0,
                    url = "https://example.com/video1.mp4",
                    thumbnail_url = "https://example.com/video"
                )
            ),
            LoadableValue(
                value = MediaItem(
                    id = 1,
                    url = "https://example.com/video1.mp4",
                    thumbnail_url = "https://example.com/video"
                )
            ),
            LoadableValue(
                value = MediaItem(
                    id = 2,
                    url = "https://example.com/video1.mp4",
                    thumbnail_url = "https://example.com/video"
                )
            ),
        )
    )
    val goals = listOf(
        StringItem(code = "hobby", label = "Hobby"),
        StringItem(code = "professional", label = "Professional")
    )
    val styles = listOf(
        StringItem(code = "shortform", label = "Shortform"),
        StringItem(code = "longform", label = "Longform"),
        StringItem(code = "battles", label = "Баттлы")
    )

    val cities = listOf(
        City(id = 1, name = "New York"),
        City(id = 2, name = "Los Angeles"),
        City(id = 3, name = "Chicago")
    )
    val genders = listOf(
        StringItem(code = "male", label = "Male"),
        StringItem(code = "female", label = "Female")
    )

    EditProfileScreen(
        profileState = profileState,
        cities = cities,
        genders = genders,
        improvStyles = styles,
        improvGoals = goals,
        isLoading = false,
        uploadAvatar = {_, _ -> },
        uploadVideo = {_, _ -> },
        removeAvatar = {},
        updateFullName = {},
        updateBirthday = {},
        updateGender = {},
        updateCityId = {},
        updateBio = {},
        updateGoal = {},
        toggleStyle = {},
        updateLookingForTeam = {},
        removeVideo = {},
    )
}