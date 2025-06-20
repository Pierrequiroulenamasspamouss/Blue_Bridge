package com.bluebridge.bluebridgeapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class NearbyUser(
    val userId: String = "",
    val username: String = "",
    val firstName: String,
    val lastName: String,
    val email: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val waterNeeds: List<WaterNeed> = emptyList(),
    val lastActive: String = "",
    val distance: Double
)

@Serializable
sealed class NearbyUsersState {
    object Loading : NearbyUsersState()
    data class Success(val users: List<NearbyUser>) : NearbyUsersState()
    data class Error(val message: String) : NearbyUsersState()
}

@Serializable
data class NearbyUsersRequest(
    val latitude: Double,
    val longitude: Double,
    val radius: Double,
    val userId: String,
    val loginToken: String
)

@Serializable
data class NearbyUsersResponse(
    val status: String,
    val message: String,
    val data: List<NearbyUser> = emptyList()
)
