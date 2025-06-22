@file:Suppress("UNCHECKED_CAST")

package com.bluebridge.bluebridgeapp.ui.screens.wellscreens

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.navigation.NavController
import com.bluebridge.bluebridgeapp.data.local.WellPreferences
import com.bluebridge.bluebridgeapp.data.model.Location
import com.bluebridge.bluebridgeapp.data.model.ShortenedWellData
import com.bluebridge.bluebridgeapp.data.model.UserData
import com.bluebridge.bluebridgeapp.data.model.WellData
import com.bluebridge.bluebridgeapp.data.model.getLatitude
import com.bluebridge.bluebridgeapp.data.model.getLongitude
import com.bluebridge.bluebridgeapp.events.AppEvent
import com.bluebridge.bluebridgeapp.events.AppEventChannel
import com.bluebridge.bluebridgeapp.navigation.Routes
import com.bluebridge.bluebridgeapp.network.RetrofitBuilder
import com.bluebridge.bluebridgeapp.ui.components.EnhancedWellCard
import com.bluebridge.bluebridgeapp.ui.components.FiltersSection
import com.bluebridge.bluebridgeapp.ui.components.MapView
import com.bluebridge.bluebridgeapp.ui.dialogs.EnhancedWellDetailsDialog
import kotlinx.coroutines.launch
import kotlinx.serialization.InternalSerializationApi
import java.net.URLEncoder

@OptIn(InternalSerializationApi::class, ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BrowseWellsScreen(

    userData: UserData?,
    navController: NavController

) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Pagination state
    var currentPage by remember { mutableIntStateOf(1) }
    val pageSize = 20
    var hasMorePages by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }

    // Filter states
    var searchQuery by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    var selectedWaterType by remember { mutableStateOf<String?>(null) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }
    var minWaterLevel by remember { mutableStateOf<Int?>(null) }
    var maxWaterLevel by remember { mutableStateOf<Int?>(null) }
    var showNearbyOnly by remember { mutableStateOf(false) }
    var userLocation: Location? by remember { mutableStateOf<Location?>(userData?.location) }

    // View state
    var showMap by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var selectedWell by remember { mutableStateOf<WellData?>(null) }
    var wells by remember { mutableStateOf<List<WellData>>(emptyList()) }
    val wellPreferences = WellPreferences(context)
    // Load initial data
    LaunchedEffect(Unit) {
        loadWells(
            page = currentPage,
            pageSize = pageSize,
            searchQuery = searchQuery,
            waterType = selectedWaterType,
            status = selectedStatus,
            minWaterLevel = minWaterLevel,
            maxWaterLevel = maxWaterLevel,
            context = context,
            onSuccess = { newWells, hasMore ->
                wells = newWells
                hasMorePages = hasMore
            }
        )
    }

    // Load more when reaching end of list
    LaunchedEffect(wells.size, hasMorePages) {
        if (!isLoading && hasMorePages) {
            currentPage++
            isLoading = true
            coroutineScope.launch {
                loadWells(
                    page = currentPage,
                    pageSize = pageSize,
                    searchQuery = searchQuery,
                    waterType = selectedWaterType,
                    status = selectedStatus,
                    minWaterLevel = minWaterLevel,
                    maxWaterLevel = maxWaterLevel,
                    context = context,
                    onSuccess = { newWells, hasMore ->
                        wells = wells + newWells
                        hasMorePages = hasMore
                        isLoading = false
                    }
                )
            }
        }
    }

    // Apply filters with search button
    val applyFilters = {
        currentPage = 1
        isLoading = true
        coroutineScope.launch {
            loadWells(
                page = currentPage,
                pageSize = pageSize,
                searchQuery = searchQuery,
                waterType = selectedWaterType,
                status = selectedStatus,
                minWaterLevel = minWaterLevel,
                maxWaterLevel = maxWaterLevel,
                context = context,
                onSuccess = { newWells, hasMore ->
                    wells = newWells
                    hasMorePages = hasMore
                    isLoading = false
                }
            )
        }
    }


    // Capacity range filter
    var capacityRange by remember { mutableStateOf<ClosedRange<Int>?>(null) }
    val updateCapacityRange = { range: ClosedRange<Int>? ->
        capacityRange = range
        minWaterLevel = range?.start
        maxWaterLevel = range?.endInclusive
        applyFilters()
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
            if (userData?.role == "well_owner" || userData?.role == "admin") {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Routes.WELL_CONFIG_NEW) },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                    text = { Text("Add New Well") }
                )
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth(),
                placeholder = { Text("Search well by name or description") },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = {
                        // Call getWellsWithFilters with current filter values
                        applyFilters()
                    }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )

            // Filters section
            if (showFilters) {
                FiltersSection(
                    selectedWaterType = selectedWaterType,
                    selectedStatus = selectedStatus,
                    showNearbyOnly = showNearbyOnly,
                    onWaterTypeSelected = { selectedWaterType = it },
                    onStatusSelected = { selectedStatus = it },
                    capacityRange = capacityRange,
                    onCapacityRangeChange = updateCapacityRange,
                    onNearbyOnlyChange = { showNearbyOnly = it }
                )
            }

            // Content
            if (showMap) {
                // Map view
                MapView(
                    wells = wells.map { well ->
                        ShortenedWellData( // Ensure the fields here match the expected type
                            wellName = well.wellName, // Assuming these fields exist in WellData
                            wellLocation = well.wellLocation, // Handle potential nulls
                            wellWaterType = well.wellWaterType, // Assuming these fields exist in WellData
                            wellStatus = well.wellStatus, // Assuming these fields exist in WellData
                            wellCapacity = well.wellCapacity, // Handle potential nulls
                            wellWaterLevel = well.wellWaterLevel, // Handle potential nulls
                            espId = well.espId // Handle potential nulls
                        )
                    }.toList(), // Make sure it's a List<ShortenedWellData>
                    userLocation = userLocation
                )
            } else {
                // List view
                if (wells.isEmpty() && !isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No wells found with the applied filters",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(wells) { well ->
                            EnhancedWellCard(
                                well = well,
                                onClick = {
                                    selectedWell = well
                                },
                                onNavigateClick = {
                                    val encodedName = URLEncoder.encode(well.wellName, "UTF-8")
                                    val lat = well.getLatitude()
                                    val lon = well.getLongitude()

                                    navController.navigate("${Routes.COMPASS_SCREEN}?lat=$lat&lon=$lon&name=$encodedName")

                                }
                            )
                        }

                        if (hasMorePages) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }

        // Well details dialog
        selectedWell?.let { well ->
            EnhancedWellDetailsDialog(
                well = well,
                onDismiss = { selectedWell = null },
                onMoreDetails = {
                    // Use the temporary route for espId
                    selectedWell = null
                    navController.navigate("${Routes.WELL_DETAILS_TEMP_SCREEN}/${well.espId}")
                },
                onAdd = {
                    coroutineScope.launch {
                        wellPreferences.saveWell(well)
                    }
                    selectedWell = null
                    //navController.navigate(Routes.MONITORING_SCREEN) //Should it go back th the monitoring screen after adding a well ?
                }
            )
        }
    }
}

private suspend fun loadWells(
    page: Int,
    pageSize: Int,
    searchQuery: String,
    waterType: String?,
    status: String?,
    minWaterLevel: Int?,
    maxWaterLevel: Int?,
    context: Context,
    onSuccess: (List<WellData>, Boolean) -> Unit
) {
    try {
        val serverApi = RetrofitBuilder.getServerApi(context)
        val response = serverApi.getWellsWithFilters(
            page = page,
            limit = pageSize,
            wellName = searchQuery.takeIf { it.isNotBlank() },
            wellStatus = status,
            wellWaterType = waterType,
            minWaterLevel = minWaterLevel,
            maxWaterLevel = maxWaterLevel
        )

        if (response.isSuccessful) {
            val wellsResponse = response.body()
            if (wellsResponse != null) {
                Log.d("BrowseWellsScreen", "Wells loaded successfully")
                onSuccess(wellsResponse.data, 10 > page * pageSize)
            } else {
                AppEventChannel.sendEvent(AppEvent.ShowError("No wells found"))
            }
        } else {
            AppEventChannel.sendEvent(AppEvent.ShowError("Failed to load wells"))
        }
    } catch (e: Exception) {
        Log.e("BrowseWellsScreen", "Error loading wells", e)
        AppEventChannel.sendEvent(AppEvent.ShowError("Error: ${e.message}"))
    }
}