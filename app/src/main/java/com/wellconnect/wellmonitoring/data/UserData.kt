package com.wellconnect.wellmonitoring.data

import kotlinx.serialization.Serializable

@Serializable
data class UserData(
    val email: String,
    val firstName: String,
    val lastName: String,
    val username: String,
    val role: String,
    val themePreference: Int = 0, // 0: System Default, 1: Light, 2: Dark
    val location: Location = Location(0.0, 0.0),
    val waterNeeds: List<WaterNeed> = emptyList(),
    val isWellOwner: Boolean = false //TODO: fix that this is not saved anywhere I think.
)

@Serializable
data class Location(
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class WaterNeed(
    val amount: Double,
    val usageType: String,
    val description: String,
    val priority: Int
)

// Response data classes for server communication
@Serializable
data class LoginResponse(
    val status: String,
    val message: String? = null,
    val data: ServerData? = null
)

@Serializable
data class ServerData(
    val user: ServerUser,
    val location: ServerLocation? = null,
    val waterNeeds: List<ServerWaterNeed>? = null
)

@Serializable
data class ServerUser(
    val email: String,
    val firstName: String,
    val lastName: String,
    val username: String,
    val role: String
)

@Serializable
data class ServerLocation(
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class ServerWaterNeed(
    val amount: Double,
    val usageType: String,
    val description: String,
    val priority: Int
)

@Serializable
data class NearbyUser(
    val firstName: String,
    val lastName: String,
//    val username: String,
    val distance: Double,
//    val location: Location,
    val waterNeeds: List<WaterNeed>,
    val isOnline: Boolean
)