package com.bluebridgeapp.bluebridge.ui.components

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
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bluebridgeapp.bluebridge.R
import com.bluebridgeapp.bluebridge.data.model.WaterNeed

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
    priority: Int?,
    onPriorityChange: (Int) -> Unit,
    customType: String,
    onCustomTypeChange: (String) -> Unit
) {
    val absoluteEmergency = stringResource(R.string.absolute_emergency)
    val medical = stringResource(R.string.medical)
    val drinking = stringResource(R.string.drinking)
    val farming = stringResource(R.string.farming)
    val industry = stringResource(R.string.industry)
    val other = stringResource(R.string.other)

    Column {
        Button(
            onClick = onToggle,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (showWaterNeeds) stringResource(R.string.hide_water_needs) else stringResource(R.string.add_water_needs_optional))
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

                if (priority != null) {
                    WaterUsageTypeSelector(
                        currentWaterNeedType = type,
                        onWaterNeedTypeChange = onTypeChange,
                        currentPriority = priority,
                        onPriorityChange = onPriorityChange,
                        customType = customType,
                        onCustomTypeChange = onCustomTypeChange
                    )
                }

                OutlinedTextField(
                    value = desc,
                    onValueChange = onDescChange,
                    label = { Text(stringResource(R.string.description_optional)) },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val amountValue = amount.toIntOrNull()
                        if (amountValue != null && amountValue > 0) {
                            when (type) {
                                absoluteEmergency -> 0
                                medical -> 1
                                drinking -> 2
                                farming -> 3
                                industry -> 4
                                else -> priority
                            }


                            onAmountChange("")
                            onTypeChange("")
                            onDescChange("")
                            onCustomTypeChange("")
                            onPriorityChange(6)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.add_water_need))
                }
            }
        }

        if (waterNeeds.isNotEmpty()) {
            Text(
                text = stringResource(R.string.your_water_needs),
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
                            Text(stringResource(R.string.amount_liters, need.amount), style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.type_usage, need.usageType), style = MaterialTheme.typography.bodySmall)
                            Text(stringResource(R.string.priority_p, need.priority), style = MaterialTheme.typography.bodySmall)
                            if (need.description.isNotBlank()) {
                                Text(stringResource(R.string.description_desc, need.description), style = MaterialTheme.typography.bodySmall)
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
    val absoluteEmergency = stringResource(R.string.absolute_emergency)
    val medical = stringResource(R.string.medical)
    val drinking = stringResource(R.string.drinking)
    val farming = stringResource(R.string.farming)
    val industry = stringResource(R.string.industry)
    val other = stringResource(R.string.other)

    val usageTypes = listOf(
        absoluteEmergency, medical, drinking, farming, industry, other
    )

    var expanded by remember { mutableStateOf(false) }
    var showPrioritySelector by remember { mutableStateOf(false) }

    Column {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = currentWaterNeedType,
                onValueChange = {},
                label = { Text(stringResource(R.string.usage_type)) },
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true),
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
                                absoluteEmergency -> 0
                                medical -> 1
                                drinking -> 2
                                farming -> 3
                                industry -> 4
                                else -> 6 // Default for "Other"
                            }
                            onPriorityChange(priority)
                            showPrioritySelector = usageType == other
                            expanded = false
                        }
                    )
                }
            }
        }
        // If "Other" is selected, show custom type field and priority selector
        if (currentWaterNeedType == other) {
            OutlinedTextField(
                value = customType,
                onValueChange = { onCustomTypeChange(it) },
                label = { Text(stringResource(R.string.custom_usage_type)) },
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
            text = stringResource(R.string.priority_for_other_type),
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
                    label = { Text(stringResource(R.string.p_priority, priority)) },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }
    }
}