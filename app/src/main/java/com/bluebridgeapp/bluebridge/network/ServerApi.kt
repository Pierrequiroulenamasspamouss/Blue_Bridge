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
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ServerApi {

    @GET("/api/wells/{espId}/details")
    suspend fun getWellDataById(
        @Path("espId") espId: String
    ): WellData

    @POST("/api/wells")
    suspend fun createWell(
        @Body wellData: WellData,
        @Query("email") email: String,
        @Query("loginToken") token: String
    ): Response<BasicResponse>

    @POST("/api/wells/edit")
    suspend fun editWell(
        @Body wellData: WellData,
        @Query("email") email: String,
        @Query("loginToken") token: String
    ): Response<BasicResponse>

    @DELETE("/api/wells/{espId}")
    suspend fun deleteWell(
        @Path("espId") espId: String,
        @Query("email") email: String,
        @Query("loginToken") token: String
    ): Response<BasicResponse>

    @POST("/api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("/api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<RegisterResponse>



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

    @GET("/api/wells/{espId}/stats")
    suspend fun getWellStats(
        @Path("espId") espId: String
    ) : Response<WellStatsResponse>

    @POST("/api/nearby-users")
    suspend fun getNearbyUsers(
        @Body request: NearbyUsersRequest
    ): Response<NearbyUsersResponse>

    @POST("/api/update-location")
    suspend fun updateLocation(
        @Body request: UpdateLocationRequest
    ): Response<BasicResponse>

    @POST("/api/update-water-needs")
    suspend fun updateWaterNeeds(
        @Body request: UpdateWaterNeedsRequest
    ): Response<BasicResponse>

    @POST("/api/users/update-profile")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): Response<BasicResponse>
    
    @POST("/api/auth/delete-account")
    suspend fun deleteAccount(
        @Body request: DeleteAccountRequest
    ): Response<DeleteAccountResponse>
    
    @POST("/api/notifications/register")
    suspend fun registerNotificationToken(
        @Body request: NotificationTokenRequest
    ): Response<BasicResponse>
    
    @POST("/api/notifications/unregister")
    suspend fun unregisterNotificationToken(
        @Body request: NotificationTokenRequest
    ): Response<BasicResponse>
    
    @POST("/api/weather")
    suspend fun getWeather(
        @Body request: WeatherRequest
    ): Response<WeatherResponse>

    @POST("/api/users/private-location")
    suspend fun doNotShareLocation(
        @Body request: BasicRequest
    ): Response<BasicResponse>

    @GET("/status")
    suspend fun getServerStatus(): Response<ServerStatusResponse>

    @GET("/api/certificates")
    suspend fun getServerCertificate(): Response<CertificateResponse>

    @POST("/api/bugreports")
    suspend fun submitBugReport(
        @Body bugReport: BugReportRequest
    ): Response<BasicResponse>

    @POST("/api/auth/validate")
    suspend fun validateAuthToken(
        @Body request: ValidateAuthTokenRequest
    ): Response<BasicResponse>
}