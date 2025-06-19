package com.bluebridge.bluebridgeapp.network

import com.bluebridge.bluebridgeapp.data.model.BasicResponse
import com.bluebridge.bluebridgeapp.data.model.CertificateResponse
import com.bluebridge.bluebridgeapp.data.model.DeleteAccountRequest
import com.bluebridge.bluebridgeapp.data.model.DeleteAccountResponse
import com.bluebridge.bluebridgeapp.data.model.LoginRequest
import com.bluebridge.bluebridgeapp.data.model.LoginResponse
import com.bluebridge.bluebridgeapp.data.model.NearbyUsersRequest
import com.bluebridge.bluebridgeapp.data.model.NearbyUsersResponse
import com.bluebridge.bluebridgeapp.data.model.NotificationTokenRequest
import com.bluebridge.bluebridgeapp.data.model.RegisterRequest
import com.bluebridge.bluebridgeapp.data.model.RegisterResponse
import com.bluebridge.bluebridgeapp.data.model.ServerStatusResponse
import com.bluebridge.bluebridgeapp.data.model.UpdateLocationRequest
import com.bluebridge.bluebridgeapp.data.model.UpdateProfileRequest
import com.bluebridge.bluebridgeapp.data.model.UpdateWaterNeedsRequest
import com.bluebridge.bluebridgeapp.data.model.WeatherRequest
import com.bluebridge.bluebridgeapp.data.model.WeatherResponse
import com.bluebridge.bluebridgeapp.data.model.WellData
import com.bluebridge.bluebridgeapp.data.model.WellStatsResponse
import com.bluebridge.bluebridgeapp.data.model.WellsResponse
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
    suspend fun getAllWells(): Response<WellsResponse>

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

    @GET("/api/wellStats")
    suspend fun getWellsStats(): Response<WellStatsResponse>

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

    @GET("/status")
    suspend fun getServerStatus(): Response<ServerStatusResponse>

    @GET("/api/certificates")
    suspend fun getServerCertificate(): Response<CertificateResponse>
}