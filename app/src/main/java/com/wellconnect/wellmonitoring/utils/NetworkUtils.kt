package com.wellconnect.wellmonitoring.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.compose.material3.SnackbarHostState
import com.wellconnect.wellmonitoring.R
import com.wellconnect.wellmonitoring.data.ShortenedWellData
import com.wellconnect.wellmonitoring.data.WellData
import com.wellconnect.wellmonitoring.data.WellDataStore
import com.wellconnect.wellmonitoring.network.RetrofitBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "NetworkUtils"

/**
 * Get the base API URL from strings.xml
 */
fun getBaseApiUrl(context: Context): String {
    return context.getString(R.string.GeneralIp)
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
 * Fetch all available wells from the server_crt
 * Returns a list of minimal well data objects
 */
suspend fun fetchAllWellsFromServer(
    snackbarHostState: SnackbarHostState,
    context: Context,
    maxRetries: Int = 3
): List<ShortenedWellData> = withContext(Dispatchers.IO) {
    val baseUrl = getBaseApiUrl(context)
    Log.d(TAG, "Fetching wells from server_crt at URL: $baseUrl")
    var retryCount = 0
    
    while (retryCount < maxRetries) {
        try {
            return@withContext withTimeout(30_000L) {
                // Use createFresh to get a new client instance each time
                val apiService = RetrofitBuilder.createFresh(baseUrl)
                val response = apiService.getAllWells()
                
                // Handle response based on how the API actually returns data
                Log.d(TAG, "Successfully fetched ${response.size} wells from server_crt")
                response
            }
        } catch (e: TimeoutCancellationException) {
            retryCount++
            val remainingRetries = maxRetries - retryCount
            
            if (remainingRetries > 0) {
                val backoffDelay = 1000L * retryCount
                Log.w(TAG, "Timeout fetching wells, attempt ${retryCount}/$maxRetries. Retrying in ${backoffDelay}ms... (${remainingRetries} attempts remaining)")
                delay(backoffDelay)
            } else {
                Log.e(TAG, "Failed to fetch wells after $maxRetries attempts due to timeout")
                return@withContext listOf<ShortenedWellData>()
            }
        } catch (e: Exception) {
            retryCount++
            val remainingRetries = maxRetries - retryCount
            
            if (remainingRetries > 0) {
                val backoffDelay = 1000L * retryCount
                Log.w(TAG, "Error fetching wells, attempt ${retryCount}/$maxRetries. Error: ${e.message}. Retrying in ${backoffDelay}ms... (${remainingRetries} attempts remaining)")
                delay(backoffDelay)
            } else {
                Log.e(TAG, "Failed to fetch wells after $maxRetries attempts", e)
                return@withContext listOf<ShortenedWellData>()
            }
        }
    }
    
    listOf<ShortenedWellData>() // Return empty list instead of throwing exception
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
    wellDataStore: WellDataStore,
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
            val currentList = wellDataStore.wellListFlow.first()
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
            wellDataStore.saveWell(updatedWell)

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
 * Refresh all wells in the data store
 */
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

/**
 * Refresh a single well by ID
 */
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