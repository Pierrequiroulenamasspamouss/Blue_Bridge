package com.bluebridge.bluebridgeapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bluebridge.bluebridgeapp.data.`interface`.NearbyUsersRepository
import com.bluebridge.bluebridgeapp.data.`interface`.UserRepository
import com.bluebridge.bluebridgeapp.data.`interface`.WellRepository
import com.bluebridge.bluebridgeapp.data.repository.ServerRepository

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
    object Empty : UiState<Nothing>()
}

class ViewModelFactory(
    private val userRepository: UserRepository,
    private val wellRepository: WellRepository,
    private val nearbyUsersRepository: NearbyUsersRepository,
    private val serverRepository: ServerRepository // Add this parameter
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(UserViewModel::class.java) -> {
                UserViewModel(userRepository) as T
            }
            modelClass.isAssignableFrom(WellViewModel::class.java) -> {
                WellViewModel(wellRepository) as T
            }
            modelClass.isAssignableFrom(NearbyUsersViewModel::class.java) -> {
                NearbyUsersViewModel(nearbyUsersRepository) as T
            }
            modelClass.isAssignableFrom(WeatherViewModel::class.java) -> {
                WeatherViewModel(userRepository) as T
            }
            modelClass.isAssignableFrom(ServerViewModel::class.java) -> { // Add this case
                ServerViewModel(serverRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}