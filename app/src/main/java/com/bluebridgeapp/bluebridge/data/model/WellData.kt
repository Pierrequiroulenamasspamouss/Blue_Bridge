package com.bluebridgeapp.bluebridge.data.model

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class WellData(
    var id: Int = 0,
    val wellName: String = "",
    val wellLocation: Location = Location(latitude = 0.0, longitude = 0.0),
    val wellWaterType: String = "",
    val wellCapacity: String = "",
    val wellWaterLevel: String = "",
    var lastRefreshTime: Long = 0L, // Unix timestamp in millis --> local information
    val wellStatus: String = "Unknown",
    val waterQuality: WaterQuality = WaterQuality(),
    val extraData: Map<String, JsonElement> = emptyMap(),
    val description: String = "",
    val lastUpdated: String? = null,
    var espId: String,
    val wellWaterConsumption: String,
    val wellOwner: String
)

fun WellData.getLatitude(): Double {
    return wellLocation.latitude
}

fun WellData.getLongitude(): Double {
    return wellLocation.longitude
}



@RequiresApi(Build.VERSION_CODES.O)
fun WellData.hasValidCoordinates(): Boolean {
    return wellLocation.latitude.toString().isNotBlank() &&
            wellLocation.longitude.toString().isNotBlank()
}

@Serializable
data class ShortenedWellData(
    val wellName: String = "Well",
    val wellLocation: Location = Location(latitude = 0.0, longitude = 0.0),
    val wellWaterType: String = "None",
    val wellStatus: String = "Unknown",
    val wellCapacity: String = "No capacity available",
    val wellWaterLevel: String = "0",
    val espId: String = "0"
) {
    fun getLatitude(): Double {
        return wellLocation.latitude
    }
    fun getLongitude(): Double {
        return wellLocation.longitude
    }
}

@Serializable
data class WellsResponse(
    val status: String,
    val data: List<WellData>,
    val pagination: Pagination
)

@Serializable
data class Pagination(
    val total: Int,
    val page: Int,
    val limit: Int,
    val pages: Int
)