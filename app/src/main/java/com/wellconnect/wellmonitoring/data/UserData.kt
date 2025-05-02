package com.wellconnect.wellmonitoring.data

import kotlinx.serialization.Serializable

@Serializable
data class UserData(
    val email: String,
    val firstName: String = "Guest",
    val lastName: String = "Guest",
    val username: String = "GuestUsername",
    val role: String = "user",
    val location: Location = Location(0.0, 0.0),
    val phoneNumber: String? = null ,
    val themePreference: Int = 0, // 0: System Default, 1: Light, 2: Dark
    val lastLogin: String? = null,
    val waterNeeds: List<WaterNeed> = emptyList(),
    val isWellOwner: Boolean = false,
    val movementSpeeds: MovementSpeeds = MovementSpeeds()
)


@Serializable
data class NearbyUser(
    val firstName: String,
    val lastName: String,
    val distance: Double,
    val waterNeeds: List<WaterNeed>,
    val isOnline: Boolean
)

