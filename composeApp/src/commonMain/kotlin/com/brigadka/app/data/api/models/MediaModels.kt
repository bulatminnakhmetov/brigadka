package com.brigadka.app.data.api.models

data class Media(
    val id: Int,
    val profile_id: Int,
    val role: String,
    val type: String,
    val uploaded_at: String,
    val url: String
)

data class MediaListResponse(val media: List<Media>)

data class MediaResponse(val media: Media)