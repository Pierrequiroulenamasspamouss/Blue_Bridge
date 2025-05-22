package com.bluebridge.bluebridgeapp.ui.components

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bluebridge.bluebridgeapp.data.model.WellData
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun WellCard(
    well: WellData,
    isWellOwner: Boolean = false,
    showAdminActions: Boolean = false,
    showLastRefresh: Boolean = false,
    showLastUpdate: Boolean = false,
    onDelete: () -> Unit = {},
    onEdit: () -> Unit = {},
    onItemClick: (String) -> Unit = {},
    onViewDetails: () -> Unit = {},
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetails() },
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

                        IconButton(onClick = onDelete) {
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
                Text("Details")
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun formatDateTime(dateTime: String): String {
    return try {
        val instant = Instant.parse(dateTime)
        val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
        localDateTime.format(formatter)
    } catch (e: Exception) {
        Log.e("DateTimeFormat", "Error formatting date", e)
        dateTime
    }
}
