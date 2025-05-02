package com.wellconnect.wellmonitoring.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServerData(
    val user: UserData,
    val location: Location? = null,
)
@Serializable
data class BasicRequest(
    val status: String,
    val message: String,
    val timestamp: String
)

@Serializable
data class BasicResponse(
    val status: String,
    val message: String,
    val timestamp: String
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
    val isWellOwner: Boolean = false,
    val role: String = "user",
)

@Serializable
data class RegisterResponse(
    val status: String,
    //val timestamp: String,
    val message: String,
    @SerialName("data")
    val userData: UserData? = null
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
    val data: ServerData? = null
)

@Serializable
data class UpdateLocationRequest(
    val email: String,
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class UpdateWaterNeedsRequest(
    val email: String,
    val waterNeeds: List<WaterNeed>
)

@Serializable
data class NearbyUsersResponse(
    val status: String,
    val users: List<NearbyUser>,
    val timestamp: String,
    val waterNeeds: List<WaterNeed> = emptyList()
)

@Serializable
data class WellsStatistics(
    val number : Int,
    val waterType : String,
    val totalCapacity : String,
)
