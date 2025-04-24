package com.wellconnect.wellmonitoring.data

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val username: String? = null,
    val isRegistration: Boolean = false
)