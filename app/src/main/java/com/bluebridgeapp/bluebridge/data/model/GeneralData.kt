package com.bluebridgeapp.bluebridge.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Location(
    val latitude: Double,
    val longitude: Double,
    val lastUpdated: String = "never"
)

@Serializable
data class MovementSpeeds(
    val walkingSpeed: Double = 6.0, // km/h
    val drivingSpeed: Double = 70.0 // km/h
)

@Serializable
data class WaterNeed(
    val amount: Float,
    val usageType: String,
    val description: String,
    val priority: Int
)

@Serializable
data class WaterQuality(
    val ph: Double = 7.0,
    val turbidity: Double = 0.0,
    val tds: Int = 0
)