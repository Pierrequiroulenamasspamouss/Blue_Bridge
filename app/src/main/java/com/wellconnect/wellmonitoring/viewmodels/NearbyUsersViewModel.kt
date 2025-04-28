package com.wellconnect.wellmonitoring.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellconnect.wellmonitoring.data.NearbyUser
import com.wellconnect.wellmonitoring.data.UserDataStoreImpl
import com.wellconnect.wellmonitoring.data.WaterNeed
import com.wellconnect.wellmonitoring.network.RetrofitBuilder
import com.wellconnect.wellmonitoring.network.UpdateLocationRequest
import com.wellconnect.wellmonitoring.network.UpdateWaterNeedsRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.InternalSerializationApi

sealed class NearbyUsersState {
    object Loading : NearbyUsersState()
    data class Success(val users: List<NearbyUser>) : NearbyUsersState()
    data class Error(val message: String) : NearbyUsersState()
    object NoUsers : NearbyUsersState()
    object LocationPermissionDenied : NearbyUsersState()
}

class NearbyUsersViewModel(context: Context) : ViewModel() {
    private val userDataStore = UserDataStoreImpl(context)
    private val _uiState = MutableStateFlow<NearbyUsersState>(NearbyUsersState.Loading)
    val uiState: StateFlow<NearbyUsersState> = _uiState

    private val api = RetrofitBuilder.create("http://192.168.0.98:8090")

    @OptIn(InternalSerializationApi::class)
    fun refreshNearbyUsers(latitude: Double, longitude: Double, radius: Double = 50.0) {
        viewModelScope.launch {
            _uiState.value = NearbyUsersState.Loading
            try {
                val userData = userDataStore.getUserData().first()
                if (userData == null) {
                    _uiState.value = NearbyUsersState.Error("User not logged in")
                    return@launch
                }

                val response = api.getNearbyUsers(
                    latitude = latitude,
                    longitude = longitude,
                    radius = radius,
                    email = userData.email
                )

                if (response.isSuccessful && response.body() != null) {
                    val nearbyUsers = response.body()!!.users
                    if (nearbyUsers.isEmpty()) {
                        _uiState.value = NearbyUsersState.NoUsers
                    } else {
                        _uiState.value = NearbyUsersState.Success(nearbyUsers)
                    }
                } else {
                    _uiState.value = NearbyUsersState.Error(
                        response.errorBody()?.string() ?: "Unknown error"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = NearbyUsersState.Error(e.message ?: "Unknown error")
            }
        }
    }

    @OptIn(InternalSerializationApi::class)
    fun updateLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                val userData = userDataStore.getUserData().first()
                if (userData == null) {
                    _uiState.value = NearbyUsersState.Error("User not logged in")
                    return@launch
                }

                val request = UpdateLocationRequest(
                    email = userData.email,
                    latitude = latitude,
                    longitude = longitude
                )

                val response = api.updateLocation(request)
                if (!response.isSuccessful) {
                    _uiState.value = NearbyUsersState.Error(
                        response.errorBody()?.string() ?: "Failed to update location"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = NearbyUsersState.Error(e.message ?: "Unknown error")
            }
        }
    }

    @OptIn(InternalSerializationApi::class)
    fun updateWaterNeeds(waterNeeds: List<WaterNeed>) {
        viewModelScope.launch {
            try {
                val userData = userDataStore.getUserData().first()
                if (userData == null) {
                    _uiState.value = NearbyUsersState.Error("User not logged in")
                    return@launch
                }

                val request = UpdateWaterNeedsRequest(
                    email = userData.email,
                    waterNeeds = waterNeeds
                )

                val response = api.updateWaterNeeds(request)
                if (!response.isSuccessful) {
                    _uiState.value = NearbyUsersState.Error(
                        response.errorBody()?.string() ?: "Failed to update water needs"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = NearbyUsersState.Error(e.message ?: "Unknown error")
            }
        }
    }
} 