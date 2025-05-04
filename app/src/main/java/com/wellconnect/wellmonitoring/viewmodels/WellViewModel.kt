package com.wellconnect.wellmonitoring.viewmodels

import WellData
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellconnect.wellmonitoring.data.WellEvents
import com.wellconnect.wellmonitoring.data.`interface`.WellRepository
import kotlinx.coroutines.launch

class WellViewModel(val repository: WellRepository) : ViewModel() {
    // Current well state
    private val _currentWellState = mutableStateOf<UiState<WellData>>(UiState.Empty)
    val currentWellState: State<UiState<WellData>> = _currentWellState

    // List of wells state
    private val _wellsListState = mutableStateOf<UiState<List<WellData>>>(UiState.Loading)
    val wellsListState: State<UiState<List<WellData>>> = _wellsListState

    init {
        loadWells()
    }
    private fun loadWells() {
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
                repository.saveWell(currentWell)
                _currentWellState.value = UiState.Success(currentWell)
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

    fun deleteWell(wellId: Int) {
        viewModelScope.launch {
            try {
                // First find the index of the well to delete
                val wells = (_wellsListState.value as? UiState.Success)?.data ?: return@launch
                val index = wells.indexOfFirst { it.id == wellId }
                if (index != -1) {
                    repository.deleteWellAt(index)
                    loadWells() // Refresh the list
                }
            } catch (e: Exception) {
                _wellsListState.value = UiState.Error(e.message ?: "Failed to delete well")
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


}