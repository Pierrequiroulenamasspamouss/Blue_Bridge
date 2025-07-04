package com.bluebridgeapp.bluebridge.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bluebridgeapp.bluebridge.R
import com.bluebridgeapp.bluebridge.data.model.WaterNeed


@Composable
fun WaterNeedCard(
    waterNeed: WaterNeed,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon and title in a Row with width constraint
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f) // This ensures the title takes only available space
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = stringResource(R.string.water_drop_icon_desc),
                        tint = colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        // Truncate long titles
                        val displayUsageType = if (waterNeed.usageType.length > 15) {
                            waterNeed.usageType.take(15) + stringResource(R.string.ellipsis)
                        } else {
                            waterNeed.usageType
                        }

                        Text(
                            text = stringResource(R.string.water_need_liters_usage_type, waterNeed.amount, displayUsageType),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = stringResource(R.string.priority_label, waterNeed.priority),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (waterNeed.description.isNotBlank()) {
                            Text(
                                text = waterNeed.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Fixed-width action buttons
                Row(
                    modifier = Modifier.width(100.dp), // Fixed width for buttons
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_button_desc),
                            tint = colorScheme.primary
                        )
                    }

                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete_button_desc),
                            tint = colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

