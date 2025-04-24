package com.wellconnect.wellmonitoring.data

import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class NearbyUser(
    val id: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val latitude: Double,
    val longitude: Double,
    val lastSeen: Instant,
    val distance: Double = 0.0,
    val isOnline: Boolean = false
)

@Serializable
data class WaterNeeds(
    val amount: Double, // in liters
    val priority: Priority,
    val description: String? = null
)

enum class Priority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
} 