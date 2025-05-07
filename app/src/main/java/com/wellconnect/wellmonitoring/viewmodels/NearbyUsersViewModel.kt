package com.wellconnect.wellmonitoring.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellconnect.wellmonitoring.data.NearbyUserEvent
import com.wellconnect.wellmonitoring.data.`interface`.NearbyUsersRepository
import com.wellconnect.wellmonitoring.data.model.NearbyUser
import com.wellconnect.wellmonitoring.data.model.NearbyUsersState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay


class NearbyUsersViewModel(
    private val repository: NearbyUsersRepository
) : ViewModel() {

    // Store the last known list of users for persistence during refreshes
    val lastKnownUsers = mutableListOf<NearbyUser>()

    private val _uiState = MutableStateFlow<NearbyUsersState>(NearbyUsersState.Loading)
    val uiState: StateFlow<NearbyUsersState> = _uiState

    fun handleEvent(event: NearbyUserEvent) {
        when (event) {
            is NearbyUserEvent.SearchUser -> searchUser(event.latitude, event.longitude, event.radius)
            is NearbyUserEvent.UpdateRadius -> updateRadius(event.radius)
            is NearbyUserEvent.ApplyFilters -> applyFilters(event.filters)
            is NearbyUserEvent.ResetFilters -> resetFilters()
            is NearbyUserEvent.Refresh -> refreshNearbyUsers(event.latitude, event.longitude, event.radius)
        }
    }

    fun searchUser(latitude: Double, longitude: Double, radius: Double) {
        viewModelScope.launch {
            _uiState.value = NearbyUsersState.Loading
            try {
                val result = repository.getNearbyUsers(latitude, longitude, radius)
                
                if (result.isSuccess) {
                    val users = result.getOrNull() ?: emptyList()
                    if (users.isNotEmpty()) {
                        // Save successful results
                        lastKnownUsers.clear()
                        lastKnownUsers.addAll(users)
                        _uiState.value = NearbyUsersState.Success(users)
                    } else {
                        _uiState.value = NearbyUsersState.NoUsers
                    }
                } else {
                    val exception = result.exceptionOrNull()
                    _uiState.value = NearbyUsersState.Error(exception?.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                Log.e("NearbyUsersViewModel", "Error in searchUser", e)
                _uiState.value = NearbyUsersState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun updateRadius(radius: Double) {
        viewModelScope.launch {
            _uiState.value = NearbyUsersState.Loading
            try {
                val users = repository.updateRadius(radius)
                if (users.isNotEmpty()) {
                    // Save successful results
                    lastKnownUsers.clear() 
                    lastKnownUsers.addAll(users)
                    _uiState.value = NearbyUsersState.Success(users)
                } else {
                    _uiState.value = NearbyUsersState.NoUsers
                }
            } catch (e: Exception) {
                Log.e("NearbyUsersViewModel", "Error updating radius", e)
                _uiState.value = NearbyUsersState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun applyFilters(filters: Map<String, String>) {
        viewModelScope.launch {
            _uiState.value = NearbyUsersState.Loading
            try {
                val users = repository.applyFilters(filters)
                processUsersList(users)
            } catch (e: Exception) {
                Log.e("NearbyUsersViewModel", "Error applying filters", e)
                _uiState.value = NearbyUsersState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun resetFilters() {
        viewModelScope.launch {
            _uiState.value = NearbyUsersState.Loading
            try {
                val users = repository.resetFilters()
                processUsersList(users)
            } catch (e: Exception) {
                Log.e("NearbyUsersViewModel", "Error resetting filters", e)
                _uiState.value = NearbyUsersState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // Helper method to process list of users and update state
    private fun processUsersList(users: List<NearbyUser>) {
        if (users.isNotEmpty()) {
            // Save successful results
            lastKnownUsers.clear()
            lastKnownUsers.addAll(users)
            _uiState.value = NearbyUsersState.Success(users)
        } else {
            _uiState.value = NearbyUsersState.NoUsers
        }
    }

    // Refresh nearby users using the repository
    fun refreshNearbyUsers(latitude: Double, longitude: Double, radius: Double = 50.0) {
        viewModelScope.launch {
            // Set the state as loading nearby users
            _uiState.value = NearbyUsersState.Loading

            // Set a timeout to ensure we don't get stuck in loading state
            val timeoutJob = viewModelScope.launch {
                kotlinx.coroutines.delay(10000) // 10 second timeout
                if (_uiState.value is NearbyUsersState.Loading) {
                    Log.w("NearbyUsersViewModel", "⚠️ Refresh TIMEOUT - Force updating UI state")
                    // If we're still loading after 10 seconds, show the last known users or empty state
                    if (lastKnownUsers.isNotEmpty()) {
                        _uiState.value = NearbyUsersState.Success(lastKnownUsers)
                    } else {
                        _uiState.value = NearbyUsersState.NoUsers
                    }
                }
            }

            try {
                Log.d("NearbyUsersViewModel", "Refreshing nearby users at ($latitude, $longitude) with radius $radius km")
                val result = repository.getNearbyUsers(latitude, longitude, radius)
                
                // Cancel the timeout job as we got a response
                timeoutJob.cancel()
                
                if (result.isSuccess) {
                    val users = result.getOrNull() ?: emptyList()
                    Log.d("NearbyUsersViewModel", "✅ Refresh SUCCESS! Found ${users.size} nearby users")
                    
                    if (users.isNotEmpty()) {
                        // Save successful results
                        lastKnownUsers.clear()
                        lastKnownUsers.addAll(users)
                        Log.d("NearbyUsersViewModel", "Setting UI state to Success with ${users.size} users")
                        _uiState.value = NearbyUsersState.Success(users)
                    } else {
                        Log.d("NearbyUsersViewModel", "No users found nearby")
                        _uiState.value = NearbyUsersState.NoUsers
                    }
                } else {
                    val exception = result.exceptionOrNull()
                    Log.e("NearbyUsersViewModel", "❌ Error during refresh: ${exception?.message}")
                    _uiState.value = NearbyUsersState.Error(exception?.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                // Cancel the timeout job as we got an exception
                timeoutJob.cancel()
                
                Log.e("NearbyUsersViewModel", "❌ Exception during refresh", e)
                _uiState.value = NearbyUsersState.Error(e.message ?: "Unknown error")
            }
        }
    }
}