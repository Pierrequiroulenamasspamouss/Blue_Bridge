package com.wellconnect.wellmonitoring.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.compose.material3.SnackbarHostState
import com.wellconnect.wellmonitoring.data.WellDataStore
import com.wellconnect.wellmonitoring.network.RetrofitBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

suspend fun checkDuplicateIpAddress(ip: String, currentWellId: Int, wellDataStore: WellDataStore): Boolean {
    return wellDataStore.wellListFlow.first().any { it.ipAddress == ip && it.id != currentWellId }
}

fun checkInternetConnection(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = connectivityManager.activeNetwork
    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
    return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}




suspend fun retrieveDataFromServer(
    id: Int = 0,
    ip: String,
    snackbarHostState: SnackbarHostState,
    wellDataStore: WellDataStore,
    context: Context
): Boolean {
    return withContext(Dispatchers.IO) {

        try {
            if (checkInternetConnection(context) == false) {
                withContext(Dispatchers.Main) {
                    snackbarHostState.showSnackbar("No internet connection")
                }
                Log.d("InternetConnection", "No internet connection")
                false

            }

            // 1. Get current well data by IP
            val currentList = wellDataStore.wellListFlow.first()
            val matchingWell = currentList.find { it.id == id } ?: run {
                withContext(Dispatchers.Main) {
                    snackbarHostState.showSnackbar("Well with ID $id not found")
                }
                return@withContext false
            }

            // 2. Fetch new data from server
            val baseUrl = "http://$ip/"
            val api = RetrofitBuilder.create(baseUrl)

            val newData = withTimeout(5_000) {
                api.getWellData()
            }

            // 3. Update only the timestamp and the id
            val updatedWell = newData
            updatedWell.lastRefreshTime = System.currentTimeMillis()
            updatedWell.id = matchingWell.id
            updatedWell.ipAddress = matchingWell.ipAddress
            // 4. Save back to datastore
            Log.d("DataSave", "Saving updated data: $updatedWell")
            wellDataStore.saveWell(updatedWell)

            Log.d("DataSave", "Successfully saved data for well ID ${matchingWell.id}")

            Log.d("DataSave", "Verification:  ${wellDataStore.getWell(matchingWell.id)}}")

            true

        } catch (e: TimeoutCancellationException) {
            withContext(Dispatchers.Main) {
                snackbarHostState.showSnackbar("Connection timeout")
            }
            false
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                snackbarHostState.showSnackbar("Error: ${e.localizedMessage ?: "Unknown error"}")
            }
            false
        }
    }
}

suspend fun refreshAllWells(context: Context, wellDataStore: WellDataStore): Pair<Int, Int> {
    val wells = wellDataStore.wellListFlow.first()
    var successCount = 0

    wells.forEach { well ->
        if (refreshSingleWell(well.id, wellDataStore, context)) {
            successCount++
        }
    }

    return Pair(successCount, wells.size)
}

suspend fun refreshSingleWell(
    wellId: Int,
    wellDataStore: WellDataStore,
    context: Context,
): Boolean {
    return try {
        Log.d("RefreshDebug", "Starting refresh for well ID: $wellId")

        // Get current well data
        val currentWell = wellDataStore.getWell(wellId) ?: run {
            Log.e("RefreshDebug", "Well not found")
            return false
        }

        // Only refresh if IP is available
        if (currentWell.ipAddress.isBlank()) {
            Log.e("RefreshDebug", "IP address is blank")
            return false
        }

        // Fetch fresh data with timeout
        Log.d("RefreshDebug", "Fetching data from server...")
        val success = withTimeoutOrNull(5_000) {
            retrieveDataFromServer(
                id = wellId,
                ip = currentWell.ipAddress,
                snackbarHostState = SnackbarHostState(),
                wellDataStore = wellDataStore,
                context = context
            )
        } ?: false
        Log.d("RefreshDebug", "Refresh ${if (success) "succeeded" else "failed"}")
        success
    } catch (e: Exception) {
        Log.e("RefreshDebug", "Refresh error: ${e.message}", e)
        false
    }
}