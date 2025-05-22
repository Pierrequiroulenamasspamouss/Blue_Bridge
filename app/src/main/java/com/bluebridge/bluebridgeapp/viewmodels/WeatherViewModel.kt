package com.bluebridge.bluebridgeapp.viewmodels

import android.content.Context
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluebridge.bluebridgeapp.data.`interface`.UserRepository
import com.bluebridge.bluebridgeapp.data.model.WeatherData
import com.bluebridge.bluebridgeapp.data.model.WeatherRequest
import com.bluebridge.bluebridgeapp.data.repository.WeatherRepository
import com.bluebridge.bluebridgeapp.network.RetrofitBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.bluebridge.bluebridgeapp.data.model.Location as LocationData

class WeatherViewModel(
    private val weatherRepository: WeatherRepository,
    private val userRepository: UserRepository,
    private val context: Context
) : ViewModel() {
    // Weather state
    private val _weatherState = mutableStateOf<UiState<List<WeatherData>>>(UiState.Empty)
    val weatherState = _weatherState
    
    // Location state
    private val _location = mutableStateOf<Location?>(null)
    val location = _location

    /**
     * Set the current location for weather data
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun setLocation(latitude: Double, longitude: Double) {
        val newLocation = Location("manual").apply {
            this.latitude = latitude
            this.longitude = longitude
        }
        _location.value = newLocation
        fetchWeatherForecast()
    }


    /**
     * Fetch the weather forecast from the server
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun fetchWeatherForecast() {
        val currentLocation = _location.value ?: return
        
        _weatherState.value = UiState.Loading
        
        viewModelScope.launch {
            try {

                val token = userRepository.getLoginToken() ?: ""

                val weatherData = fetchFromServer(
                    location = LocationData(currentLocation.latitude, currentLocation.longitude,),
                    userId = userRepository.getUserId(),
                    loginToken = token
                )
                _weatherState.value = UiState.Success(weatherData)
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error fetching weather: ${e.message}", e)
                _weatherState.value = UiState.Error("Failed to load weather data: ${e.message}")
            }
        }
    }
    
    /**
     * Fetch weather data from the server
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun fetchFromServer(
        location: LocationData,
        userId: String,
        loginToken: String
    ): List<WeatherData> = withContext(Dispatchers.IO) {
        try {
            // Get the API client
            val api = RetrofitBuilder.getServerApi(context)

            val request = WeatherRequest(
                location = location,
                userId = userId,
                loginToken = loginToken,
            )
            // Make the API call
            val response = api.getWeather(request)
            
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                throw Exception("API call failed: $errorBody")
            }
            
            val responseBody = response.body()
            if (responseBody == null) {
                throw Exception("Empty response from server")
            }
            
            // Check if the status is success
            val status = responseBody.status
            if (status != "success") {
                val message = responseBody.message ?: "Unknown error"
                throw Exception(message)
            }
            
            // Extract the weather data
            val weatherData = responseBody.data
                ?: throw Exception("Invalid data format or 'data' field missing")

            val weatherList = mutableListOf<WeatherData>()
            weatherList.add(weatherData)
            
            return@withContext weatherList
        } catch (e: Exception) {
            Log.e("WeatherViewModel", "Error fetching weather from API: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Refresh the weather data
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun refreshWeather() {
        fetchWeatherForecast()
    }

} 