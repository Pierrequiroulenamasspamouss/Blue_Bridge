package com.bluebridge.bluebridgeapp.data.interfaces

import com.bluebridge.bluebridgeapp.data.model.ShortenedWellData
import com.bluebridge.bluebridgeapp.data.model.WellData
import com.bluebridge.bluebridgeapp.data.model.WellStatsResponse
import com.bluebridge.bluebridgeapp.data.model.WellsResponse
import kotlinx.coroutines.flow.Flow

interface WellRepository {
    val wellListFlow: Flow<List<WellData>>
    suspend fun getWellById(id: Int): WellData? //Local
    suspend fun getAllWells(): List<ShortenedWellData> //Server
    suspend fun getSavedWells(): List<WellData> //Local
    suspend fun getFilteredWells(
        page: Int = 1,
        limit: Int = 20,
        wellName: String? = null,
        wellStatus: String? = null,
        wellWaterType: String? = null,
        wellOwner: String? = null,
        espId: String? = null,
        minWaterLevel: Int? = null,
        maxWaterLevel: Int? = null
    ): Result<WellsResponse> //Server
    suspend fun saveWell(well: WellData): Boolean //Local
    suspend fun deleteWell(espId: String): Boolean //Local
    suspend fun getStats(espId: String): WellStatsResponse? //Server
    suspend fun isEspIdUnique(espId: String): Boolean //Local
    suspend fun swapWells(from: Int, to: Int) //Local
    suspend fun saveWellToServer(wellData: WellData, email: String, token: String): Boolean //Server
    suspend fun deleteWellFromServer(espId: String, email: String, token: String): Boolean //Server
}


