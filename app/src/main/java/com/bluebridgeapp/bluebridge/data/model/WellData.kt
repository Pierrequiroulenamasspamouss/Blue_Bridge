package com.bluebridgeapp.bluebridge.data.model

import android.media.Image
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlin.js.ExperimentalJsFileName

@Serializable
data class WellData(
    var wellId: String,
    val wellName: String,
    val wellLocation: Location,
    val wellWaterType: String,
    val wellCapacity: String,
    val wellWaterLevel: String,
    var lastRefreshTime: Long? = null,
    val wellStatus: String = "Unknown",
    val waterQuality: WaterQuality? = null,
    val description: String? = null,
    val lastUpdated: String? = null,
    val wellWaterConsumption: String? = null,
    var wellOwner: String?= null,
    val images: List<ImageData>? = emptyList()) {
    fun toShortenedWell(wellData: WellData): ShortenedWellData {
        return ShortenedWellData(
            wellName = wellData.wellName,
            wellLocation = wellData.wellLocation,
            wellWaterType = wellData.wellWaterType,
            wellStatus = wellData.wellStatus,
            wellCapacity = wellData.wellCapacity,
            wellWaterLevel = wellData.wellWaterLevel,
            wellId = wellData.wellId
        )
    }
}

fun WellData.getLatitude(): Double = wellLocation.latitude
fun WellData.getLongitude(): Double = wellLocation.longitude

@RequiresApi(Build.VERSION_CODES.O)
fun WellData.hasValidCoordinates(): Boolean {
    return wellLocation.latitude.toString().isNotBlank() &&
            wellLocation.longitude.toString().isNotBlank()
}

@Serializable
data class ShortenedWellData(
    val wellName: String ,
    val wellLocation: Location ,
    val wellWaterType: String ,
    val wellStatus: String,
    val wellCapacity: String ,
    val wellWaterLevel: String ,
    val wellId: String
) {
    fun getLatitude(): Double = wellLocation.latitude
    fun getLongitude(): Double = wellLocation.longitude
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

@Serializable
data class ImageData(
    val imageNumber: Int,
    val description: String,
    val fileName: String,
    val uploadDate: String,
    val fileSize: Long
)