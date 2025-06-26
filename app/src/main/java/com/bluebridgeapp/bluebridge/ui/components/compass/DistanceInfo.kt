package com.bluebridgeapp.bluebridge.ui.components.compass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bluebridgeapp.bluebridge.utils.formatDistance

/**
 * Displays distance information and estimated travel times
 */
@Composable
fun DistanceInfo(
    distance: Float,
    isInTargetZone: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Target zone indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (isInTargetZone) Color(0xFF4CAF50) else Color(0xFFE57373),
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isInTargetZone) "On Target!" else "Turn to Target",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            
            // Distance information
            Text(
                text = "Distance to Target",
                style = MaterialTheme.typography.titleMedium
            )
            
            Text(
                text = formatDistance(distance),
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 32.sp
            )
            
            // Travel information
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Walking",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = calculateWalkingTime(distance),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Driving",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = calculateDrivingTime(distance),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}



fun calculateWalkingTime(distanceInMeters: Float): String {
    // Walking speed: 5 km/h = 1.389 m/s
    val walkingSpeedMS = 1.389
    val timeSeconds = distanceInMeters / walkingSpeedMS
    
    return formatTime(timeSeconds)
}

fun calculateDrivingTime(distanceInMeters: Float): String {
    // Average driving speed: 40 km/h = 11.111 m/s (considering urban areas)
    val drivingSpeedMS = 11.111
    val timeSeconds = distanceInMeters / drivingSpeedMS
    
    return formatTime(timeSeconds)
}

fun formatTime(timeSeconds: Double): String {
    return when {
        timeSeconds < 60 -> "${timeSeconds.toInt()} seconds"
        timeSeconds < 3600 -> "${(timeSeconds / 60).toInt()} minutes"
        else -> String.format("%.1f hours", timeSeconds / 3600)
    }
} 