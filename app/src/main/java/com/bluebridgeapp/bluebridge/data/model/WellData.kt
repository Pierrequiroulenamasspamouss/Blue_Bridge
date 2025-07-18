package com.bluebridgeapp.bluebridge.data.model

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class WellDetailsResponse(
    val status: String,
    val data: WellDetailsData
)

@Serializable
data class WellDetailsData(
    val wellId: String,
    val wellName: String,
    val wellOwner: String?,
    val wellLocation: String, // This is a JSON string
    val wellWaterType: String,
    val wellCapacity: Int,
    val wellWaterLevel: Int,
    val wellWaterConsumption: Int?,
    val wellStatus: String,
    val waterQuality: String, // This is a JSON string
    val createdAt: String,
    val updatedAt: String,
    val imageCount: Int
)

// Update your WellData class to include parsing helpers
@Serializable
data class WellData(
    var wellId: String? = null,
    val wellName: String? = null,
    val wellLocation: Location? = null,
    val wellWaterType: String? = null,
    val wellCapacity: String? = null,
    val wellWaterLevel: String? = null,
    var lastRefreshTime: Long? = null,
    val wellStatus: String? = "Unknown",
    val waterQuality: WaterQuality? = null,
    val description: String? = null,
    val lastUpdated: String? = null,
    val wellWaterConsumption: String? = null,
    var wellOwner: String? = null,
    val images: List<ImageData>? = emptyList()
) {
    fun toShortenedWell(data: WellData): ShortenedWellData {
        return ShortenedWellData(
            wellId = data.wellId ?: "",
            wellName = data.wellName ?: "",
            wellLocation = data.wellLocation ?: Location(0.0, 0.0),
            wellWaterType = data.wellWaterType ?: "Unknown",
            wellStatus = data.wellStatus ?: "Unknown",
            wellCapacity = data.wellCapacity ?: "0",
            wellWaterLevel = data.wellWaterLevel ?: "0"
        )
    }

    companion object {
        fun fromDetailsResponse(response: WellDetailsResponse): WellData {
            val details = response.data
            return WellData(
                wellId = details.wellId,
                wellName = details.wellName,
                wellLocation = Json.decodeFromString(details.wellLocation),
                wellWaterType = details.wellWaterType,
                wellCapacity = details.wellCapacity.toString(),
                wellWaterLevel = details.wellWaterLevel.toString(),
                wellStatus = details.wellStatus,
                waterQuality = try {
                    Json.decodeFromString(details.waterQuality)
                } catch (e: Exception) {
                    null
                },
                wellWaterConsumption = details.wellWaterConsumption?.toString(),
                wellOwner = details.wellOwner,
                images = emptyList() // Will be populated separately
            )
        }
    }
}

fun WellData.getLatitude(): Double = wellLocation?.latitude ?: 0.0
fun WellData.getLongitude(): Double = wellLocation?.longitude ?: 0.0

@RequiresApi(Build.VERSION_CODES.O)
fun WellData.hasValidCoordinates(): Boolean {
    return wellLocation?.latitude.toString().isNotBlank() &&
            wellLocation?.longitude.toString().isNotBlank()
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
data class WellImageResponse(
    val status: String,
    val data: ImageData
)

@Serializable
data class ImageData(
    val description: String,
    val uploadDate: String,
    val fileSize: Long,
    val base64encodedImage: String
)