package com.bluebridge.bluebridgeapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BasicRequest(
    val status: String,
    val message: String,
    val email: String? = null,
    val timestamp: String,
    val loginToken: String? = null,
)

@Serializable
data class BasicResponse(
    val status: String, //success or not
    val message: String,
    val timestamp: String = "0000-00-00 00:00:00"
)
@Serializable
data class CertificateResponse(
    val status: String,
    val data: String // Base64 encoded certificate
)

@Serializable
data class ServerStatusResponse(
    val status: String,
    val data: ServerStatusData
)

@Serializable
data class ServerStatusData(
    val message: String,
    val mode: String,
    val status: String,
    val timestamp: String,
    val versions: ServerVersions
)

@Serializable
data class ServerVersions(
    val server: String,
    val mobile: String
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val username: String,
    val location: Location,
    val waterNeeds: List<WaterNeed>,
    val phoneNumber: String? = null,
    val role: String = "user",
    val themePreference: Int = 0, // 0: System Default, 1: Light, 2: Dark
)

@Serializable
data class RegisterResponse(
    val status: String,
    val message: String,
    val userData: UserData? = null,
    val loginToken: String? = null // to store locally for future authentication
)


@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val status: String,
    val message: String? = null,
    val data: UserData // Get back the user data stored in the database
)


@Serializable
data class UpdateLocationRequest(
    val email: String,
    val latitude: Double,
    val longitude: Double,
    val token: String? = null
)

@Serializable
data class UpdateWaterNeedsRequest(
    val email: String,
    val waterNeeds: List<WaterNeed>,
    val token: String
)

@Serializable
data class UpdateProfileRequest(
    val email: String,
    val firstName: String,
    val lastName: String,
    val username: String,
    val location: Location,
    val token: String
)

/**
 * Request model for deleting a user account
 */
@Serializable
data class DeleteAccountRequest(
    val email: String,
    val password: String,
    val token: String
)

/**
 * Response model for delete account operation
 */
@Serializable
data class DeleteAccountResponse(
    val status: String,
    val message: String? = null
)

/**
 * Request model for registering or unregistering a notification loginToken
 */
@Serializable
data class NotificationTokenRequest(
    val userId: String,
    val loginToken: String,
    val deviceToken: String
)

@Serializable
data class WellStatsResponse(
    val status: String,
    val stats: WellStats
)

@Serializable
data class WellStats(
    val totalWells: Int,
    val avgCapacity: Double,
    val avgWaterLevel: Double,
    val avgConsumption: Double,
    val totalCapacity: Int,
    val totalWaterLevel: Int,
    val percentageAvailable: Double,
    val statusCounts: Map<String, Int>,
    val waterTypeCounts: Map<String, Int>,
    val recentlyUpdated: Int
)