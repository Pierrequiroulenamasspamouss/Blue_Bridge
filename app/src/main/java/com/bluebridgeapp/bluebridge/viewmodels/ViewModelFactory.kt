package com.bluebridgeapp.bluebridge.viewmodels

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bluebridgeapp.bluebridge.data.interfaces.NearbyUsersRepository
import com.bluebridgeapp.bluebridge.data.interfaces.ServerRepository
import com.bluebridgeapp.bluebridge.data.interfaces.UserRepository
import com.bluebridgeapp.bluebridge.data.interfaces.WellRepository
import com.bluebridgeapp.bluebridge.data.repository.WeatherRepository
import com.bluebridgeapp.bluebridge.network.SmsApi


sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
    object Empty : UiState<Nothing>()
}

class ViewModelFactory(
    private val context: Context,
    private val userRepository: UserRepository,
    private val wellRepository: WellRepository,
    private val nearbyUsersRepository: NearbyUsersRepository,
    private val weatherRepository: WeatherRepository,
    private val serverRepository: ServerRepository,
    private val smsApi: SmsApi

) : ViewModelProvider.Factory {

    @RequiresApi(Build.VERSION_CODES.O)
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(UserViewModel::class.java) -> {
                UserViewModel(
                    repository = userRepository,
                ) as T
            }

            modelClass.isAssignableFrom(WellViewModel::class.java) -> {
                WellViewModel(wellRepository ,context) as T
            }

            modelClass.isAssignableFrom(NearbyUsersViewModel::class.java) -> {
                NearbyUsersViewModel(nearbyUsersRepository) as T
            }

            modelClass.isAssignableFrom(WeatherViewModel::class.java) -> {
                WeatherViewModel(
                    userRepository = userRepository,
                    context = context,
                ) as T
            }

            modelClass.isAssignableFrom(ServerViewModel::class.java) -> {
                ServerViewModel(
                    serverRepository,
                    context = context,
                ) as T
            }

            modelClass.isAssignableFrom(SmsViewModel::class.java) -> {
                SmsViewModel(smsApi) as T
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

