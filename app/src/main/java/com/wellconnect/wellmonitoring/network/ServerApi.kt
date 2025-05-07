package com.wellconnect.wellmonitoring.network

import PaginatedWellsResponse
import WellData
import com.wellconnect.wellmonitoring.data.model.BasicResponse
import com.wellconnect.wellmonitoring.data.model.DeleteAccountRequest
import com.wellconnect.wellmonitoring.data.model.DeleteAccountResponse
import com.wellconnect.wellmonitoring.data.model.LoginRequest
import com.wellconnect.wellmonitoring.data.model.LoginResponse
import com.wellconnect.wellmonitoring.data.model.NearbyUsersResponse
import com.wellconnect.wellmonitoring.data.model.NotificationTokenRequest
import com.wellconnect.wellmonitoring.data.model.RegisterRequest
import com.wellconnect.wellmonitoring.data.model.RegisterResponse
import com.wellconnect.wellmonitoring.data.model.UpdateLocationRequest
import com.wellconnect.wellmonitoring.data.model.UpdateProfileRequest
import com.wellconnect.wellmonitoring.data.model.UpdateWaterNeedsRequest
import com.wellconnect.wellmonitoring.data.model.WellStatsResponse
import retrofit2.Response
import retrofit2.http.Body
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
        @Query("token") token: String
    ): Response<BasicResponse>

    @POST("/api/wells/delete/{espId}")
    suspend fun deleteWell(
        @Path("espId") espId: String,
        @Query("email") email: String,
        @Query("token") token: String
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
    suspend fun getAllWells(): Response<PaginatedWellsResponse>
    
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
    ): Response<PaginatedWellsResponse>

    @GET("/api/wellStats")
    suspend fun getWellsStats(): Response<WellStatsResponse>

    @GET("/api/nearby-users")
    suspend fun getNearbyUsers(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radius") radius: Double,
        @Query("email") email: String,
        @Query("token") token: String
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
    
    @GET("/api/weather")
    suspend fun getWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("email") email: String,
        @Query("token") token: String
    ): Response<Map<String, Any>>
}