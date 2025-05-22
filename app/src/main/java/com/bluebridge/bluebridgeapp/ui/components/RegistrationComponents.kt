package com.bluebridge.bluebridgeapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bluebridge.bluebridgeapp.data.model.WaterNeed

@Composable
fun WaterNeedsSection(
    showWaterNeeds: Boolean,
    onToggle: () -> Unit,
    waterNeeds: List<WaterNeed>,
    amount: String,
    onAmountChange: (String) -> Unit,
    type: String,
    onTypeChange: (String) -> Unit,
    desc: String,
    onDescChange: (String) -> Unit,
    priority: Int,
    onPriorityChange: (Int) -> Unit,
    customType: String,
    onCustomTypeChange: (String) -> Unit
) {
    Column {
        Button(
            onClick = onToggle,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (showWaterNeeds) "Hide Water Needs" else "Add Water Needs (Optional)")
        }

        if (showWaterNeeds) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.isEmpty() || it.all(Char::isDigit)) onAmountChange(it) },
                    label = { Text("Amount (liters)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                WaterUsageTypeSelector(
                    currentWaterNeedType = type,
                    onWaterNeedTypeChange = onTypeChange,
                    currentPriority = priority,
                    onPriorityChange = onPriorityChange,
                    customType = customType,
                    onCustomTypeChange = onCustomTypeChange
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = onDescChange,
                    label = { Text("Description (Optional)") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val amountValue = amount.toIntOrNull()
                        if (amountValue != null && amountValue > 0) {
                            val finalPriority = when (type) {
                                "Absolute emergency" -> 0
                                "Medical" -> 1
                                "Drinking" -> 2
                                "Farming" -> 3
                                "Industry" -> 4
                                else -> priority
                            }

                            val newNeed = WaterNeed(
                                amount = amountValue,
                                usageType = if (type == "Other") customType else type,
                                description = desc,
                                priority = finalPriority
                            )

                            onAmountChange("")
                            onTypeChange("")
                            onDescChange("")
                            onCustomTypeChange("")
                            onPriorityChange(6)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Water Need")
                }
            }
        }

        if (waterNeeds.isNotEmpty()) {
            Text(
                text = "Your Water Needs",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                waterNeeds.forEach { need ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Amount: ${need.amount} liters", style = MaterialTheme.typography.bodyMedium)
                            Text("Type: ${need.usageType}", style = MaterialTheme.typography.bodySmall)
                            Text("Priority: P${need.priority}", style = MaterialTheme.typography.bodySmall)
                            if (need.description.isNotBlank()) {
                                Text("Description: ${need.description}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

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