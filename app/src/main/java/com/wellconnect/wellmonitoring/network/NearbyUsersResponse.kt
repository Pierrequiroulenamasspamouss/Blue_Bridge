package com.wellconnect.wellmonitoring.network

import com.wellconnect.wellmonitoring.data.WaterNeeds
import kotlinx.serialization.Serializable

@Serializable
data class NearbyUser(
    val firstName: String,
    val lastName: String,
    val waterNeeds: List<WaterNeeds>,
    val distance: Double // Distance in kilometers from the requesting user
)

@Serializable
data class NearbyUsersResponse(
    val status: String,
    val message: String,
    val users: List<NearbyUser>
) 