package com.wellconnect.wellmonitoring.ui.screens

import ShortenedWellData
import UserData
import WellData
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.navigation.NavController
import com.wellconnect.wellmonitoring.data.`interface`.WellRepository
import com.wellconnect.wellmonitoring.data.model.Location
import com.wellconnect.wellmonitoring.network.RetrofitBuilder
import com.wellconnect.wellmonitoring.ui.components.EnhancedWellCard
import com.wellconnect.wellmonitoring.ui.components.EnhancedWellDetailsDialog
import com.wellconnect.wellmonitoring.ui.components.FiltersSection
import com.wellconnect.wellmonitoring.ui.components.MapView
import com.wellconnect.wellmonitoring.ui.components.calculateDistance
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
                // Fetch stats when dialog is shown
                LaunchedEffect(showStats) {
                    coroutineScope.launch {
                        try {
                            val serverApi = RetrofitBuilder.getServerApi(context)
                            val response = serverApi.getWellsStats()
                            if (response.isSuccessful) {
                                val statsData = response.body()?.stats
                                stats = statsData?.let {
                                    mapOf(
                                        "totalWells" to it.totalWells,
                                        "totalCapacity" to it.totalCapacity,
                                        "totalWaterLevel" to it.totalWaterLevel,
                                        "percentageAvailable" to it.percentageAvailable,
                                        "avgConsumption" to it.avgConsumption,
                                        "statusCounts" to it.statusCounts,
                                        "waterTypeCounts" to it.waterTypeCounts
                                    )
                                }
                            } else {
                                snackbarHostState.showSnackbar("Failed to load statistics")
                            }
                        } catch (e: Exception) {
                            Log.e("WellPickerScreen", "Error fetching stats", e)
                            snackbarHostState.showSnackbar("Error: ${e.message}")
                        }
                    }
                }

                AlertDialog(
                    onDismissRequest = { showStats = false },
                    title = { Text("Well Statistics") },
                    text = {
                        if (stats == null) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            Column {
                                Text("Total Wells: ${stats?.get("totalWells") ?: "Loading..."}")
                                Text("Total Capacity: ${stats?.get("totalCapacity")} L")
                                Text("Water Level: ${stats?.get("totalWaterLevel")} L (${stats?.get("percentageAvailable")}% available)")
                                Text("Average Consumption: ${stats?.get("avgConsumption")} L/day")
                                
                                Spacer(Modifier.height(8.dp))
                                Text("Wells by Status:", style = MaterialTheme.typography.titleMedium)
                                (stats?.get("statusCounts") as? Map<*, *>)?.forEach { (status, count) ->
                                    Text("$status: $count")
                                }
                                
                                Spacer(Modifier.height(8.dp))
                                Text("Wells by Type:", style = MaterialTheme.typography.titleMedium)
                                (stats?.get("waterTypeCounts") as? Map<*, *>)?.forEach { (type, count) ->
                                    Text("$type: $count")
                                }
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

