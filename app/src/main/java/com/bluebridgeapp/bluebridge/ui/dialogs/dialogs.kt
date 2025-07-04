package com.bluebridgeapp.bluebridge.ui.dialogs

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.bluebridgeapp.bluebridge.R
import com.bluebridgeapp.bluebridge.data.model.WellData
import com.bluebridgeapp.bluebridge.data.repository.WaterNeedsManager
import com.bluebridgeapp.bluebridge.ui.components.StatusIndicator
import com.bluebridgeapp.bluebridge.ui.components.ThemeOption
import com.bluebridgeapp.bluebridge.ui.components.calculateWaterLevelProgress
import com.bluebridgeapp.bluebridge.viewmodels.UserViewModel

@Composable
fun BasicConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = stringResource(R.string.confirm),
    dismissText: String = stringResource(R.string.cancel),
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
        title = stringResource(R.string.delete_water_need_title),
        message = stringResource(R.string.delete_water_need_message),
        confirmText = stringResource(R.string.delete),
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
    val absoluteEmergency = stringResource(R.string.absolute_emergency)
    val medical = stringResource(R.string.medical)
    val drinking = stringResource(R.string.drinking)
    val farming = stringResource(R.string.farming)
    val industry = stringResource(R.string.industry)
    val other = stringResource(R.string.other)

    val usageTypes = remember {
        listOf(
            absoluteEmergency,
            medical,
            drinking,
            farming,
            industry,
            other
        )
    }
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
                Text(stringResource(R.string.amount_liters), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))

                Text(
                    "${state.newAmountSlider.toInt()} ${stringResource(R.string.liters)}",
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
                    label = { Text(stringResource(R.string.amount_liters)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                Text(stringResource(R.string.select_usage_type), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = state.newUsageType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.select_usage_type)) },
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
                                    if (type != other) {
                                        state.newPriority = when (type) {
                                            absoluteEmergency -> 0
                                            medical -> 1
                                            drinking -> 2
                                            farming -> 3
                                            industry -> 4
                                            else -> 3
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                if (state.newUsageType == other) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.customType,
                        onValueChange = { if (it.length <= 15) state.customType = it },
                        label = { Text(stringResource(R.string.custom_usage_type)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(stringResource(R.string.priority), style = MaterialTheme.typography.titleSmall)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    (0..5).forEach { p ->
                        FilterChip(
                            selected = state.newPriority == p,
                            onClick = { state.newPriority = p },
                            label = { Text(stringResource(R.string.p_priority, p)) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = state.newDescription,
                    onValueChange = { state.newDescription = it },
                    label = { Text(stringResource(R.string.description_optional)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = state.newAmount.isNotBlank() && state.newUsageType.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
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
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.location_permission)) },
        text = { Text(stringResource(R.string.location_permission_message)) },
        confirmButton = {
            Button(onClick = { requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
                Text(stringResource(R.string.allow))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
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
            onDismiss()
        }
    }
    if (!isPermissionGranted) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.notification_permission)) },
            text = { Text(stringResource(R.string.enable_notifications)) },
            confirmButton = {
                Button(onClick = { requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                    Text(stringResource(R.string.allow))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.later))
                }
            }
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
                    Text("${stringResource(R.string.well_owner)}: ${well.wellOwner}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                }
                Text("${stringResource(R.string.location)}: ${well.wellLocation}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text("${stringResource(R.string.type_label)}: ${well.wellWaterType}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                if (well.wellCapacity.isNotBlank()) {
                    Text("${stringResource(R.string.well_capacity)}: ${well.wellCapacity}${stringResource(R.string.liters)}",
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                }
                if (well.wellStatus.isNotBlank()) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("${stringResource(R.string.status_label)}:", style = MaterialTheme.typography.bodyMedium)
                        StatusIndicator(well.wellStatus)
                    }
                }
                if (well.wellWaterLevel.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("${stringResource(R.string.water_level)}: ${well.wellWaterLevel}${stringResource(R.string.liters)}",
                        style = MaterialTheme.typography.bodyMedium)
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
                    Text("${stringResource(R.string.daily_consumption)}: ${well.wellWaterConsumption}${stringResource(R.string.liters_per_day)}",
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(onClick = onAdd) {
                Text(stringResource(R.string.add_to_my_wells))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                TextButton(onClick = onMoreDetails) {
                    Text(stringResource(R.string.details))
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
                Text("${stringResource(R.string.status_label)}: ${well.wellStatus}")
                if (well.wellWaterLevel.isNotBlank()) Text("${stringResource(R.string.water_level)}: ${well.wellWaterLevel}${stringResource(R.string.liters)}")
                if (well.wellCapacity.isNotBlank()) Text("${stringResource(R.string.well_capacity)}: ${well.wellCapacity}${stringResource(R.string.liters)}")
                if (well.wellWaterType.isNotBlank()) Text("${stringResource(R.string.water_type)}: ${well.wellWaterType}")
            }
        },
        confirmButton = {
            Button(onClick = onNavigateToDetails) {
                Text(stringResource(R.string.details))
            }
        },
        dismissButton = {
            TextButton(onClick = onNavigateToDirections) {
                Text(stringResource(R.string.navigate_there))
            }
        }
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
        stringResource(R.string.system_default) to 0,
        stringResource(R.string.light) to 1,
        stringResource(R.string.dark) to 2,
        stringResource(R.string.green) to 3,
        stringResource(R.string.pink) to 4,
        stringResource(R.string.red) to 5,
        stringResource(R.string.purple) to 6,
        stringResource(R.string.yellow) to 7,
        stringResource(R.string.tan) to 8,
        stringResource(R.string.orange) to 9,
        stringResource(R.string.cyan) to 10
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.theme)) },
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
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun LogoutConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    BasicConfirmationDialog(
        title = stringResource(R.string.logout_confirmation_title),
        message = stringResource(R.string.logout_confirmation_message),
        confirmText = stringResource(R.string.logout),
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
            title = { Text(stringResource(R.string.delete_account)) },
            text = {
                Column {
                    if (!confirmed) {
                        Text(
                            stringResource(R.string.delete_account_password_prompt),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(stringResource(R.string.password)) },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) stringResource(R.string.hide_password) else stringResource(R.string.show_password)
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            stringResource(R.string.delete_account_final_warning),
                            color = colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                val buttonText = if (confirmed) stringResource(R.string.delete_account) else stringResource(R.string.continue_text)
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
                    Text(stringResource(R.string.cancel))
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

    val categories = listOf(
        stringResource(R.string.bug_category_major),
        stringResource(R.string.bug_category_minor),
        stringResource(R.string.bug_category_edge_case),
        stringResource(R.string.bug_category_enhancement),
        stringResource(R.string.other)
    )
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(categories.first()) }
    var extra by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.send_bug_report)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).wrapContentHeight()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.your_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.bug_description)) },
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
                        label = { Text(stringResource(R.string.category)) },
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
                    label = { Text(stringResource(R.string.extra_info_optional)) },
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
            ) { Text(stringResource(R.string.send)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}