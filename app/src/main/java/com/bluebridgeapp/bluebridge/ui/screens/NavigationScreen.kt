package com.bluebridgeapp.bluebridge.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import com.bluebridgeapp.bluebridge.events.AppEvent
import com.bluebridgeapp.bluebridge.events.AppEventChannel
import com.bluebridgeapp.bluebridge.data.model.WellData
import com.bluebridgeapp.bluebridge.data.model.getLatitude
import com.bluebridgeapp.bluebridge.data.model.getLongitude
import com.bluebridgeapp.bluebridge.ui.components.compass.CompassView
import com.bluebridgeapp.bluebridge.ui.components.compass.DistanceInfo
import com.bluebridgeapp.bluebridge.ui.components.compass.MiniMapCard
import com.bluebridgeapp.bluebridge.ui.components.compass.rememberCompassSensor
import com.bluebridgeapp.bluebridge.ui.dialogs.LocationPermissionDialog
import com.bluebridgeapp.bluebridge.navigation.Routes
import com.bluebridgeapp.bluebridge.utils.calculateDistance
import com.bluebridgeapp.bluebridge.utils.findNearestWells
import com.bluebridgeapp.bluebridge.utils.formatDistance
import com.bluebridgeapp.bluebridge.utils.lastKnownLocation
import com.bluebridgeapp.bluebridge.viewmodels.WellViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CompassScreen(
    navController: NavController,
    latitude: Double? = null,
    longitude: Double? = null,
    locationName: String? = null,
    wellViewModel: WellViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showNearestWellsDialog by remember { mutableStateOf(false) }
    var nearestWells by remember { mutableStateOf<List<WellData>>(emptyList()) }
    var currentLocation by remember { mutableStateOf(lastKnownLocation) }
    var distance by remember { mutableStateOf<Float?>(null) }
    var bearing by remember { mutableStateOf<Float?>(null) }
    val azimuth by rememberCompassSensor(context)

    // Load saved wells
    LaunchedEffect(Unit) {
        wellViewModel.getSavedWells()
    }

    // Navigation mode
    val isPointingNorth = remember(locationName) {
        locationName?.replace("+", " ")?.let {
            try { java.net.URLDecoder.decode(it, "UTF-8") } catch (_: Exception) { it }
        } == "North"
    }

    // Calculate rotation
    var currentRotation by remember { mutableFloatStateOf(0f) }
    var isInTargetZone by remember { mutableStateOf(false) }

    LaunchedEffect(currentLocation, latitude, longitude) {
        if (currentLocation != null && latitude != null && longitude != null) {
            val results = FloatArray(2)
            Location.distanceBetween(
                currentLocation!!.latitude, currentLocation!!.longitude,
                latitude, longitude, results
            )
            distance = results[0]
            bearing = results[1]
        }
    }

    LaunchedEffect(bearing, azimuth) {
        bearing?.let {
            currentRotation = it - azimuth
            isInTargetZone = ((currentRotation + 360) % 360).let {
                it in 340f..360f || it in 0f..20f
            }
        }
    }

    // Location handling
    LaunchedEffect(Unit) {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            showPermissionDialog = true
            return@LaunchedEffect
        }

        LocationServices.getFusedLocationProviderClient(context).apply {
            lastLocation.addOnSuccessListener { location ->
                location?.let { currentLocation = it; lastKnownLocation = it }
            }
            try {
                requestLocationUpdates(
                    com.google.android.gms.location.LocationRequest.Builder(
                        Priority.PRIORITY_HIGH_ACCURACY, 5000
                    ).build(),
                    object : com.google.android.gms.location.LocationCallback() {
                        override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                            result.lastLocation?.let {
                                currentLocation = it
                                lastKnownLocation = it
                            }
                        }
                    },
                    null
                )
            } catch (_: Exception) {
                getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { location ->
                        location?.let {
                            currentLocation = it
                            lastKnownLocation = it
                        }
                    }
            }
        }
    }

    // Dialogs
    if (showPermissionDialog) {
        LocationPermissionDialog(
            onDismiss = { showPermissionDialog = false },
            onAllow = { showPermissionDialog = false }
        )
    }

    if (showNearestWellsDialog) {
        NearestWellsDialog(
            wells = nearestWells,
            currentLocation = currentLocation,
            onDismiss = { showNearestWellsDialog = false },
            onWellSelected = { well ->
                showNearestWellsDialog = false
                well.getLatitude()?.let { lat ->
                    well.getLongitude()?.let { lon ->
                        navController.navigate(
                            "${Routes.COMPASS_SCREEN}?lat=$lat&lon=$lon&name=${
                                URLEncoder.encode(well.wellName, "UTF-8")
                            }"
                        )
                    }
                }
            }
        )
    }

    // Main UI
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isPointingNorth) "Navigation" else "Navigate to ${locationName ?: "Target"}") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isPointingNorth) {
                CompassView(
                    currentRotation = currentRotation,
                    isTargetMode = true,
                    isInTargetZone = isInTargetZone,
                    modifier = Modifier.size(200.dp)
                )

                currentLocation?.let { location ->
                    MiniMapCard(
                        userLocation = location,
                        targetLat = latitude,
                        targetLon = longitude,
                        azimuth = azimuth
                    )
                }

                distance?.let {
                    DistanceInfo(
                        distance = it,
                        isInTargetZone = isInTargetZone
                    )
                }
            } else {
                currentLocation?.let { location ->
                    MiniMapCard(
                        userLocation = location,
                        azimuth = azimuth
                    )
                }

                Button(
                    onClick = {
                        currentLocation?.let {
                            nearestWells = findNearestWells(it, wellViewModel)
                            showNearestWellsDialog = true
                        } ?: scope.launch {
                            AppEventChannel.sendEvent(
                                AppEvent.ShowError("Location unavailable. Please try again.")
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Text("Find Nearest Wells")
                }
            }
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun NearestWellsDialog(
    wells: List<WellData>,
    currentLocation: Location?,
    onDismiss: () -> Unit,
    onWellSelected: (WellData) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nearest Wells") },
        text = {
            Column {
                wells.forEach { well ->
                    TextButton(
                        onClick = { onWellSelected(well) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(well.wellName)
                            currentLocation?.let {
                                Text(
                                    "Distance: ${formatDistance(calculateDistance(it, well))}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}