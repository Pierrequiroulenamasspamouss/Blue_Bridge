import android.os.Build
import androidx.annotation.RequiresApi
import com.bluebridge.bluebridgeapp.data.model.Location
import com.bluebridge.bluebridgeapp.data.model.WaterQuality
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class WellData(
    var id: Int = 0,
    val wellName: String = "",
    val wellOwner: String = "",
    val wellLocation: Location = Location(latitude = 0.0, longitude = 0.0),
    val wellWaterType: String = "",
    val wellCapacity: String = "",
    val wellWaterLevel: String = "",
    val wellWaterConsumption: String = "", 
    var espId: String = "",
    var lastRefreshTime: Long = 0L, // Unix timestamp in millis --> local information
    val wellStatus: String = "Unknown",
    val waterQuality: WaterQuality = WaterQuality(),
    val extraData: Map<String, JsonElement> = emptyMap()
    //TODO: create a dataAge field to have the data was the last time updated by an owner for example
)

@RequiresApi(Build.VERSION_CODES.O)
fun WellData.hasValidCoordinates(): Boolean {
    return wellLocation.latitude.toString().isNotBlank() &&
            wellLocation.longitude.toString().isNotBlank()

}

@RequiresApi(Build.VERSION_CODES.O)
fun WellData.getLatitude(): Double? {
    return (wellLocation.latitude)
}

@RequiresApi(Build.VERSION_CODES.O)
fun WellData.getLongitude(): Double? {
    return (wellLocation.longitude)
}


@Serializable
data class ShortenedWellData(
    val id: String = "0", //TODO : not needed. Remove
    val wellName: String = "Well",
    val wellLocation: Location = Location(latitude = 0.0, longitude = 0.0),
    val wellWaterType: String = "None",
    val espId: String = "", //TODO : not needed. Remove
    val wellStatus: String = "Unknown",
    val wellCapacity: String = "No capacity available",
    val wellWaterLevel: String = "0",
    val wellWaterConsumption: String = "0", //TODO : not needed. Remove
    val wellOwner: String = "" //TODO : not needed. Remove
)

@RequiresApi(Build.VERSION_CODES.O)
fun ShortenedWellData.getLatitude(): Double? {
    return (wellLocation.latitude)
}

@RequiresApi(Build.VERSION_CODES.O)
fun ShortenedWellData.getLongitude(): Double? {
    return (wellLocation.longitude)
}


@Serializable
//TODO : fix with the response {"status":"success","data":[],"pagination":{"total":0,"page":1,"limit":20,"pages":0}} and remove the backwards compatibility
data class PaginatedWellsResponse(
    val status: String,
    val data: List<WellData>? = null,
    val pagination: PaginationInfo? = null,
    // Keep old fields for backward compatibility
    val page: Int = 0,
    val limit: Int = 0,
    val totalWells: Int = 0,
    val pages: Int = 0,
    val wells: List<ShortenedWellData> = emptyList()
) {
    // Add computed properties to maintain backward compatibility
    val processedWells: List<ShortenedWellData>
        get() = if (data != null) {
            data.map { wellData ->
                ShortenedWellData(
                    id = wellData.id.toString(),
                    wellName = wellData.wellName,
                    wellLocation = wellData.wellLocation,
                    wellWaterType = wellData.wellWaterType,
                    espId = wellData.espId,
                    wellStatus = wellData.wellStatus,
                    wellOwner = wellData.wellOwner,
                    wellCapacity = wellData.wellCapacity,
                    wellWaterLevel = wellData.wellWaterLevel,
                    wellWaterConsumption = wellData.wellWaterConsumption
                )
            }
        } else {
            wells
        }
        
    val processedTotalWells: Int
        get() = pagination?.total ?: totalWells

}

@Serializable
data class PaginationInfo(
    val total: Int,
    val page: Int,
    val limit: Int,
    val pages: Int
)