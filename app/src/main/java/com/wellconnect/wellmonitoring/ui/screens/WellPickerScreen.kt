package com.wellconnect.wellmonitoring.ui.screens

import ShortenedWellData
import UserData
import WellData
import android.annotation.SuppressLint
import android.os.Build
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.wellconnect.wellmonitoring.data.`interface`.WellRepository
import com.wellconnect.wellmonitoring.data.model.Location
import com.wellconnect.wellmonitoring.ui.navigation.Routes
import com.wellconnect.wellmonitoring.utils.fetchAllWellsFromServer
import com.wellconnect.wellmonitoring.utils.fetchWellDetailsFromServer
import getLatitude
import getLongitude
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.InternalSerializationApi
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(InternalSerializationApi::class, ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun WellPickerScreen(
    userData: UserData?,
    navController: NavController,
    wellRepository: WellRepository
) {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var allWells by remember { mutableStateOf<List<ShortenedWellData>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var showNearbyOnly by remember { mutableStateOf(false) }
    var userLocation: Location? by remember { mutableStateOf<Location?>(userData?.location) }
    var selectedWell by remember { mutableStateOf<WellData?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // State for view type (list or map)
    var showMap by remember { mutableStateOf(false) }
    
    // Filter states
    var showFilters by remember { mutableStateOf(false) }
    var selectedWaterType by remember { mutableStateOf<String?>(null) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }
    var capacityRange by remember { mutableStateOf(0f..10000f) }
    var showStats by remember { mutableStateOf(false) }
    var stats by remember { mutableStateOf<Map<String, Any>?>(null) }

    // Fetch user location (if needed for "Nearby")
    LaunchedEffect(showNearbyOnly) {
        if (showNearbyOnly) {
            userLocation = userData?.location
        }
    }

    // Fetch all wells from server_crt on first load
    LaunchedEffect(Unit) {
        isLoading = true
        allWells = fetchAllWellsFromServer(
            snackbarHostState = snackbarHostState,
            context = context,
            maxRetries = 3
        )
        isLoading = false
    }

    // Filtered list
    val filteredWells = allWells.filter { well ->
        val matchesSearch = searchQuery.isBlank() ||
                well.wellName.contains(searchQuery, ignoreCase = true)
        
        val matchesWaterType = selectedWaterType == null || well.wellWaterType == selectedWaterType
        // No need to filter by status, capacity or consumption as these aren't in ShortenedWellData
        
        val loc = userLocation
        val isNearby = if (showNearbyOnly && loc != null) {
            val wellLat = well.getLatitude()
            val wellLon = well.getLongitude()
            if (wellLat != null && wellLon != null) {
                calculateDistance(loc.latitude, loc.longitude, wellLat, wellLon) < 50000 // 50km
            } else false
        } else true
        
        matchesSearch && isNearby && matchesWaterType
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Browse Wells") },
                actions = {
                    IconButton(onClick = { showMap = !showMap }) {
                        Icon(
                            if (showMap) Icons.Default.Search else Icons.Default.Map, 
                            contentDescription = if (showMap) "List View" else "Map View"
                        )
                    }
                    IconButton(onClick = { showStats = !showStats }) {
                        Icon(Icons.Default.BarChart, "Statistics")
                    }
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(Icons.Default.FilterList, "Filters")
                    }
                }
            )
        },
        floatingActionButton = {
            // Only show if the user is a well owner or admin
            if (userData?.isWellOwner == true || userData?.role == "admin") {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate("${Routes.WELL_CONFIG_SCREEN}/-1") },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                    text = { Text("Add New Well") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // Search bar with icon
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search wells...") },
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                leadingIcon = { Icon(Icons.Default.Search, "Search") }
            )
            
            // Filters section
            if (showFilters) {
                FiltersSection(
                    selectedWaterType = selectedWaterType,
                    onWaterTypeSelected = { selectedWaterType = it },
                    selectedStatus = selectedStatus,
                    onStatusSelected = { selectedStatus = it },
                    capacityRange = capacityRange,
                    onCapacityRangeChange = { capacityRange = it },
                    showNearbyOnly = showNearbyOnly,
                    onNearbyOnlyChange = { showNearbyOnly = it }
                )
            }

            // Stats dialog
            if (showStats) {
                AlertDialog(
                    onDismissRequest = { showStats = false },
                    title = { Text("Well Statistics") },
                    text = {
                        Column {
                            Text("Total Wells: ${stats?.get("totalWells") ?: "Loading..."}")
                            Text("Total Capacity: ${stats?.get("totalCapacity")} L")
                            Text("Current Water Level: ${stats?.get("totalCurrentLevel")} L")
                            Text("Daily Consumption: ${stats?.get("totalConsumption")} L")
                            
                            Spacer(Modifier.height(8.dp))
                            Text("Wells by Status:", style = MaterialTheme.typography.titleMedium)
                            (stats?.get("wellsByStatus") as? Map<*, *>)?.forEach { (status, count) ->
                                Text("$status: $count")
                            }
                            
                            Spacer(Modifier.height(8.dp))
                            Text("Wells by Type:", style = MaterialTheme.typography.titleMedium)
                            (stats?.get("wellsByType") as? Map<*, *>)?.forEach { (type, count) ->
                                Text("$type: $count")
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showStats = false }) {
                            Text("Close")
                        }
                    }
                )
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (showMap) {
                // Map View
                MapView(
                    wells = filteredWells,
                    userLocation = userLocation,
                    onWellClicked = { well ->
                        coroutineScope.launch {
                            isLoading = true
                            val fullWell = fetchWellDetailsFromServer(
                                well.espId,
                                snackbarHostState = snackbarHostState,
                                context = context
                            )
                            isLoading = false
                            selectedWell = fullWell
                        }
                    },
                    onNavigateToWell = { latitude, longitude, name ->
                        val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
                        navController.navigate(
                            "${Routes.COMPASS_SCREEN}?lat=$latitude&lon=$longitude&name=$encodedName"
                        )
                    }
                )
            } else {
                // List View
                LazyColumn(Modifier.weight(1f)) {
                    items(filteredWells) { well ->
                        EnhancedWellCard(
                            well = well, 
                            onClick = {
                                coroutineScope.launch {
                                    isLoading = true
                                    val fullWell = fetchWellDetailsFromServer(
                                        well.espId,
                                        snackbarHostState = snackbarHostState,
                                        context = context
                                    )
                                    isLoading = false
                                    selectedWell = fullWell
                                }
                            },
                            onNavigateClick = { 
                                val latitude = well.getLatitude()
                                val longitude = well.getLongitude()
                                if (latitude != null && longitude != null) {
                                    val encodedName = URLEncoder.encode(well.wellName, StandardCharsets.UTF_8.toString())
                                    navController.navigate(
                                        "${Routes.COMPASS_SCREEN}?lat=$latitude&lon=$longitude&name=$encodedName"
                                    )
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Well location not available")
                                    }
                                }
                            }
                        )
                    }
                }
            }
            
            // Well details dialog
            selectedWell?.let { well ->
                EnhancedWellDetailsDialog(
                    well = well,
                    onAdd = {
                        coroutineScope.launch {
                            // Check if we already have a well with this EspId
                            val currentWells = wellRepository.wellListFlow.first()
                            val duplicateExists = currentWells.any { it.espId == well.espId }
                            
                            if (duplicateExists) {
                                snackbarHostState.showSnackbar("This well is already in your list")
                            } else {
                                // Find the highest existing ID and increment by 1, or start with 1 if list is empty
                                val newId = (currentWells.maxOfOrNull { it.id } ?: 0) + 1
                                
                                // Create a new well with the new ID but keep all other properties
                                val newWell = well.copy(id = newId)
                                
                                wellRepository.saveWell(newWell)
                                snackbarHostState.showSnackbar("Well added to your list")
                                selectedWell = null
                            }
                        }
                    },
                    onDismiss = { selectedWell = null },
                    onNavigate = {
                        val latitude = well.getLatitude()
                        val longitude = well.getLongitude()
                        if (latitude != null && longitude != null) {
                            val encodedName = URLEncoder.encode(well.wellName, StandardCharsets.UTF_8.toString())
                            navController.navigate(
                                "${Routes.COMPASS_SCREEN}?lat=$latitude&lon=$longitude&name=$encodedName"
                            )
                            selectedWell = null
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Well location not available")
                            }
                        }
                    }
                )
            }
        }
    }
}

// Helper function to calculate distance between two points using Haversine formula
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6371000.0 // meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * acos(sqrt(a))
    return earthRadius * c
}

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapView(
    wells: List<ShortenedWellData>,
    userLocation: Location?,
    onWellClicked: (ShortenedWellData) -> Unit,
    onNavigateToWell: (Double, Double, String) -> Unit
) {
    var webView: WebView? = null

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    webViewClient = WebViewClient()
                    
                    // Load basic OpenStreetMap with markers for wells
                    val wellMarkers = wells.mapNotNull { well ->
                        val lat = well.getLatitude()
                        val lon = well.getLongitude()
                        if (lat != null && lon != null) {
                            """
                            L.marker([${lat}, ${lon}], {title: "${well.wellName}"})
                              .addTo(map)
                              .bindPopup("${well.wellName}<br>${well.wellWaterType}<br><button onclick='navigateToWell(${lat},${lon},\"${well.wellName}\")'>Navigate</button><button onclick='viewWellDetails(\"${well.espId}\")'>Details</button>");
                            """
                        } else null
                    }.joinToString("\n")
                    
                    // Center point based on user location or first well
                    val centerLat = userLocation?.latitude ?: wells.firstOrNull()?.getLatitude() ?: 0.0
                    val centerLon = userLocation?.longitude ?: wells.firstOrNull()?.getLongitude() ?: 0.0
                    
                    // Set up user location marker if available
                    val userMarker = if (userLocation != null) {
                        """
                        L.marker([${userLocation.latitude}, ${userLocation.longitude}], {
                            icon: L.divIcon({
                                className: 'user-location',
                                html: '<div style="background-color:blue;width:15px;height:15px;border-radius:50%;"></div>'
                            })
                        }).addTo(map).bindPopup("Your location");
                        """
                    } else ""
                    
                    val html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.7.1/dist/leaflet.css" />
                        <script src="https://unpkg.com/leaflet@1.7.1/dist/leaflet.js"></script>
                        <style>
                            html, body { height: 100%; margin: 0; }
                            #map { height: 100%; width: 100%; }
                            .user-location { border-radius: 50%; }
                        </style>
                    </head>
                    <body>
                        <div id="map"></div>
                        <script>
                            var map = L.map('map').setView([${centerLat}, ${centerLon}], 10);
                            
                            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                            }).addTo(map);
                            
                            $wellMarkers
                            $userMarker
                            
                            // JavaScript function to communicate back to the app
                            function viewWellDetails(espId) {
                                window.AndroidInterface.onWellSelected(espId);
                            }
                            
                            function navigateToWell(lat, lon, name) {
                                window.AndroidInterface.navigateToWell(lat, lon, name);
                            }
                        </script>
                    </body>
                    </html>
                    """.trimIndent()
                    
                    // Set up JavaScript interface for callback from WebView
                    addJavascriptInterface(object : Any() {
                        @JavascriptInterface
                        fun onWellSelected(espId: String) {
                            val well = wells.find { it.espId == espId }
                            well?.let { onWellClicked(it) }
                        }
                        
                        @JavascriptInterface
                        fun navigateToWell(lat: Double, lon: Double, name: String) {
                            onNavigateToWell(lat, lon, name)
                        }
                    }, "AndroidInterface")
                    
                    loadDataWithBaseURL("https://openstreetmap.org", html, "text/html", "UTF-8", null)
                    webView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun EnhancedWellCard(
    well: ShortenedWellData, 
    onClick: () -> Unit,
    onNavigateClick: () -> Unit
) {
    // Calculate water level percentage safely outside composable
    val waterLevelInfo = if (well.wellWaterLevel.toString().isNotBlank() && well.wellCapacity.isNotBlank()) {
        calculateWaterLevelInfo(well.wellWaterLevel, well.wellCapacity)
    } else null

    Card(
        Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(well.wellName, style = MaterialTheme.typography.titleMedium)
                StatusIndicator(well.wellStatus)
            }
            
            Text(text = "Latitude: ${well.wellLocation.latitude}\n Longitude: ${well.wellLocation.longitude}" , style = MaterialTheme.typography.bodySmall)
            
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Type: ${well.wellWaterType}", style = MaterialTheme.typography.bodySmall)
                
                if (well.wellCapacity.isNotBlank()) {
                    Text("Capacity: ${well.wellCapacity}L", style = MaterialTheme.typography.bodySmall)
                }
            }
            
            // Water level indicator using pre-calculated values
            waterLevelInfo?.let { (progress, percentage) ->
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    color = when {
                        percentage > 70 -> Color.Green
                        percentage > 30 -> Color.Yellow
                        else -> Color.Red
                    }
                )
                Text("Water Level: $percentage%", style = MaterialTheme.typography.bodySmall)
            }
            
            // Navigate button
            Button(
                onClick = onNavigateClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Navigate")
            }
        }
    }
}

@Composable
fun EnhancedWellDetailsDialog(
    well: WellData,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
    onNavigate: () -> Unit
) {
    // Pre-calculate water level progress outside of composable
    val waterLevelProgress = if (well.wellWaterLevel.isNotBlank() && well.wellCapacity.isNotBlank()) {
        calculateWaterLevelProgress(well.wellWaterLevel, well.wellCapacity)
    } else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(well.wellName) },
        text = {
            Column {
                if (well.wellOwner.isNotBlank()) {
                    Text("Owner: ${well.wellOwner}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                }
                
                Text("Location: ${well.wellLocation}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                
                Text("Type: ${well.wellWaterType}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                
                if (well.wellCapacity.isNotBlank()) {
                    Text("Capacity: ${well.wellCapacity}L", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                }
                
                if (well.wellStatus.isNotBlank()) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Status:", style = MaterialTheme.typography.bodyMedium)
                        StatusIndicator(well.wellStatus)
                    }
                }
                
                if (well.wellWaterLevel.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Water Level: ${well.wellWaterLevel}L", style = MaterialTheme.typography.bodyMedium)
                    
                    // Use pre-calculated progress value
                    waterLevelProgress?.let { progress ->
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }
                }
                
                if (well.wellWaterConsumption.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Consumption: ${well.wellWaterConsumption}L/day", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(onClick = onAdd) { Text("Add to My Wells") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) { 
                    Text("Cancel") 
                }
                TextButton(onClick = onNavigate) { 
                    Text("Navigate") 
                }
            }
        }
    )
}

// Helper functions to safely calculate water level information outside composables
private fun calculateWaterLevelInfo(waterLevelStr: String, capacityStr: String): Pair<Float, Int>? {
    return try {
        val waterLevel = waterLevelStr.toFloatOrNull() ?: return null
        val capacity = capacityStr.toFloatOrNull() ?: return null
        if (capacity <= 0) return null
        
        val progress = (waterLevel / capacity).coerceIn(0f, 1f)
        val percentage = (waterLevel / capacity * 100).toInt()
        Pair(progress, percentage)
    } catch (_: Exception) {
        null
    }
}

private fun calculateWaterLevelProgress(waterLevelStr: String, capacityStr: String): Float? {
    return try {
        val waterLevel = waterLevelStr.toFloatOrNull() ?: return null
        val capacity = capacityStr.toFloatOrNull() ?: return null
        if (capacity <= 0) return null
        
        (waterLevel / capacity).coerceIn(0f, 1f)
    } catch (_: Exception) {
        null
    }
}

@Composable
fun FiltersSection(
    selectedWaterType: String?,
    onWaterTypeSelected: (String?) -> Unit,
    selectedStatus: String?,
    onStatusSelected: (String?) -> Unit,
    capacityRange: ClosedFloatingPointRange<Float>,
    onCapacityRangeChange: (ClosedFloatingPointRange<Float>) -> Unit,
    showNearbyOnly: Boolean,
    onNearbyOnlyChange: (Boolean) -> Unit
) {
    Column(Modifier.padding(8.dp)) {
        Text("Filters", style = MaterialTheme.typography.titleMedium)
        
        // Water Type filters
        Row(Modifier.padding(vertical = 4.dp)) {
            FilterChip(
                selected = selectedWaterType == "Clean",
                onClick = { onWaterTypeSelected(if (selectedWaterType != "Clean") "Clean" else null) },
                label = { Text("Clean") }
            )
            Spacer(Modifier.width(4.dp))
            FilterChip(
                selected = selectedWaterType == "Grey",
                onClick = { onWaterTypeSelected(if (selectedWaterType != "Grey") "Grey" else null) },
                label = { Text("Grey") }
            )
        }
        
        // Status filters
        Row(Modifier.padding(vertical = 4.dp)) {
            FilterChip(
                selected = selectedStatus == "Active",
                onClick = { onStatusSelected(if (selectedStatus != "Active") "Active" else null) },
                label = { Text("Active") }
            )
            Spacer(Modifier.width(4.dp))
            FilterChip(
                selected = selectedStatus == "Maintenance",
                onClick = { onStatusSelected(if (selectedStatus != "Maintenance") "Maintenance" else null) },
                label = { Text("Maintenance") }
            )
            Spacer(Modifier.width(4.dp))
            FilterChip(
                selected = selectedStatus == "Inactive",
                onClick = { onStatusSelected(if (selectedStatus != "Inactive") "Inactive" else null) },
                label = { Text("Inactive") }
            )
        }
        
        // Capacity range
        Text("Capacity Range: ${capacityRange.start.toInt()} - ${capacityRange.endInclusive.toInt()}")
        RangeSlider(
            value = capacityRange,
            onValueChange = onCapacityRangeChange,
            valueRange = 0f..10000f,
            steps = 20
        )
        
        // Nearby filter
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = showNearbyOnly,
                onCheckedChange = onNearbyOnlyChange
            )
            Text("Show only nearby wells")
        }
    }
}

@Composable
fun StatusIndicator(status: String) {
    val color = when (status) {
        "Active" -> Color.Green
        "Maintenance" -> Color.Yellow
        "Inactive" -> Color.Red
        else -> Color.Gray
    }
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(4.dp))
        Text(status, style = MaterialTheme.typography.bodySmall)
    }
}