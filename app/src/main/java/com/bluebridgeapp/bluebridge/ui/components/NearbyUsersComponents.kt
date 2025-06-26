package com.bluebridgeapp.bluebridge.ui.components

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bluebridgeapp.bluebridge.data.model.NearbyUser

// Helper function to format doubles with specified decimal places
fun Double.format(digits: Int) = "%.${digits}f".format(this)

@SuppressLint("DefaultLocale")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NearbyUserCard(
    user: NearbyUser,
) {
    // Calculate if user is online based on lastActive timestamp
    val isUserOnline = remember(user.lastActive) {
        if (user.lastActive.isBlank()) {
            false
        } else {
            try {
                // Consider user online if lastActive is within the last 5 minutes
                val lastActiveTime = java.time.Instant.parse(user.lastActive).toEpochMilli()
                val currentTime = System.currentTimeMillis()
                val fiveMinutesInMillis = 5 * 60 * 1000L
                (currentTime - lastActiveTime) < fiveMinutesInMillis
            } catch (_: Exception) {
                false
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isUserOnline) Color.Green else Color.Gray
                    )
                    Column {
                        Text(
                            text = "${user.firstName} ${user.lastName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${String.format("%.2f", user.distance)} km away",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Display water needs
            if (user.waterNeeds.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Water Needs:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    user.waterNeeds.forEach { need ->
                        Text(
                            text = "${need.usageType}: ${need.amount}L",
                            style = MaterialTheme.typography.bodySmall
                        )
                        PriorityChip(priority = need.priority)
                    }
                }
            }
        }
    }
}

@Composable
fun PriorityChip(priority: Int) {
    val color = when (priority) {
        0 -> Color(red = 1.0f, green = 0.2f, blue = 0.2f)
        1 -> Color(red = 0.6f, green = 0.2f, blue = 0.2f)
        2 -> Color(red = 0.5f, green = 0.4f, blue = 0.2f)
        3 -> Color(red = 0.5f, green = 0.5f, blue = 0.2f)
        4 -> Color(red = 0.2f, green = 0.4f, blue = 0.2f)
        5 -> Color(red = 0.2f, green = 0.6f, blue = 0.2f)
        else -> MaterialTheme.colorScheme.outline
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(4.dp)
    ) {
        Text(
            text = "P$priority",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = color,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No nearby users found",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun LocationPermissionDeniedMessage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Location permission is required to find nearby users",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
    }
}
