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
import com.bluebridge.bluebridgeapp.network.RetrofitBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date

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
            val parsedData = parseWeatherData(weatherData)
            
            weatherList.add(parsedData)
            
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun parseWeatherData(data: Map<String, Any>): WeatherData {
        val main = data["main"] as? Map<String, Any> ?: emptyMap()
        val weather = (data["weather"] as? List<Map<String, Any>>)?.firstOrNull() ?: emptyMap()
        val wind = data["wind"] as? Map<String, Any> ?: emptyMap()
        val sys = data["sys"] as? Map<String, Any> ?: emptyMap()
        val rain = data["rain"] as? Map<String, Any> ?: emptyMap()
        val dt = (data["dt"] as? Number)?.toLong() ?: Instant.now().epochSecond
        val date = Date(dt * 1000)

        return WeatherData(
            temperature = (main["temp"] as? Number)?.toDouble() ?: 0.0,
            feelsLike = (main["feels_like"] as? Number)?.toDouble() ?: 0.0,
            minTemperature = (main["temp_min"] as? Number)?.toDouble() ?: 0.0,
            maxTemperature = (main["temp_max"] as? Number)?.toDouble() ?: 0.0,
            humidity = (main["humidity"] as? Number)?.toInt() ?: 0,
            description = (weather["description"] as? String) ?: "",
            icon = (weather["icon"] as? String) ?: "",
            windSpeed = (wind["speed"] as? Number)?.toDouble() ?: 0.0,
            pressure = (main["pressure"] as? Number)?.toInt() ?: 0,
            windDirection = (wind["deg"] as? Number)?.toInt() ?: 0,
            sunset = (sys["sunset"] as? Number)?.toLong()?.let { Date(it * 1000) } ?: Date(),
            date = SimpleDateFormat("yyyy-MM-dd").format(date),
            time = SimpleDateFormat("HH:mm").format(date),
            rainAmount = (rain["1h"] as? Number)?.toDouble() ?: 0.0
        )
    }
} 