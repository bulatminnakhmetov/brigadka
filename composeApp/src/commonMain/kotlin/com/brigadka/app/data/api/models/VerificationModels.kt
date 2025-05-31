// First, let's add new model classes for the verification endpoints
package com.brigadka.app.data.api.models

import kotlinx.serialization.Serializable

@Serializable
data class VerifyEmailRequest(
    val token: String
)

@Serializable
data class VerificationResponse(
    val success: Boolean,
    val message: String
)

@Serializable
data class ResendVerificationRequest(
    val ignore_cooldown: Boolean = false
)

@Serializable
data class VerificationStatusResponse(
    val verified: Boolean
)