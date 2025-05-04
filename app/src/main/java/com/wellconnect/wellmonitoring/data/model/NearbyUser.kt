package com.wellconnect.wellmonitoring.data.model

import kotlinx.serialization.Serializable

@Serializable
data class NearbyUser(
    val userId: String = "",
    val firstName: String,
    val lastName: String,
    val distance: Double,
    val waterNeeds: List<WaterNeed>,
    val isOnline: Boolean
)

@Serializable
sealed class NearbyUsersState {
    object Loading : NearbyUsersState()
    data class Success(val users: List<NearbyUser>) : NearbyUsersState()
    data class Error(val message: String) : NearbyUsersState()
    object NoUsers : NearbyUsersState()
    object LocationPermissionDenied : NearbyUsersState()
}

@Serializable
data class NearbyUsersResponse(
    val status: String,
    val users: List<NearbyUser>,
    val waterNeeds: List<WaterNeed> = emptyList()
)