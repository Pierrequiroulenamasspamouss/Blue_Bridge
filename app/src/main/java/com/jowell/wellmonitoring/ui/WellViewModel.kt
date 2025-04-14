package com.jowell.wellmonitoring.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jowell.wellmonitoring.data.WellConfigEvents
import com.jowell.wellmonitoring.data.WellData
import com.jowell.wellmonitoring.data.WellDataStore
import com.jowell.wellmonitoring.network.RetrofitBuilder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WellViewModel(private val wellDataStore: WellDataStore) : ViewModel() {
    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> get() = _errorMessage

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    private val _wellLoaded = mutableStateOf(false)
    val wellLoaded: State<Boolean> get() = _wellLoaded

    private val _wellData = mutableStateOf<WellData>(WellData())
    val wellData: State<WellData> get() = _wellData

    private val _lastSavedData = mutableStateOf<WellData>(WellData())
    val lastSavedData: State<WellData> get() = _lastSavedData

    // Get well data (loads from data store)
    fun getWellData(wellId: Int) {
        viewModelScope.launch {
            val data = wellDataStore.getWellData(wellId) ?: WellData(id = wellId)
            _wellData.value = data
            _lastSavedData.value = data
            _wellLoaded.value = true
        }
    }

    // Clear all well data
    fun clearWellData() {
        _wellData.value = WellData()
        _lastSavedData.value = WellData()
        _wellLoaded.value = false
    }

    // Save a well data after configuration changes
    fun onConfigEvent(event: WellConfigEvents) {
        _wellData.value = when (event) {
            is WellConfigEvents.SaveWell -> {
                val data = _wellData.value.copy(id = event.wellId)
                viewModelScope.launch {
                    wellDataStore.saveWellData(event.wellId, data)
                    _lastSavedData.value = data
                }
                data
            }
            is WellConfigEvents.WellNameEntered -> _wellData.value.copy(wellName = event.wellName)
            is WellConfigEvents.OwnerEntered -> _wellData.value.copy(wellOwner = event.wellOwner)
            is WellConfigEvents.WellLocationEntered -> _wellData.value.copy(wellLocation = event.wellLocation)
            is WellConfigEvents.WellCapacityEntered -> _wellData.value.copy(wellCapacity = event.wellCapacity)
            is WellConfigEvents.WaterLevelEntered -> _wellData.value.copy(wellWaterLevel = event.wellWaterLevel)
            is WellConfigEvents.ConsumptionEntered -> _wellData.value.copy(wellWaterConsumption = event.wellWaterConsumption)
            is WellConfigEvents.WaterTypeEntered -> _wellData.value.copy(wellWaterType = event.wellWaterType)
            is WellConfigEvents.IpAddressEntered -> _wellData.value.copy(ipAddress = event.espId)
            else -> _wellData.value // In case event is not recognized, return current data
        }
    }

    // Add a new well to the list
    fun addWell(well: WellData) {
        val updatedList = wellList.value.toMutableList().apply { add(well) }
        saveWells(updatedList)
    }

    // Save the list of wells
    fun saveWells(newList: List<WellData>) {
        viewModelScope.launch {
            wellDataStore.saveWellList(newList)
        }
    }
    // Swap wells in the list
    fun swapWells(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val updatedWells = wellList.value.toMutableList().apply {
                val temp = this[fromIndex]
                this[fromIndex] = this[toIndex]
                this[toIndex] = temp
            }
            saveWells(updatedWells)
        }
    }

    // Fetch Well data from web server
    fun fetchWellDataFromESP(ip: String, context: Context) {
        viewModelScope.launch {
            try {
                val baseUrl = "http://$ip/"
                val api = RetrofitBuilder.create(baseUrl)

                // Fetch data from server
                val newData = api.getWellData()

                // Find and update the well in the list
                val updatedList = wellList.value.toMutableList().apply {
                    val existingIndex = indexOfFirst { it.ipAddress == ip }
                    if (existingIndex >= 0) {
                        val originalWell = this[existingIndex]
                        val updatedWell = newData.copy(id = originalWell.id, ipAddress = ip)
                        this[existingIndex] = updatedWell

                        // Save this well to the persistent store
                        wellDataStore.saveWellData(originalWell.id, updatedWell)
                    }
                }

                // Save updated list locally
                saveWells(updatedList)

            } catch (e: Exception) {
                _errorMessage.value = "Failed to fetch from $ip: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }



    // Delete well by index and save updated list
    fun deleteWell(index: Int) {
        viewModelScope.launch {
            val currentList = wellList.value.toMutableList()
            val wellToDelete = currentList.getOrNull(index) ?: return@launch
            currentList.removeAt(index)
            saveWells(currentList)
            wellDataStore.deleteWellById(wellToDelete.id)
        }
    }

    // Revert to last saved well data
    fun revertToLastSavedData() {
        _wellData.value = _lastSavedData.value
    }
    fun resetAllWells() {
        viewModelScope.launch {
            wellDataStore.resetWellList()
        }
    }
    fun isConnectedToInternet(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    // To get all wells stored in data store
    val wellList: StateFlow<List<WellData>> = wellDataStore.wellListFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList()
    )
}
