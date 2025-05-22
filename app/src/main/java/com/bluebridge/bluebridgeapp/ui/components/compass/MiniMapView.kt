@file:Suppress("DEPRECATION")

package com.bluebridge.bluebridgeapp.ui.components.compass

import android.location.Location
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bluebridge.bluebridgeapp.R
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider

// Store user's preferred zoom level persistently
private var userPreferredZoomLevel = 18.0
private const val TAG = "MiniMapView"
private const val MIN_ZOOM_LEVEL = 2.0 // Minimum zoom level allowed

/**
 * A mini map that shows the user's location and optionally the target location
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MiniMapView(
    userLocation: Location,
    targetLatitude: Double? = null,
    targetLongitude: Double? = null,
    modifier: Modifier = Modifier,
    azimuth: Float = 0f  // Add compass bearing input
) {
    val context = LocalContext.current
    
    // State to hold reference to the mapView
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    
    // Keep direct references to markers
    var userMarkerRef by remember { mutableStateOf<Marker?>(null) }
    
    // State for tracking mode - initially follow user, disable on map interaction
    var isFollowingUser by remember { mutableStateOf(true) }
    
    // Save the initial setup state to prevent recentering on every recomposition
    val isInitialSetup = remember { mutableStateOf(true) }
    
    // Remember current zoom level to maintain it when updating
    val currentZoom = remember { mutableStateOf(userPreferredZoomLevel) }
    
    // Rotation state for the user marker (in degrees, 0 = North)
    val rotation = remember { mutableStateOf(0f) }
    
    // Update rotation based on compass
    LaunchedEffect(azimuth) {
        rotation.value = -azimuth // Negative because we want clockwise rotation
        
        // Update user marker rotation if available
        try {
            userMarkerRef?.let { marker ->
                marker.rotation = rotation.value
                mapViewInstance?.invalidate()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating marker rotation: ${e.message}")
        }
    }
    
    // Update user marker position without recomposing entire view
    LaunchedEffect(userLocation) {
        Log.d(TAG, "LaunchedEffect for userLocation: lat=${userLocation.latitude}, lon=${userLocation.longitude}")
        
        mapViewInstance?.let { mapView ->
            try {
                // Store current zoom (if not initial setup)
                if (!isInitialSetup.value) {
                    currentZoom.value = mapView.zoomLevelDouble
                    userPreferredZoomLevel = currentZoom.value // Store user's preferred zoom
                    Log.d(TAG, "Stored current zoom level: ${currentZoom.value}")
                }
                
                // Always update user marker position
                val userPoint = GeoPoint(userLocation.latitude, userLocation.longitude)
                
                // Update marker position if we have a reference
                userMarkerRef?.position = userPoint
                
                // Only center on user if in following mode or initial setup
                if (isFollowingUser || isInitialSetup.value) {
                    Log.d(TAG, "Centering on user: following=$isFollowingUser, initialSetup=${isInitialSetup.value}")
                    
                    if (isInitialSetup.value) {
                        // No matter what, focus on user with user's preferred zoom
                        mapView.controller.setZoom(userPreferredZoomLevel)
                        mapView.controller.animateTo(userPoint)
                        currentZoom.value = userPreferredZoomLevel
                        Log.d(TAG, "Initial focus on user with zoom=$userPreferredZoomLevel")
                        
                        // Mark initial setup as complete
                        isInitialSetup.value = false
                        Log.d(TAG, "Initial setup completed")
                    } else {
                        // Just center on user when following, preserving zoom
                        mapView.controller.setZoom(currentZoom.value)
                        mapView.controller.animateTo(userPoint)
                        Log.d(TAG, "Following user - map centered, maintaining zoom at ${currentZoom.value}")
                    }
                } else {
                    Log.d(TAG, "Not following user - marker updated but map not centered")
                }
                
                // Force redraw
                mapView.invalidate()
            } catch (e: Exception) {
                Log.e(TAG, "Error updating map: ${e.message}")
            }
        }
    }
    
    // Setup map configuration
    LaunchedEffect(Unit) {
        try {
            val config = Configuration.getInstance()
            config.load(
                context,
                PreferenceManager.getDefaultSharedPreferences(context)
            )
            config.userAgentValue = context.packageName
            
            // Increase the tile cache size to prevent frequent reloading
            config.tileFileSystemCacheMaxBytes = 100L * 1024 * 1024 // 100MB cache
            
            // Don't reload tiles if they're less than 7 days old
            config.expirationOverrideDuration = 7 * 24 * 60 * 60 * 1000L
            
            Log.d(TAG, "OSMDroid configuration completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring OSMDroid: ${e.message}")
        }
    }
    
    // Create a Box to contain the map, compass indicator, and focus button
    Box(modifier = modifier) {
        // Create map view
        AndroidView(
            factory = { ctx ->
                try {
                    MapView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        
                        // Set minimum zoom level
                        minZoomLevel = MIN_ZOOM_LEVEL
                        
                        // Create user marker with rotation
                        val userPoint = GeoPoint(userLocation.latitude, userLocation.longitude)
                        
                        // Create and add user marker
                        val marker = Marker(this)
                        marker.position = userPoint
                        // Set anchor to center for both horizontal and vertical to rotate around center
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        marker.title = "Your Location"
                        marker.icon = context.getDrawable(
                            R.drawable.small_map_arrow
                        )
                        
                        // Store reference to user marker
                        userMarkerRef = marker
                        
                        // Add the marker to map
                        try {
                            overlayManager.add(marker)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error adding user marker: ${e.message}")
                        }
                        
                        // Add target marker if provided
                        if (targetLatitude != null && targetLongitude != null) {
                            try {
                                val targetPoint = GeoPoint(targetLatitude, targetLongitude)
                                val targetMarker = Marker(this)
                                targetMarker.position = targetPoint
                                targetMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                targetMarker.title = "Target Location"
                                targetMarker.icon = context.getDrawable(
                                    R.drawable.small_water_drop_icon
                                )
                                overlayManager.add(targetMarker)
                                Log.d(TAG, "Target marker added at: lat=$targetLatitude, lon=$targetLongitude")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error adding target marker: ${e.message}")
                            }
                        }
                        
                        // Add compass overlay to show north direction
                        try {
                            val compassOverlay = CompassOverlay(
                                context,
                                InternalCompassOrientationProvider(context),
                                this
                            )
                            compassOverlay.enableCompass()
                            overlayManager.add(compassOverlay)
                            Log.d(TAG, "Compass overlay added to map")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error adding compass overlay: ${e.message}")
                        }
                        
                        // Add listener to detect map interactions
                        setOnTouchListener { _, event ->
                            when (event.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    // Log touch start
                                    Log.d(TAG, "Map touch detected (ACTION_DOWN)")
                                }
                                MotionEvent.ACTION_MOVE -> {
                                    // User is dragging the map, disable following
                                    if (isFollowingUser) {
                                        isFollowingUser = false
                                        currentZoom.value = zoomLevelDouble // Store zoom level when user takes control
                                        userPreferredZoomLevel = currentZoom.value // Update preferred zoom
                                        Log.d(TAG, "Map drag detected - disabling follow mode, stored zoom=${currentZoom.value}")
                                    }
                                }
                            }
                            // Return false to allow the map to handle the touch event
                            false
                        }
                        
                        // Add a zoom listener to track zoom changes
                        addMapListener(object : MapListener {
                            override fun onScroll(event: ScrollEvent?): Boolean {
                                return false
                            }
                            
                            override fun onZoom(event: ZoomEvent): Boolean {
                                // Ensure we don't go below minimum zoom
                                if (event.zoomLevel < MIN_ZOOM_LEVEL) {
                                    controller.setZoom(MIN_ZOOM_LEVEL)
                                    return true // Handled
                                }
                                
                                currentZoom.value = event.zoomLevel
                                userPreferredZoomLevel = event.zoomLevel // Store user's preferred zoom
                                Log.d(TAG, "User changed zoom to ${event.zoomLevel}")
                                return false
                            }
                        })
                        
                        // Initially zoom to user with preferred zoom level
                        controller.setZoom(userPreferredZoomLevel)
                        controller.setCenter(userPoint)
                        
                        // Store map instance for future updates
                        mapViewInstance = this
                        
                        // Return the MapView
                        this
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating map view: ${e.message}")
                    // Return a dummy view in case of error
                    FrameLayout(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    }
                }
            },
            update = { mapView ->
                // Update the user marker direction based on compass bearing
                try {
                    userMarkerRef?.let { marker ->
                        marker.rotation = rotation.value
                        mapView.invalidate()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating marker rotation: ${e.message}")
                }
            }
        )

        // Only show focus-on-target button if a target exists
        if (targetLatitude != null && targetLongitude != null) {
            // Focus-on-target button in bottom-left corner
            SmallFloatingActionButton(
                onClick = {
                    mapViewInstance?.let { mapView ->
                        val targetPoint = GeoPoint(targetLatitude, targetLongitude)
                        
                        // Use the current zoom level (user's preferred) but ensure minimum
                        val zoomToUse = currentZoom.value.coerceAtLeast(MIN_ZOOM_LEVEL)
                        mapView.controller.setZoom(zoomToUse)
                        mapView.controller.animateTo(targetPoint)
                        Log.d(TAG, "User clicked target button, focusing on target with zoom=$zoomToUse")
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "Focus on target",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
        
        // Center-on-user button in bottom-right corner
        SmallFloatingActionButton(
            onClick = {
                mapViewInstance?.let { mapView ->
                    isFollowingUser = true
                    val userPoint = GeoPoint(userLocation.latitude, userLocation.longitude)
                    // Use the current zoom level (user's preferred)
                    mapView.controller.setZoom(currentZoom.value)
                    mapView.controller.animateTo(userPoint)
                    Log.d(TAG, "User clicked center button, focusing with zoom=${currentZoom.value}")
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                imageVector = Icons.Default.NearMe,
                contentDescription = "Center on me",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
    
    // Clean up map resources when the composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            try {
                // Clear references
                userMarkerRef = null
                mapViewInstance?.onDetach()
                mapViewInstance = null
                Log.d(TAG, "Map view detached, saving user zoom preference: $userPreferredZoomLevel")
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up map resources: ${e.message}")
            }
        }
    }
}

