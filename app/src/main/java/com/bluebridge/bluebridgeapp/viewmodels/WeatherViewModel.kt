package com.bluebridge.bluebridgeapp.viewmodels

import android.content.Context
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluebridge.bluebridgeapp.data.interfaces.UserRepository
import com.bluebridge.bluebridgeapp.data.model.WeatherData
import com.bluebridge.bluebridgeapp.data.model.WeatherRequest
import com.bluebridge.bluebridgeapp.network.RetrofitBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.bluebridge.bluebridgeapp.data.model.Location as LocationData
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val Context.weatherDataStore by preferencesDataStore(name = "weather_cache")
private val WEATHER_KEY = stringPreferencesKey("weather_data")

@RequiresApi(Build.VERSION_CODES.O)
class WeatherViewModel(
    private val userRepository: UserRepository,
    private val context: Context
) : ViewModel() {
    // Weather state
    private val _weatherState = mutableStateOf<UiState<List<WeatherData>>>(UiState.Empty)
    val weatherState = _weatherState
    
    // Location state
    private val _location = mutableStateOf<Location?>(null)
    val location = _location

    init {
        // Try to load cached weather on init
        viewModelScope.launch {
            loadWeatherFromCache()
        }
    }

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
                    location = LocationData(currentLocation.latitude, currentLocation.longitude),
                    userId = userRepository.getUserId(),
                    loginToken = token
                )
                _weatherState.value = UiState.Success(weatherData)
                saveWeatherToCache(weatherData)
            } catch (e: Exception) {
                // Try to load from cache if server fails
                val loaded = loadWeatherFromCache()
                if (!loaded) {
                    _weatherState.value = UiState.Error("Failed to load weather data: ${e.message}\nNo offline data available.")
                } else {
                    _weatherState.value = UiState.Error("Failed to update weather: ${e.message}\nShowing offline data.")
                }
                if (e.message == "Invalid loginToken") {
                    userRepository.logout()
                }
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
                if (errorBody.contains("Invalid loginToken")) {
                    userRepository.logout()
                    Log.e("WeatherViewModel", "Invalid loginToken, logging out")
                }
                throw Exception("API call failed: $errorBody")

            }

            val responseBody = response.body()
            if (responseBody == null) {
                throw Exception("Empty response from server")
            }

            // Check if the status is success
            val status = responseBody.status
            if (status != "success") {
                val message = responseBody.message
                throw Exception(message)
            }

            // Extract the weather data
            val weatherData = responseBody.data



            return@withContext weatherData
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

    private suspend fun saveWeatherToCache(weatherList: List<WeatherData>) {
        val json = Json.encodeToString(weatherList)
        context.weatherDataStore.edit { prefs ->
            prefs[WEATHER_KEY] = json
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun loadWeatherFromCache(): Boolean {
        val prefs = context.weatherDataStore.data.firstOrNull() ?: return false
        val json = prefs[WEATHER_KEY] ?: return false
        return try {
            val weatherList = Json.decodeFromString<List<WeatherData>>(json)
            // Only show if the latest date is today or newer
            val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
            val validWeather = weatherList.filter { it.date >= today }
            if (validWeather.isNotEmpty()) {
                _weatherState.value = UiState.Success(validWeather)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
} 