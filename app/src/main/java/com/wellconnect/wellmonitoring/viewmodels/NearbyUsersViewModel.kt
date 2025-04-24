package com.wellconnect.wellmonitoring.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellconnect.wellmonitoring.data.NearbyUser
import com.wellconnect.wellmonitoring.data.NearbyUsersState
import com.wellconnect.wellmonitoring.data.UserDataStore
import com.wellconnect.wellmonitoring.utils.LocationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class NearbyUsersViewModel @Inject constructor(
    private val userDataStore: UserDataStore,
    private val locationUtils: LocationUtils
) : ViewModel() {

    private val _uiState = MutableStateFlow<NearbyUsersState>(NearbyUsersState.Loading)
    val uiState: StateFlow<NearbyUsersState> = _uiState.asStateFlow()

    private var currentLat: Double = 0.0
    private var currentLon: Double = 0.0

    init {
        refreshNearbyUsers()
    }

    fun refreshNearbyUsers() {
        viewModelScope.launch {
            _uiState.value = NearbyUsersState.Loading
            try {
                val location = locationUtils.getCurrentLocation()
                if (location == null) {
                    _uiState.value = NearbyUsersState.LocationPermissionDenied
                    return@launch
                }

                currentLat = location.latitude
                currentLon = location.longitude

                val nearbyUsers = userDataStore.getNearbyUsers()
                    .map { user ->
                        user.copy(
                            distance = calculateDistance(
                                currentLat,
                                currentLon,
                                user.latitude,
                                user.longitude
                            ),
                            isOnline = isUserOnline(user.lastSeen)
                        )
                    }
                    .sortedBy { it.distance }

                _uiState.value = if (nearbyUsers.isEmpty()) {
                    NearbyUsersState.NoUsers
                } else {
                    NearbyUsersState.Success(nearbyUsers)
                }
            } catch (e: Exception) {
                _uiState.value = NearbyUsersState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val distance = locationUtils.calculateDistance(lat1, lon1, lat2, lon2)
        return (distance * 10.0).roundToInt() / 10.0 // Round to 1 decimal place
    }

    private fun isUserOnline(lastSeen: Instant): Boolean {
        val now = Instant.now()
        val fiveMinutesAgo = now.minusSeconds(300) // 5 minutes in seconds
        return lastSeen.isAfter(fiveMinutesAgo)
    }
} 
} 