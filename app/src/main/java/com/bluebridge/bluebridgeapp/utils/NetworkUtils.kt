package com.bluebridge.bluebridgeapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.compose.material3.SnackbarHostState
import com.bluebridge.bluebridgeapp.data.model.WellData
import com.bluebridge.bluebridgeapp.data.repository.WellRepositoryImpl
import com.bluebridge.bluebridgeapp.network.RetrofitBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
}
/**
 * Check if internet connection is available
 */
fun checkInternetConnection(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = connectivityManager.activeNetwork
    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
    return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}

/**
 * Fetch details for a specific well by ESP ID
 */
suspend fun fetchWellDetailsFromServer(
    espId: String,
    snackbarHostState: SnackbarHostState,
    context: Context
): WellData? = withContext(Dispatchers.IO) {
    try {
        if (!checkInternetConnection(context)) {
            withContext(Dispatchers.Main) {
                snackbarHostState.showSnackbar("No internet connection")
            }
            Log.d("fetchWellDetails", "No internet connection")
            return@withContext null
        }

        val api = RetrofitBuilder.getServerApi(context)
        Log.d("fetchWellDetails", "Fetching details for well $espId from server_crt")
        val wellData = withTimeout(5_000) {
            api.getWellDataById(espId)
        }

        Log.d("fetchWellDetails", "Fetched well details: $wellData")
        wellData
    } catch (e: TimeoutCancellationException) {
        withContext(Dispatchers.Main) {
            snackbarHostState.showSnackbar("Connection timeout")
        }
        Log.e("fetchWellDetails", "Timeout: ${e.message}")
        null
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            snackbarHostState.showSnackbar("Error: ${e.localizedMessage ?: "Unknown error"}")
        }
        Log.e("fetchWellDetails", "Error: ${e.message}", e)
        null
    }
}

/**
 * Retrieve data from server_crt for a specific well and update it in the data store
 */
suspend fun retrieveDataFromServer(
    id: Int = 0,
    snackbarHostState: SnackbarHostState,
    wellRepositoryImpl: WellRepositoryImpl,
    context: Context
): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            if (!checkInternetConnection(context)) {
                withContext(Dispatchers.Main) {
                    snackbarHostState.showSnackbar("No internet connection")
                }
                Log.d("InternetConnection", "No internet connection")
                return@withContext false
            }

            // 1. Get current well data by ID
            val currentList = wellRepositoryImpl.wellListFlow.first()
            val matchingWell = currentList.find { it.id == id } ?: run {
                withContext(Dispatchers.Main) {
                    snackbarHostState.showSnackbar("Well with ID $id not found")
                }
                return@withContext false
            }

            // 2. Fetch new data from server_crt by endpoint /data/wells/{espId}
            val espId = matchingWell.espId
            Log.d("DataFetch", "Fetching data for ESP ID: $espId")
            
            val newData = fetchWellDetailsFromServer(espId, snackbarHostState, context) ?: return@withContext false

            // 3. Update only the timestamp and the id
            val updatedWell = newData
            updatedWell.lastRefreshTime = System.currentTimeMillis()
            updatedWell.id = matchingWell.id
            updatedWell.espId = matchingWell.espId

            // 4. Save back to datastore
            Log.d("DataSave", "Saving updated data: $updatedWell")
            wellRepositoryImpl.saveWell(updatedWell)

            Log.d("DataSave", "Successfully saved data for well ID ${matchingWell.id}")
            true
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                snackbarHostState.showSnackbar("Error: ${e.localizedMessage ?: "Unknown error"}")
            }
            false
        }
    }
}

/**
 * Refresh a single well by ID
 */
suspend fun refreshSingleWell(
    wellId: Int,
    wellRepositoryImpl: WellRepositoryImpl,
    context: Context,
): Boolean {
    return try {
        Log.d("RefreshDebug", "Starting refresh for well ID: $wellId")

        // Get current well data
        val currentWell = wellRepositoryImpl.getWell(wellId) ?: run {
            Log.e("RefreshDebug", "Well not found")
            return false
        }

        // Only refresh if ESP ID is available
        if (currentWell.espId.isBlank()) {
            Log.e("RefreshDebug", "No Esp ID. Cannot refresh")
            return false
        }

        // Fetch fresh data with timeout
        Log.d("RefreshDebug", "Fetching data from server_crt...")
        val success = withTimeoutOrNull(5_000) {
            retrieveDataFromServer(
                id = wellId,
                snackbarHostState = SnackbarHostState(),
                wellRepositoryImpl = wellRepositoryImpl,
                context = context
            )
        } == true
        Log.d("RefreshDebug", "Refresh ${if (success) "succeeded" else "failed"}")
        success
    } catch (e: Exception) {
        Log.e("RefreshDebug", "Refresh error: ${e.message}", e)
        false
    }
}
