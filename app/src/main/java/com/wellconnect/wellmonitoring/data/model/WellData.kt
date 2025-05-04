
import android.os.Build
import androidx.annotation.RequiresApi
import com.wellconnect.wellmonitoring.data.model.Location
import com.wellconnect.wellmonitoring.data.model.WaterQuality
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
    val wellName: String = "Well",
    val wellLocation: Location = Location(latitude = 0.0, longitude = 0.0),
    val wellWaterType: String = "None",
    val espId: String,
    val wellStatus: String = "Unknown",
    val wellCapacity: String = "No capacity available",
    val wellWaterLevel: String = "0",



    )

@RequiresApi(Build.VERSION_CODES.O)
fun ShortenedWellData.getLatitude(): Double? {
    return (wellLocation.latitude)
}

@RequiresApi(Build.VERSION_CODES.O)
fun ShortenedWellData.getLongitude(): Double? {
    return (wellLocation.longitude)
}