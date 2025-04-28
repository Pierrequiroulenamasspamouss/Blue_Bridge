package com.wellconnect.wellmonitoring.data

import android.os.Build
import androidx.annotation.RequiresApi
import com.wellconnect.wellmonitoring.utils.parseLocationInput
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

fun WellData.isSavable(): Boolean {
    return espId.isNotBlank()
}
suspend fun hasNoDuplicateEspId(espId: String, currentWellId: Int, wellDataStore: WellDataStore): Boolean {
    return wellDataStore.wellListFlow.first().none { it.espId == espId && it.id != currentWellId }
}

@RequiresApi(Build.VERSION_CODES.O)
fun WellData.hasValidCoordinates(): Boolean {
    val locationData = parseLocationInput(wellLocation)
    return locationData?.latitude != null
}

@RequiresApi(Build.VERSION_CODES.O)
fun WellData.getLatitude(): Double? {
    val locationData = parseLocationInput(wellLocation)
    return (locationData?.latitude)
}

@RequiresApi(Build.VERSION_CODES.O)
fun WellData.getLongitude(): Double? {
    val locationData = parseLocationInput(wellLocation)
    return (locationData?.longitude)
}

@RequiresApi(Build.VERSION_CODES.O)
fun WellData.formatLocation(): String {
    return if (wellLocation.isBlank()) {
        "Location not set"
    } else if (!hasValidCoordinates()) {
        wellLocation
    } else {
        val locationData = parseLocationInput(wellLocation)
        "Location:\nlat: ${locationData?.latitude}\nlon: ${locationData?.longitude}"
    }
}

// Extension functions for ShortenedWellData
@RequiresApi(Build.VERSION_CODES.O)
fun ShortenedWellData.hasValidCoordinates(): Boolean {
    val locationData = parseLocationInput(wellLocation)
    return locationData?.latitude != null
}

@RequiresApi(Build.VERSION_CODES.O)
fun ShortenedWellData.getLatitude(): Double? {
    val locationData = parseLocationInput(wellLocation)
    return (locationData?.latitude)
}

@RequiresApi(Build.VERSION_CODES.O)
fun ShortenedWellData.getLongitude(): Double? {
    val locationData = parseLocationInput(wellLocation)
    return (locationData?.longitude)
}

@RequiresApi(Build.VERSION_CODES.O)
fun ShortenedWellData.formatLocation(): String {
    return if (wellLocation.isBlank()) {
        "Location not set"
    } else if (!hasValidCoordinates()) {
        wellLocation
    } else {
        val locationData = parseLocationInput(wellLocation)
        "Location:\nlat: ${locationData?.latitude}\nlon: ${locationData?.longitude}"
    }
}

@Serializable
data class ShortenedWellData(
    val wellName: String,
    val wellLocation: String,
    val wellWaterType: String,
    val espId: String,
    val wellStatus: String = "Unknown",
    val wellOwner: String = "",
    val wellCapacity: String = "",
    val wellWaterLevel: String = "",
    val wellWaterConsumption: String = ""
)

@Serializable
data class WaterQuality(
    val ph: Double = 7.0,
    val turbidity: Double = 0.0,
    val tds: Int = 0
)

@Serializable(with = WellDataSerializer::class)
data class WellData(
    var id: Int = 0,
    val wellName: String = "",
    val wellOwner: String = "",
    val wellLocation: String = "", //Location:\nlon:float\nlat:\float
    val wellWaterType: String = "",
    val wellCapacity: String = "",
    val wellWaterLevel: String = "",
    val wellWaterConsumption: String = "",
    var espId: String = "",
    //var ipAddress: String = "",
    var lastRefreshTime: Long = 0L, // Unix timestamp in millis --> local information
    val extraData: Map<String, JsonElement> = emptyMap(),
    val wellStatus: String = "Unknown",
    val waterQuality: WaterQuality = WaterQuality()
)
