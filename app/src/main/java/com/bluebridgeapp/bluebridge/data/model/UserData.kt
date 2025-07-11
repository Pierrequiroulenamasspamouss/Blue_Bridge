package com.bluebridgeapp.bluebridge.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserData(
    val email: String,
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    var role: String = "",
    val location: Location = Location(0.0, 0.0),
    val phoneNumber: String? = null,
    val themePreference: Int = 0,
    val lastLogin: String? = null,
    val waterNeeds: List<WaterNeed> = emptyList(),
    val movementSpeeds: MovementSpeeds = MovementSpeeds(),
    val loginToken: String? = null,
    val userId: String = "",
    val languageCode: String = "en"
)


