package com.bluebridge.bluebridgeapp.data.model

import kotlinx.serialization.Serializable

/**
 * Data class representing weather forecast data
 */
@Serializable
data class WeatherData(
    val date: String,
    val time: String,
    val temperature: Double,
    val feelsLike: Double,
    val minTemperature: Double,
    val maxTemperature: Double,
    val humidity: Int,
    val description: String,
    val icon: String,
    val windSpeed: Double,
    val rainAmount: Double,
    val pressure: Int,
    val windDirection: Int,
    val sunset: String,
)

@Serializable
data class WeatherRequest(
    val location: Location,
    val userId: String,
    val loginToken: String,
)
@Serializable
data class WeatherResponse(
    val status: String,
    val message: String,
    val data: WeatherData,
)
