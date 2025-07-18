package com.bluebridgeapp.bluebridge.ui.components

import android.util.Base64
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bluebridgeapp.bluebridge.R
import com.bluebridgeapp.bluebridge.data.model.ImageData
import com.bluebridgeapp.bluebridge.data.model.ShortenedWellData
import com.bluebridgeapp.bluebridge.data.model.WellData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.io.encoding.ExperimentalEncodingApi

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
    // Safe property access with fallbacks
    val wellName = well.wellName ?: "Unnamed Well"
    val wellStatus = well.wellStatus ?: "Unknown"
    val capacity = well.wellCapacity?.toFloatOrNull() ?: 0f
    val waterLevel = well.wellWaterLevel?.toFloatOrNull() ?: 0f
    val waterLevelRatio = if (capacity > 0) (waterLevel / capacity).coerceIn(0f, 1f) else 0f
    val waterLevelPercent = (waterLevelRatio * 100).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row - Safe with fallback name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = wellName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (isWellOwner || showAdminActions) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isWellOwner) {
                            IconButton(onClick = onEdit) {
                                Icon(Icons.Default.Edit, stringResource(R.string.edit_well))
                            }
                        }
                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                Icons.Default.Delete,
                                stringResource(R.string.delete_well),
                                tint = if (showAdminActions) MaterialTheme.colorScheme.error
                                else LocalContentColor.current
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Safe status display
            Text(
                text = stringResource(R.string.status_label, wellStatus),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            // Water level indicator - safe with calculated values
            LinearProgressIndicator(
                progress = { waterLevelRatio },
                modifier = Modifier
                    .height(8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.water_level_liters, waterLevel.toString()),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = stringResource(R.string.water_level_percentage, waterLevelPercent),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Safe time displays
            well.lastRefreshTime?.let { refreshTime ->
                if (showLastRefresh && refreshTime > 0) {
                    val date = Date(refreshTime)
                    val formatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date)
                    Text(
                        text = stringResource(R.string.last_refreshed_label, formatted),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            well.lastUpdated?.let { updated ->
                if (showLastUpdate) {
                    Text(
                        text = stringResource(R.string.last_update_label, formatDateTime(updated)),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { well.wellId?.let(onItemClick) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Navigation, stringResource(R.string.details))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.go_button))
            }
        }
    }
}

@Composable
fun EnhancedWellCard(
    well: ShortenedWellData,
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

            Text(
                text = stringResource(
                    R.string.location_coordinates,
                    well.wellLocation.latitude,
                    well.wellLocation.longitude
                ), style = MaterialTheme.typography.bodySmall
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.type_label, well.wellWaterType), style = MaterialTheme.typography.bodySmall)

                if (well.wellCapacity.isNotBlank()) {
                    Text(stringResource(R.string.capacity_liters, well.wellCapacity), style = MaterialTheme.typography.bodySmall)
                }
            }

            // Water level indicator using pre-calculated values
            waterLevelInfo?.let { (progress, percentage) ->
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    color = when {
                        percentage > 70 -> Color.Green
                        percentage > 30 -> Color.Yellow
                        else -> Color.Red
                    },
                )
                Text(
                    stringResource(R.string.water_level_percentage_label, percentage),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Navigate button
            Button(
                onClick = onNavigateClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.navigate_there))
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
        Log.e("DateTimeFormat", "Error formatting date: $dateTime", e) // Log with specific error message
        dateTime
    }
}
@OptIn(ExperimentalEncodingApi::class)
@Composable
fun WellImage(
    imageData: ImageData?,
    modifier: Modifier = Modifier,
    loadingContent: @Composable () -> Unit = { CircularProgressIndicator() },
    errorContent: @Composable () -> Unit = { Text("Invalid image", color = Color.Red) },
    emptyContent: @Composable () -> Unit = { Text("No image available") }
) {
    val bitmap = remember(imageData) {
        try {
            imageData?.base64encodedImage?.let { base64 ->
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        } catch (e: Exception) {
            null
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when {
            imageData == null -> emptyContent()
            bitmap == null -> errorContent()
            else -> {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = imageData.description,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

