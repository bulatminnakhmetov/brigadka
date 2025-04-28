package com.brigadka.app.data.api.models

data class ProfileSearchResult(
    val activity_type: String,
    val age: Int,
    val city: String,
    val description: String,
    val full_name: String,
    val gender: String,
    val improv_goal: String?,
    val improv_looking_for_team: Boolean,
    val improv_styles: List<String>,
    val music_genres: List<String>,
    val music_instruments: List<String>,
    val profile_id: Int,
    val user_id: Int
)

data class ProfileSearchResponse(
    val current_page: Int,
    val page_size: Int,
    val results: List<ProfileSearchResult>,
    val total_count: Int,
    val total_pages: Int
)
data class ProfileSearchRequest(
    val activity_type: String? = null,
    val age_max: Int? = null,
    val age_min: Int? = null,
    val city_id: Int? = null,
    val full_name: String? = null,
    val gender: String? = null,
    val improv_goal: String? = null,
    val improv_looking_for_team: Boolean? = null,
    val improv_styles: List<String>? = null,
    val limit: Int? = null,
    val music_genres: List<String>? = null,
    val music_instruments: List<String>? = null,
    val offset: Int? = null
)