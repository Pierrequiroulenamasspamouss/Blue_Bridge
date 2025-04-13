package com.jowell.wellmonitoring.ui

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jowell.wellmonitoring.data.WellConfigEvents
import com.jowell.wellmonitoring.data.WellData
import com.jowell.wellmonitoring.data.WellDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WellViewModel(private val wellDataStore: WellDataStore) : ViewModel() {
    private val _currentWellId = mutableStateOf(-1)
    val currentWellId: State<Int> get() = _currentWellId

    private val _wellLoaded = mutableStateOf(false)
    val wellLoaded: State<Boolean> get() = _wellLoaded

    private val _wellData = mutableStateOf<WellData>(WellData())
    val wellData: State<WellData> get() = _wellData

    private val _lastSavedData = mutableStateOf<WellData>(WellData())
    val lastSavedData: State<WellData> get() = _lastSavedData

    fun getWellData(wellId: Int) {
        _currentWellId.value = wellId
        _wellLoaded.value = false
        viewModelScope.launch {
            val retrievedWell = wellDataStore.getWellData(wellId)
            if (retrievedWell != null) {
                _wellData.value = retrievedWell
                _lastSavedData.value = retrievedWell
            }
            _wellLoaded.value = true
        }
    }
    fun clearWellData() {
        _wellData.value = WellData() // empty model
        _lastSavedData.value = WellData()
        _wellLoaded.value = false
    }

    fun resetLoadedFlag() {
        _wellLoaded.value = false
    }


    // Handling configuration changes
    fun onConfigEvent(event: WellConfigEvents) {
        when (event) {
            is WellConfigEvents.SaveWell -> {
                viewModelScope.launch {
                    wellDataStore.saveWellData(event.wellId, _wellData.value)
                    _lastSavedData.value = _wellData.value
                }
            }
            is WellConfigEvents.WellNameEntered -> {
                _wellData.value = _wellData.value.copy(wellName = event.wellName)
            }

            is WellConfigEvents.OwnerEntered -> {
                _wellData.value = _wellData.value.copy(wellOwner = event.wellOwner)
            }

            is WellConfigEvents.WellLocationEntered -> {
                _wellData.value = _wellData.value.copy(wellLocation = event.wellLocation)
            }

            is WellConfigEvents.WellCapacityEntered -> {
                _wellData.value = _wellData.value.copy(wellCapacity = event.wellCapacity)
            }

            is WellConfigEvents.WaterLevelEntered -> {
                _wellData.value = _wellData.value.copy(wellWaterLevel = event.wellWaterLevel)
            }

            is WellConfigEvents.ConsumptionEntered -> {
                _wellData.value = _wellData.value.copy(wellWaterConsumption = event.wellWaterConsumption)
            }

            is WellConfigEvents.WaterTypeEntered -> {
                _wellData.value = _wellData.value.copy(wellWaterType = event.wellWaterType)
            }
        }
    }

    // To get all wells stored in data store
    val wellList: StateFlow<List<WellData>> = wellDataStore.wellListFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList()
    )

    // Reset all wells
    fun resetAllWells() {
        viewModelScope.launch {
            wellDataStore.resetWellList()
        }
    }

    // Save the new list of wells
    fun saveWells(newList: List<WellData>) {
        viewModelScope.launch {
            wellDataStore.saveWellList(newList)
        }
    }

    // Add a new well
    fun addWell(well: WellData) {
        val updatedList = wellList.value.toMutableList().apply { add(well) }
        saveWells(updatedList)
    }

    // Delete a well by index
    fun deleteWell(index: Int) {
        val updatedList = wellList.value.toMutableList().apply { removeAt(index) }
        saveWells(updatedList)
    }
    fun revertToLastSavedData() {
        _wellData.value = _lastSavedData.value
    }
}
