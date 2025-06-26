package com.bluebridgeapp.bluebridge.viewmodels


import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluebridgeapp.bluebridge.data.interfaces.WellRepository
import com.bluebridgeapp.bluebridge.data.local.UserPreferences
import com.bluebridgeapp.bluebridge.data.model.WellData
import com.bluebridgeapp.bluebridge.events.WellEvents
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Action state for tracking mutations like delete/update
sealed class ActionState {
    object Idle : ActionState()
    object Loading : ActionState()
    data class Success(val message: String) : ActionState()
    data class Error(val error: String) : ActionState()
}

@RequiresApi(Build.VERSION_CODES.O)
class WellViewModel(
    val repository: WellRepository,
    private val context: Context,
    ) : ViewModel() {
    private val userPreferences = UserPreferences(context)

        // Current well state
    private val _currentWellState = mutableStateOf<UiState<WellData>>(UiState.Empty)
    val currentWellState: State<UiState<WellData>> = _currentWellState

    // List of wells state
    private val _wellsListState = mutableStateOf<UiState<List<WellData>>>(UiState.Loading)
    val wellsListState: State<UiState<List<WellData>>> = _wellsListState

    // Action state
    private val _actionState = mutableStateOf<ActionState>(ActionState.Idle)
    val actionState: State<ActionState> = _actionState



    private fun saveCurrentWell() {
        //This function should after saving locally the well, send it to the server for it to add it to the database
        viewModelScope.launch {
            val currentWell = (_currentWellState.value as? UiState.Success)?.data ?: return@launch
            _currentWellState.value = UiState.Loading
            try {
                // Ensure the well has a valid owner even if user is not a well owner
                if (currentWell.wellOwner.isBlank()) {
                    // Set a default owner if none specified - this prevents server-side validation errors
                    val defaultOwner = "BlueBridge User"
                    val updatedWell = currentWell.copy(wellOwner = defaultOwner)
                    repository.saveWell(updatedWell)
                    _currentWellState.value = UiState.Success(updatedWell)

                    //Save the well to the server
                    viewModelScope.launch {
                        saveWellToServer(currentWell)
                    }

                } else {
                    repository.saveWell(currentWell)
                    _currentWellState.value = UiState.Success(currentWell)
                }
                getSavedWells() // Refresh the list
            } catch (e: Exception) {
                _currentWellState.value = UiState.Error(e.message ?: "Failed to save well")
            }
        }
    }
    private fun updateCurrentWell(transform: WellData.() -> WellData) {
        val currentWell = (_currentWellState.value as? UiState.Success)?.data ?: return
        val updatedWell = currentWell.transform()
        _currentWellState.value = UiState.Success(updatedWell)
        //Save the well to the server
        viewModelScope.launch {
            saveWellToServer(currentWell)
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
            is WellEvents.EspIdEntered -> updateCurrentWell { copy(espId = event.espId) }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadWell(id: Int) {
        viewModelScope.launch {
            _currentWellState.value = UiState.Loading
            try {
                val well = repository.getWellById(id)
                if (well != null) {
                    val currentTime = (System.currentTimeMillis() / 1000)
                    val updatedWell = well.copy(lastRefreshTime = currentTime)
                    _currentWellState.value = UiState.Success(updatedWell)

                    // Update the well in the list if it exists
                    val currentWells = (_wellsListState.value as? UiState.Success)?.data ?: emptyList()
                    val updatedWells = currentWells.map {
                        if (it.id == id) updatedWell else it
                    }
                    _wellsListState.value = UiState.Success(updatedWells)

                    _actionState.value = ActionState.Success("Well loaded successfully")
                    Log.d("WellViewModel", "Successfully loaded well with ID: $id")
                } else {
                    _currentWellState.value = UiState.Error("Well not found")
                    _actionState.value = ActionState.Error("Well not found")
                    Log.e("WellViewModel", "Well not found with ID: $id")
                }
            } catch (e: Exception) {
                _currentWellState.value = UiState.Error(e.message ?: "Failed to load well")
                _actionState.value = ActionState.Error(e.message ?: "Failed to load well")
                Log.e("WellViewModel", "Error loading well with ID: $id", e)
            }
        }
    }
    fun deleteWell(espId: String) {
        // This function should delete the well locally.
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            try {
                val success = repository.deleteWell(espId)
                if (success) {
                    _actionState.value = ActionState.Success("Well deleted successfully")
                    // Refresh the wells list
                    getSavedWells()
                } else {
                    _actionState.value = ActionState.Error("Failed to delete well")
                }
            } catch (e: Exception) {
                _actionState.value = ActionState.Error("Error: ${e.message}")
            }
        }
    }

    fun swapWells(from: Int, to: Int) {
        viewModelScope.launch {
            try {
                repository.swapWells(from, to)
                getSavedWells() // Refresh the list
            } catch (e: Exception) {
                _wellsListState.value = UiState.Error(e.message ?: "Failed to swap wells")
            }
        }
    }

    suspend fun saveWellToServer(wellData: WellData): Boolean {
        return try {
            // Get user data
            val userData = userPreferences.getUserData().first()
            repository.saveWellToServer(
                wellData,
                userData?.email ?: "",
                userData?.loginToken ?: ""
            )
        } catch (e: Exception) {
            // Log error and return false to indicate failure
            android.util.Log.e("WellViewModel", "Error saving well to server: ${e.message}", e)
            false
        }
    }

    suspend fun deleteWellFromServer(espId: String): Boolean {
        return try {
            // Get user data
            val userData = userPreferences.getUserData().first()
            repository.deleteWellFromServer(
                espId,
                userData?.email ?: "",
                userData?.loginToken ?: ""
            )
        } catch (e: Exception) {
            // Log error and return false to indicate failure
            android.util.Log.e("WellViewModel", "Error deleting well from server: ${e.message}", e)
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
                _wellsListState.value =
                    UiState.Error(e.message ?: "Failed to load saved wells")
                _actionState.value = ActionState.Error(e.message ?: "Failed to load saved wells")
            }
        }
    }

}