package com.bluebridgeapp.bluebridge.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluebridgeapp.bluebridge.data.interfaces.WellRepository
import com.bluebridgeapp.bluebridge.data.model.ShortenedWellData
import com.bluebridgeapp.bluebridge.data.model.WellData
import com.bluebridgeapp.bluebridge.events.AppEvent
import com.bluebridgeapp.bluebridge.events.AppEventChannel
import com.bluebridgeapp.bluebridge.events.WellEvents
import com.bluebridgeapp.bluebridge.network.RetrofitBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class ActionState {
    object Idle : ActionState()
    object Loading : ActionState()
    data class Success(val message: String) : ActionState()
    data class Error(val error: String) : ActionState()
}

@RequiresApi(Build.VERSION_CODES.O)
class WellViewModel(
    val repository: WellRepository //TODO: make it as private later
) : ViewModel() {
    private val _currentWellState = mutableStateOf<UiState<WellData>>(UiState.Empty)
    val currentWellState: State<UiState<WellData>> = _currentWellState
    private val _wellsListState = mutableStateOf<UiState<List<WellData>>>(UiState.Loading)
    val wellsListState: State<UiState<List<WellData>>> = _wellsListState
    private val _actionState = mutableStateOf<ActionState>(ActionState.Idle)
    val actionState: State<ActionState> = _actionState

    private fun saveCurrentWell() {
        viewModelScope.launch {
            val currentWell = (_currentWellState.value as? UiState.Success)?.data ?: return@launch
            _currentWellState.value = UiState.Loading
            try {
                if (currentWell.wellOwner?.isBlank() == true) {
                    val defaultOwner = "BlueBridge User"
                    val updatedWell = currentWell.copy(wellOwner = defaultOwner)
                    repository.saveWell(updatedWell)
                    _currentWellState.value = UiState.Success(updatedWell)
                    viewModelScope.launch {
                        saveWellToServer(updatedWell)
                    }
                } else {
                    repository.saveWell(currentWell)
                    _currentWellState.value = UiState.Success(currentWell)
                }
                getSavedWells()
            } catch (e: Exception) {
                _currentWellState.value = UiState.Error(e.message ?: "Failed to save well")
            }
        }
    }
    private fun updateCurrentWell(transform: WellData.() -> WellData) {
        val currentWell = (_currentWellState.value as? UiState.Success)?.data ?: return
        val updatedWell = currentWell.transform()
        _currentWellState.value = UiState.Success(updatedWell)
        viewModelScope.launch {
            saveWellToServer(updatedWell)
        }
    }
    fun handleEvent(event: WellEvents) {
        when (event) {
            is WellEvents.SaveWell -> saveCurrentWell()
            is WellEvents.WellNameEntered -> updateCurrentWell { copy(wellName = event.wellName) }
            is WellEvents.OwnerEntered -> updateCurrentWell { copy(wellOwner = event.wellOwner) }
            is WellEvents.WellLocationEntered -> updateCurrentWell { copy(wellLocation = event.wellLocation) }
            is WellEvents.WaterTypeEntered -> updateCurrentWell { copy(wellWaterType = event.wellWaterType) }
            is WellEvents.WellCapacityEntered -> updateCurrentWell { copy(wellCapacity = event.wellCapacity) }
            is WellEvents.WaterLevelEntered -> updateCurrentWell { copy(wellWaterLevel = event.wellWaterLevel) }
            is WellEvents.ConsumptionEntered -> updateCurrentWell { copy(wellWaterConsumption = event.wellWaterConsumption) }
            is WellEvents.WellIdEntered -> updateCurrentWell { copy(wellId = event.wellId) }
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun loadWell(wellId: String?) {
        viewModelScope.launch {
            _currentWellState.value = UiState.Loading
            try {
                val well = repository.getWellById(wellId)
                if (well != null) {
                    val currentTime = (System.currentTimeMillis() / 1000)
                    val updatedWell = well.copy(lastRefreshTime = currentTime)
                    _currentWellState.value = UiState.Success(updatedWell)
                    val currentWells = (_wellsListState.value as? UiState.Success)?.data ?: emptyList()
                    val updatedWells = currentWells.map {
                        if (it.wellId == wellId) updatedWell else it
                    }
                    _wellsListState.value = UiState.Success(updatedWells)
                    _actionState.value = ActionState.Success("Well loaded successfully")
                } else {
                    _currentWellState.value = UiState.Error("Well not found")
                    _actionState.value = ActionState.Error("Well not found")
                }
            } catch (e: Exception) {
                _currentWellState.value = UiState.Error(e.message ?: "Failed to load well")
                _actionState.value = ActionState.Error(e.message ?: "Failed to load well")
            }
        }
    }
    suspend fun getWellFromServer(wellId: String): WellData? { // Does the same thing as loadWell,I believe, but in a different way.TODO("Unify them")
        return repository.getWellFromServer(wellId)
    }


    fun deleteWell(espId: String) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            try {
                val success = repository.deleteWell(espId)
                if (success) {
                    _actionState.value = ActionState.Success("Well deleted successfully")
                    getSavedWells()
                } else {
                    _actionState.value = ActionState.Error("Failed to delete well")
                }
            } catch (e: Exception) {
                _actionState.value = ActionState.Error("Error: ${e.message}")
            }
        }
    }
    fun swapWells(from: String, to: String) {
        viewModelScope.launch {
            try {
                repository.swapWells(from, to)
                getSavedWells()
            } catch (e: Exception) {
                _wellsListState.value = UiState.Error(e.message ?: "Failed to swap wells")
            }
        }
    }
    suspend fun saveWellToServer(wellData: WellData): Boolean {
        return try {
            // You should get user data from a UserRepository if needed, not from preferences directly
            // For now, just pass empty email/token or refactor as needed
            repository.saveWellToServer(wellData, "", "")
        } catch (e: Exception) {
            false
        }
    }
    suspend fun deleteWellFromServer(espId: String): Boolean {
        return try {
            repository.deleteWellFromServer(espId, "", "")
        } catch (e: Exception) {
            false
        }
    }
    fun getSavedWells() {
        viewModelScope.launch {
            _wellsListState.value = UiState.Loading
            try {
                val savedWells = repository.getSavedWells()
                _wellsListState.value = UiState.Success(savedWells)
                _actionState.value = ActionState.Success("Saved wells loaded successfully")
            } catch (e: Exception) {
                _wellsListState.value = UiState.Error(e.message ?: "Failed to load saved wells")
                _actionState.value = ActionState.Error(e.message ?: "Failed to load saved wells")
            }
        }
    }
    suspend fun isWellIdUnique(wellId: String)= repository.isEspIdUnique(wellId)
    suspend fun getAllImages(wellId: String): List<Bitmap> = withContext(Dispatchers.IO) {
        (0 until 10).mapNotNull { imageNumber ->
            try {
                repository.getWellImageAsBitmap(wellId, imageNumber)
            } catch (e: Exception) {
                Log.e("WellViewModel", "Error loading image $imageNumber for well $wellId: ${e.message}")
                null
            }
        }
    }
    suspend fun loadWells(
        page: Int,
        pageSize: Int,
        searchQuery: String,
        waterType: String?,
        status: String?,
        minWaterLevel: Int?,
        maxWaterLevel: Int?,
        context: Context
    ): List<ShortenedWellData> {
        return try {
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

            if (response.isSuccessful && response.body()?.data != null) {
                val wellsResponse = response.body()!!
                wellsResponse.data.map { it.toShortenedWell(it) }
            } else {
                AppEventChannel.sendEvent(AppEvent.ShowError("Failed to load wells"))
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("BrowseWellsScreen", "Error loading wells", e)
            AppEventChannel.sendEvent(AppEvent.ShowError("Error: ${e.message}"))
            emptyList()
        }
    }


    suspend fun getSingleWellImage(wellId: String, imageNumber: Int): Bitmap? {
        return repository.getWellImageAsBitmap(wellId, imageNumber)
    }
    suspend fun deleteWellImage(wellId: String, imageNumber: Int) {repository.deleteWellImage(wellId, imageNumber)}
    suspend fun uploadWellPicture(wellId: String, imageNumber: Int, bitmap: Bitmap) {repository.uploadWellPicture(wellId, imageNumber, bitmap)}


}