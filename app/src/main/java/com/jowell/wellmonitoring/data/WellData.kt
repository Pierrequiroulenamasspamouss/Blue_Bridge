package com.jowell.wellmonitoring.data

import kotlinx.serialization.Serializable

@Serializable
data class WellData(
    val id: Int = 0,
    val wellName: String = "",
    val wellOwner: String = "",
    val wellLocation: String = "",
    val wellWaterType: String = "",
    val wellCapacity: Int = 0,
    val wellWaterLevel: Int = 0,
    val wellWaterConsumption: Int = 0,
    val espId: String = "",
    val ipAddress: String = "",
    val extraData: Map<String, String> = emptyMap()
)

