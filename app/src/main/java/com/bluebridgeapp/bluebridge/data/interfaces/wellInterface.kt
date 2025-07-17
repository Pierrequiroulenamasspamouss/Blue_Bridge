package com.bluebridgeapp.bluebridge.data.interfaces

import android.media.Image
import com.bluebridgeapp.bluebridge.data.model.ShortenedWellData
import com.bluebridgeapp.bluebridge.data.model.WellData
import com.bluebridgeapp.bluebridge.data.model.WellStatsResponse
import com.bluebridgeapp.bluebridge.data.model.WellsResponse
import kotlinx.coroutines.flow.Flow

interface WellRepository {
    val wellListFlow: Flow<List<WellData>>
    suspend fun getWellById(id: Int): WellData?
    suspend fun getAllWells(): List<ShortenedWellData>
    suspend fun getSavedWells(): List<WellData>
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
    ): Result<WellsResponse>
    suspend fun saveWell(well: WellData): Boolean
    suspend fun deleteWell(espId: String): Boolean
    suspend fun getStats(espId: String): WellStatsResponse?
    suspend fun isEspIdUnique(espId: String): Boolean
    suspend fun swapWells(from: String, to: String)
    suspend fun saveWellToServer(wellData: WellData, email: String, token: String): Boolean
    suspend fun deleteWellFromServer(espId: String, email: String, token: String): Boolean
    suspend fun getAllImages(wellId: String): List<android.graphics.Bitmap>
    suspend fun uploadWellPicture(wellId: String, imageNumber: Int, image: android.graphics.Bitmap): Boolean
    suspend fun getWellFromServer(wellId: String): WellData?
    suspend fun getWellImageAsComposeBitmap(imageNumber: Int): androidx.compose.ui.graphics.ImageBitmap?
    suspend fun getWellImageAsBitmap(imageNumber: Int): android.graphics.Bitmap?
}


