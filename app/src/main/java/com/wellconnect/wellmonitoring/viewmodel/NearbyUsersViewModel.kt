package com.wellconnect.wellmonitoring.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellconnect.wellmonitoring.data.NearbyUser
import com.wellconnect.wellmonitoring.data.NearbyUsersState
import com.wellconnect.wellmonitoring.data.UserDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class NearbyUsersViewModel(
    private val userDataStore: UserDataStore
) : ViewModel() {

    private val _nearbyUsersState = MutableStateFlow<NearbyUsersState>(NearbyUsersState.Loading)
    val nearbyUsersState: StateFlow<NearbyUsersState> = _nearbyUsersState.asStateFlow()

    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0

    fun updateLocation(latitude: Double, longitude: Double) {
        currentLatitude = latitude
        currentLongitude = longitude
        refreshNearbyUsers()
    }

    fun refreshNearbyUsers() {
        viewModelScope.launch {
            _nearbyUsersState.value = NearbyUsersState.Loading
            try {
                val nearbyUsers = userDataStore.getNearbyUsers()
                    .map { user ->
                        val distance = calculateDistance(
                            currentLatitude, currentLongitude,
                            user.latitude, user.longitude
                        )
                        user.copy(distance = distance)
                    }
                    .sortedBy { it.distance }
                _nearbyUsersState.value = NearbyUsersState.Success(nearbyUsers)
            } catch (e: Exception) {
                _nearbyUsersState.value = NearbyUsersState.Error(e.message ?: "Failed to fetch nearby users")
            }
        }
    }

    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val R = 6371.0 // Earth's radius in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    fun calculateBearing(targetLat: Double, targetLon: Double): Float {
        val dLon = Math.toRadians(targetLon - currentLongitude)
        val startLat = Math.toRadians(currentLatitude)
        val endLat = Math.toRadians(targetLat)
        
        val y = sin(dLon) * cos(endLat)
        val x = cos(startLat) * sin(endLat) -
                sin(startLat) * cos(endLat) * cos(dLon)
        
        var bearing = Math.toDegrees(atan2(y, x))
        if (bearing < 0) {
            bearing += 360
        }
        return bearing.toFloat()
    }
} 