package com.bluebridgeapp.bluebridge.network

import com.bluebridgeapp.bluebridge.data.model.BasicRequest
import com.bluebridgeapp.bluebridge.data.model.BasicResponse
import com.bluebridgeapp.bluebridge.data.model.BugReportRequest
import com.bluebridgeapp.bluebridge.data.model.CertificateResponse
import com.bluebridgeapp.bluebridge.data.model.DeleteAccountRequest
import com.bluebridgeapp.bluebridge.data.model.DeleteAccountResponse
import com.bluebridgeapp.bluebridge.data.model.LoginRequest
import com.bluebridgeapp.bluebridge.data.model.LoginResponse
import com.bluebridgeapp.bluebridge.data.model.NearbyUsersRequest
import com.bluebridgeapp.bluebridge.data.model.NearbyUsersResponse
import com.bluebridgeapp.bluebridge.data.model.NotificationTokenRequest
import com.bluebridgeapp.bluebridge.data.model.RegisterRequest
import com.bluebridgeapp.bluebridge.data.model.RegisterResponse
import com.bluebridgeapp.bluebridge.data.model.ServerStatusResponse
import com.bluebridgeapp.bluebridge.data.model.UpdateLocationRequest
import com.bluebridgeapp.bluebridge.data.model.UpdateProfileRequest
import com.bluebridgeapp.bluebridge.data.model.UpdateWaterNeedsRequest
import com.bluebridgeapp.bluebridge.data.model.ValidateAuthTokenRequest
import com.bluebridgeapp.bluebridge.data.model.WeatherRequest
import com.bluebridgeapp.bluebridge.data.model.WeatherResponse
import com.bluebridgeapp.bluebridge.data.model.WellData
import com.bluebridgeapp.bluebridge.data.model.WellStatsResponse
import com.bluebridgeapp.bluebridge.data.model.WellsResponse
import com.bluebridgeapp.bluebridge.data.model.ImageData
import com.bluebridgeapp.bluebridge.data.model.WellImageResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ServerApi {


    //------------------------------------------------------------------------//
    //Authentication
    @POST("/api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("/api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<RegisterResponse>

    @POST("/api/auth/delete-account")
    suspend fun deleteAccount(
        @Body request: DeleteAccountRequest
    ): Response<DeleteAccountResponse>

    @POST("/api/auth/validate")
    suspend fun validateAuthToken(
        @Body request: ValidateAuthTokenRequest
    ): Response<BasicResponse>


    //------------------------------------------------------------------------//
    // Well information

    // Get the full data of a well
    @GET("/api/wells/{espId}/details")
    suspend fun getWellDataById(
        @Path("espId") espId: String
    ): WellData

    // Upload a well to the server TODO(implement this to the server)
    @POST("/api/wells")
    suspend fun createWell(
        @Body wellData: WellData,
        @Query("userId") email: String,
        @Query("loginToken") token: String
    ): Response<BasicResponse>

    // Edit a well
    @POST("/api/wells/edit")
    suspend fun editWell(
        @Body wellData: WellData,
        @Query("email") email: String,
        @Query("loginToken") token: String
    ): Response<BasicResponse>

    // Delete a well from the server
    @DELETE("/api/wells/{espId}")
    suspend fun deleteWell(
        @Path("espId") espId: String,
        @Query("email") email: String,
        @Query("loginToken") token: String
    ): Response<BasicResponse>

    // Get a list of the available wells with filters
    @GET("/api/wells")
    suspend fun getWellsWithFilters(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("wellName") wellName: String? = null,
        @Query("wellStatus") wellStatus: String? = null,
        @Query("wellWaterType") wellWaterType: String? = null,
        @Query("wellOwner") wellOwner: String? = null,
        @Query("espId") espId: String? = null,
        @Query("minWaterLevel") minWaterLevel: Int? = null,
        @Query("maxWaterLevel") maxWaterLevel: Int? = null
    ): Response<WellsResponse>

    // Get the statistics of a specific well
    @GET("/api/wells/{espId}/stats")
    suspend fun getWellStats(
        @Path("espId") espId: String
    ) : Response<WellStatsResponse>


    // Get an image of a specific well (returns ImageData, no base64)
    @GET("/api/wells/{espId}/images/{imageNumber}")
    suspend fun getWellImage(
        @Path("espId") espId: String,
        @Path("imageNumber") imageNumber: Int
    ) : Response<WellImageResponse>

    // Upload a picture of a specific well to the server at a specific imageNumber
    @Multipart
    @POST("/api/wells/{espId}/images/{imageNumber}/upload")
    suspend fun uploadWellPicture(
        @Path("espId") wellId: String,
        @Path("imageNumber") imageNumber: Int,
        @Part image: MultipartBody.Part
    ): Response<BasicResponse>

    // Delete the image of a specific well at a specific imageNumber
    @DELETE("/api/wells/{espId}/images/{imageNumber}")
    suspend fun deleteWellImage(
        @Path("espId") wellId: String,
        @Path("imageNumber") imageNumber: Int
    ): Response<BasicResponse>

    //------------------------------------------------------------------------//
    // Nearby Users

    // Get the nearby users (with filters)
    @POST("/api/nearby-users")
    suspend fun getNearbyUsers(
        @Body request: NearbyUsersRequest
    ): Response<NearbyUsersResponse>

    //------------------------------------------------------------------------//
    // User management

    // Not used, maybe in the future to get more precise location of where the user is ?
    @POST("/api/update-location")
    suspend fun updateLocation(
        @Body request: UpdateLocationRequest
    ): Response<BasicResponse>

    // Update the water needs of a user
    @POST("/api/update-water-needs")
    suspend fun updateWaterNeeds(
        @Body request: UpdateWaterNeedsRequest
    ): Response<BasicResponse>

    // Update the profile of a user
    @POST("/api/users/update-profile")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): Response<BasicResponse>


    // Ask to not share the location with other users
    @POST("/api/users/private-location")
    suspend fun doNotShareLocation(
        @Body request: BasicRequest
    ): Response<BasicResponse>


    //------------------------------------------------------------------------//
    // Weather
    @POST("/api/weather")
    suspend fun getWeather(
        @Body request: WeatherRequest
    ): Response<WeatherResponse>

    //------------------------------------------------------------------------//
    // Notifications
    @POST("/api/notifications/register")
    suspend fun registerNotificationToken(
        @Body request: NotificationTokenRequest
    ): Response<BasicResponse>

    @POST("/api/notifications/unregister")
    suspend fun unregisterNotificationToken(
        @Body request: NotificationTokenRequest
    ): Response<BasicResponse>

    //------------------------------------------------------------------------//
    // Various tools
    @GET("/status")
    suspend fun getServerStatus(): Response<ServerStatusResponse>

    @GET("/api/certificates")
    suspend fun getServerCertificate(): Response<CertificateResponse>

    @POST("/api/bugreports")
    suspend fun submitBugReport(
        @Body bugReport: BugReportRequest
    ): Response<BasicResponse>



}