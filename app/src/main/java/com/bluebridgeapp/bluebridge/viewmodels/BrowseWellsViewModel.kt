package com.bluebridgeapp.bluebridge.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluebridgeapp.bluebridge.data.model.Location
import com.bluebridgeapp.bluebridge.events.BrowseWellsEvent
import com.bluebridgeapp.bluebridge.data.model.WellData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BrowseWellsViewModel() : ViewModel() {
    // State management
    private val _state = mutableStateOf<UiState<List<WellData>>>(UiState.Loading)
    val state: State<UiState<List<WellData>>> = _state

    // TODO: Replace with actual data source/repository
    private val allWells = MutableStateFlow<List<WellData>>(emptyList())

    // Filter state
    data class WellFilters(
        val query: String = "",
        val waterType: String? = null,
        val status: String? = null
    )

    private val _filters = MutableStateFlow(WellFilters())
    val filters: StateFlow<WellFilters> = _filters.asStateFlow()

    private val _filteredWells = MutableStateFlow<List<WellData>>(emptyList())
    val filteredWells: StateFlow<List<WellData>> = _filteredWells.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()



    init {
        // TODO: Load initial data from repository
        // For now, using dummy data
        allWells.value = listOf(
            WellData(
                wellId = "1",
                wellName = "Well A",
                wellWaterType = "Fresh",
                wellStatus = "Active",
                wellLocation = Location(latitude = 34.0522, longitude = -118.2437),
                wellCapacity = "1000.0",
                wellWaterLevel = "750.0",

            ),
            WellData(
                wellId = "2",
                wellName = "Well B",
                wellWaterType = "Brackish",
                wellStatus = "Inactive",
                wellLocation = Location(latitude = 36.7783, longitude = -119.4179),
                wellCapacity = "500.0",
                wellWaterLevel = "200.0"
            ),
            WellData(
                wellId = "3",
                wellName = "Well C",
                wellWaterType = "Fresh",
                wellStatus = "Active",
                wellLocation = Location(latitude = 40.7128, longitude = -74.0060),
                wellCapacity = "1200.0",
                wellWaterLevel = "900.0"
            )
        )
        refreshFilteredWells()
    }

    fun handleEvent(event: BrowseWellsEvent) {
        when (event) {

            is BrowseWellsEvent.UpdateSearchQuery -> updateSearchQuery(event.query)
            is BrowseWellsEvent.UpdateWaterTypeFilter -> updateWaterTypeFilter(event.waterType)
            is BrowseWellsEvent.UpdateStatusFilter -> updateStatusFilter(event.status)
            is BrowseWellsEvent.ResetFilters -> resetFilters()
            is BrowseWellsEvent.RefreshFilteredWells -> refreshFilteredWells()
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
        _isLoading.value = true
        val currentFilters = _filters.value
        val wells = allWells.value

        val newFilteredList = wells.filter { well ->
            val matchesQuery = currentFilters.query.isBlank() ||
                    well.wellName.contains(currentFilters.query, ignoreCase = true) ||
                    (well.wellId?.contains(currentFilters.query, ignoreCase = true) == true)
            val matchesWaterType = currentFilters.waterType == null ||
                    well.wellWaterType == currentFilters.waterType
            val matchesStatus = currentFilters.status == null ||
                    well.wellStatus == currentFilters.status
            matchesQuery && matchesWaterType && matchesStatus
        }
        _filteredWells.value = newFilteredList
        _state.value = UiState.Success(newFilteredList)
        _isLoading.value = false
    }
}