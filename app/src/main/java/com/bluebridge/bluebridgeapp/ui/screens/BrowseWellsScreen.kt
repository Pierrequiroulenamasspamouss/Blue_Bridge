package com.bluebridge.bluebridgeapp.ui.screens

import ShortenedWellData
import WellData
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bluebridge.bluebridgeapp.data.`interface`.WellRepository
import com.bluebridge.bluebridgeapp.data.model.Location
import com.bluebridge.bluebridgeapp.data.model.UserData
import com.bluebridge.bluebridgeapp.network.RetrofitBuilder
import com.bluebridge.bluebridgeapp.ui.components.EnhancedWellCard
import com.bluebridge.bluebridgeapp.ui.components.EnhancedWellDetailsDialog
import com.bluebridge.bluebridgeapp.ui.components.FiltersSection
import com.bluebridge.bluebridgeapp.ui.components.MapView
import com.bluebridge.bluebridgeapp.ui.navigation.Routes
import com.bluebridge.bluebridgeapp.utils.fetchWellDetailsFromServer
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
    val snackbarHostState = remember { SnackbarHostState() }

    // Pagination state
    var currentPage by remember { mutableStateOf(1) }
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
    var wells by remember { mutableStateOf<List<ShortenedWellData>>(emptyList()) }
    var stats by remember { mutableStateOf<Map<String, Any>?>(null) }

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
            snackbarHostState = snackbarHostState,
            onSuccess = { newWells, hasMore ->
                wells = newWells
                hasMorePages = hasMore
            }
        )
    }

    // Load more when reaching end of list
    val loadMore = {
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
                    snackbarHostState = snackbarHostState,
                    onSuccess = { newWells, hasMore ->
                        wells = wells + newWells
                        hasMorePages = hasMore
                        isLoading = false
                    }
                )
            }
        }
    }
    //TODO: the filtering is done by the server (with the research), not the app. Fix that. the filters should have a "search" button to send a request to the server for the filtered list
    // Apply filters
    LaunchedEffect(searchQuery, selectedWaterType, selectedStatus, minWaterLevel, maxWaterLevel) {
        currentPage = 1
        isLoading = true
        loadWells(
            page = currentPage,
            pageSize = pageSize,
            searchQuery = searchQuery,
            waterType = selectedWaterType,
            status = selectedStatus,
            minWaterLevel = minWaterLevel,
            maxWaterLevel = maxWaterLevel,
            context = context,
            snackbarHostState = snackbarHostState,
            onSuccess = { newWells, hasMore ->
                wells = newWells
                hasMorePages = hasMore
                isLoading = false
            }
        )
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
                    onClick = { navController.navigate("${Routes.WELL_CONFIG_SCREEN}/-1") },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                    text = { Text("Add New Well") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // Search bar
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
                    showNearbyOnly = showNearbyOnly,
                    onNearbyOnlyChange = { showNearbyOnly = it },
                    capacityRange = (minWaterLevel?.toFloat() ?: 0f)..(maxWaterLevel?.toFloat() ?: 1000f),
                    onCapacityRangeChange = { range ->
                        minWaterLevel = range.start.toInt()
                        maxWaterLevel = range.endInclusive.toInt()
                    }
                )
            }

            // Content
            if (isLoading && wells.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (showMap) {
                MapView(
                    wells = wells,
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
                LazyColumn(Modifier.weight(1f)) {
                    items(wells) { well ->
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

        // Well details dialog
        selectedWell?.let { well ->
            EnhancedWellDetailsDialog(
                well = well,
                onAdd = {
                    coroutineScope.launch {
                        val currentWells = wellRepository.wellListFlow.first()
                        val duplicateExists = currentWells.any { it.espId == well.espId }

                        if (duplicateExists) {
                            snackbarHostState.showSnackbar("This well is already in your list")
                        } else {
                            val newId = (currentWells.maxOfOrNull { it.id } ?: 0) + 1
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

private suspend fun loadWells(
    page: Int,
    pageSize: Int,
    searchQuery: String,
    waterType: String?,
    status: String?,
    minWaterLevel: Int?,
    maxWaterLevel: Int?,
    context: Context,
    snackbarHostState: SnackbarHostState,
    onSuccess: (List<ShortenedWellData>, Boolean) -> Unit
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
                Log.d("WellPickerScreen", "Wells loaded successfully")
                onSuccess(wellsResponse.wells, 10 > page * pageSize) //TODO : fix that because there is no defined value for the max pages , 10 is a placeholder. there should be a max size, defined by the server response {"status":"success","data":[],"pagination":{"total":0,"page":1,"limit":20,"pages":0}}.
            //TODO : also make a grayed out message for saying there is no wells found with the applied filters if the list is empty ("data":[])
            } else {
                snackbarHostState.showSnackbar("No wells found")
            }
        } else {
            snackbarHostState.showSnackbar("Failed to load wells")
        }
    } catch (e: Exception) {
        Log.e("WellPickerScreen", "Error loading wells", e)
        snackbarHostState.showSnackbar("Error: ${e.message}")
    }
}

