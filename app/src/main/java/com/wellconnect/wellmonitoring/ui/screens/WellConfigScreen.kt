package com.wellconnect.wellmonitoring.ui.screens

import WellData
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.wellconnect.wellmonitoring.data.WellEvents
import com.wellconnect.wellmonitoring.data.model.Location
import com.wellconnect.wellmonitoring.ui.components.WellField
import com.wellconnect.wellmonitoring.viewmodels.UiState
import com.wellconnect.wellmonitoring.viewmodels.WellViewModel
import kotlinx.coroutines.launch

const val debug = true

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellConfigScreen(
    wellId: Int,
    wellViewModel: WellViewModel,
    navController: NavController,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    var locationInput by remember { mutableStateOf("") }

    val currentWellState = wellViewModel.currentWellState.value
    val wellData = (currentWellState as? UiState.Success)?.data ?: WellData(id = wellId)
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
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = it,
                    duration = SnackbarDuration.Short
                )
            }
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
                title = { Text(if (wellId == -1) "Add New Well" else "Edit Well") },
                navigationIcon = {
                    IconButton(onClick = { onBackPressed() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                SectionHeader(title = "Basic Information", icon = Icons.Default.Person)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WellField(
                            label = "Well Name",
                            value = wellData.wellName,
                            keyId = wellData.id,
                            onValueChange = { wellViewModel.handleEvent(WellEvents.WellNameEntered(it)) }
                        )

                        WellField(
                            label = "Well owner",
                            value = wellData.wellOwner,
                            keyId = wellData.id,
                            onValueChange = { wellViewModel.handleEvent(WellEvents.OwnerEntered(it)) }
                        )
                    }
                }

                // Location Section
                SectionHeader(title = "Location", icon = Icons.Default.LocationOn)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    OutlinedTextField(
                        value = locationInput,
                        onValueChange = { locationInput = it },
                        label = { Text("Location (format: Location:\nlat: 0.000000\nlon: 25.000000)") },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
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
                                            // Optionally update the well location here
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
                                                snackbarHostState.showSnackbar("Unable to get current location")
                                            }
                                        }
                                    }.addOnFailureListener {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Failed to get location: ${it.localizedMessage}")
                                        }
                                    }
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Location permission needed")
                                    }
                                    (context as? Activity)?.requestPermissions(
                                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                        1002
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Use My Current Location")
                    }
                }

                // Water Specifications Section
                SectionHeader(title = "Water Specifications", icon = Icons.Default.Water)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WellField(
                            label = "Water type",
                            value = wellData.wellWaterType,
                            keyId = wellData.id,
                            onValueChange = { wellViewModel.handleEvent(WellEvents.WaterTypeEntered(it)) }
                        )

                        WellField(
                            label = "Well Capacity (L)",
                            value = wellData.wellCapacity,
                            keyId = wellData.id,
                            onValueChange = { wellViewModel.handleEvent(WellEvents.WellCapacityEntered(it)) },
                            isNumeric = true
                        )

                        WellField(
                            label = "Water Level (L)",
                            value = wellData.wellWaterLevel,
                            keyId = wellData.id,
                            onValueChange = { wellViewModel.handleEvent(WellEvents.WaterLevelEntered(it)) },
                            isNumeric = true
                        )

                        WellField(
                            label = "Daily Consumption (L)",
                            value = wellData.wellWaterConsumption,
                            keyId = wellData.id,
                            onValueChange = { wellViewModel.handleEvent(WellEvents.ConsumptionEntered(it)) },
                            isNumeric = true
                        )
                    }
                }

                // Technical Details Section
                SectionHeader(title = "Technical Details", icon = Icons.Default.SettingsRemote)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        WellField(
                            label = "ESP ID",
                            value = wellData.espId,
                            keyId = wellData.id,
                            onValueChange = { newEspId ->
                                scope.launch {
                                    val isUnique = wellViewModel.repository.isEspIdUnique(newEspId, wellData.id)
                                    if (isUnique) {
                                        wellViewModel.handleEvent(WellEvents.EspIdEntered(newEspId))
                                    } else {
                                        snackbarHostState.showSnackbar("ESP ID already in use")
                                    }
                                }
                            }
                        )

                        Text(
                            text = "Unique identifier for the ESP32 microcontroller",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save Button
                Button(
                    onClick = {
                        scope.launch {
                            val isUnique = wellViewModel.repository.isEspIdUnique(wellData.espId, wellData.id)
                            if (!isUnique) {
                                snackbarHostState.showSnackbar("ESP ID already in use by another well")
                                return@launch
                            }
                            wellViewModel.handleEvent(WellEvents.SaveWell(wellId))
                            lastSavedData = wellData
                            navigateBack = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = wellData.isValid() && wellData != lastSavedData
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Save Well")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Unsaved Changes Dialog
            if (showUnsavedChangesDialog) {
                AlertDialog(
                    onDismissRequest = { showUnsavedChangesDialog = false },
                    title = { Text("Unsaved Changes") },
                    text = { Text("You have unsaved changes. Do you want to discard them?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showUnsavedChangesDialog = false
                                navigateBack = true
                            }
                        ) {
                            Text("Discard")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showUnsavedChangesDialog = false }
                        ) {
                            Text("Keep Editing")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp)
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

