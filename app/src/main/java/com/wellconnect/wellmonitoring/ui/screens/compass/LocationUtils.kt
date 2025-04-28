package com.wellconnect.wellmonitoring.ui.screens.compass

import android.annotation.SuppressLint
import android.location.Location
import android.os.Build
import androidx.annotation.RequiresApi
import com.wellconnect.wellmonitoring.data.WellData
import com.wellconnect.wellmonitoring.data.getLatitude
import com.wellconnect.wellmonitoring.data.getLongitude
import com.wellconnect.wellmonitoring.data.hasValidCoordinates
import com.wellconnect.wellmonitoring.viewmodels.WellViewModel

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
    return wellViewModel.wellList.value
        .filter { it.hasValidCoordinates() }
        .map { well -> well to calculateDistance(location, well) }
        .sortedBy { it.second }
        .take(3)
        .map { it.first }
}

/**
 * Format time in minutes to a human-readable string
 */
@SuppressLint("DefaultLocale")
fun formatTime(minutes: Float): String {
    return when {
        minutes < 1 -> "less than a minute"
        minutes < 60 -> "${minutes.toInt()} minutes"
        else -> String.format("%.1f hours", minutes / 60)
    }
} 