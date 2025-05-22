package com.bluebridge.bluebridgeapp.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluebridge.bluebridgeapp.data.WellPickerEvent
import com.bluebridge.bluebridgeapp.data.`interface`.WellRepository
import com.bluebridge.bluebridgeapp.data.model.WellData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WellPickerViewModel(private val repository: WellRepository) : ViewModel() {
    // State management
    private val _state = mutableStateOf<UiState<List<WellData>>>(UiState.Loading)
    val state: State<UiState<List<WellData>>> = _state

    // Filter state
    data class WellFilters(
        val query: String = "",
        val waterType: String? = null,
        val status: String? = null
    )

    private val _filters = MutableStateFlow(WellFilters())
    val filters: StateFlow<WellFilters> = _filters.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadWells()
    }

    fun handleEvent(event: WellPickerEvent) {
        when (event) {
            is WellPickerEvent.UpdateSearchQuery -> updateSearchQuery(event.query)
            is WellPickerEvent.UpdateWaterTypeFilter -> updateWaterTypeFilter(event.waterType)
            is WellPickerEvent.UpdateStatusFilter -> updateStatusFilter(event.status)
            is WellPickerEvent.Refresh -> refreshWells()
            is WellPickerEvent.ResetFilters -> resetFilters()
        }
    }

    private fun loadWells() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.wellListFlow.collect { wells ->
                    _state.value = if (wells.isEmpty()) {
                        UiState.Empty
                    } else {
                        UiState.Success(applyFilters(wells))
                    }
                }
            } catch (e: Exception) {
                _state.value = UiState.Error(e.message ?: "Failed to load wells")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun refreshWells() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Force repository refresh if needed
                val wells = repository.getWells()
                _state.value = UiState.Success(applyFilters(wells))
            } catch (e: Exception) {
                _state.value = UiState.Error("Refresh failed: ${e.message}")
            } finally {
                _isLoading.value = false
            }

        }
    }

    private fun applyFilters(wells: List<WellData>): List<WellData> {
        return wells.filter { well ->
            val matchesQuery = _filters.value.query.isEmpty() ||
                    well.wellName.contains(_filters.value.query, ignoreCase = true) ||
                    well.wellOwner?.contains(_filters.value.query, ignoreCase = true) == true ||
                    well.espId?.contains(_filters.value.query, ignoreCase = true) == true

            val matchesWaterType = _filters.value.waterType == null ||
                    well.wellWaterType == _filters.value.waterType

            val matchesStatus = _filters.value.status == null ||
                    well.wellStatus == _filters.value.status

            matchesQuery && matchesWaterType && matchesStatus
        }
    }

    private fun updateSearchQuery(query: String) {
        _filters.update { it.copy(query = query) }
        refreshFilteredWells()
    }

    private fun updateWaterTypeFilter(waterType: String?) {
        _filters.update { it.copy(waterType = waterType) }
        refreshFilteredWells()
    }

    private fun updateStatusFilter(status: String?) {
        _filters.update { it.copy(status = status) }
        refreshFilteredWells()
    }

    private fun resetFilters() {
        _filters.value = WellFilters()
        refreshFilteredWells()
    }

    private fun refreshFilteredWells() {
        val currentWells = when (val current = _state.value) {
            is UiState.Success -> current.data
            else -> emptyList()
        }
        _state.value = UiState.Success(applyFilters(currentWells))
    }
}