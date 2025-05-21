package com.bluebridge.bluebridgeapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterUsageTypeSelector(
    currentWaterNeedType: String,
    currentPriority: Int,
    onWaterNeedTypeChange: (String) -> Unit,
    onPriorityChange: (Int) -> Unit,
    customType: String,
    onCustomTypeChange: (String) -> Unit
) {
    val usageTypes = listOf("Absolute emergency", "Medical", "Drinking", "Farming", "Industry", "Other")
    var expanded by remember { mutableStateOf(false) }
    var showPrioritySelector by remember { mutableStateOf(false) }

    Column {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = currentWaterNeedType,
                onValueChange = {},
                label = { Text("Usage Type") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                usageTypes.forEach { usageType ->
                    DropdownMenuItem(
                        text = { Text(text = usageType) },
                        onClick = {
                            onWaterNeedTypeChange(usageType)
                            // Automatically set priority based on type
                            val priority = when (usageType) {
                                "Absolute emergency" -> 0
                                "Medical" -> 1
                                "Drinking" -> 2
                                "Farming" -> 3
                                "Industry" -> 4
                                else -> 6 // Default for "Other"
                            }
                            onPriorityChange(priority)
                            showPrioritySelector = usageType == "Other"
                            expanded = false
                        }
                    )
                }
            }
        }

        // If "Other" is selected, show custom type field and priority selector
        if (currentWaterNeedType == "Other") {
            OutlinedTextField(
                value = customType,
                onValueChange = { onCustomTypeChange(it) },
                label = { Text("Custom Usage Type") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (showPrioritySelector) {
                PrioritySelector(
                    currentPriority = currentPriority,
                    onPriorityChange = onPriorityChange
                )
            }
        }
    }
}

@Composable
fun PrioritySelector(
    currentPriority: Int,
    onPriorityChange: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = "Priority for Other Type",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(0, 1, 2, 3, 4, 5, 6).forEach { priority ->
                FilterChip(
                    selected = currentPriority == priority,
                    onClick = { onPriorityChange(priority) },
                    label = { Text("P$priority") },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }
    }
}