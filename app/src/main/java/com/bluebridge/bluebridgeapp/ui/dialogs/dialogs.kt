package com.bluebridge.bluebridgeapp.ui.dialogs

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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


@Composable
fun BasicConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    confirmButtonColor: androidx.compose.ui.graphics.Color = colorScheme.primary,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = confirmButtonColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}

@Composable
fun DeleteWaterNeedDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BasicConfirmationDialog(
        title = "Delete Water Need",
        message = "Are you sure you want to delete this water need?",
        confirmText = "Delete",
        confirmButtonColor = colorScheme.error,
        onConfirm = onConfirm,
        onDismiss = onDismiss
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
                Text("Amount (liters)", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))

                Text(
                    "${state.newAmountSlider.toInt()} liters",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Slider(
                    value = state.newAmountSlider,
                    onValueChange = {
                        state.newAmountSlider = it
                        state.newAmount = it.toInt().toString()
                    },
                    valueRange = 0f..1000f
                )

                OutlinedTextField(
                    value = state.newAmount,
                    onValueChange = {
                        val filtered = it.filter(Char::isDigit)
                        if (filtered.length <= 4) {
                            state.newAmount = filtered
                            state.newAmountSlider = filtered.toFloatOrNull() ?: 0f
                        }
                    },
                    label = { Text("Amount (liters)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                Text("Usage Type", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = state.newUsageType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select usage type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
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

                if (state.newUsageType == "Other") {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.customType,
                        onValueChange = { if (it.length <= 15) state.customType = it },
                        label = { Text("Custom Usage Type (max 15 chars)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text("Priority (0 = Highest, 5 = Lowest)", style = MaterialTheme.typography.titleSmall)

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

                Spacer(Modifier.height(16.dp))

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
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun LocationPermissionDialog(onDismiss: () -> Unit, onAllow: () -> Unit) {
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            onAllow()
        } else {
            // Optionally, show a message explaining why the permission is needed
            // and provide an option to open app settings manually.
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Location Permission") },
        text = { Text("BlueBridge needs location access for nearby water sources") },
        confirmButton = {
            Button(onClick = { requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) { Text("Allow") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun NotificationPermissionDialog(onDismiss: () -> Unit, onAllow: () -> Unit) {
    val context = LocalContext.current
    val isPermissionGranted = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            onAllow()
        } else {
            var showDialog = true
            onDismiss()
        }
    }
    if (!isPermissionGranted) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Notification Permission") },
            text = { Text("Enable notifications for water source updates") },
            confirmButton = {
                Button(onClick = { requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Allow") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Later") } }
        )
    }
}

@Composable
fun EnhancedWellDetailsDialog(
    well: WellData,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
    onMoreDetails: () -> Unit
) {
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
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Status:", style = MaterialTheme.typography.bodyMedium)
                        StatusIndicator(well.wellStatus)
                    }
                }
                if (well.wellWaterLevel.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Water Level: ${well.wellWaterLevel}L", style = MaterialTheme.typography.bodyMedium)
                    waterLevelProgress?.let {
                        LinearProgressIndicator(
                            progress = { it },
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
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(onClick = onMoreDetails) { Text("More details") }
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
        "System Default" to 0, "Light" to 1, "Dark" to 2, "Green" to 3, "Pink" to 4,
        "Red" to 5, "Purple" to 6, "Yellow" to 7, "Tan" to 8, "Orange" to 9, "Cyan" to 10
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select Theme") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    themes.forEach { (label, value) ->
                        ThemeOption(
                            title = label,
                            isSelected = currentThemePreference == value,
                            onClick = {
                                userViewModel.updateTheme(value)
                                onDismiss()
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun LogoutConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    BasicConfirmationDialog(
        title = "Confirm Logout",
        message = "Are you sure you want to sign out?",
        confirmText = "Logout",
        confirmButtonColor = colorScheme.error,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}



@Composable
fun DeleteAccountDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirmFinal: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmed by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Delete Account") },
            text = {
                Column {
                    if (!confirmed) {
                        Text(
                            "To delete your account, please enter your password. This action is permanent and cannot be undone.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
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
                    } else {
                        Text(
                            "⚠️ WARNING: This will permanently delete your account and all associated data. This action cannot be undone.\n\nAre you absolutely sure?",
                            color = colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                val buttonText = if (confirmed) "Delete Account" else "Continue"
                val buttonColor = if (confirmed) colorScheme.error else colorScheme.primary

                TextButton(
                    onClick = {
                        if (!confirmed && password.isNotBlank()) {
                            confirmed = true
                        } else if (confirmed) {
                            onConfirmFinal(password)
                        }
                    }
                ) {
                    Text(buttonText, color = buttonColor)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BugReportDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (name: String, description: String, category: String, extra: Map<String, String>) -> Unit
) {
    if (!showDialog) return

    val categories = listOf("Major Bug", "Minor Bug", "Edge Case", "Enhancement", "Other")
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(categories.first()) }
    var extra by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Submit Bug Report") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).wrapContentHeight()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Bug Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = extra,
                    onValueChange = { extra = it },
                    label = { Text("Extra Info (optional, key1:value1,key2:value2)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val extraMap = extra.split(',').mapNotNull {
                        val parts = it.split(':', limit = 2)
                        if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
                    }.toMap()
                    onSubmit(name, description, category, extraMap)
                },
                enabled = name.isNotBlank() && description.isNotBlank()
            ) { Text("Send") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}


