package com.wellconnect.wellmonitoring.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.wellconnect.wellmonitoring.R
import com.wellconnect.wellmonitoring.data.WellData
import com.wellconnect.wellmonitoring.data.getLatitude
import com.wellconnect.wellmonitoring.data.getLongitude
import com.wellconnect.wellmonitoring.ui.components.TopBar
import com.wellconnect.wellmonitoring.ui.navigation.Routes
import com.wellconnect.wellmonitoring.ui.screens.compass.CompassView
import com.wellconnect.wellmonitoring.ui.screens.compass.DistanceInfo
import com.wellconnect.wellmonitoring.ui.screens.compass.MiniMapView
import com.wellconnect.wellmonitoring.ui.screens.compass.calculateDistance
import com.wellconnect.wellmonitoring.ui.screens.compass.findNearestWells
import com.wellconnect.wellmonitoring.ui.screens.compass.formatDistance
import com.wellconnect.wellmonitoring.ui.screens.compass.lastKnownLocation
import com.wellconnect.wellmonitoring.ui.screens.compass.rememberCompassSensor
import com.wellconnect.wellmonitoring.viewmodels.WellViewModel
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
    wellViewModel: WellViewModel? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // State
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showNearestWellsDialog by remember { mutableStateOf(false) }
    var nearestWells by remember { mutableStateOf<List<WellData>>(emptyList()) }
    var currentLocation by remember { mutableStateOf(lastKnownLocation) }
    var distance by remember { mutableStateOf<Float?>(null) }
    var bearing by remember { mutableStateOf<Float?>(null) }

    // Compass direction from sensor
    val azimuth by rememberCompassSensor(context)

    // Decode location name
    val decodedLocationName = remember(locationName) {
        try {
            locationName?.replace("+", " ")?.let { 
                java.net.URLDecoder.decode(it, "UTF-8") 
            }
        } catch (e: Exception) {
            locationName // Fallback to original name if decoding fails
        }
    }
    
    // Navigation mode
    val isPointingNorth = decodedLocationName == "North"
    
    // Rotation values
    var currentRotation by remember { mutableStateOf(0f) }
    var isInTargetZone by remember { mutableStateOf(false) }
    
    // Calculate distance and bearing
    LaunchedEffect(currentLocation, latitude, longitude) {
        if (currentLocation != null && latitude != null && longitude != null) {
            val results = FloatArray(2)
            Location.distanceBetween(
                currentLocation!!.latitude,
                currentLocation!!.longitude,
                latitude, longitude,
                results
            )
            distance = results[0]
            bearing = results[1]
        }
    }

    // Calculate rotation from bearing and azimuth
    LaunchedEffect(bearing, azimuth) {
        if (bearing != null) {
            currentRotation = bearing!! - azimuth
            val normalizedRotation = (currentRotation + 360) % 360
            isInTargetZone = normalizedRotation in 340f..360f || normalizedRotation in 0f..20f
        }
    }
    
    // Get location
    LaunchedEffect(Unit) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            showPermissionDialog = true
            return@LaunchedEffect
        }
        
        // Get current location immediately and then receive updates
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        
        // First get last known location
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = location
                    lastKnownLocation = location
                }
            }
        
        // Then request location updates for more accuracy
        try {
            val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 
                5000 // Update every 5 seconds
            ).build()
            
            val locationCallback = object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                    result.lastLocation?.let { newLocation ->
                        currentLocation = newLocation
                        lastKnownLocation = newLocation
                    }
                }
            }
            
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
        } catch (e: Exception) {
            // If we can't get updates, fall back to getCurrentLocation
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        currentLocation = location
                        lastKnownLocation = location
                    }
            }
        }
    }

    // Permission dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Location Permission Required") },
            text = { Text("The compass needs location permission to show directions accurately.") },
            confirmButton = {
                Button(onClick = {
                    showPermissionDialog = false
                    (context as? Activity)?.requestPermissions(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ),
                        1001
                    )
                }) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showPermissionDialog = false
                    navController.navigateUp()
                }) {
                    Text("Go Back")
                }
            }
        )
    }

    // Nearest wells dialog
    if (showNearestWellsDialog && nearestWells.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showNearestWellsDialog = false },
            title = { Text("Nearest Wells") },
            text = {
                Column {
                    nearestWells.forEach { well ->
                        TextButton(
                            onClick = {
                                showNearestWellsDialog = false
                                val encodedName = URLEncoder.encode(well.wellName, "UTF-8")
                                val lat = well.getLatitude()
                                val lon = well.getLongitude()
                                if (lat != null && lon != null) {
                                navController.navigate(
                                        "${Routes.COMPASS_SCREEN}?lat=$lat&lon=$lon&name=$encodedName"
                                )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(well.wellName)
                                Text(
                                    "Distance: ${
                                        formatDistance(
                                            calculateDistance(
                                                currentLocation,
                                                well
                                            )
                                        )
                                    }",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNearestWellsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
            // Custom Top bar with back button and logo
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    TopBar(
                        topBarMessage = if (isPointingNorth) "Navigation" else "Navigate to ${decodedLocationName ?: "Target"}",
                        isIcon = true,
                        iconId = R.drawable.app_logo
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Compass view - ONLY show when navigating to a specific location
            if (!isPointingNorth) {
                item {
                    CompassView(
                        currentRotation = currentRotation,
                        isTargetMode = true, // Always true here since we check isPointingNorth above
                        isInTargetZone = isInTargetZone,
                        modifier = Modifier
                            .size(200.dp)
                            .padding(vertical = 16.dp)
                    )
                }
            }
            
            // If we're in navigate mode, show map and distance info
            if (!isPointingNorth) {
                // Map View
                item {
                    if (currentLocation != null && latitude != null && longitude != null) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(350.dp)
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            ) {
                                MiniMapView(
                                    userLocation = currentLocation!!,
                                    targetLatitude = latitude,
                                    targetLongitude = longitude,
                                    azimuth = azimuth,
                                    modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

                // Distance information
                item {
                    if (distance != null) {
                        DistanceInfo(
                            distance = distance!!,
                            isInTargetZone = isInTargetZone,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            } else {
                // In North mode, show the "Find Nearest Wells" button
                item {
                    // Add map view in North mode too
                    if (currentLocation != null) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(350.dp)
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            ) {
                                MiniMapView(
                                    userLocation = currentLocation!!,
                                    azimuth = azimuth,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                    
                    Button(
                    onClick = {
                            if (currentLocation != null && wellViewModel != null) {
                                nearestWells = findNearestWells(currentLocation!!, wellViewModel)
                                showNearestWellsDialog = true
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Unable to find nearby wells. Please try again.")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp)
                            .padding(vertical = 8.dp, horizontal = 16.dp)
                    ) {
                        Text(
                            "Find Nearest Wells",
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
        
        // Snackbar host stays at the bottom
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
} 