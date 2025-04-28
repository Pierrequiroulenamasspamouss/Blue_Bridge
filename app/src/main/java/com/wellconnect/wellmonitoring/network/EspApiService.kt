package com.wellconnect.wellmonitoring.network

import com.wellconnect.wellmonitoring.data.LoginResponse
import com.wellconnect.wellmonitoring.data.NearbyUser
import com.wellconnect.wellmonitoring.data.ShortenedWellData
import com.wellconnect.wellmonitoring.data.WellData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class WaterNeed(
    val amount: Int,
    val usageType: String,
    val description: String,
    val priority: Int
)
@Serializable
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val lastUpdated: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val username: String,
    val location: LocationData,
    val waterNeeds: MutableList<WaterNeed>,
    val isWellOwner: Boolean = false,
    val role: String = "user",
)


@Serializable
data class NearbyUsersResponse(
    val status: String,
    val users: List<NearbyUser>,
    val timestamp: String,
    val waterNeeds: List<WaterNeed> = emptyList()
)

@Serializable
data class UpdateLocationRequest(
    val email: String,
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class UpdateWaterNeedsRequest(
    val email: String,
    val waterNeeds: List<com.wellconnect.wellmonitoring.data.WaterNeed>
)

@Serializable
data class BasicResponse(
    val status: String,
    val message: String,
    val timestamp: String
)



@Serializable
data class RegisterResponse(
    val status: String,
    //val timestamp: String,
    val message: String,
    @SerialName("data")
    val userData: UserResponseData? = null
)

@Serializable
data class UserResponseData(
    val user: UserData,
)

@Serializable
data class UserData(
    val email: String,
    val firstName: String,
    val lastName: String,
    val username: String,
    val role: String,
    val location: LocationData = LocationData(0.0, 0.0, ""), //TODO: fix this default value --> be able to accept null and if null, it means there is no location data
    val phoneNumber: String? = null,
    val waterNeeds: Int = 0,
    val isWellOwner: Boolean = false,
    val lastLogin: String? = null,
    val movementSpeeds: MovementSpeeds = MovementSpeeds()
)

@Serializable
data class MovementSpeeds(
    val walkingSpeed: Double = 5.0, // km/h
    val drivingSpeed: Double = 60.0 // km/h
)




interface EspApiService {


    @GET("/api/wells/{espId}")
    suspend fun getWellDataById(
        @Path("espId") espId: String
    ): WellData

    @POST("/api/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("/api/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<RegisterResponse>

    @GET("/api/wells")
    suspend fun getAllWells(): List<ShortenedWellData>

    @GET("/api/nearby-users")
    suspend fun getNearbyUsers(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radius") radius: Double,
        @Query("email") email: String
    ): Response<NearbyUsersResponse>

    @POST("/api/update-location")
    suspend fun updateLocation(
        @Body request: UpdateLocationRequest
    ): Response<BasicResponse>

    @POST("/api/update-water-needs")
    suspend fun updateWaterNeeds(
        @Body request: UpdateWaterNeedsRequest
    ): Response<BasicResponse>
}