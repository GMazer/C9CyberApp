package com.c9cyber.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val user_id: String,
    val public_key: String
)

@Serializable
data class ChallengeRequest(
    val user_id: String
)

@Serializable
data class ChallengeResponse(
    val challenge: String
)

@Serializable
data class VerifyRequest(
    val user_id: String,
    val encrypted_challenge: String
)