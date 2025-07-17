package com.bluebridgeapp.bluebridge.data.repository
import android.annotation.SuppressLint
import android.media.Image
import com.bluebridgeapp.bluebridge.data.interfaces.WellRepository
import com.bluebridgeapp.bluebridge.data.local.WellPreferences
import com.bluebridgeapp.bluebridge.data.model.ShortenedWellData
import com.bluebridgeapp.bluebridge.data.model.WellData
import com.bluebridgeapp.bluebridge.data.model.WellStatsResponse
import com.bluebridgeapp.bluebridge.data.model.WellsResponse
import com.bluebridgeapp.bluebridge.network.ServerApi
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import kotlinx.coroutines.withContext

class WellRepositoryImpl(
    private val api: ServerApi,
    private val preferences: WellPreferences,
) : WellRepository {
    override val wellListFlow: Flow<List<WellData>> = preferences.wellListFlow

    override suspend fun getSavedWells(): List<WellData> = withContext(Dispatchers.IO) {
        preferences.getAllWells()
    }

    override suspend fun getWellById(id: Int): WellData? = withContext(Dispatchers.IO) {
        try {
            preferences.getWellById(id)
        } catch (e: Exception) {
            Log.e("WellRepository", "Error fetching well by id from preferences: ${e.message}", e)
            null
        }
    }

    override suspend fun getAllWells(): List<ShortenedWellData> = withContext(Dispatchers.IO) {
        try {
            val response = api.getWellsWithFilters(page = 1, limit = 100)
            if (response.isSuccessful && response.body() != null)  {
                val wellsResponse = response.body()!!
                wellsResponse.data.map { well ->
                    ShortenedWellData(
                        wellName = well.wellName,
                        wellLocation = well.wellLocation,
                        wellWaterType = well.wellWaterType,
                        wellStatus = well.wellStatus,
                        wellCapacity = well.wellCapacity,
                        wellWaterLevel = well.wellWaterLevel,
                        wellId = well.wellId
                    )
                }
            } else {
                Log.e("WellRepository", "Error fetching wells: ${response.errorBody()?.string()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("WellRepository", "Error fetching wells: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun getFilteredWells(
        page: Int,
        limit: Int,
        wellName: String?,
        wellStatus: String?,
        wellWaterType: String?,
        wellOwner: String?,
        espId: String?,
        minWaterLevel: Int?,
        maxWaterLevel: Int?
    ): Result<WellsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getWellsWithFilters(
                page = page,
                limit = limit,
                wellName = wellName,
                wellStatus = wellStatus,
                wellWaterType = wellWaterType,
                wellOwner = wellOwner,
                espId = espId,
                minWaterLevel = minWaterLevel,
                maxWaterLevel = maxWaterLevel
            )
            if (response.isSuccessful && response.body() != null) {
                val wellsResponse = response.body()!!
                Result.success(wellsResponse)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveWell(well: WellData): Boolean = withContext(Dispatchers.IO) {
        try {
            preferences.saveWell(well, api)
            true
        } catch (e: Exception) {
            Log.e("WellRepository", "Error saving well: ${e.message}", e)
            false
        }
    }

    override suspend fun deleteWell(espId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            preferences.deleteWell(espId)
            true
        } catch (e: Exception) {
            Log.e("WellRepository", "Error deleting well: ${e.message}", e)
            false
        }
    }

    override suspend fun getStats(espId: String): WellStatsResponse? = withContext(Dispatchers.IO) {
        try {
            val response = api.getWellStats(espId)
            if (response.isSuccessful) {
                response.body()
            } else {
                Log.e("WellRepository", "Error fetching well stats: ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e("WellRepository", "Network error fetching well stats: ${e.message}", e)
            null
        }
    }

    override suspend fun isEspIdUnique(espId: String): Boolean = withContext(Dispatchers.IO) {
        true // TODO: Implement this
    }

    override suspend fun swapWells(from: String, to: String) = withContext(Dispatchers.IO) {
        val wells = preferences.getAllWells().toMutableList()
        val fromWellIndex = wells.indexOfFirst { it.wellId == from }
        val toWellIndex = wells.indexOfFirst { it.wellId == to}
        if (fromWellIndex != -1 && toWellIndex != -1) {
            val fromWell = wells[fromWellIndex]
            val toWell = wells[toWellIndex]
            val tempWellId = fromWell.wellId
            fromWell.wellId = toWell.wellId
            toWell.wellId = tempWellId
            preferences.saveWell(fromWell, api)
            preferences.saveWell(toWell, api)
        }
    }

    override suspend fun saveWellToServer(wellData: WellData, email: String, token: String): Boolean = withContext(Dispatchers.IO) {
        try {
            api.editWell(wellData, email, token)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteWellFromServer(
        espId: String,
        email: String,
        token: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            api.deleteWell(espId, email, token)
            true
        } catch (e: Exception) {
            false
        }
    }
    override suspend fun getAllImages(wellId: String): List<android.graphics.Bitmap> = withContext(Dispatchers.IO) {
        try {
            val wellData = api.getWellDataById(wellId)
            val imagesList = mutableListOf<android.graphics.Bitmap>()
            
            wellData.images?.forEach { imageData ->
                val bitmap = preferences.loadWellImageAsBitmap(imageData.imageNumber)
                if (bitmap != null) {
                    imagesList.add(bitmap)
                } else {
                    // Try to download from server if not cached
                    try {
                        val response = api.getWellImage(wellId, imageData.imageNumber)
                        if (response.isSuccessful) {
                            response.body()?.let { responseBody ->
                                val imagePath = com.bluebridgeapp.bluebridge.utils.ImageUtils.downloadAndSaveImage(
                                    preferences.context, 
                                    imageData.imageNumber, 
                                    responseBody
                                )
                                if (imagePath != null) {
                                    val downloadedBitmap = preferences.loadWellImageAsBitmap(imageData.imageNumber)
                                    downloadedBitmap?.let { imagesList.add(it) }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("WellRepository", "Error downloading image ${imageData.imageNumber}: ${e.message}", e)
                    }
                }
            }
            
            imagesList
        } catch (e: Exception) {
            Log.e("WellRepository", "Error fetching images for wellId $wellId: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun uploadWellPicture(wellId: String, imageNumber: Int, image: android.graphics.Bitmap): Boolean = withContext(Dispatchers.IO) {
        try {
            // Convert bitmap to file for upload
            val imageFile = java.io.File(preferences.context.cacheDir, "upload_image.jpg")
            val outputStream = java.io.FileOutputStream(imageFile) // Use context.cacheDir for temp files
            image.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.close()
            
            val requestBody = okhttp3.RequestBody.create(
                "image/jpeg".toMediaTypeOrNull(),
                imageFile
            )
            val multipartBody = okhttp3.MultipartBody.Part.createFormData("image", "image.jpg", requestBody)
            
            val response = api.uploadWellPicture(wellId = wellId, imageNumber = imageNumber, image = multipartBody)
            imageFile.delete()
            
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("WellRepository", "Error uploading image for wellId $wellId and imageNumber $imageNumber: ${e.message}", e)
            false
        }
    }

    override suspend fun getWellFromServer(wellId: String): WellData? = withContext(Dispatchers.IO) {
        try {
            val well = api.getWellDataById(wellId)
            Log.i("WellRepository", "Successfully fetched well $wellId from server.")
            well
        } catch (e: Exception) {
            Log.e("WellRepository", "Error fetching well $wellId from server: ${e.message}", e)
            null
        }
    }
    
    override suspend fun getWellImageAsComposeBitmap(imageNumber: Int): androidx.compose.ui.graphics.ImageBitmap? {
        return preferences.loadWellImageAsComposeBitmap(imageNumber)
    }
    
    override suspend fun getWellImageAsBitmap(imageNumber: Int): android.graphics.Bitmap? {
        return preferences.loadWellImageAsBitmap(imageNumber)
    }

}