@file:Suppress("DEPRECATION")

package com.bluebridge.bluebridgeapp.ui.screens

import android.os.Build
import android.preference.PreferenceManager
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.bluebridge.bluebridgeapp.R
import com.bluebridge.bluebridgeapp.data.model.WellData
import com.bluebridge.bluebridgeapp.data.model.getLatitude
import com.bluebridge.bluebridgeapp.data.model.getLongitude
import com.bluebridge.bluebridgeapp.data.model.hasValidCoordinates
import com.bluebridge.bluebridgeapp.ui.dialogs.WellDetailsDialog
import com.bluebridge.bluebridgeapp.ui.navigation.Routes
import com.bluebridge.bluebridgeapp.viewmodels.UiState
import com.bluebridge.bluebridgeapp.viewmodels.WellViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

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
    val context = LocalContext.current
    rememberCoroutineScope()
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
        Box(modifier = Modifier.fillMaxSize()) {
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

                        // Add user location marker if available
                        userLat?.let { lat ->
                            userLon?.let { lon ->
                                Marker(this).apply {
                                    position = GeoPoint(lat, lon)
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    title = "Your Location"
                                    icon = ContextCompat.getDrawable(context, R.drawable.small_map_arrow)
                                    setInfoWindowAnchor(0.5f, 0f)
                                    overlays.add(this)
                                }
                            }
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

            // Focus button (floating action button)
            if (userLat != null && userLon != null) {
                androidx.compose.material3.FloatingActionButton(
                    onClick = {
                        mapViewRef?.controller?.apply {
                            animateTo(GeoPoint(userLat, userLon))
                            setZoom(15.0)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(innerPadding) // Apply the padding here
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Focus on my location")
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



// Helper extension function
private fun UiState<List<WellData>>.dataOrEmpty(): List<WellData> {
    return if (this is UiState.Success) data else emptyList()
}