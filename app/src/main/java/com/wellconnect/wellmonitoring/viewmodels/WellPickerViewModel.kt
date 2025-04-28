package com.wellconnect.wellmonitoring.viewmodels

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellconnect.wellmonitoring.data.WellData
import com.wellconnect.wellmonitoring.utils.fetchWellDetailsFromServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WellPickerViewModel : ViewModel() {
    private val _wellList = MutableStateFlow<List<WellData>>(emptyList())
    val wellList: StateFlow<List<WellData>> = _wellList.asStateFlow()
    
    var isLoading by mutableStateOf(false)
        private set
    
    var searchQuery by mutableStateOf("")
        private set
    
    var selectedWaterType by mutableStateOf<String?>(null)
        private set
    
    var selectedStatus by mutableStateOf<String?>(null)
        private set
    
//    fun setSearchQuery(query: String) {
//        searchQuery = query
//    }
    
    fun setWaterTypeFilter(waterType: String?) {
        selectedWaterType = waterType
    }
    
    fun setStatusFilter(status: String?) {
        selectedStatus = status
    }
    
    fun getFilteredWells(): List<WellData> {
        return wellList.value.filter { well ->
            val matchesQuery = searchQuery.isEmpty() || 
                well.wellName.contains(searchQuery, ignoreCase = true) ||
                well.wellOwner.contains(searchQuery, ignoreCase = true) ||
                well.espId.contains(searchQuery, ignoreCase = true)
            
            val matchesWaterType = selectedWaterType == null || 
                well.wellWaterType == selectedWaterType
            
            val matchesStatus = selectedStatus == null || 
                well.wellStatus == selectedStatus
            
            matchesQuery && matchesWaterType && matchesStatus
        }
    }
    
//    fun fetchWells(context: Context, snackbarHostState: SnackbarHostState) {
//        viewModelScope.launch {
//            isLoading = true
//            val wells = fetchAllWellsFromServer(snackbarHostState, context)
//            _wellList.value = wells
//            isLoading = false
//        }
//    }
    
    fun fetchWellDetails(
        espId: String,
        context: Context,
        snackbarHostState: SnackbarHostState,
        onResult: (WellData?) -> Unit
    ) {
        viewModelScope.launch {
            isLoading = true
            val wellData = fetchWellDetailsFromServer(espId, snackbarHostState, context)
            onResult(wellData)
            isLoading = false
        }
    }
}


