package com.bluebridge.bluebridgeapp.data.model

import java.util.Date

/**
 * Data class representing weather forecast data
 */
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
    val sunset: Date
) 