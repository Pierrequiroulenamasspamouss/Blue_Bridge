@file:Suppress("DEPRECATION")

package com.wellconnect.wellmonitoring.ui.screens

import WellData
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.wellconnect.wellmonitoring.R
import com.wellconnect.wellmonitoring.ui.navigation.Routes
import com.wellconnect.wellmonitoring.viewmodels.WellViewModel
import getLatitude
import getLongitude
import hasValidCoordinates
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

private const val TAG = "MapScreen"
private const val MIN_ZOOM_LEVEL = 2.0 // Minimum zoom level allowed

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MapScreen(
    navController: NavController,
    wellViewModel: WellViewModel,
    userLat: Double? = null,
    userLon: Double? = null,
    targetLat: Double? = null,
    targetLon: Double? = null
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val wellsState = wellViewModel.wellsListState.value
    val wells: List<WellData> = when (wellsState) {
        is com.wellconnect.wellmonitoring.viewmodels.UiState.Success -> wellsState.data
        else -> emptyList()
    }

    var isLoading by remember { mutableStateOf(true) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var selectedWell by remember { mutableStateOf<WellData?>(null) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    // Check location permission
    LaunchedEffect(Unit) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            showPermissionDialog = true
        }
        
        try {
            // Initialize OSMDroid configuration
            Configuration.getInstance().load(
                context,
                PreferenceManager.getDefaultSharedPreferences(context)
            )
            Configuration.getInstance().userAgentValue = context.packageName
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing OSMDroid: ${e.message}")
        }
        
        isLoading = false
    }

    // Handle permission dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Location Permission Required") },
            text = { Text("The map needs location permission to show your current location.") },
            confirmButton = {
                Button(onClick = { showPermissionDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Show well details dialog if a well is selected
    selectedWell?.let { well ->
        AlertDialog(
            onDismissRequest = { selectedWell = null },
            title = { Text(well.wellName) },
            text = {
                Column {
                    Text("Status: ${well.wellStatus}")
                    
                    // Only show water level if it's not blank
                    if (well.wellWaterLevel.isNotBlank()) {
                        Text("Water Level: ${well.wellWaterLevel}L")
                    }
                    
                    // Only show capacity if it's not blank
                    if (well.wellCapacity.isNotBlank()) {
                        Text("Capacity: ${well.wellCapacity}L")
                    }
                    
                    // Only show water type if it's not blank
                    if (well.wellWaterType.isNotBlank()) {
                        Text("Water Type: ${well.wellWaterType}")
                    }
                    
                    // Remove explicit location coordinates display
                    if (well.hasValidCoordinates()) {
                        Text("Location available") 
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    // Navigate to well details
                    navController.navigate("${Routes.WELL_DETAILS_SCREEN}/${well.id}")
                    selectedWell = null
                }) {
                    Text("Details")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    // Navigate to compass screen for this well
                    try {
                        val encodedName = java.net.URLEncoder.encode(well.wellName, "UTF-8")
                        val lat = well.getLatitude()
                        val lon = well.getLongitude()
                        if (lat != null && lon != null) {
                            navController.navigate(
                                "${Routes.COMPASS_SCREEN}?lat=$lat&lon=$lon&name=$encodedName"
                            )
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Cannot navigate: Invalid coordinates")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error navigating to compass: ${e.message}")
                        scope.launch {
                            snackbarHostState.showSnackbar("Error navigating to compass")
                        }
                    }
                    selectedWell = null
                }) {
                    Text("Show directions")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Well Map") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                // OSMDroid map view
                AndroidView(
                    factory = { ctx ->
                        try {
                            MapView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                
                                // Set minimum zoom level
                                minZoomLevel = MIN_ZOOM_LEVEL
                                
                                // Add location overlay if permission granted
                                if (ActivityCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    // Add default location overlay to show user's live location
                                    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), this)
                                    locationOverlay.enableMyLocation()
                                    
                                    // Remove default overlay and use our custom marker
                                    // overlays.add(locationOverlay)
                                    
                                    Log.d(TAG, "Set up location tracking")
                                }
                                
                                // Set initial position and zoom
                                val mapController = controller
                                mapController.setZoom(12.0.coerceAtLeast(MIN_ZOOM_LEVEL)) // Ensure minimum zoom
                                
                                // Center map at appropriate location
                                val initialPoint = when {
                                    targetLat != null && targetLon != null -> GeoPoint(targetLat, targetLon)
                                    userLat != null && userLon != null -> GeoPoint(userLat, userLon)
                                    else -> {
                                        val firstWellWithCoords = wells.firstOrNull { it.hasValidCoordinates() }
                                        if (firstWellWithCoords != null) {
                                            val lat = firstWellWithCoords.getLatitude() ?: 0.0
                                            val lon = firstWellWithCoords.getLongitude() ?: 0.0
                                            GeoPoint(lat, lon)
                                        } else {
                                            GeoPoint(0.0, 0.0)
                                        }
                                    }
                                }
                                mapController.setCenter(initialPoint)
                                
                                // Add user location marker if available
                                if (userLat != null && userLon != null) {
                                    try {
                                        val userMarker = Marker(this)
                                        userMarker.position = GeoPoint(userLat, userLon)
                                        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                        userMarker.title = "Your Location"
                                        userMarker.icon = ContextCompat.getDrawable(context, R.drawable.small_map_arrow)
                                        userMarker.setInfoWindowAnchor(0.5f, 0f) // Center info window
                                        
                                        overlays.add(userMarker)
                                        Log.d(TAG, "Added user marker at position: $userLat, $userLon")
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error adding user marker: ${e.message}")
                                    }
                                } else {
                                    Log.d(TAG, "User location not available for marker")
                                }
                                
                                // Add target location marker if available
                                if (targetLat != null && targetLon != null) {
                                    try {
                                        val targetMarker = Marker(this)
                                        targetMarker.position = GeoPoint(targetLat, targetLon)
                                        targetMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                        targetMarker.title = "Target Location"
                                        targetMarker.icon = ContextCompat.getDrawable(context, R.drawable.small_water_drop_icon)
                                        
                                        overlays.add(targetMarker)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error adding target marker: ${e.message}")
                                    }
                                }
                                
                                // Add well markers
                                for (well in wells) {
                                    if (targetLat != null && targetLon != null) {
                                        continue
                                    }
                                    if (well.hasValidCoordinates()) {
                                        val lat = well.getLatitude()
                                        val lon = well.getLongitude()
                                        if (lat != null && lon != null) {
                                            try {
                                                val wellMarker = Marker(this)
                                                wellMarker.id = "well_${well.id}"
                                                wellMarker.position = GeoPoint(lat, lon)
                                                wellMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                                wellMarker.title = well.wellName
                                                wellMarker.snippet = "Status: ${well.wellStatus}"
                                                wellMarker.icon = ContextCompat.getDrawable(context, R.drawable.small_water_drop_icon)
                                                wellMarker.setInfoWindowAnchor(0.5f, 0f)
                                                wellMarker.setOnMarkerClickListener { marker, _ ->
                                                    scope.launch {
                                                        selectedWell = well
                                                    }
                                                    true
                                                }
                                                overlays.add(wellMarker)
                                            } catch (e: Exception) {
                                                Log.e(TAG, "Error adding well marker: ${e.message}")
                                            }
                                        }
                                    }
                                }
                                
                                // Store reference to the map view
                                mapViewRef = this
                                
                                this
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error creating map view: ${e.message}")
                            // Return an empty View if map creation fails
                            View(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        try {
                            // Only try to enforce minimum zoom if it's actually a MapView
                            if (view is MapView) {
                                // Check current zoom and adjust if needed
                                if (view.zoomLevelDouble < MIN_ZOOM_LEVEL) {
                                    view.controller.setZoom(MIN_ZOOM_LEVEL)
                                }
                            }
                            
                            // Update is handled here if needed
                            view.invalidate()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating map view: ${e.message}")
                        }
                    }
                )
                
                // Control buttons
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Focus on user location button
                        if (userLat != null && userLon != null) {
                            androidx.compose.material3.FloatingActionButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            mapViewRef?.let { mapView ->
                                                val userPoint = GeoPoint(userLat, userLon)
                                                mapView.controller.animateTo(userPoint)
                                                mapView.controller.setZoom(15.0)
                                            }
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Error focusing on user location: ${e.message}")
                                        }
                                    }
                                },
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.MyLocation,
                                    contentDescription = "My Location"
                                )
                            }
                        }
                        
                        // Focus on target location button
                        if (targetLat != null && targetLon != null) {
                            androidx.compose.material3.FloatingActionButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            mapViewRef?.let { mapView ->
                                                val targetPoint = GeoPoint(targetLat, targetLon)
                                                mapView.controller.animateTo(targetPoint)
                                                mapView.controller.setZoom(15.0)
                                            }
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Error focusing on target location: ${e.message}")
                                        }
                                    }
                                },
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Place,
                                    contentDescription = "Target Location"
                                )
                            }
                        }
                        
                        // Reset view button to see all wells
                        androidx.compose.material3.FloatingActionButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        mapViewRef?.let { mapView ->
                                            val initialPoint = when {
                                                targetLat != null && targetLon != null -> GeoPoint(targetLat, targetLon)
                                                userLat != null && userLon != null -> GeoPoint(userLat, userLon)
                                                else -> {
                                                    val firstWellWithCoords = wells.firstOrNull { it.hasValidCoordinates() }
                                                    if (firstWellWithCoords != null) {
                                                        val lat = firstWellWithCoords.getLatitude() ?: 0.0
                                                        val lon = firstWellWithCoords.getLongitude() ?: 0.0
                                                        GeoPoint(lat, lon)
                                                    } else {
                                                        GeoPoint(0.0, 0.0)
                                                    }
                                                }
                                            }
                                            mapView.controller.animateTo(initialPoint)
                                            mapView.controller.setZoom(12.0.coerceAtLeast(MIN_ZOOM_LEVEL))
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error resetting view: ${e.message}")
                                    }
                                }
                            },
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Reset View"
                            )
                        }
                    }
                }
            }
        }
    }
} 