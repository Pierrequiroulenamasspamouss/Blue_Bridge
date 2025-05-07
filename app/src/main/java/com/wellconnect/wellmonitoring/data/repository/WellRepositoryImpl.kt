package com.wellconnect.wellmonitoring.data.repository

import PaginatedWellsResponse
import ShortenedWellData
import WellData
import android.util.Log
import com.wellconnect.wellmonitoring.data.`interface`.WellRepository
import com.wellconnect.wellmonitoring.data.local.WellPreferences
import com.wellconnect.wellmonitoring.network.ServerApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.Serializable

class WellRepositoryImpl(
    private val api: ServerApi,
    private val preferences: WellPreferences,
) : WellRepository {

    override suspend fun getAllWells(): List<ShortenedWellData> = withContext(Dispatchers.IO) {
        try {
            Log.d("WellRepository", "Fetching all wells from server")
            // Using the paginated API to get all wells (first page with default limit)
            val response = api.getWellsWithFilters(page = 1, limit = 100)
            if (response.isSuccessful && response.body() != null) {
                val wellsResponse = response.body()!!
                Log.d("WellRepository", "Fetched ${wellsResponse.processedWells.size} wells, total: ${wellsResponse.processedTotalWells}")
                return@withContext wellsResponse.processedWells
            } else {
                Log.e("WellRepository", "Error fetching wells: ${response.errorBody()?.string()}")
                return@withContext emptyList()
            }
        } catch (e: Exception) {
            Log.e("WellRepository", "Error fetching wells: ${e.message}", e)
            return@withContext emptyList()
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
    ): Result<PaginatedWellsResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("WellRepository", "Fetching filtered wells with pagination (page $page, limit $limit)")
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
                Log.d("WellRepository", "Fetched ${wellsResponse.processedWells.size} wells, total: ${wellsResponse.processedTotalWells}")
                return@withContext Result.success(wellsResponse)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e("WellRepository", "Error fetching filtered wells: $errorMsg")
                return@withContext Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("WellRepository", "Error fetching filtered wells: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }

    override suspend fun addFavoriteWell(well: ShortenedWellData) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("WellRepository", "Adding well ${well.espId} to favorites")
                val currentFavorites = preferences.getFavoriteWells().toMutableList()
                if (!currentFavorites.any { it.espId == well.espId }) {
                    currentFavorites.add(well)
                    preferences.saveFavoriteWells(currentFavorites)
                    Log.d("WellRepository", "Well added to favorites successfully")
                } else {
                    Log.d("WellRepository", "Well ${well.espId} already in favorites")
                }
            } catch (e: Exception) {
                Log.e("WellRepository", "Error adding well to favorites: ${e.message}", e)
            }
        }
    }

    override suspend fun removeFavoriteWell(espId: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("WellRepository", "Removing well $espId from favorites")
                val currentFavorites = preferences.getFavoriteWells().toMutableList()
                val wellRemoved = currentFavorites.removeIf { it.espId == espId }
                if (wellRemoved) {
                    preferences.saveFavoriteWells(currentFavorites)
                    Log.d("WellRepository", "Well removed from favorites successfully")
                } else {
                    Log.d("WellRepository", "Well $espId not found in favorites")
                }
            } catch (e: Exception) {
                Log.e("WellRepository", "Error removing well from favorites: ${e.message}", e)
            }
        }
    }
    
    override suspend fun getFavoriteWells(): Flow<List<ShortenedWellData>> = flow {
        try {
            Log.d("WellRepository", "Getting favorite wells")
            emit(preferences.getFavoriteWells())
        } catch (e: Exception) {
            Log.e("WellRepository", "Error getting favorite wells: ${e.message}", e)
            emit(emptyList())
        }
    }

    override suspend fun isWellFavorite(espId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("WellRepository", "Checking if well $espId is favorite")
            val favorites = preferences.getFavoriteWells()
            return@withContext favorites.any { it.espId == espId }
        } catch (e: Exception) {
            Log.e("WellRepository", "Error checking favorite well: ${e.message}", e)
            return@withContext false
        }
    }

    override suspend fun deleteWell(espId: String): Boolean {
        return try {
            Log.d("WellRepository", "Deleting well with ESP ID $espId")
            
            // Get user context
            val context = preferences.context
            
            // Get credentials from user preferences
            val userPrefs = context.getSharedPreferences("user_preferences", android.content.Context.MODE_PRIVATE)
            val email = userPrefs.getString("email", "")
            val token = userPrefs.getString("login_token", "")
            
            if (email.isNullOrBlank() || token.isNullOrBlank()) {
                Log.e("WellRepository", "Missing credentials for deleting well")
                return false
            }
            
            // Delete the well on the server
            val response = api.deleteWell(espId, email, token)
            
            if (response.isSuccessful) {
                // Also remove from local storage by getting current list and filtering it
                val currentWells = getWells().toMutableList()
                val updatedWells = currentWells.filter { well -> well.espId != espId }
                preferences.saveWellList(updatedWells)
                
                Log.d("WellRepository", "Successfully deleted well with ESP ID $espId")
                true
            } else {
                Log.e("WellRepository", "Server error deleting well: ${response.errorBody()?.string()}")
                false
            }
        } catch (e: Exception) {
            Log.e("WellRepository", "Error deleting well: ${e.message}", e)
            false
        }
    }

    override suspend fun getStats(): Flow<List<Serializable>> = flow {
        try {
            Log.d("WellRepository", "Fetching well statistics")
            val response = api.getWellsStats()
            if (response.isSuccessful && response.body() != null) {
                val statsData = response.body()!!.stats
                // Convert to a list of Serializable objects
                val serializableStats = listOf(
                    statsData.totalWells,
                    statsData.totalCapacity,
                    statsData.totalWaterLevel,
                    statsData.avgConsumption
                ) as List<Serializable>
                emit(serializableStats)
            } else {
                Log.e("WellRepository", "Error fetching statistics: ${response.errorBody()?.string()}")
                emit(emptyList<Serializable>())
            }
        } catch (e: Exception) {
            Log.e("WellRepository", "Error fetching well statistics: ${e.message}", e)
            emit(emptyList<Serializable>())
        }
    }

    override suspend fun isEspIdUnique(espId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("WellRepository", "Checking if ESP ID $espId is unique")
            val wells = getWells()
            val isUnique = wells.none { it.espId == espId }
            Log.d("WellRepository", "ESP ID $espId is unique: $isUnique")
            return@withContext isUnique
        } catch (e: Exception) {
            Log.e("WellRepository", "Error checking ESP ID uniqueness: ${e.message}", e)
            return@withContext false
        }
    }

    override suspend fun saveWell(well: WellData): Boolean {
        try {
            Log.d("WellRepository", "Saving well ${well.espId}")
            preferences.saveWell(well)
            Log.d("WellRepository", "Well saved successfully")
            return true
        } catch (e: Exception) {
            Log.e("WellRepository", "Error saving well: ${e.message}", e)
            return false
        }
    }

    override suspend fun saveWellList(wells: List<WellData>) {
        try {
            Log.d("WellRepository", "Saving well list with ${wells.size} wells")
            preferences.saveWellList(wells)
            Log.d("WellRepository", "Well list saved successfully")
        } catch (e: Exception) {
            Log.e("WellRepository", "Error saving well list: ${e.message}", e)
        }
    }

    override suspend fun getWell(wellId: Int): WellData? {
        return try {
            Log.d("WellRepository", "Getting well with ID $wellId")
            preferences.getWell(wellId)
        } catch (e: Exception) {
            Log.e("WellRepository", "Error getting well: ${e.message}", e)
            null
        }
    }

    override suspend fun deleteWellAt(index: Int) {
        try {
            Log.d("WellRepository", "Deleting well at index $index")
            preferences.deleteWellAt(index)
            Log.d("WellRepository", "Well at index $index deleted successfully")
        } catch (e: Exception) {
            Log.e("WellRepository", "Error deleting well at index: ${e.message}", e)
        }
    }

    override suspend fun swapWells(from: Int, to: Int) {
        try {
            Log.d("WellRepository", "Swapping wells from index $from to $to")
            preferences.swapWells(from, to)
            Log.d("WellRepository", "Wells swapped successfully")
        } catch (e: Exception) {
            Log.e("WellRepository", "Error swapping wells: ${e.message}", e)
        }
    }

    override suspend fun getWellById(espId: String): WellData = withContext(Dispatchers.IO) {
        try {
            Log.d("WellRepository", "Fetching well data for ESP ID $espId")
            val response = api.getWellDataById(espId)
            Log.d("WellRepository", "Successfully fetched well with ESP ID $espId")
            return@withContext response
        } catch (e: Exception) {
            Log.e("WellRepository", "Error fetching well by ID: ${e.message}", e)
            // Return empty WellData object instead of throwing an exception
            return@withContext WellData(
                id = -1,
                espId = espId,
                wellName = "New Well",
                wellStatus = "Inactive"
            )
        }
    }

    override suspend fun getWells(): List<WellData> {
        return try {
            Log.d("WellRepository", "Getting all local wells")
            val wellsFlow = preferences.wellListFlow
            val wells = wellsFlow.first()
            Log.d("WellRepository", "Retrieved ${wells.size} wells")
            wells
        } catch (e: Exception) {
            Log.e("WellRepository", "Error getting wells: ${e.message}", e)
            emptyList()
        }
    }

    override val wellListFlow: Flow<List<WellData>> = preferences.wellListFlow

    override suspend fun saveWellToServer(wellData: WellData, email: String, token: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("WellRepository", "Attempting to save well to server: ${wellData.espId}")
            val response = api.createWell(wellData, email, token)
            
            if (response.isSuccessful) {
                Log.d("WellRepository", "Well saved to server successfully")
                return@withContext true
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                Log.e("WellRepository", "Error saving well to server: $errorMessage")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e("WellRepository", "Exception saving well to server: ${e.message}", e)
            return@withContext false
        }
    }
}
