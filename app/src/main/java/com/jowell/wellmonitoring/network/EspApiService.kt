package com.jowell.wellmonitoring.network

import com.jowell.wellmonitoring.data.WellData
import retrofit2.http.GET

interface EspApiService {
    @GET("/data")
    suspend fun getWellData(): WellData
}
