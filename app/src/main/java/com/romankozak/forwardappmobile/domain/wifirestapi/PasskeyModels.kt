package com.romankozak.forwardappmobile.domain.wifirestapi

import kotlinx.serialization.Serializable

@Serializable
data class UsernameRequest(val username: String)

@Serializable
data class PasskeyOptionsResponse(
    // Ми отримуємо JSON як рядок і передаємо його напряму в Credential Manager
    val options: String
)

@Serializable
data class VerifyRegistrationRequest(
    val username: String,
    val credential: String // JSON-рядок з відповіддю від Credential Manager
)

@Serializable
data class VerifyAuthRequest(
    val username: String,
    val credential: String // JSON-рядок з відповіддю від Credential Manager
)

@Serializable
data class AuthResponse(
    val verified: Boolean,
    val access_token: String? = null
)