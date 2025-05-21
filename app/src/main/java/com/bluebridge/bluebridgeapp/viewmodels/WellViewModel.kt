package com.bluebridge.bluebridgeapp.viewmodels

import WellData
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluebridge.bluebridgeapp.data.WellEvents
import com.bluebridge.bluebridgeapp.data.`interface`.WellRepository
import kotlinx.coroutines.launch

// Action state for tracking mutations like delete/update
sealed class ActionState {
    object Idle : ActionState()
    object Loading : ActionState()
    data class Success(val message: String) : ActionState()
    data class Error(val error: String) : ActionState()
}

class WellViewModel(val repository: WellRepository) : ViewModel() {
    // Current well state
    private val _currentWellState = mutableStateOf<UiState<WellData>>(UiState.Empty)
    val currentWellState: State<UiState<WellData>> = _currentWellState

    // List of wells state
    private val _wellsListState = mutableStateOf<UiState<List<WellData>>>(UiState.Loading)
    val wellsListState: State<UiState<List<WellData>>> = _wellsListState

    // Action state
    private val _actionState = mutableStateOf<ActionState>(ActionState.Idle)
    val actionState: State<ActionState> = _actionState

    init {
        loadWells()
    }
    fun loadWells() {
        viewModelScope.launch {
            _wellsListState.value = UiState.Loading
            try {
                repository.wellListFlow.collect { wells ->
                    _wellsListState.value = if (wells.isEmpty()) {
                        UiState.Empty
                    } else {
                        UiState.Success(wells)
                    }
                }
            } catch (e: Exception) {
                _wellsListState.value = UiState.Error(e.message ?: "Failed to load wells")
            }
        }
    }
    private fun saveCurrentWell() {
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
                } else {
                    repository.saveWell(currentWell)
                    _currentWellState.value = UiState.Success(currentWell)
                }
                loadWells() // Refresh the list
            } catch (e: Exception) {
                _currentWellState.value = UiState.Error(e.message ?: "Failed to save well")
            }
        }
    }
    private fun updateCurrentWell(transform: WellData.() -> WellData) {
        val currentWell = (_currentWellState.value as? UiState.Success)?.data ?: return
        _currentWellState.value = UiState.Success(currentWell.transform())
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

    fun loadWell(wellId: Int) {
        viewModelScope.launch {
            _currentWellState.value = UiState.Loading
            try {
                val well = repository.getWell(wellId) ?: WellData(id = wellId)
                _currentWellState.value = UiState.Success(well)
            } catch (e: Exception) {
                _currentWellState.value = UiState.Error(e.message ?: "Failed to load well")
            }
        }
    }

    fun deleteWell(espId: String) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            try {
                val success = repository.deleteWell(espId)
                if (success) {
                    _actionState.value = ActionState.Success("Well deleted successfully")
                    // Refresh the wells list
                    loadWells()
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
                loadWells() // Refresh the list
            } catch (e: Exception) {
                _wellsListState.value = UiState.Error(e.message ?: "Failed to swap wells")
            }
        }
    }

    suspend fun saveWellToServer(wellData: WellData, email: String, token: String): Boolean {
        return try {
            repository.saveWellToServer(wellData, email, token)
        } catch (e: Exception) {
            // Log error and return false to indicate failure
            android.util.Log.e("WellViewModel", "Error saving well to server: ${e.message}", e)
            false
        }
    }
}