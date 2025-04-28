package com.brigadka.app.data.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Auth Models
data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(
    val age: Int,
    val city_id: Int,
    val email: String,
    val full_name: String,
    val gender: String,
    val password: String
)

data class User(
    val age: Int,
    val city_id: Int,
    val email: String,
    val full_name: String,
    val gender: String,
    val id: Int
)

data class AuthResponse(val token: String, val user: User)

@Serializable
data class RefreshRequest(
    @SerialName("refresh_token")
    val refresh_token: String
)