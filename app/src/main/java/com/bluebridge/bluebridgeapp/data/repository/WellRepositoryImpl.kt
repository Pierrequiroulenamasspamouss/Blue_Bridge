package com.bluebridge.bluebridgeapp.data.repository
import android.content.Context
import android.util.Log
import com.bluebridge.bluebridgeapp.data.AppEvent
import com.bluebridge.bluebridgeapp.data.AppEventChannel
import com.bluebridge.bluebridgeapp.data.`interface`.WellRepository
import com.bluebridge.bluebridgeapp.data.local.WellPreferences
import com.bluebridge.bluebridgeapp.data.model.ShortenedWellData
import com.bluebridge.bluebridgeapp.data.model.WellData
import com.bluebridge.bluebridgeapp.data.model.WellsResponse
import com.bluebridge.bluebridgeapp.network.RetrofitBuilder
import com.bluebridge.bluebridgeapp.network.ServerApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.Serializable

class WellRepositoryImpl(
    private val api: ServerApi,
    private val preferences: WellPreferences,
) : WellRepository {

    override val wellListFlow: Flow<List<WellData>> = preferences.wellListFlow

    override suspend fun getSavedWells(): List<WellData> = withContext(Dispatchers.IO) {
        preferences.getAllWells()
    }

    override suspend fun getWells(): List<WellData> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAllWells()
            if (response.isSuccessful && response.body() != null) {
                val wellsResponse = response.body()!!
                return@withContext wellsResponse.data
            } else {
                Log.e("WellRepository", "Error fetching wells: ${response.errorBody()?.string()}")
                return@withContext emptyList()
            }
        } catch (e: Exception) {
            Log.e("WellRepository", "Error fetching wells: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    override suspend fun getWellById(id: Int): WellData? = withContext(Dispatchers.IO) {
        try {
            val allWells = getWells()
            return@withContext allWells.find { it.id == id }
        } catch (e: Exception) {
            Log.e("WellRepository", "Error fetching well by id: ${e.message}", e)
            return@withContext null
        }
    }

    override suspend fun getAllWells(): List<ShortenedWellData> = withContext(Dispatchers.IO) {
        try {
            val response = api.getWellsWithFilters(page = 1, limit = 100)
            if (response.isSuccessful && response.body() != null)  {
                val wellsResponse = response.body()!!
                return@withContext wellsResponse.data.map { well ->
                    ShortenedWellData(
                        wellName = well.wellName,
                        wellLocation = well.wellLocation,
                        wellWaterType = well.wellWaterType,
                        wellStatus = well.wellStatus,
                        wellCapacity = well.wellCapacity,
                        wellWaterLevel = well.wellWaterLevel,
                        espId = well.espId
                    )
                }
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
                return@withContext Result.success(wellsResponse)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                return@withContext Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    override suspend fun saveWell(well: WellData): Boolean = withContext(Dispatchers.IO) {
        // Implement saving logic (local or remote as needed)
        try {
            preferences.saveWell(well)
            return@withContext true
        } catch (e: Exception) {
            Log.e("WellRepository", "Error saving well: ${e.message}", e)
            return@withContext false
        }
    }

    override suspend fun updateWell(well: WellData): Boolean = withContext(Dispatchers.IO) {
        // Implement update logic (local or remote as needed)
        try {
            preferences.updateWell(well)
            return@withContext true
        } catch (e: Exception) {
            Log.e("WellRepository", "Error updating well: ${e.message}", e)
            return@withContext false
        }
    }

    override suspend fun getWell(wellId: Int): WellData? = withContext(Dispatchers.IO) {
        try {
            return@withContext preferences.getWell(wellId)
        } catch (e: Exception) {
            Log.e("WellRepository", "Error getting well: ${e.message}", e)
            return@withContext null
        }
    }

    override suspend fun deleteWell(espId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            preferences.deleteWell(espId)
            return@withContext true
        } catch (e: Exception) {
            Log.e("WellRepository", "Error deleting well: ${e.message}", e)
            return@withContext false
        }
    }

    override suspend fun getFavoriteWells(): Flow<List<ShortenedWellData>> = flow {
        try {
            emit(preferences.getFavoriteWells())
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun addFavoriteWell(well: ShortenedWellData) {
        withContext(Dispatchers.IO) {
            try {
                val currentFavorites = preferences.getFavoriteWells().toMutableList()
                if (!currentFavorites.any { it.espId == well.espId }) {
                    currentFavorites.add(well)
                    preferences.saveFavoriteWells(currentFavorites)
                }
            } catch (_: Exception) {}
        }
    }

    override suspend fun removeFavoriteWell(espId: String) {
        withContext(Dispatchers.IO) {
            try {
                val currentFavorites = preferences.getFavoriteWells().toMutableList()
                currentFavorites.removeIf { it.espId == espId }
                preferences.saveFavoriteWells(currentFavorites)
            } catch (_: Exception) {}
        }
    }

    override suspend fun isWellFavorite(espId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val favorites = preferences.getFavoriteWells()
            return@withContext favorites.any { it.espId == espId }
        } catch (_: Exception) {
            return@withContext false
        }
    }

    override suspend fun getStats(): Flow<List<Serializable>> = flow {
        emit(emptyList()) // Implement as needed //TODO: make this grab the espId/stats
    }

    override suspend fun isEspIdUnique(espId: String): Boolean = withContext(Dispatchers.IO) {
        // Implement uniqueness check as needed
        return@withContext true
    }

    override suspend fun swapWells(from: Int, to: Int) {
        // Implement swap logic as needed
    }

    override suspend fun deleteWellAt(index: Int) {
        // Implement delete at index as needed
    }

    override suspend fun saveWellToServer(wellData: WellData, email: String, token: String): Boolean = withContext(Dispatchers.IO) {
        // Implement server save logic as needed
        return@withContext true
    }

    suspend fun loadWells(
        page: Int,
        pageSize: Int,
        searchQuery: String,
        waterType: String?,
        status: String?,
        minWaterLevel: Int?,
        maxWaterLevel: Int?,
        context: Context,
        onSuccess: (List<WellData>, Boolean) -> Unit
    ) {
        try {
            val serverApi = RetrofitBuilder.getServerApi(context)
            val response = serverApi.getWellsWithFilters(
                page = page,
                limit = pageSize,
                wellName = searchQuery.takeIf { it.isNotBlank() },
                wellStatus = status,
                wellWaterType = waterType,
                minWaterLevel = minWaterLevel,
                maxWaterLevel = maxWaterLevel
            )

            if (response.isSuccessful) {
                val wellsResponse = response.body()
                if (wellsResponse != null) {
                    Log.d("BrowseWellsScreen", "Wells loaded successfully")
                    onSuccess(wellsResponse.data, 10 > page * pageSize)
                } else {
                    AppEventChannel.sendEvent(AppEvent.ShowError("No wells found"))

                }
            } else {
                AppEventChannel.sendEvent(AppEvent.ShowError("Failed to load wells"))

            }
        } catch (e: Exception) {
            Log.e("BrowseWellsScreen", "Error loading wells", e)
            AppEventChannel.sendEvent(AppEvent.ShowError("Error: ${e.message}"))

        }
    }
}
