package com.bluebridgeapp.bluebridge.ui.screens.wellscreens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bluebridgeapp.bluebridge.R
import com.bluebridgeapp.bluebridge.data.model.Location
import com.bluebridgeapp.bluebridge.data.model.WellData
import com.bluebridgeapp.bluebridge.events.AppEvent
import com.bluebridgeapp.bluebridge.events.AppEventChannel
import com.bluebridgeapp.bluebridge.events.WellEvents
import com.bluebridgeapp.bluebridge.ui.components.WellField
import com.bluebridgeapp.bluebridge.viewmodels.UiState
import com.bluebridgeapp.bluebridge.viewmodels.UserViewModel
import com.bluebridgeapp.bluebridge.viewmodels.WellViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch

const val debug = true

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellConfigScreen(
    wellId: Int,
    wellViewModel: WellViewModel,
    userViewModel: UserViewModel,
    navController: NavController,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var locationInput by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    val currentWellState = wellViewModel.currentWellState.value
    
    val wellData = (currentWellState as? UiState.Success)?.data ?: WellData(
        id = wellId,
        wellName = "",
        wellLocation = Location(0.0, 0.0),
        wellWaterType = "",
        wellCapacity = "",
        wellWaterLevel = "",
        lastRefreshTime = 0,
        wellStatus = "",
        //TODO: maybe have the WaterQuality ?
        extraData = emptyMap(),
        description = "",
        lastUpdated = "",
        espId = "",
        wellWaterConsumption = "",
        wellOwner = "",
    )
    var lastSavedData by remember { mutableStateOf(wellData) }
    val isLoading = currentWellState is UiState.Loading
    val errorMessage = (currentWellState as? UiState.Error)?.message
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    var navigateBack by remember { mutableStateOf(false) }

    // Load well data when screen is first shown
    LaunchedEffect(wellId) {
        wellViewModel.loadWell(wellId)
    }

    // Show error messages in snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            AppEventChannel.sendEvent(AppEvent.ShowError(it))

        }
    }

    // Format location input when wellData changes
    LaunchedEffect(wellData.wellLocation) {
        if (wellData.wellLocation.latitude != 0.0 || wellData.wellLocation.longitude != 0.0) {
            locationInput = "Location:\nlat: ${wellData.wellLocation.latitude}\nlon: ${wellData.wellLocation.longitude}"
        }
    }

    // Handle navigation with unsaved changes check
    fun onBackPressed() {
        if (wellData != lastSavedData) {
            showUnsavedChangesDialog = true
        } else {
            navigateBack = true
        }
    }

    // Handle actual navigation
    LaunchedEffect(navigateBack) {
        if (navigateBack) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (wellId == -1) stringResource(R.string.add_new_well) else stringResource(R.string.edit_well)) },
                navigationIcon = {
                    IconButton(onClick = { onBackPressed() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Basic Info Section
                SectionHeader(title = stringResource(R.string.basic_information), icon = Icons.Default.Person)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WellField(
                            label = stringResource(R.string.well_name),
                            value = wellData.wellName,
                            keyId = wellData.id,
                            onValueChange = { wellViewModel.handleEvent(WellEvents.WellNameEntered(it)) }
                        )

                        WellField(
                            label = stringResource(R.string.well_owner),
                            value = wellData.wellOwner,
                            keyId = wellData.id,
                            onValueChange = { wellViewModel.handleEvent(WellEvents.OwnerEntered(it)) }
                        )
                    }
                }

                // Location Section
                SectionHeader(title = stringResource(R.string.location), icon = Icons.Default.LocationOn)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = locationInput,
                            onValueChange = { locationInput = it },
                            label = { Text(stringResource(R.string.location)) },
                            singleLine = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Map preview placeholder (could be replaced with actual map)
                        if (wellData.wellLocation.latitude != 0.0 || wellData.wellLocation.longitude != 0.0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null)
                                    Text(
                                        text = "Location: ${wellData.wellLocation.latitude}, ${wellData.wellLocation.longitude}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    val hasLocationPermission = context.checkSelfPermission(
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasLocationPermission) {
                                        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                                        fusedLocationClient.getCurrentLocation(
                                            Priority.PRIORITY_HIGH_ACCURACY,
                                            null
                                        ).addOnSuccessListener { location ->
                                            if (location != null) {
                                                locationInput = "Location:\nlat: ${location.latitude}\nlon: ${location.longitude}"
                                                // Update the well location
                                                wellViewModel.handleEvent(
                                                    WellEvents.WellLocationEntered(
                                                        Location(
                                                            latitude = location.latitude,
                                                            longitude = location.longitude
                                                        )
                                                    )
                                                )
                                            } else {
                                                scope.launch {
                                                    AppEventChannel.sendEvent(AppEvent.ShowError("Unable to get current location"))
                                                }
                                            }
                                        }.addOnFailureListener {
                                            scope.launch {
                                                AppEventChannel.sendEvent(AppEvent.ShowError("Failed to get location: ${it.localizedMessage}"))
                                            }
                                        }
                                    } else {
                                        AppEventChannel.sendEvent(AppEvent.ShowError("Location permission needed"))

                                        (context as? Activity)?.requestPermissions(
                                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                            1002
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(stringResource(R.string.use_current_location))
                        }
                    }
                }

                // Water Specifications Section
                SectionHeader(title = stringResource(R.string.water_specifications), icon = Icons.Default.Water)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WellField(
                            label = stringResource(R.string.water_type),
                            value = wellData.wellWaterType,
                            keyId = wellData.id,
                            onValueChange = { wellViewModel.handleEvent(WellEvents.WaterTypeEntered(it)) }
                        )

                        WellField(
                            label = stringResource(R.string.well_capacity),
                            value = wellData.wellCapacity,
                            keyId = wellData.id,
                            onValueChange = { wellViewModel.handleEvent(WellEvents.WellCapacityEntered(it)) },
                            isNumeric = true
                        )

                        WellField(
                            label = stringResource(R.string.water_level),
                            value = wellData.wellWaterLevel,
                            keyId = wellData.id,
                            onValueChange = { wellViewModel.handleEvent(WellEvents.WaterLevelEntered(it)) },
                            isNumeric = true
                        )

                        WellField(
                            label = stringResource(R.string.daily_consumption),
                            value = wellData.wellWaterConsumption,
                            keyId = wellData.id,
                            onValueChange = { wellViewModel.handleEvent(WellEvents.ConsumptionEntered(it)) },
                            isNumeric = true
                        )
                        
                        // Water level visualization
                        if (wellData.wellCapacity.isNotBlank() && wellData.wellWaterLevel.isNotBlank()) {
                            val capacity = wellData.wellCapacity.toFloat()
                            val level = wellData.wellWaterLevel.toFloat()
                            val percentage =
                                if (capacity > 0) (level / capacity).coerceIn(0f, 1f) else 0f

                            Column {
                                Text(
                                    "Water Level: ${(percentage * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )

                                LinearProgressIndicator(
                                    progress = { percentage },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    color = when {
                                        percentage < 0.3f -> MaterialTheme.colorScheme.error
                                        percentage < 0.7f -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.tertiary
                                    }
                                )
                            }

                        }
                    }
                }

                // Technical Details Section
                SectionHeader(title = stringResource(R.string.technical_details), icon = Icons.Default.SettingsRemote)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        WellField(
                            label = stringResource(R.string.esp_id),
                            value = wellData.espId,
                            keyId = wellData.id,
                            onValueChange = { newEspId ->
                                scope.launch {
                                    val isUnique = wellViewModel.repository.isEspIdUnique(newEspId)
                                    if (isUnique) {
                                        wellViewModel.handleEvent(WellEvents.EspIdEntered(newEspId))
                                    } else {
                                        AppEventChannel.sendEvent(AppEvent.ShowError("ESP ID already in use"))
                                    }
                                }
                            }
                        )

                        Text(
                            text = stringResource(R.string.unique_identifier_for_esp32_microcontroller),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save Button with loading indicator
                Button(
                    onClick = {
                        scope.launch {
                            isSaving = true
                            try {
                                val isUnique = wellViewModel.repository.isEspIdUnique(wellData.espId)
                                if (!isUnique && wellId == -1) {
                                    AppEventChannel.sendEvent(AppEvent.ShowError("ESP ID already in use by another well"))
                                    isSaving = false
                                    return@launch
                                }
                                
                                // Get user credentials for server authentication
                                val email = userViewModel.getUserEmail()
                                val token = userViewModel.getLoginToken()
                                
                                if (email != null && token != null) {
                                    // Create or update well via server API
                                    val success = wellViewModel.saveWellToServer(wellData)
                                    
                                    if (success) {
                                        // Also save locally
                                        wellViewModel.handleEvent(WellEvents.SaveWell(wellId))
                                        lastSavedData = wellData
                                        AppEventChannel.sendEvent(AppEvent.ShowError("Well saved successfully!"))
                                        navigateBack = true
                                    } else {
                                        // If server save fails, at least save locally
                                        wellViewModel.handleEvent(WellEvents.SaveWell(wellId))
                                        lastSavedData = wellData
                                        AppEventChannel.sendEvent(AppEvent.ShowError("Could not save to server but saved locally"))
                                        navigateBack = true
                                    }
                                } else {
                                    // No valid user credentials, just save locally
                                    wellViewModel.handleEvent(WellEvents.SaveWell(wellId))
                                    lastSavedData = wellData
                                    AppEventChannel.sendEvent(AppEvent.ShowError("Well saved locally"))
                                    navigateBack = true
                                }
                            } catch (e: Exception) {
                                AppEventChannel.sendEvent(AppEvent.ShowError("Error saving well: ${e.message}"))
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = wellData.isValid() && wellData != lastSavedData && !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(stringResource(R.string.save_well))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Unsaved Changes Dialog
            if (showUnsavedChangesDialog) {
                AlertDialog(
                    onDismissRequest = { showUnsavedChangesDialog = false },
                    title = { Text(stringResource(R.string.unsaved_changes)) },
                    text = { Text(stringResource(R.string.discard_changes)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showUnsavedChangesDialog = false
                                navigateBack = true
                            }
                        ) {
                            Text(stringResource(R.string.discard))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showUnsavedChangesDialog = false }
                        ) {
                            Text(stringResource(R.string.keep_editing))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(4.dp)
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Divider(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

// Helper function to validate well data
private fun WellData.isValid(): Boolean {
    // ESP ID is required
    if (espId.isBlank()) return false
    
    // At least one other field must be filled
    val hasValidLocation = wellLocation.latitude != 0.0 || wellLocation.longitude != 0.0
    return wellName.isNotBlank() || 
           wellOwner.isNotBlank() ||
           hasValidLocation ||
           wellCapacity.isNotBlank() ||
           wellWaterLevel.isNotBlank() ||
           wellWaterConsumption.isNotBlank() ||
           wellWaterType.isNotBlank()
}

