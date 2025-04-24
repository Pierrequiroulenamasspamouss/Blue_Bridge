package com.wellconnect.wellmonitoring.data

import kotlinx.serialization.Serializable

@Serializable
data class Well(
    val id: Int,
    val name: String,
    val description: String,
    val status: String,
    val waterLevel: Double,
    val lastUpdate: String,
    val latitude: Double,
    val longitude: Double
) 