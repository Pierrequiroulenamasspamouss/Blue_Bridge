package com.wellconnect.wellmonitoring.viewmodel

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellconnect.wellmonitoring.data.WellConfigEvents
import com.wellconnect.wellmonitoring.data.WellData
import com.wellconnect.wellmonitoring.data.WellDataStore
import com.wellconnect.wellmonitoring.utils.persistWellData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class WellViewModel(private val wellDataStore: WellDataStore) : ViewModel() {
    // ---- State ----

    private val _wellData = mutableStateOf(WellData())
    val wellData: State<WellData> get() = _wellData

    private val _lastSavedData = mutableStateOf(WellData())
    val lastSavedData: State<WellData> get() = _lastSavedData

    private val _isRefreshing = mutableStateOf<Int?>(null) // Track which well is refreshing
    val isRefreshing: State<Int?> get() = _isRefreshing

    private val _wellLoaded = mutableStateOf(false)
    val wellLoaded: State<Boolean> get() = _wellLoaded

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> get() = _errorMessage

    // Well List from Data Store (state flow)
    val wellList: StateFlow<List<WellData>> = wellDataStore.wellListFlow.stateIn(
        viewModelScope,
        SharingStarted.Companion.Eagerly,
        emptyList()
    )

    // ---- Events ----
    fun handleConfigEvent(event: WellConfigEvents) {
        _wellData.value = when (event) {
            is WellConfigEvents.WellNameEntered -> _wellData.value.copy(wellName = event.wellName)
            is WellConfigEvents.OwnerEntered -> _wellData.value.copy(wellOwner = event.wellOwner)
            is WellConfigEvents.WellLocationEntered -> _wellData.value.copy(wellLocation = event.wellLocation)
            is WellConfigEvents.WellCapacityEntered -> _wellData.value.copy(wellCapacity = event.wellCapacity)
            is WellConfigEvents.WaterLevelEntered -> _wellData.value.copy(wellWaterLevel = event.wellWaterLevel)
            is WellConfigEvents.ConsumptionEntered -> _wellData.value.copy(wellWaterConsumption = event.wellWaterConsumption)
            is WellConfigEvents.WaterTypeEntered -> _wellData.value.copy(wellWaterType = event.wellWaterType)
            is WellConfigEvents.IpAddressEntered -> _wellData.value.copy(ipAddress = event.espId)
            is WellConfigEvents.SaveWell -> {
                // Make sure we have the latest data before saving
                val dataToSave = _wellData.value.copy(id = event.wellId)
                persistWellData(
                    event.wellId,
                    dataToSave,
                    wellDataStore,
                    viewModelScope,
                    _errorMessage,
                    _lastSavedData,
                    _wellData
                )
                dataToSave // Return the saved data
            }
            else -> _wellData.value // fallback in case an unknown event comes in
        }
    }

    // ---- Load / Save ----

    fun loadWellData(wellId: Int) {
        """load the data in the json file of the well with that ID """
        com.wellconnect.wellmonitoring.utils.loadWellData(
            wellId,
            wellDataStore,
            viewModelScope,
            _wellData,
            _lastSavedData,
            _wellLoaded
        )
    }

    fun resetWellDataState() {
        """ reset all the wells saved """
        com.wellconnect.wellmonitoring.utils.resetWellDataState(
            _wellData,
            _lastSavedData,
            _wellLoaded
        )
    }

    fun restorePreviousWellData() {
        """ restore the local well data to the last saved values """
        com.wellconnect.wellmonitoring.utils.restorePreviousWellData(_wellData, _lastSavedData)
    }

    // ---- Deletion ----

    fun removeWellByIndex(index: Int) {
        """deletes the data of the well with that ID """
        com.wellconnect.wellmonitoring.utils.removeWellByIndex(index, wellDataStore, viewModelScope)
    }



    // ---- Server sync ----


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

    // ---- Utility ----
    fun exchangeWells(fromIndex: Int, toIndex: Int) {
        """ swap a well from index ID with another well in index """
        com.wellconnect.wellmonitoring.utils.exchangeWells(
            fromIndex,
            toIndex,
            wellDataStore,
            viewModelScope
        )
    }



    suspend fun checkDuplicateIpAddress(ip: String, currentWellId: Int): Boolean {
        """ check if the ip address is already in use by another well """
        return com.wellconnect.wellmonitoring.utils.checkDuplicateIpAddress(
            ip,
            currentWellId,
            wellDataStore
        )
    }

    fun resetErrorMessage() {
        """ reset the error messages shown in the snackbars """
        com.wellconnect.wellmonitoring.utils.resetErrorMessage(_errorMessage)
    }

    fun checkInternetConnection(context: Context): Boolean {
        """ check if the user is connected to the Internet or not """
        return checkInternetConnection(context)
    }
    fun clearAllWellData() {
        com.wellconnect.wellmonitoring.utils.clearAllWellData(wellDataStore, viewModelScope)
    }

}