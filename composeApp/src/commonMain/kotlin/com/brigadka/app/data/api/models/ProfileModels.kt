package com.brigadka.app.data.api.models

data class CreateProfileRequest(
    val activity_type: String,
    val description: String,
    val user_id: Int
)

data class Profile(
    val activity_type: String,
    val created_at: String,
    val description: String,
    val profile_id: Int,
    val user_id: Int
)
data class ImprovProfile(
    val activity_type: String,
    val created_at: String,
    val description: String,
    val goal: String,
    val looking_for_team: Boolean,
    val profile_id: Int,
    val styles: List<String>,
    val user_id: Int
)

data class MusicProfile(
    val activity_type: String,
    val created_at: String,
    val description: String,
    val genres: List<String>,
    val instruments: List<String>,
    val profile_id: Int,
    val user_id: Int
)

data class ProfileResponse(
    val improv_profile: ImprovProfile?,
    val music_profile: MusicProfile?
)