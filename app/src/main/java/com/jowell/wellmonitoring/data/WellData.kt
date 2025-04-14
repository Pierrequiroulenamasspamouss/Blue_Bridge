package com.jowell.wellmonitoring.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

fun WellData.isSavable(): Boolean {
    return wellName.isNotBlank() ||
            ipAddress.isNotBlank()
}



@Serializable(with = WellDataSerializer::class)
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
    var lastRefreshTime: Long = 0L, // Unix timestamp in millis
    val extraData: Map<String, JsonElement> = emptyMap()
)
