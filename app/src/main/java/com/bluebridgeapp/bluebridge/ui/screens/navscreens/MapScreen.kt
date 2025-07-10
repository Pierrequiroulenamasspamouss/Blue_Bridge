@file:Suppress("DEPRECATION")

package com.bluebridgeapp.bluebridge.ui.screens.navscreens

import OfflineMapDownloader
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.navigation.NavController
import com.bluebridgeapp.bluebridge.R
import com.bluebridgeapp.bluebridge.data.model.WellData
import com.bluebridgeapp.bluebridge.data.model.getLatitude
import com.bluebridgeapp.bluebridge.data.model.getLongitude
import com.bluebridgeapp.bluebridge.data.model.hasValidCoordinates
import com.bluebridgeapp.bluebridge.ui.dialogs.WellDetailsDialog
import com.bluebridgeapp.bluebridge.ui.navigation.Routes
import com.bluebridgeapp.bluebridge.viewmodels.UiState
import com.bluebridgeapp.bluebridge.viewmodels.WellViewModel
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Suppress("DEPRECATION")
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
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val wellsState = wellViewModel.wellsListState.value
    val wells = remember { wellsState.dataOrEmpty() }
    var selectedWell by remember { mutableStateOf<WellData?>(null) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    // Initialize OSMDroid
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            PreferenceManager.getDefaultSharedPreferences(context)
        )
        Configuration.getInstance().userAgentValue = context.packageName
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
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            // Map View
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)

                        // Set initial position
                        val initialPoint = when {
                            targetLat != null && targetLon != null -> GeoPoint(targetLat, targetLon)
                            userLat != null && userLon != null -> GeoPoint(userLat, userLon)
                            wells.any { it.hasValidCoordinates() } -> {
                                val well = wells.first { it.hasValidCoordinates() }
                                GeoPoint(well.getLatitude(), well.getLongitude())
                            }
                            else -> GeoPoint(0.0, 0.0)
                        }
                        controller.setCenter(initialPoint)
                        controller.setZoom(15.0)

                        // Add user location overlay
                        val locationProvider = GpsMyLocationProvider(context)
                        val locationOverlay = MyLocationNewOverlay(locationProvider, this)
                        locationOverlay.enableMyLocation()
                        locationOverlay.enableFollowLocation()
                        locationOverlay.setDirectionArrow(
                            ContextCompat.getDrawable(context, R.drawable.small_map_arrow)!!.toBitmap(),
                            ContextCompat.getDrawable(context, R.drawable.small_map_arrow)!!.toBitmap()
                        )
                        overlays.add(locationOverlay)

                        // Add compass overlay for orientation
                        val compassOverlay = CompassOverlay(context, this)
                        compassOverlay.enableCompass()
                        overlays.add(compassOverlay)

                        // Center map on user location if available, otherwise on target or first well
                        if (userLat != null && userLon != null) {
                            controller.setCenter(GeoPoint(userLat, userLon))
                        } else if (targetLat != null && targetLon != null) {
                            controller.setCenter(GeoPoint(targetLat, targetLon))
                        } else if (wells.any { it.hasValidCoordinates() }) {
                            val well = wells.first { it.hasValidCoordinates() }
                            controller.setCenter(GeoPoint(well.getLatitude(), well.getLongitude()))
                        }

                        // Add well markers
                        wells.filter { it.hasValidCoordinates() }.forEach { well ->
                            Marker(this).apply {
                                position = GeoPoint(well.getLatitude(), well.getLongitude())
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                title = well.wellName
                                snippet = "Status: ${well.wellStatus}"
                                icon = ContextCompat.getDrawable(context, R.drawable.small_water_drop_icon)
                                setOnMarkerClickListener { _, _ ->
                                    selectedWell = well
                                    true
                                }
                                overlays.add(this)
                            }
                        }

                        mapViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Location FAB
            FloatingActionButton(
                onClick = {
                    userLat?.let { lat ->
                        userLon?.let { lon ->
                            mapViewRef?.controller?.animateTo(GeoPoint(lat, lon))
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Focus on my location")
            }

            // Download FAB - positioned above the location FAB
            FloatingActionButton(
                onClick = {

                    userLat?.let { lat ->
                        userLon?.let { lon ->
                            mapViewRef?.let { mapView ->
                                isDownloading = true
                                downloadProgress = 0
                                Log.d("MapScreen", "Downloading map for location: $lat, $lon")
                                coroutineScope.launch {
                                    val downloader = OfflineMapDownloader(context, mapView)
                                    downloader.downloadArea(
                                        center = GeoPoint(lat, lon),
                                        radiusKm = 10,
                                        minZoom = 12,
                                        maxZoom = 18,
                                        onProgress = { progress ->
                                            downloadProgress = progress
                                        }
                                    )
                                    isDownloading = false
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 80.dp, end = 16.dp),
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        progress = { downloadProgress.toFloat() / 100f },
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                } else {
                    Icon(Icons.Default.CloudDownload, contentDescription = "Download offline map")
                }
            }
        }
    }

    // Well details dialog
    selectedWell?.let { well ->
        WellDetailsDialog(
            well = well,
            onDismiss = { selectedWell = null },
            onNavigateToDetails = {
                navController.navigate("${Routes.WELL_DETAILS_SCREEN}/${well.id}")
                selectedWell = null
            },
            onNavigateToDirections = {
                well.getLatitude().let { lat ->
                    well.getLongitude().let { lon ->
                        navController.navigate(
                            "${Routes.COMPASS_SCREEN}?lat=$lat&lon=$lon&name=${well.wellName}"
                        )
                    }
                }
                selectedWell = null
            }
        )
    }
}

private fun android.graphics.drawable.Drawable.toBitmap(): android.graphics.Bitmap {
    if (this is android.graphics.drawable.BitmapDrawable) {
        return bitmap
    }
    val bitmap = createBitmap(intrinsicWidth, intrinsicHeight)
    val canvas = android.graphics.Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}

// Helper extension function
private fun UiState<List<WellData>>.dataOrEmpty(): List<WellData> {
    return if (this is UiState.Success) data else emptyList()
}