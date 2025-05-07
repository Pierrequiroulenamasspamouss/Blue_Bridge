package com.wellconnect.wellmonitoring.viewmodels

import android.content.Context
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wellconnect.wellmonitoring.data.`interface`.UserRepository
import com.wellconnect.wellmonitoring.data.model.WeatherData
import com.wellconnect.wellmonitoring.network.RetrofitBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class WeatherViewModel(private val userRepository: UserRepository) : ViewModel() {
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
     * Use the device's location
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun useDeviceLocation(context: Context, latitude: Double, longitude: Double) {
        setLocation(latitude, longitude)
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
                val email = userRepository.getUserEmail() ?: ""
                val token = userRepository.getAuthToken() ?: ""

                val weatherData = fetchFromServer(
                    currentLocation.latitude, 
                    currentLocation.longitude,
                    email,
                    token
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
        latitude: Double, 
        longitude: Double,
        email: String,
        token: String
    ): List<WeatherData> = withContext(Dispatchers.IO) {
        try {
            // Get the API client
            val api = RetrofitBuilder.getServerApi(userRepository as Context)
            
            // Make the API call
            val response = api.getWeather(latitude, longitude, email, token)
            
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                throw Exception("API call failed: $errorBody")
            }
            
            val responseBody = response.body()
            if (responseBody == null) {
                throw Exception("Empty response from server")
            }
            
            // Check if the status is success
            val status = responseBody["status"] as? String
            if (status != "success") {
                val message = responseBody["message"] as? String ?: "Unknown error"
                throw Exception(message)
            }
            
            // Extract the weather data
            val weatherData = responseBody["data"] as? Map<String, Any> ?: throw Exception("Invalid data format")
            val weatherList = mutableListOf<WeatherData>()
            
            // Create a weather data entry
            val main = weatherData["main"] as? Map<String, Any> ?: throw Exception("Missing main data")
            val weatherArray = weatherData["weather"] as? List<Map<String, Any>> ?: throw Exception("Missing weather data")
            val weatherObj = weatherArray.firstOrNull() ?: throw Exception("Empty weather array")
            val wind = weatherData["wind"] as? Map<String, Any> ?: throw Exception("Missing wind data")
            
            val dateTime = LocalDateTime.now()
            val date = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val time = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
            
            val data = WeatherData(
                date = date,
                time = time,
                temperature = (main["temp"] as? Number)?.toDouble() ?: 0.0,
                feelsLike = (main["feels_like"] as? Number)?.toDouble() ?: 0.0,
                minTemperature = (main["temp_min"] as? Number)?.toDouble() ?: 0.0,
                maxTemperature = (main["temp_max"] as? Number)?.toDouble() ?: 0.0,
                humidity = (main["humidity"] as? Number)?.toInt() ?: 0,
                description = (weatherObj["description"] as? String) ?: "",
                icon = "https://openweathermap.org/img/wn/${weatherObj["icon"] as? String ?: ""}@2x.png",
                windSpeed = (wind["speed"] as? Number)?.toDouble() ?: 0.0,
                rainAmount = 0.0 // Not available in current weather
            )
            
            weatherList.add(data)
            
            if (weatherData.containsKey("cachedAt")) {
                // Add info about cache timestamp if available
                Log.d("WeatherViewModel", "Weather data cached at: ${weatherData["cachedAt"]}")
            }
            
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