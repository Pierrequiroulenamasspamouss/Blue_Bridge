package com.wellconnect.wellmonitoring.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

fun WellData.isSavable(): Boolean {
    return wellName.isNotBlank() ||
            ipAddress.isNotBlank()
}

fun WellData.getLatitude(): Double? {
    return try {
        wellLocation.lines()
            .find { it.trim().startsWith("lat:") }
            ?.substringAfter("lat:")
            ?.trim()
            ?.toDoubleOrNull()
    } catch (e: Exception) {
        null
    }
}

fun WellData.getLongitude(): Double? {
    return try {
        wellLocation.lines()
            .find { it.trim().startsWith("lon:") }
            ?.substringAfter("lon:")
            ?.trim()
            ?.toDoubleOrNull()
    } catch (e: Exception) {
        null
    }
}

fun WellData.hasValidCoordinates(): Boolean {
    return getLatitude() != null && getLongitude() != null
}

fun WellData.formatLocation(): String {
    return if (wellLocation.isBlank()) {
        "Location not set"
    } else if (!hasValidCoordinates()) {
        wellLocation
    } else {
        "Location:\nlat: ${getLatitude()}\nlon: ${getLongitude()}"
    }
}

@Serializable(with = WellDataSerializer::class)
data class WellData(
    var id: Int = 0,
    val wellName: String = "",
    val wellOwner: String = "",
    val wellLocation: String = "",
    val wellWaterType: String = "",
    val wellCapacity: Int = 0,
    val wellWaterLevel: Int = 0,
    val wellWaterConsumption: Int = 0,
    val espId: String = "",
    var ipAddress: String = "",
    var lastRefreshTime: Long = 0L, // Unix timestamp in millis
    val extraData: Map<String, JsonElement> = emptyMap(),
    val wellStatus: String = "Unknown"
)
