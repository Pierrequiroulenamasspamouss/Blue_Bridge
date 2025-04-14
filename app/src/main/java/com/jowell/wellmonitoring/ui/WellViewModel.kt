package com.jowell.wellmonitoring.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jowell.wellmonitoring.data.WellConfigEvents
import com.jowell.wellmonitoring.data.WellData
import com.jowell.wellmonitoring.data.WellDataStore
import com.jowell.wellmonitoring.network.RetrofitBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout


class WellViewModel(private val wellDataStore: WellDataStore) : ViewModel() {
    // ---- State ----

    private val _wellData = mutableStateOf(WellData())
    val wellData: State<WellData> get() = _wellData

    private val _lastSavedData = mutableStateOf(WellData())
    val lastSavedData: State<WellData> get() = _lastSavedData

    private val _wellLoaded = mutableStateOf(false)
    val wellLoaded: State<Boolean> get() = _wellLoaded

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> get() = _errorMessage

    // Well List from Data Store (state flow)
    val wellList: StateFlow<List<WellData>> = wellDataStore.wellListFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList()
    )

    // ---- Events ----
    fun onConfigEvent(event: WellConfigEvents) {
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
                saveWellData(event.wellId) // Modified to accept explicit data
                dataToSave // Return the saved data
            }
            else -> _wellData.value // fallback in case an unknown event comes in
        }


    }

    // ---- Load / Save ----

    fun getWellData(wellId: Int) {
        viewModelScope.launch {
            val data = wellDataStore.getWellData(wellId) ?: WellData(id = wellId)
            _wellData.value = data
            _lastSavedData.value = data
            _wellLoaded.value = true
        }
    }

    private fun saveWellData(wellId: Int, data: WellData = _wellData.value) {
        viewModelScope.launch {
            try {
                // 1. Validate input data
                if (wellId <= 0) {
                    _errorMessage.value = "Invalid well ID"
                    return@launch
                }

                if (data.ipAddress.isBlank()) {
                    _errorMessage.value = "IP address cannot be empty"
                    return@launch
                }

                // 2. Create updated data with proper ID
                val currentData = data.copy(
                    id = wellId,
                    //lastModified = System.currentTimeMillis() // Add timestamp if your model has it
                )

                // 3. Save to individual well file
                wellDataStore.saveWellData(wellId, currentData).also {
                    println("Saved individual well data: $currentData") // Debug log
                }

                // 4. Update global well list
                val currentList = wellList.value.toMutableList()
                val existingIndex = currentList.indexOfFirst { it.id == wellId }

                if (existingIndex >= 0) {
                    currentList[existingIndex] = currentData
                } else {
                    currentList.add(currentData)
                }

                wellDataStore.saveWellList(currentList).also {
                    println("Updated well list with ${currentList.size} items") // Debug log
                }

                // 5. Update state
                _lastSavedData.value = currentData
                _wellData.value = currentData // Ensure UI state is updated

                // Optional: Clear any previous error
                _errorMessage.value = null

            } catch (e: Exception) {
                _errorMessage.value = "Save failed: ${e.localizedMessage ?: "Unknown error"}"
                println("Error saving well data: ${e.stackTraceToString()}") // Detailed error log
            }
        }
    }
    fun clearWellData() {
        _wellData.value = WellData()
        _lastSavedData.value = WellData()
        _wellLoaded.value = false
    }

    fun revertToLastSavedData() {
        _wellData.value = _lastSavedData.value
    }

    // ---- Deletion ----

    fun deleteWell(index: Int) {
        viewModelScope.launch {
            val currentList = wellList.value.toMutableList()
            val wellToDelete = currentList.getOrNull(index) ?: return@launch
            currentList.removeAt(index)
            wellDataStore.saveWellList(currentList)
            wellDataStore.deleteWellById(wellToDelete.id)
        }
    }

    fun resetAllWells() {
        viewModelScope.launch {
            wellDataStore.resetAllWellData()
        }
    }

    // ---- ESP Sync ----

    suspend fun fetchWellDataFromESP(
        ip: String,
        context: Context,
        snackbarHostState: SnackbarHostState,
        scope: CoroutineScope
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Check internet connection
                if (!isConnectedToInternet(context)) {
                    withContext(Dispatchers.Main) {
                        snackbarHostState.showSnackbar("No internet connection available.")
                    }
                    return@withContext false
                }

                // 2. Validate IP
                if (ip.isBlank()) {
                    withContext(Dispatchers.Main) {
                        snackbarHostState.showSnackbar("Invalid IP address")
                    }
                    return@withContext false
                }

                // 3. Create API client
                val baseUrl = "http://$ip/"
                val api = RetrofitBuilder.create(baseUrl)

                // 4. Fetch data with timeout
                val newData = try {
                    withTimeout(10_000) { // 10 second timeout
                        api.getWellData()
                    }
                } catch (e: TimeoutCancellationException) {
                    withContext(Dispatchers.Main) {
                        snackbarHostState.showSnackbar("Connection timeout")
                    }
                    return@withContext false
                }

                // 5. Update well data
                val matchingWell = wellList.value.find { it.ipAddress == ip }
                if (matchingWell != null) {
                    val updatedWell = newData.copy(
                        id = matchingWell.id,
                        ipAddress = ip,
                        lastRefreshTime = System.currentTimeMillis()
                    )
                    wellDataStore.saveWellData(matchingWell.id, updatedWell)
                }

                true
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    snackbarHostState.showSnackbar("Fetch error: ${e.localizedMessage ?: "Unknown error"}")
                }
                false
            }
        }
    }



    // ---- Utility ----
    fun swapWells(fromIndex: Int, toIndex: Int) {
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

    fun updateLastRefreshTime(index: Int, time: Long) {
        viewModelScope.launch {
            val updated = wellList.value.toMutableList()
            val existing = updated[index].copy(lastRefreshTime = time)
            updated[index] = existing
            wellDataStore.saveWellList(updated)
        }
    }

    fun isIpAddressDuplicate(ip: String, currentWellId: Int): Boolean {
        return wellList.value.any { it.ipAddress == ip && it.id != currentWellId }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun isConnectedToInternet(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
    // In WellViewModel.kt
    private val _isRefreshing = mutableStateOf<Int?>(null) // Track which well is refreshing
    val isRefreshing: State<Int?> get() = _isRefreshing

    suspend fun refreshAllWells(context: Context): Pair<Int, Int> {
        val wells = wellList.value
        var successCount = 0

        wells.forEach { well ->
            if (refreshSingleWell(well.id, context)) {
                successCount++
            }
        }

        return Pair(successCount, wells.size)
    }

    suspend fun refreshSingleWell(wellId: Int, context: Context): Boolean {
        return try {
            _isRefreshing.value = wellId // Set loading state

            // 1. Get the current well list
            val currentList = wellList.value

            // 2. Find the specific well to refresh
            val wellToRefresh = currentList.find { it.id == wellId } ?: return false

            // 3. Only refresh if IP is available
            if (wellToRefresh.ipAddress.isBlank()) return false

            // 4. Fetch fresh data from ESP with timeout
            val success = withTimeout(15_000) { // 15 second timeout

                fetchWellDataFromESP(
                    ip = wellToRefresh.ipAddress,
                    context = context,
                    snackbarHostState = SnackbarHostState(), // Dummy snackbar state
                    scope = this
                )


            }

            // 5. Update state if successful
            if (success) {
                val updatedWell = wellDataStore.getWellData(wellId)?.copy(
                    lastRefreshTime = System.currentTimeMillis()
                ) ?: return false

                // Update the well in the list
                val updatedList = wellList.value.map {
                    if (it.id == wellId) updatedWell else it
                }
                wellDataStore.saveWellList(updatedList)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            _errorMessage.value = when (e) {
                is TimeoutCancellationException -> "Refresh timed out"
                else -> "Refresh failed: ${e.localizedMessage ?: "Unknown error"}"
            }
            false
        } finally {
            _isRefreshing.value = null // Clear loading state
        }
    }
}
