package com.bluebridgeapp.bluebridge.data.repository
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.bluebridgeapp.bluebridge.data.interfaces.WellRepository
import com.bluebridgeapp.bluebridge.data.local.WellPreferences
import com.bluebridgeapp.bluebridge.data.model.ShortenedWellData
import com.bluebridgeapp.bluebridge.data.model.WellData
import com.bluebridgeapp.bluebridge.data.model.WellStatsResponse
import com.bluebridgeapp.bluebridge.data.model.WellsResponse
import com.bluebridgeapp.bluebridge.network.ServerApi
import android.util.Log
import com.bluebridgeapp.bluebridge.data.model.ImageData
import com.bluebridgeapp.bluebridge.data.model.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import kotlinx.serialization.json.Json
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlin.io.encoding.ExperimentalEncodingApi
import android.util.Base64 // Import Android's Base64
import okhttp3.RequestBody.Companion.asRequestBody

class WellRepositoryImpl(
    private val api: ServerApi,
    private val preferences: WellPreferences,
) : WellRepository {
    override val wellListFlow: Flow<List<WellData>> = preferences.wellListFlow

    override suspend fun getSavedWells(): List<WellData> = withContext(Dispatchers.IO) {
        preferences.getAllWells()
    }

    override suspend fun getWellById(id: String?): WellData? = withContext(Dispatchers.IO) {
        if (id == null) return@withContext null

        try {
            // First try to get from local preferences
            val localWell = preferences.getWellById(id)
            if (localWell != null) {
                return@withContext localWell
            }

            // If not found locally, try to fetch from server
            val serverWell = getWellFromServer(id)
            if (serverWell != null) {
                // Optionally save the server-fetched well to local storage for future use
                preferences.saveWell(serverWell, api)
                return@withContext serverWell
            }

            // If not found either locally or on server
            null
        } catch (e: Exception) {
            Log.e("WellRepository", "Error fetching well by id '$id': ${e.message}", e)
            null
        }
    }

    override suspend fun getAllWells(): List<ShortenedWellData> = withContext(Dispatchers.IO) {
        try {
            val response = api.getWellsWithFilters(page = 1, limit = 100)
            if (response.isSuccessful && response.body() != null) {
                val wellsResponse = response.body()!!
                wellsResponse.data.map { well ->
                    // Safely parse wellLocation
                    val wellLocation = try {
                        Json.decodeFromString<Location>(well.wellLocation.toString())
                    } catch (e: Exception) {
                        Log.e("WellRepository", "Error parsing wellLocation for wellId ${well.wellId}: ${e.message}")

                    }

                    ShortenedWellData(
                        wellName = well.wellName.toString(),
                        wellLocation = wellLocation as Location, // Re-serialize if needed, or use the object directly
                        wellWaterType = well.wellWaterType.toString(),
                        wellStatus = well.wellStatus.toString(),
                        wellCapacity = well.wellCapacity.toString(),
                        wellWaterLevel = well.wellWaterLevel.toString(),
                        wellId = well.wellId.toString()
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
                // Process the data if needed (handle potential errors)
                val processedData = wellsResponse.data.map { well ->
                    try {
                        Json.decodeFromString<Location>(well.wellLocation.toString())
                        well
                    } catch (e: Exception) {
                        Log.e("WellRepository", "Error parsing wellLocation in getFilteredWells for wellId ${well.wellId}: ${e.message}")
                        well
                    }
                }
                Log.d("WellRepository", "Successfully fetched filtered wells from server. Procesed data : $processedData")
                Result.success(wellsResponse.copy(data = processedData))
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
        val wells = preferences.getAllWells()
        wells.none { it.wellId == espId }
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
            Log.e("WellRepository", "Error saving well to server: ${e.message}", e)
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
            Log.e("WellRepository", "Error deleting well from server: ${e.message}", e)
            false
        }
    }

    override suspend fun uploadWellPicture(wellId: String, imageNumber: Int, image: android.graphics.Bitmap): Boolean = withContext(Dispatchers.IO) {
        try {
            // Convert bitmap to file for upload
            val imageFile = java.io.File(preferences.context.cacheDir, "upload_image.jpg")
            val outputStream = java.io.FileOutputStream(imageFile) // Use context.cacheDir for temp files
            image.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.close()
            
            val requestBody = imageFile
                .asRequestBody("image/jpeg".toMediaTypeOrNull())
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
            // Get basic well details
            val detailsResponse = api.getWellDetails(wellId)
            val wellData = WellData.fromDetailsResponse(detailsResponse)

            // Fetch images if available
            val imageCount = detailsResponse.data.imageCount
            if (imageCount > 0) {
                val images = (0 until imageCount).mapNotNull { imageNumber ->
                    try {
                        val imageResponse = api.getWellImage(wellId, imageNumber)
                        imageResponse.body()?.data
                    } catch (e: Exception) {
                        Log.e("WellRepository", "Error fetching image $imageNumber for well $wellId: ${e.message}")
                        null
                    }
                }
                wellData.copy(images = images)
            } else {
                wellData
            }
        } catch (e: Exception) {
            Log.e("WellRepository", "Error fetching well $wellId from server: ${e.message}", e)
            null
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun getWellImageAsBitmap(wellId: String, imageNumber: Int): Bitmap? = withContext(Dispatchers.IO) {

        // First try to load from local cache
        try {
            Log.d("WellRepositoryImpl", "Trying to load image from local cache for well $wellId, image $imageNumber")
            val localBitmap = preferences.loadWellImageAsBitmap(wellId, imageNumber)
            if (localBitmap != null) {
                Log.d("WellRepositoryImpl", "Successfully loaded image from local cache")
                return@withContext localBitmap
            }
        } catch (e: Exception) {
            Log.d("WellRepositoryImpl", "Local cache fetch failed for well $wellId, image $imageNumber. Error: ${e.message}")
            // Continue to server fetch
        }

        // If local fetch failed or returned null, try fetching from server
        try {
            Log.d("WellRepositoryImpl", "Fetching image from server for well $wellId, image $imageNumber")
            val response = api.getWellImage(wellId, imageNumber)

            if (response.isSuccessful && response.body() != null) {
                val imageData = response.body()!!.data
                val base64 = imageData.base64encodedImage
                Log.d("WellRepositoryImpl", "Fetched image meta from server: $imageData")

                val bytes = Base64.decode(base64, Base64.DEFAULT) // Use Android's Base64
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                if (bitmap != null) {
                    // Optionally save the fetched image to local cache for future use
                    try {
                        val well = preferences.getWellById(wellId)
                        if (well != null) {
                            val updatedImages = well.images?.toMutableList() ?: mutableListOf()
                            // Ensure we have space for this image number
                            while (updatedImages.size <= imageNumber) {
                                updatedImages.add(ImageData("", "", 0, ""))
                            }
                            updatedImages[imageNumber] = imageData
                            preferences.saveWell(well.copy(images = updatedImages))
                        }
                    } catch (e: Exception) {
                        Log.e("WellRepositoryImpl", "Failed to save image to local cache", e)
                    }
                }

                return@withContext bitmap
            } else {
                Log.e("WellRepositoryImpl", "Error fetching image from server: ${response.errorBody()?.string()}")
                return@withContext null
            }
        } catch (serverException: Exception) {
            Log.e("WellRepositoryImpl", "Error fetching image from server for well $wellId, image $imageNumber: ${serverException.message}", serverException)
            return@withContext null
        }
    }

    override suspend fun getAllImages(wellId: String): List<Bitmap> = withContext(Dispatchers.IO) {
        (0 until 10).mapNotNull { imageNumber -> // Assuming a maximum of 10 images, adjust as needed
            async { getWellImageAsBitmap(wellId, imageNumber) }
        }.mapNotNull { it.await() }
    }

    override suspend fun deleteWellImage(wellId: String, imageNumber: Int) {
        // This deletes the image from the server. You cannot remove an image only locally ( you shouldn't be able to anyways) .
        Log.d("WellRepositoryImpl", "Deleting image for well $wellId, image $imageNumber from server")
        api.deleteWellImage(wellId, imageNumber)
    }

}