package com.wellconnect.wellmonitoring.network

import com.wellconnect.wellmonitoring.data.BasicResponse
import com.wellconnect.wellmonitoring.data.LoginRequest
import com.wellconnect.wellmonitoring.data.LoginResponse
import com.wellconnect.wellmonitoring.data.NearbyUsersResponse
import com.wellconnect.wellmonitoring.data.RegisterRequest
import com.wellconnect.wellmonitoring.data.RegisterResponse
import com.wellconnect.wellmonitoring.data.ShortenedWellData
import com.wellconnect.wellmonitoring.data.UpdateLocationRequest
import com.wellconnect.wellmonitoring.data.UpdateWaterNeedsRequest
import com.wellconnect.wellmonitoring.data.WellData
import com.wellconnect.wellmonitoring.data.WellsStatistics
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query


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

    @GET("/api/stats")
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
}