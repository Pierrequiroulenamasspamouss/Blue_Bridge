package com.wellconnect.wellmonitoring.data.model

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
    val rainAmount: Double
) 