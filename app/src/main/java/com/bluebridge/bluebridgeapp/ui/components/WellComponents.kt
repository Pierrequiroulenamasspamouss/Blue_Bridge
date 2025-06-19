package com.bluebridge.bluebridgeapp.ui.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bluebridge.bluebridgeapp.data.model.WellData
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun WellCard(
    well: WellData,
    isWellOwner: Boolean = false,
    showAdminActions: Boolean = false,
    showLastRefresh: Boolean = false,
    showLastUpdate: Boolean = false,
    onEdit: () -> Unit = {},
    onItemClick: (String) -> Unit = {},
    onNavigate: () -> Unit = {},
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header Row: Title + Admin Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = well.wellName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (isWellOwner || showAdminActions) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isWellOwner) {
                            IconButton(onClick = onEdit) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Well"
                                )
                            }
                        }

                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Well",
                                tint = if (showAdminActions) MaterialTheme.colorScheme.error else LocalContentColor.current
                            )
                        }
                    }
                }
            }

            // Status or Quality Info
            Spacer(modifier = Modifier.height(8.dp))

            well.wellStatus?.let {
                Text(
                    text = "Status: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } ?: run {
                Text(
                    text = "Quality: ${well.waterQuality}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Water Level Info
            val waterLevel = if (well.wellCapacity.toInt() > 0) {
                (well.wellWaterLevel.toFloat() / well.wellCapacity.toFloat()).coerceIn(0f, 1f)
            } else 0f
            val waterLevelPercentage = (waterLevel * 100).toInt()

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { waterLevel },
                modifier = Modifier
                    .height(8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${well.wellWaterLevel} L",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "$waterLevelPercentage%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (showLastRefresh || showLastUpdate) {
                Spacer(modifier = Modifier.height(8.dp))
                if (showLastRefresh) {
                    Text(
                        text = "Last Refreshed: ${well.lastRefreshTime?.let { formatDateTime(it.toString()) } ?: "Never"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (showLastUpdate) {
                    Text(
                        text = "Last Update: ${well.lastUpdated?.let { formatDateTime(it) } ?: "Never"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onItemClick(well.id.toString()) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = "Details"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("GO")
            }
        }
    }
}

@Composable
fun EnhancedWellCard(
    well: WellData,
    onClick: () -> Unit,
    onNavigateClick: () -> Unit
) {
    // Calculate water level percentage safely outside composable
    val waterLevelInfo = if (well.wellWaterLevel.toString().isNotBlank() && well.wellCapacity.isNotBlank()) {
        calculateWaterLevelInfo(well.wellWaterLevel, well.wellCapacity)
    } else null

    Card(
        Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(well.wellName, style = MaterialTheme.typography.titleMedium)
                StatusIndicator(well.wellStatus)
            }

            Text(text = "Latitude: ${well.wellLocation.latitude}\n Longitude: ${well.wellLocation.longitude}" , style = MaterialTheme.typography.bodySmall)

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Type: ${well.wellWaterType}", style = MaterialTheme.typography.bodySmall)

                if (well.wellCapacity.isNotBlank()) {
                    Text("Capacity: ${well.wellCapacity}L", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Water level indicator using pre-calculated values
            waterLevelInfo?.let { (progress, percentage) ->
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    color = when {
                        percentage > 70 -> Color.Green
                        percentage > 30 -> Color.Yellow
                        else -> Color.Red
                    }
                )
                Text("Water Level: $percentage%", style = MaterialTheme.typography.bodySmall)
            }

            // Navigate button
            Button(
                onClick = onNavigateClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Navigate there")
            }
        }
    }
}

private fun formatDateTime(dateTime: String): String {
    return try {
        // Assuming the input dateTime string is in ISO 8601 format like "2023-10-27T10:15:30Z"
        // Adjust the inputFormat pattern if your dateTime string format is different.
        // Use SimpleDateFormat for compatibility with API 25 and earlier.
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'") // Adjust if needed
        inputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC") // Assuming input is UTC
        val date: Date? = inputFormat.parse(dateTime)
        val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm")
        outputFormat.timeZone = java.util.TimeZone.getDefault() // Format to local time zone
        date?.let { outputFormat.format(it) } ?: dateTime
    } catch (e: Exception) {
        Log.e("DateTimeFormat", "Error formatting date: $dateTime", e)
        dateTime
    }
}

