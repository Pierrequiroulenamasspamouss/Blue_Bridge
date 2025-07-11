package com.bluebridgeapp.bluebridge.utils

import android.location.Location
import android.os.Build
import androidx.annotation.RequiresApi
import com.bluebridgeapp.bluebridge.data.model.WellData
import com.bluebridgeapp.bluebridge.data.model.getLatitude
import com.bluebridgeapp.bluebridge.data.model.getLongitude
import com.bluebridgeapp.bluebridge.data.model.hasValidCoordinates
import com.bluebridgeapp.bluebridge.viewmodels.UiState
import com.bluebridgeapp.bluebridge.viewmodels.WellViewModel


/**
 * Helper functions for location and distance calculations
 */

// Store last known location for reuse across compass sessions
var lastKnownLocation: Location? = null

/**
 * Calculate distance between a location and a well
 */
@RequiresApi(Build.VERSION_CODES.O)
fun calculateDistance(location: Location?, well: WellData): Float {
    if (location == null || !well.hasValidCoordinates()) return Float.POSITIVE_INFINITY
    val results = FloatArray(1)
    Location.distanceBetween(
        location.latitude,
        location.longitude,
        well.getLatitude()!!,
        well.getLongitude()!!,
        results
    )
    return results[0]
}

/**
 * Format distance in meters to a human-readable string
 */
fun formatDistance(meters: Float): String {
    return when {
        meters == Float.POSITIVE_INFINITY -> "Unknown distance"
        meters >= 1000 -> "%.1f km".format(meters / 1000)
        else -> "${meters.toInt()} m"
    }
}

/**
 * Find the nearest wells to a location
 */
@RequiresApi(Build.VERSION_CODES.O)
fun findNearestWells(location: Location, wellViewModel: WellViewModel): List<WellData> {
    val wells = (wellViewModel.wellsListState.value as? UiState.Success<List<WellData>>)?.data ?: emptyList()
    return wells
        .filter { it.hasValidCoordinates() }
        .map { well -> well to calculateDistance(location, well) }
        .sortedBy { it.second }
        .take(3)
        .map { it.first }
}

