@file:Suppress("DEPRECATION")

package com.bluebridgeapp.bluebridge.ui.components.compass

import android.location.Location
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bluebridgeapp.bluebridge.R
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

private const val TAG = "MiniMapView"
private const val MIN_ZOOM_LEVEL = 2.0
private var userPreferredZoomLevel = 18.0

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MiniMapCard(
    userLocation: Location,
    targetLat: Double? = null,
    targetLon: Double? = null,
    azimuth: Float = 0f,
    onZoomChanged: (Double) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(200.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        MiniMapView(
            userLocation = userLocation,
            targetLatitude = targetLat,
            targetLongitude = targetLon,
            azimuth = azimuth,
            modifier = Modifier.fillMaxSize(),
            onZoomChanged = onZoomChanged
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun MiniMapView(
    userLocation: Location,
    targetLatitude: Double? = null,
    targetLongitude: Double? = null,
    modifier: Modifier = Modifier,
    azimuth: Float = 0f,
    onZoomChanged: (Double) -> Unit = {}
) {
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var userMarker by remember { mutableStateOf<Marker?>(null) }
    var isFollowingUser by remember { mutableStateOf(true) }
    val isInitialSetup = remember { mutableStateOf(true) }
    val currentZoom = remember { mutableStateOf(userPreferredZoomLevel) }
    val rotation = remember { mutableStateOf(0f) }

    // --- OSMDroid Configuration ---
    LaunchedEffect(Unit) {
        val config = Configuration.getInstance()
        config.load(context, PreferenceManager.getDefaultSharedPreferences(context))
        config.userAgentValue = context.packageName
        config.tileFileSystemCacheMaxBytes = 100L * 1024 * 1024
        config.expirationOverrideDuration = 7 * 24 * 60 * 60 * 1000L
    }

    // --- Update marker rotation on azimuth change ---
    LaunchedEffect(azimuth) {
        rotation.value = -azimuth
        userMarker?.let { marker ->
            marker.rotation = rotation.value
            mapView?.invalidate()
        }
    }

    // --- Update user marker and map center on location change ---
    LaunchedEffect(userLocation) {
        mapView?.let { mv ->
            val userPoint = GeoPoint(userLocation.latitude, userLocation.longitude)
            userMarker?.position = userPoint
            if (isFollowingUser || isInitialSetup.value) {
                if (isInitialSetup.value) {
                    mv.controller.setZoom(userPreferredZoomLevel)
                    isInitialSetup.value = false
                }
                mv.controller.animateTo(userPoint)
                currentZoom.value = mv.zoomLevelDouble
                onZoomChanged(currentZoom.value)
            }
            mv.invalidate()
        }
    }

    // --- Main UI ---
    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    minZoomLevel = MIN_ZOOM_LEVEL

                    // User marker
                    val userPoint = GeoPoint(userLocation.latitude, userLocation.longitude)
                    val marker = Marker(this).apply {
                        position = userPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "Your Location"
                        icon = context.getDrawable(R.drawable.small_map_arrow)
                    }
                    userMarker = marker
                    overlayManager.add(marker)

                    // Target marker
                    if (targetLatitude != null && targetLongitude != null) {
                        val targetPoint = GeoPoint(targetLatitude, targetLongitude)
                        val targetMarker = Marker(this).apply {
                            position = targetPoint
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "Target Location"
                            icon = context.getDrawable(R.drawable.small_water_drop_icon)
                        }
                        overlayManager.add(targetMarker)
                    }

                    // Compass overlay
                    val compassOverlay = CompassOverlay(
                        context,
                        InternalCompassOrientationProvider(context),
                        this
                    )
                    compassOverlay.enableCompass()
                    overlayManager.add(compassOverlay)

                    // Touch disables follow mode
                    setOnTouchListener { _, event ->
                        if (event.action == android.view.MotionEvent.ACTION_MOVE) {
                            if (isFollowingUser) {
                                isFollowingUser = false
                                currentZoom.value = zoomLevelDouble
                                userPreferredZoomLevel = currentZoom.value
                                onZoomChanged(currentZoom.value)
                            }
                        }
                        false
                    }

                    // Zoom listener
                    addMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean = false
                        override fun onZoom(event: ZoomEvent): Boolean {
                            if (event.zoomLevel < MIN_ZOOM_LEVEL) {
                                controller.setZoom(MIN_ZOOM_LEVEL)
                                return true
                            }
                            currentZoom.value = event.zoomLevel
                            userPreferredZoomLevel = event.zoomLevel
                            onZoomChanged(currentZoom.value)
                            return false
                        }
                    })

                    // Initial center and zoom
                    controller.setZoom(userPreferredZoomLevel)
                    controller.setCenter(userPoint)
                    mapView = this
                }
            },
            update = { mv ->
                userMarker?.let { marker ->
                    marker.rotation = rotation.value
                    mv.invalidate()
                }
            }
        )

        // --- Destination Button ---
        if (targetLatitude != null && targetLongitude != null) {
            SmallFloatingActionButton(
                onClick = {
                    mapView?.let { mv ->
                        val targetPoint = GeoPoint(targetLatitude, targetLongitude)
                        val zoomToUse = currentZoom.value.coerceAtLeast(MIN_ZOOM_LEVEL)
                        mv.controller.setZoom(zoomToUse)
                        mv.controller.animateTo(targetPoint)
                        onZoomChanged(currentZoom.value)
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

        // --- My Location Button ---
        SmallFloatingActionButton(
            onClick = {
                mapView?.let { mv ->
                    isFollowingUser = true
                    val userPoint = GeoPoint(userLocation.latitude, userLocation.longitude)
                    mv.controller.setZoom(currentZoom.value)
                    mv.controller.animateTo(userPoint)
                    onZoomChanged(currentZoom.value)
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

    // --- Cleanup ---
    DisposableEffect(Unit) {
        onDispose {
            userMarker = null
            mapView?.onDetach()
            mapView = null
        }
    }
}


