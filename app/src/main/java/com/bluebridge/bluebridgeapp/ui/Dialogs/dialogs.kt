package com.bluebridge.bluebridgeapp.ui.Dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.bluebridge.bluebridgeapp.data.model.WellData
import com.bluebridge.bluebridgeapp.data.repository.WaterNeedsManager
import com.bluebridge.bluebridgeapp.ui.components.StatusIndicator
import com.bluebridge.bluebridgeapp.ui.components.ThemeOption
import com.bluebridge.bluebridgeapp.ui.components.calculateWaterLevelProgress
import com.bluebridge.bluebridgeapp.viewmodels.UserViewModel
import kotlinx.coroutines.launch

@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Water Need") },
        text = { Text("Are you sure you want to delete this water need?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterNeedDialog(
    title: String,
    state: WaterNeedsManager,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val usageTypes = listOf("Absolute emergency", "Medical", "Drinking", "Farming", "Industry", "Other")
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Amount input with slider
                Text(
                    text = "Amount (liters)",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Amount slider
                Text(
                    text = "${state.newAmountSlider.toInt()} liters",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Slider(
                    value = state.newAmountSlider,
                    onValueChange = {
                        state.newAmountSlider = it
                        state.newAmount = it.toInt().toString()
                    },
                    valueRange = 0f..1000f,
                    steps = 0
                )

                // Manual amount input
                OutlinedTextField(
                    value = state.newAmount,
                    onValueChange = {
                        val filtered = it.filter { char -> char.isDigit() }
                        if (filtered.length <= 4) {
                            state.newAmount = filtered
                            state.newAmountSlider = filtered.toFloatOrNull() ?: 0f
                        }
                    },
                    label = { Text("Amount (liters)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Usage type dropdown
                Text(
                    text = "Usage Type",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = state.newUsageType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select usage type") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        usageTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    state.newUsageType = type
                                    expanded = false

                                    // Set default priority based on type
                                    if (type != "Other") {
                                        state.newPriority = when (type) {
                                            "Absolute emergency" -> 0
                                            "Medical" -> 1
                                            "Drinking" -> 2
                                            "Farming" -> 3
                                            "Industry" -> 4
                                            else -> 3
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                // Custom type for "Other"
                if (state.newUsageType == "Other") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.customType,
                        onValueChange = {
                            // Limit to 15 characters
                            if (it.length <= 15) {
                                state.customType = it
                            }
                        },
                        label = { Text("Custom Usage Type (max 15 chars)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Priority selector
                Text(
                    text = "Priority (0 = Highest, 5 = Lowest)",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    (0..5).forEach { p ->
                        FilterChip(
                            selected = state.newPriority == p,
                            onClick = { state.newPriority = p },
                            label = { Text("P$p") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                OutlinedTextField(
                    value = state.newDescription,
                    onValueChange = { state.newDescription = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = state.newAmount.isNotBlank() && state.newUsageType.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EnhancedWellDetailsDialog(
    well: WellData,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
    onNavigate: () -> Unit
) {
    // Pre-calculate water level progress outside of composable
    val waterLevelProgress = if (well.wellWaterLevel.isNotBlank() && well.wellCapacity.isNotBlank()) {
        calculateWaterLevelProgress(well.wellWaterLevel, well.wellCapacity)
    } else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(well.wellName) },
        text = {
            Column {
                if (well.wellOwner.isNotBlank()) {
                    Text("Owner: ${well.wellOwner}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                }

                Text("Location: ${well.wellLocation}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))

                Text("Type: ${well.wellWaterType}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))

                if (well.wellCapacity.isNotBlank()) {
                    Text("Capacity: ${well.wellCapacity}L", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                }

                if (well.wellStatus.isNotBlank()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Status:", style = MaterialTheme.typography.bodyMedium)
                        StatusIndicator(well.wellStatus)
                    }
                }

                if (well.wellWaterLevel.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Water Level: ${well.wellWaterLevel}L", style = MaterialTheme.typography.bodyMedium)

                    // Use pre-calculated progress value
                    waterLevelProgress?.let { progress ->
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }
                }

                if (well.wellWaterConsumption.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Consumption: ${well.wellWaterConsumption}L/day", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(onClick = onAdd) { Text("Add to My Wells") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                TextButton(onClick = onNavigate) {
                    Text("More details")
                }
            }
        }
    )
}

@Composable
fun WellDetailsDialog(
    well: WellData,
    onDismiss: () -> Unit,
    onNavigateToDetails: () -> Unit,
    onNavigateToDirections: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(well.wellName) },
        text = {
            Column {
                Text("Status: ${well.wellStatus}")
                if (well.wellWaterLevel.isNotBlank()) Text("Water Level: ${well.wellWaterLevel}L")
                if (well.wellCapacity.isNotBlank()) Text("Capacity: ${well.wellCapacity}L")
                if (well.wellWaterType.isNotBlank()) Text("Water Type: ${well.wellWaterType}")
            }
        },
        confirmButton = { Button(onClick = onNavigateToDetails) { Text("Details") } },
        dismissButton = { TextButton(onClick = onNavigateToDirections) { Text("Directions") } }
    )
}


@Composable
fun ThemeSelectionDialog(
    showDialog: Boolean,
    currentThemePreference: Int,
    userViewModel: UserViewModel,
    onDismiss: () -> Unit
) {
    val themes = listOf(
        "System Default" to 0,
        "Light" to 1,
        "Dark" to 2,
        "Green" to 3,
        "Pink" to 4,
        "Red" to 5,
        "Purple" to 6,
        "Yellow" to 7,
        "Tan" to 8,
        "Orange" to 9,
        "Cyan" to 10
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select Theme") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    themes.forEach { (themeName, themeValue) ->
                        ThemeOption(
                            title = themeName,
                            isSelected = currentThemePreference == themeValue,
                            onClick = {
                                userViewModel.updateTheme(themeValue)
                                onDismiss()
                            })
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
    }
}

@Composable
fun LogoutConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Logout") },
        text = { Text("Are you sure you want to sign out?") },
        confirmButton = {
            TextButton(
                onClick = {
                    coroutineScope.launch {
                        onConfirm()
                    }
                    onDismiss() // Dismiss the dialog after initiating logout
                }
            ) {
                Text("Logout", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteAccountDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var passwordForDeletion by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Delete Account") },
            text = {
                Column {
                    Text(
                        "To delete your account, please enter your password to confirm. This action is permanent and cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = passwordForDeletion,
                        onValueChange = { passwordForDeletion = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (passwordForDeletion.isNotEmpty()) {
                            onConfirm(passwordForDeletion)
                        }
                    }
                ) {
                    Text("Continue", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}
@Composable
fun FinalDeleteAccountConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Delete Account") },
        text = {
            Text(
                "WARNING: This will permanently delete your account and all associated data. This action cannot be undone. Are you absolutely sure?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text("Delete Account", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}