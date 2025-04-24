package com.wellconnect.wellmonitoring.data

import kotlinx.serialization.Serializable

@Serializable
data class WaterNeeds(
    val amount: Double, // Daily water needs in liters
    val use: String,   // Purpose: "farming", "drinking", "livestock", etc.
    val priority: Int  // 1 to 5, where 1 is highest priority
)

@Serializable
data class UserData(
    val email: String,
    val firstName: String,
    val lastName: String,
    val username: String,
    val role: String,
    val themePreference: Int = 0, // 0: System Default, 1: Light, 2: Dark
    val latitude: Double? = null,
    val longitude: Double? = null,
    val waterNeeds: List<WaterNeeds> = emptyList()
) 