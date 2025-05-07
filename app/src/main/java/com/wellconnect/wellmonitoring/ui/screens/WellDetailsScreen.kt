package com.wellconnect.wellmonitoring.ui.screens

import UserData
import WellData
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wellconnect.wellmonitoring.ui.navigation.Routes
import com.wellconnect.wellmonitoring.viewmodels.UiState
import com.wellconnect.wellmonitoring.viewmodels.UserViewModel
import com.wellconnect.wellmonitoring.viewmodels.WellViewModel
import getLatitude
import getLongitude
import hasValidCoordinates
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellDetailsScreen(
    navController: NavController,
    wellViewModel: WellViewModel,
    wellId: Int,
    userViewModel: UserViewModel
) {
    val userState by userViewModel.state
    val userData = if (userState is UiState.Success) {
        (userState as UiState.Success<UserData>).data
    } else null
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val wellState = wellViewModel.currentWellState.value
    val well = (wellState as? com.wellconnect.wellmonitoring.viewmodels.UiState.Success<WellData>)?.data
    val isLoading = wellState is com.wellconnect.wellmonitoring.viewmodels.UiState.Loading
    // Load well data
    LaunchedEffect(wellId) {
        wellViewModel.loadWell(wellId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(well?.wellName ?: "Well Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (userData?.role == "admin") {
                        IconButton(onClick = {
                            navController.navigate("${Routes.WELL_CONFIG_SCREEN}/$wellId")
                        }) {
                            Icon(Icons.Default.Edit, "Edit")
                        }

                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (well == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Well not found or error loading data")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Basic Info Card
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = well.wellName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Owner: ",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = well.wellOwner.ifBlank { "Not specified" },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Status: ",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = well.wellStatus,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                // Water Specifications
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Water Specifications",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Water Type: ",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = well.wellWaterType.ifBlank { "Not specified" },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Water Level: ",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (well.wellWaterLevel.isBlank()) "Not specified" else "${well.wellWaterLevel}L",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Capacity: ",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (well.wellCapacity.isBlank()) "Not specified" else "${well.wellCapacity}L",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Consumption: ",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (well.wellWaterConsumption.isBlank()) "Not specified" else "${well.wellWaterConsumption}L/day",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                // Location
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Location",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp))



                        Text(
                            text = if (well.hasValidCoordinates())
                                "${well.wellLocation.latitude}, ${well.wellLocation.longitude}"
                            else
                                "Location not specified",
                            style = MaterialTheme.typography.bodyLarge
                        )


                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Compass Button
                            Button(
                                onClick = {
                                    if (well.hasValidCoordinates()) {
                                        val lat = well.getLatitude()
                                        val lon = well.getLongitude()
                                        if (lat != null && lon != null) {
                                            val encodedName = URLEncoder.encode(well.wellName, StandardCharsets.UTF_8.toString())
                                            navController.navigate("${Routes.COMPASS_SCREEN}?lat=$lat&lon=$lon&name=$encodedName")
                                        } else {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Invalid coordinates for this well")
                                            }
                                        }
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("No valid coordinates for this well")
                                        }
                                    }
                                },
                                enabled = well.hasValidCoordinates(),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = "Navigate")
                                Spacer(modifier = Modifier.padding(4.dp))
                                Text("Show directions")
                            }

                            // Map Button
                            Button(
                                onClick = {
                                    if (well.hasValidCoordinates()) {
                                        val lat = well.getLatitude()
                                        val lon = well.getLongitude()
                                        if (lat != null && lon != null) {
                                            navController.navigate("${Routes.MAP_SCREEN}?targetLat=$lat&targetLon=$lon")
                                        } else {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Invalid coordinates for this well")
                                            }
                                        }
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("No valid coordinates for this well")
                                        }
                                    }
                                },
                                enabled = well.hasValidCoordinates(),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Map, contentDescription = "Show on Map")
                                Spacer(modifier = Modifier.padding(4.dp))
                                Text("View on Map")
                            }
                        }
                    }
                }
            }
        }
    }
} 