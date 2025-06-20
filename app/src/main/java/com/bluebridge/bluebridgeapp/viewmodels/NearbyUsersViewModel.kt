package com.bluebridge.bluebridgeapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluebridge.bluebridgeapp.data.NearbyUserEvent
import com.bluebridge.bluebridgeapp.data.`interface`.NearbyUsersRepository
import com.bluebridge.bluebridgeapp.data.model.NearbyUsersState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NearbyUsersViewModel(
    private val repository: NearbyUsersRepository
) : ViewModel() {

    private val _state = MutableStateFlow<NearbyUsersState>(NearbyUsersState.Loading)
    val state: StateFlow<NearbyUsersState> = _state

    private var currentLatitude = 0.0
    private var currentLongitude = 0.0
    private var currentRadius = 50.0

    fun handleEvent(event: NearbyUserEvent) {
        when (event) {
            is NearbyUserEvent.SearchUser -> searchUsers(event.latitude, event.longitude, event.radius)
            is NearbyUserEvent.UpdateRadius -> updateRadius(event.radius)
            is NearbyUserEvent.ApplyFilters -> applyFilters(event.filters)
            is NearbyUserEvent.ResetFilters -> resetFilters()
            is NearbyUserEvent.Refresh -> refreshUsers()
        }
    }

    private fun searchUsers(latitude: Double, longitude: Double, radius: Double) {
        currentLatitude = latitude
        currentLongitude = longitude
        currentRadius = radius

        viewModelScope.launch {
            _state.value = NearbyUsersState.Loading
            try {
                val result = repository.getNearbyUsers(latitude, longitude, radius)
                if (result.isSuccess) {
                    _state.value = NearbyUsersState.Success(result.getOrNull() ?: emptyList())
                } else {
                    _state.value = NearbyUsersState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to fetch users"
                    )
                }
            } catch (e: Exception) {
                _state.value = NearbyUsersState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun refreshUsers() {
        searchUsers(currentLatitude, currentLongitude, currentRadius)
    }

    private fun updateRadius(radius: Double) {
        currentRadius = radius
        searchUsers(currentLatitude, currentLongitude, radius)
    }

    private fun applyFilters(filters: Map<String, String>) {
        viewModelScope.launch {
            _state.value = NearbyUsersState.Loading
            try {
                val filteredUsers = repository.applyFilters(filters)
                _state.value = NearbyUsersState.Success(filteredUsers)
            } catch (e: Exception) {
                _state.value = NearbyUsersState.Error("Failed to apply filters: ${e.message}")
            }
        }
    }

    private fun resetFilters() {
        searchUsers(currentLatitude, currentLongitude, currentRadius)
    }
}