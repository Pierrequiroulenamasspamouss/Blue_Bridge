package com.bluebridgeapp.bluebridge.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.bluebridgeapp.bluebridge.events.BrowseWellsEvent
import com.bluebridgeapp.bluebridge.data.model.WellData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BrowseWellsViewModel() : ViewModel() {
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

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()



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
        TODO()
    }
}