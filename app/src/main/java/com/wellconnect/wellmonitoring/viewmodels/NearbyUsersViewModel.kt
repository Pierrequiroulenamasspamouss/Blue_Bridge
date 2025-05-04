package com.wellconnect.wellmonitoring.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellconnect.wellmonitoring.data.NearbyUserEvent
import com.wellconnect.wellmonitoring.data.`interface`.NearbyUsersRepository
import com.wellconnect.wellmonitoring.data.model.NearbyUser
import com.wellconnect.wellmonitoring.data.model.NearbyUsersState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class NearbyUsersViewModel(
    private val repository: NearbyUsersRepository
) : ViewModel() {

    // Store the last known list of users for persistence during refreshes
    val lastKnownUsers = mutableListOf<NearbyUser>()

    fun handleEvent(event: NearbyUserEvent) {
        when (event) {
            is NearbyUserEvent.SearchUser -> searchUser(event.latitude, event.longitude, event.radius)
            is NearbyUserEvent.UpdateRadius -> updateRadius(event.radius)
            is NearbyUserEvent.ApplyFilters -> applyFilters(event.filters)
            is NearbyUserEvent.ResetFilters -> resetFilters()
            is NearbyUserEvent.Refresh -> refreshNearbyUsers(event.latitude, event.longitude, event.radius)
        }
    }

    private val _uiState = MutableStateFlow<NearbyUsersState>(NearbyUsersState.Loading)
    val uiState: StateFlow<NearbyUsersState> = _uiState

    fun searchUser(latitude: Double, longitude: Double, radius: Double) {
        viewModelScope.launch {

            _uiState.value = NearbyUsersState.Loading
            val result = runCatching { repository.getNearbyUsers(latitude, longitude, radius) }
            val users = result.getOrNull()?.getOrNull() ?: emptyList()
            val exception = result.getOrNull()?.exceptionOrNull() ?: result.exceptionOrNull()
            _uiState.value = when {
                users.isNotEmpty() -> {
                    // Save successful results
                    lastKnownUsers.clear()
                    lastKnownUsers.addAll(users)
                    NearbyUsersState.Success(users)
                }
                exception != null -> NearbyUsersState.Error(exception.message ?: "Unknown error")
                else -> NearbyUsersState.NoUsers
            }
        }
    }

    fun updateRadius(radius: Double) {
        viewModelScope.launch {
            _uiState.value = NearbyUsersState.Loading
            val result = runCatching { repository.updateRadius(radius) }
            // After update, fetch the latest users
            val users = repository.getNearbyUsersFlow()
            users.collect { list ->
                if (list.isNotEmpty()) {
                    // Save successful results
                    lastKnownUsers.clear() 
                    lastKnownUsers.addAll(list)
                    _uiState.value = NearbyUsersState.Success(list)
                } else {
                    _uiState.value = NearbyUsersState.NoUsers
                }
            }
        }
    }

    fun applyFilters(filters: Map<String, String>) {
        viewModelScope.launch {
            _uiState.value = NearbyUsersState.Loading
            repository.applyFilters(filters)
            val users = repository.getNearbyUsersFlow()
            users.collect { list ->
                if (list.isNotEmpty()) {
                    // Save successful results
                    lastKnownUsers.clear()
                    lastKnownUsers.addAll(list)
                    _uiState.value = NearbyUsersState.Success(list)
                } else {
                    _uiState.value = NearbyUsersState.NoUsers
                }
            }
        }
    }
    
    fun resetFilters() {
        viewModelScope.launch {
            _uiState.value = NearbyUsersState.Loading
            repository.resetFilters()
            val users = repository.getNearbyUsersFlow()
            users.collect { list ->
                if (list.isNotEmpty()) {
                    // Save successful results
                    lastKnownUsers.clear()
                    lastKnownUsers.addAll(list)
                    _uiState.value = NearbyUsersState.Success(list)
                } else {
                    _uiState.value = NearbyUsersState.NoUsers
                }
            }
        }
    }

    // Refresh nearby users using the repository
    fun refreshNearbyUsers(latitude: Double, longitude: Double, radius: Double = 50.0) {
        viewModelScope.launch {
            //Set the state as loading nearby users
            _uiState.value = NearbyUsersState.Loading

            val result = runCatching { repository.getNearbyUsers(latitude, longitude, radius) }
            val users = result.getOrNull()?.getOrNull() ?: emptyList()
            val exception = result.getOrNull()?.exceptionOrNull() ?: result.exceptionOrNull()
            _uiState.value = when {
                users.isNotEmpty() -> {
                    // Save successful results
                    lastKnownUsers.clear()
                    lastKnownUsers.addAll(users)
                    NearbyUsersState.Success(users)
                }
                exception != null -> NearbyUsersState.Error(exception.message ?: "Unknown error")
                else -> NearbyUsersState.NoUsers
            }
        }
    }
}