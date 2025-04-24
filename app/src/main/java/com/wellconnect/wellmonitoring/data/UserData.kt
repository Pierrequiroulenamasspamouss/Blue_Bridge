package com.wellconnect.wellmonitoring.data

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@InternalSerializationApi @Serializable
data class UserData(
    val email: String,
    val firstName: String,
    val lastName: String,
    val username: String,
    val role: String,
    val themePreference: Int = 0 // 0: System Default, 1: Light, 2: Dark
) 