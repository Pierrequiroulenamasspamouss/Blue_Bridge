package com.wellconnect.wellmonitoring.network

import ShortenedWellData
import WellData
import com.wellconnect.wellmonitoring.data.model.BasicResponse
import com.wellconnect.wellmonitoring.data.model.DeleteAccountRequest
import com.wellconnect.wellmonitoring.data.model.DeleteAccountResponse
import com.wellconnect.wellmonitoring.data.model.LoginRequest
import com.wellconnect.wellmonitoring.data.model.LoginResponse
import com.wellconnect.wellmonitoring.data.model.NearbyUsersResponse
import com.wellconnect.wellmonitoring.data.model.RegisterRequest
import com.wellconnect.wellmonitoring.data.model.RegisterResponse
import com.wellconnect.wellmonitoring.data.model.UpdateLocationRequest
import com.wellconnect.wellmonitoring.data.model.UpdateProfileRequest
import com.wellconnect.wellmonitoring.data.model.UpdateWaterNeedsRequest
import com.wellconnect.wellmonitoring.data.model.WellsStatistics
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query


interface ServerApi {


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

    @GET("/api/wells/stats/summary")
    suspend fun getWellStats(): List<WellsStatistics>

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

    @POST("/api/update-profile")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): Response<BasicResponse>
    
    @POST("/api/delete-account")
    suspend fun deleteAccount(
        @Body request: DeleteAccountRequest
    ): Response<DeleteAccountResponse>
}