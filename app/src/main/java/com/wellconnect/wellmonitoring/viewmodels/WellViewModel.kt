package com.wellconnect.wellmonitoring.viewmodels

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellconnect.wellmonitoring.data.WellConfigEvents
import com.wellconnect.wellmonitoring.data.WellData
import com.wellconnect.wellmonitoring.data.WellDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WellViewModel(private val wellDataStore: WellDataStore) : ViewModel() {
    // State
    private val _wellData = mutableStateOf(WellData())
    val wellData: State<WellData> = _wellData

    private val _lastSavedData = mutableStateOf(WellData())
    val lastSavedData: State<WellData> = _lastSavedData

    private val _wellLoaded = mutableStateOf(false)
    val wellLoaded: State<Boolean> = _wellLoaded

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    // Well List from Data Store
    val wellList: StateFlow<List<WellData>> = wellDataStore.wellListFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList()
    )

    // Event Handling
    fun handleConfigEvent(event: WellConfigEvents) {
        _wellData.value = when (event) {
            is WellConfigEvents.WellNameEntered -> _wellData.value.copy(wellName = event.wellName)
            is WellConfigEvents.OwnerEntered -> _wellData.value.copy(wellOwner = event.wellOwner)
            is WellConfigEvents.WellLocationEntered -> _wellData.value.copy(wellLocation = event.wellLocation)
            is WellConfigEvents.WellCapacityEntered -> _wellData.value.copy(wellCapacity = event.wellCapacity)
            is WellConfigEvents.WaterLevelEntered -> _wellData.value.copy(wellWaterLevel = event.wellWaterLevel)
            is WellConfigEvents.ConsumptionEntered -> _wellData.value.copy(wellWaterConsumption = event.wellWaterConsumption)
            is WellConfigEvents.WaterTypeEntered -> _wellData.value.copy(wellWaterType = event.wellWaterType)
            is WellConfigEvents.EspIdEntered -> _wellData.value.copy(espId = event.espId)
            is WellConfigEvents.SaveWell -> {
                val dataToSave = _wellData.value.copy(id = event.wellId)
                saveWell(dataToSave)
                dataToSave
            }
            else -> _wellData.value
        }
    }

    // Well Operations
    fun loadWellData(wellId: Int) {
        viewModelScope.launch {
            val data = wellDataStore.getWell(wellId) ?: WellData(id = wellId)
            _wellData.value = data
            _lastSavedData.value = data
            _wellLoaded.value = true
        }
    }

    private fun saveWell(well: WellData) {
        viewModelScope.launch {
            try {
                wellDataStore.saveWell(well)
                _lastSavedData.value = well
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save well: ${e.localizedMessage}"
            }
        }
    }

    fun removeWellByIndex(index: Int) {
        viewModelScope.launch {
            val currentList = wellList.value.toMutableList()
            if (index in currentList.indices) {
                val wellToDelete = currentList[index]
                val updatedList = currentList.filterNot { it.id == wellToDelete.id }
                wellDataStore.saveWellList(updatedList)
            }
        }
    }

    fun exchangeWells(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val currentList = wellList.value.toMutableList()
            if (fromIndex in currentList.indices && toIndex in currentList.indices) {
                val temp = currentList[fromIndex]
                currentList[fromIndex] = currentList[toIndex]
                currentList[toIndex] = temp
                wellDataStore.saveWellList(currentList)
            }
        }
    }

    suspend fun isUniqueEspId(espId: String, currentWellId: Int): Boolean {
        return wellList.value.none { it.id != currentWellId && it.espId == espId }
    }

    fun restorePreviousWellData() {
        _wellData.value = _lastSavedData.value
    }

    fun resetWellDataState() {
        _wellData.value = WellData()
        _lastSavedData.value = WellData()
        _wellLoaded.value = false
    }

    fun resetErrorMessage() {
        _errorMessage.value = null
    }

    suspend fun refreshAllWells(context: Context): Pair<Int, Int> {
        """ refresh all the wells in the list """
        return com.wellconnect.wellmonitoring.utils.refreshAllWells(context, wellDataStore)
    }
    suspend fun refreshSingleWell(wellId: Int, context: Context): Boolean {
        """ refresh a single well in the list """
        return com.wellconnect.wellmonitoring.utils.refreshSingleWell(
            wellId,
            wellDataStore,
            context
        )
    }

    fun clearAllWellData() {
        viewModelScope.launch {
            wellDataStore.saveWellList(emptyList())
            resetWellDataState()
        }
    }
}