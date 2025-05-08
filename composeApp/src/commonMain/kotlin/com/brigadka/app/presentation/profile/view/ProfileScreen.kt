package com.brigadka.app.presentation.profile.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.brigadka.app.data.api.models.MediaItem
import com.brigadka.app.presentation.profile.common.Avatar
import com.brigadka.app.presentation.profile.common.LoadableValue
import com.brigadka.app.presentation.profile.common.ProfileView
import com.brigadka.app.presentation.profile.common.VideoSection

@Composable
fun ProfileScreen(component: ProfileViewComponent, onError: (String) -> Unit) {
    val profileViewState by component.profileView.subscribeAsState()
    ProfileScreen(
        profileView = profileViewState.value,
        isLoading = profileViewState.isLoading,
        onError = onError,
        onEditProfile = component.onEditProfile
    )
}

@Composable
fun ProfileScreenPreview() {
//    val fullName: String,
//    val age: Int?,
//    val genderLabel: String?,
//    val cityLabel: String?,
//    val bio: String,
//    val goalLabel: String?,
//    val improvStylesLabels: List<String> = emptyList(),
//    val lookingForTeam: Boolean = false,
//    val avatar: MediaItem?,
//    val videos: List<MediaItem> = emptyList()
    val profileView = ProfileView(
        fullName = "John Doe",
        age = 30,
        genderLabel = "Мужчина",
        cityLabel = "Москва",
        bio = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
        goalLabel = "Хобби",
        improvStylesLabels = listOf("Длинная форма", "Реп"),
        lookingForTeam = true,
        avatar = MediaItem(
            id = 1,
            url = "https://example.com/avatar.jpg",
            thumbnail_url = "https://example.com/avatar_thumbnail.jpg"
        ),
        videos = listOf(
            MediaItem(id = 0, url = "https://example.com/video1.mp4", thumbnail_url = "https://example.com/video"),
            MediaItem(id = 1, url = "https://example.com/video1.mp4", thumbnail_url = "https://example.com/video"),
            MediaItem(id = 2, url = "https://example.com/video1.mp4", thumbnail_url = "https://example.com/video")
        )
    )
}

@Composable
fun ProfileScreen(
    profileView: ProfileView?,
    isLoading: Boolean,
    onError: (String) -> Unit,
    onEditProfile: (() -> Unit)? = null
) {

    val scrollState = rememberScrollState()

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (profileView == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Profile not found")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile header with avatar
        Avatar(
            mediaItem = profileView.avatar,
            isUploading = false,
            onError = onError,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Name and basic info
        Text(
            text = profileView.fullName,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Additional profile info (city, age)
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            profileView.genderLabel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            profileView.age?.let {
                Text(
                    text = " • ${profileView.age} years old",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            profileView.cityLabel?.let {
                Text(
                    text = " • $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bio section
        if (profileView.bio.isNotEmpty()) {
            SectionTitle(title = "Bio")
            Text(
                text = profileView.bio,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Improv details section
        SectionTitle(title = "Improv details")

        // Goal
        profileView.goalLabel?.let {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Goal: ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Looking for team
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = "Looking for team: ",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Text(
                text = if (profileView.lookingForTeam) "Yes" else "No",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Improv styles
        if (profileView.improvStylesLabels.isNotEmpty()) {
            Text(
                text = "Styles:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                profileView.improvStylesLabels.joinToString(", ").let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Videos section
        if (profileView.videos.isNotEmpty()) {
            SectionTitle(title = "Videos")
            VideoSection(
                videos = profileView.videos.map { LoadableValue(value = it) },
                onError = onError,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Edit profile button for the current user
        if (onEditProfile != null) {
            Button(
                onClick = { onEditProfile() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Profile"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Profile")
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )
}