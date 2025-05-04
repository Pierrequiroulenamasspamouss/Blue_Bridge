package com.wellconnect.wellmonitoring.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wellconnect.wellmonitoring.data.`interface`.NearbyUsersRepository
import com.wellconnect.wellmonitoring.data.`interface`.UserRepository
import com.wellconnect.wellmonitoring.data.`interface`.WellRepository

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
    object Empty : UiState<Nothing>()
}

class ViewModelFactory(
    private val userRepository: UserRepository,
    private val wellRepository: WellRepository,
    private val nearbyUsersRepository: NearbyUsersRepository
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
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}