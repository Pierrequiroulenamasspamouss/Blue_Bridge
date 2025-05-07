@file:Suppress("DEPRECATION")

package com.wellconnect.wellmonitoring.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.preference.PreferenceManager
import android.util.Log
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat.getSystemService
import com.wellconnect.wellmonitoring.R
import com.wellconnect.wellmonitoring.data.model.Location
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

private const val DEFAULT_ZOOM = 15.0
private const val TAG = "ProfileMapView"

@SuppressLint("MissingPermission")
@Composable
fun ProfileMapView(
    userLocation: Location?,
    selectedLocation: Location?,
    onLocationSelected: (Location) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // State for map and markers
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    var userMarkerRef by remember { mutableStateOf<Marker?>(null) }
    var selectedMarkerRef by remember { mutableStateOf<Marker?>(null) }
    var myLocationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    var currentZoom by remember { mutableStateOf(DEFAULT_ZOOM) }
    
    // State for rotation
    var rotation by remember { mutableFloatStateOf(0f) }
    val sensorManager = remember { getSystemService(context, SensorManager::class.java) }
    val rotationSensor = remember { sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }
    
    // Create sensor listener
    val sensorListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    // Convert rotation vector to azimuth (angle around z-axis)
                    val rotationMatrix = FloatArray(9)
                    val orientationAngles = FloatArray(3)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    
                    // Convert radians to degrees and normalize (0-360)
                    val azimuthDegrees = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                    rotation = (azimuthDegrees + 360) % 360
                    
                    // Update marker rotation
                    userMarkerRef?.let { marker ->
                        marker.rotation = rotation
                        mapViewInstance?.invalidate()
                    }
                }
            }
            
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                // Not needed for this implementation
            }
        }
    }
    
    // Register and unregister sensor listener
    DisposableEffect(Unit) {
        sensorManager?.registerListener(
            sensorListener,
            rotationSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        
        onDispose {
            sensorManager?.unregisterListener(sensorListener)
        }
    }
    
    // Update markers when locations change
    LaunchedEffect(userLocation, selectedLocation) {
        mapViewInstance?.let { mapView ->
            // Update user marker if available
            if (userLocation != null) {
                val userPoint = GeoPoint(userLocation.latitude, userLocation.longitude)
                
                if (userMarkerRef == null) {
                    // Create new marker
                    val marker = Marker(mapView).apply {
                        position = userPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = context.getDrawable(R.drawable.small_map_arrow)
                        rotation = rotation
                        setInfoWindow(null) // No info window on tap
                    }
                    userMarkerRef = marker
                    mapView.overlays.add(marker)
                } else {
                    // Update existing marker
                    userMarkerRef?.position = userPoint
                }
                
                mapView.invalidate()
            }
            
            // Update selected location marker
            if (selectedLocation != null) {
                val selectedPoint = GeoPoint(selectedLocation.latitude, selectedLocation.longitude)
                
                if (selectedMarkerRef == null) {
                    // Create new marker
                    val marker = Marker(mapView).apply {
                        position = selectedPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon = context.getDrawable(R.drawable.ic_location_pin)
                        title = "Selected Location"
                        setInfoWindow(null) // No info window on tap
                    }
                    selectedMarkerRef = marker
                    mapView.overlays.add(marker)
                } else {
                    // Update existing marker
                    selectedMarkerRef?.position = selectedPoint
                }
                
                mapView.invalidate()
            }
        }
    }
    
    Box(modifier = modifier) {
        // Map View
        AndroidView(
            factory = { ctx ->
                Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
                
                MapView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(DEFAULT_ZOOM)
                    
                    // Handle clicks on map
                    setOnTouchListener { _, event ->
                        // Let the map handle the touch event
                        false
                    }
                    
                    // Add click listener for location selection
                    val evListener = object : org.osmdroid.events.MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                            p?.let {
                                val tappedLocation = Location(
                                    latitude = it.latitude,
                                    longitude = it.longitude,
                                    lastUpdated = "manual"
                                )
                                onLocationSelected(tappedLocation)
                            }
                            return true
                        }

                        override fun longPressHelper(p: GeoPoint?): Boolean = false
                    }
                    
                    // Add MapEventsOverlay to handle map clicks
                    val evOverlay = org.osmdroid.views.overlay.MapEventsOverlay(evListener)
                    overlays.add(evOverlay)
                    
                    // Initialize with user location if available
                    if (userLocation != null) {
                        val userPoint = GeoPoint(userLocation.latitude, userLocation.longitude)
                        controller.setCenter(userPoint)
                        
                        // Create user marker
                        val marker = Marker(this).apply {
                            position = userPoint
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            icon = context.getDrawable(R.drawable.small_map_arrow)
                            rotation = rotation
                            setInfoWindow(null) // No info window on tap
                        }
                        userMarkerRef = marker
                        overlays.add(marker)
                    }
                    
                    // Add selected location marker if available
                    if (selectedLocation != null) {
                        val selectedPoint = GeoPoint(selectedLocation.latitude, selectedLocation.longitude)
                        
                        val marker = Marker(this).apply {
                            position = selectedPoint
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            icon = context.getDrawable(R.drawable.ic_location_pin)
                            title = "Selected Location"
                            setInfoWindow(null) // No info window on tap
                        }
                        selectedMarkerRef = marker
                        overlays.add(marker)
                    }
                    
                    // Add my location overlay
                    val locationProvider = GpsMyLocationProvider(context)
                    val myLocationOverlay = MyLocationNewOverlay(locationProvider, this).apply {
                        enableMyLocation()
                    }
                    overlays.add(myLocationOverlay)
                    
                    // Store references
                    mapViewInstance = this
                    
                    this
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Control buttons at the bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Focus on user location
            if (userLocation != null) {
                SmallFloatingActionButton(
                    onClick = {
                        mapViewInstance?.let { mapView ->
                            val userPoint = GeoPoint(userLocation.latitude, userLocation.longitude)
                            mapView.controller.animateTo(userPoint)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Focus on my location",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Focus on current selected location
            if (selectedLocation != null) {
                SmallFloatingActionButton(
                    onClick = {
                        mapViewInstance?.let { mapView ->
                            val selectedPoint = GeoPoint(selectedLocation.latitude, selectedLocation.longitude)
                            mapView.controller.animateTo(selectedPoint)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Focus on selected location",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            // Reset rotation/orientation
            SmallFloatingActionButton(
                onClick = {
                    mapViewInstance?.let { mapView ->
                        mapView.controller.animateTo(mapView.mapCenter, currentZoom, 0L, 0f)
                    }
                },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "Reset orientation",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
    
    // Clean up resources
    DisposableEffect(Unit) {
        onDispose {
            myLocationOverlay?.disableMyLocation()
            mapViewInstance?.onDetach()
        }
    }
} 