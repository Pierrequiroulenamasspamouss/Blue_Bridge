package com.wellconnect.wellmonitoring.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.location.Location
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.wellconnect.wellmonitoring.data.WellData
import com.wellconnect.wellmonitoring.data.formatLocation
import com.wellconnect.wellmonitoring.data.getLatitude
import com.wellconnect.wellmonitoring.data.getLongitude
import com.wellconnect.wellmonitoring.data.hasValidCoordinates
import com.wellconnect.wellmonitoring.ui.TopBar
import com.wellconnect.wellmonitoring.ui.WellViewModel
import com.wellconnect.wellmonitoring.ui.navigation.Routes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale

@Composable
fun MonitorScreen(userViewModel: WellViewModel, navController: NavController) {
    val wells by userViewModel.wellList.collectAsState()
    val errorMessage by userViewModel.errorMessage
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isRefreshingAll = remember { mutableStateOf(false) }
    
    // Get current location
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Update current location
    @SuppressLint("MissingPermission")
    LaunchedEffect(Unit) {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location ->
                currentLocation = location
            }
        }
    }

    // Handle error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
                userViewModel.resetErrorMessage()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            TopBar(topBarMessage = "Monitor Wells")
            
            // Refresh All Button
            Button(
                onClick = {
                    scope.launch {
                        isRefreshingAll.value = true
                        try {
                            val (success, total) = userViewModel.refreshAllWells(context)
                            snackbarHostState.showSnackbar("Refreshed $success/$total wells")
                        } finally {
                            isRefreshingAll.value = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = !isRefreshingAll.value
            ) {
                if (isRefreshingAll.value) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refreshing...")
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh All")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh All Wells")
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                itemsIndexed(wells) { index, well ->
                    WellCard(
                        well = well,
                        index = index,
                        totalWells = wells.size,
                        onEditClick = { wellId ->
                            navController.navigate("${Routes.WELL_CONFIG_SCREEN}/$wellId")
                        },
                        onNavigateClick = { lat, lon, name ->
                            navController.navigate("${Routes.COMPASS_SCREEN}?lat=$lat&lon=$lon&name=$name")
                        },
                        onMoveUp = { userViewModel.exchangeWells(index, index - 1) },
                        onMoveDown = { userViewModel.exchangeWells(index, index + 1) },
                        onDelete = { userViewModel.removeWellByIndex(index) },
                        onRefresh = {
                            scope.launch {
                                val success = userViewModel.refreshSingleWell(well.id, context)
                                val message = if (success) {
                                    "Successfully refreshed ${well.wellName}"
                                } else {
                                    userViewModel.errorMessage.value ?: "Refresh failed"
                                }
                                snackbarHostState.showSnackbar(message)
                            }
                        },
                        currentLocation = currentLocation,
                        snackbarHostState = snackbarHostState,
                        scope = scope
                    )
                }

                item {
                    Button(
                        onClick = {
                            val newWellId = (wells.lastOrNull()?.id ?: 0) + 1
                            navController.navigate("${Routes.WELL_CONFIG_SCREEN}/$newWellId")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Text("Add New Well")
                    }
                }
            }
        }
    }
}

@Composable
private fun WellCard(
    well: WellData,
    index: Int,
    totalWells: Int,
    onEditClick: (String) -> Unit,
    onNavigateClick: (Double, Double, String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onRefresh: () -> Unit,
    currentLocation: Location?,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    var expanded by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = well.wellName.ifBlank { "Unnamed Well ${well.id}" },
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            if (well.hasValidCoordinates()) {
                                val lat = well.getLatitude()
                                val lon = well.getLongitude()
                                if (lat != null && lon != null) {
                                    onNavigateClick(
                                        lat,
                                        lon,
                                        java.net.URLEncoder.encode(well.wellName, "UTF-8")
                                    )
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
                        modifier = Modifier.size(40.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (well.hasValidCoordinates()) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("Go", style = MaterialTheme.typography.labelMedium)
                    }

                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            if (index > 0) {
                                DropdownMenuItem(
                                    text = { Text("Move Up") },
                                    onClick = {
                                        onMoveUp()
                                        expanded = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.KeyboardArrowUp, "Move Up") }
                                )
                            }
                            if (index < totalWells - 1) {
                                DropdownMenuItem(
                                    text = { Text("Move Down") },
                                    onClick = {
                                        onMoveDown()
                                        expanded = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.KeyboardArrowDown, "Move Down") }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    onEditClick(well.id.toString())
                                    expanded = false
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, "Edit") }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    onDelete()
                                    expanded = false
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, "Delete") }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isRefreshing) "Refreshing..." else "Refresh") },
                                onClick = {
                                    isRefreshing = true
                                    onRefresh()
                                    expanded = false
                                    isRefreshing = false
                                },
                                leadingIcon = { Icon(Icons.Default.Refresh, "Refresh") },
                                enabled = !isRefreshing
                            )
                        }
                    }
                }
            }
            
            Text(
                text = "Status: ${well.wellStatus}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = well.formatLocation(),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Water Level: ${well.wellWaterLevel} L",
                style = MaterialTheme.typography.bodyMedium
            )

            // Add current distance if available
            if (well.hasValidCoordinates() && currentLocation != null) {
                val lat = well.getLatitude()
                val lon = well.getLongitude()
                if (lat != null && lon != null) {
                    val distance = calculateDistance(
                        currentLocation.latitude, currentLocation.longitude,
                        lat, lon
                    )
                    
                    Text(
                        text = "Current distance: ${
                            when {
                                distance >= 1000 -> "%.1f km".format(distance / 1000)
                                else -> "${distance.toInt()} m"
                            }
                        }",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    // Add estimated travel times
                    val (walkTime, driveTime) = calculateTravelTime(distance)
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        Text(
                            text = "🚶 Walking: $walkTime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "🚗 Driving: $driveTime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (well.lastRefreshTime > 0) {
                Text(
                    text = "Last updated: ${getRelativeTimeSpanString(well.lastRefreshTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

private fun calculateDistance(
    lat1: Double, lon1: Double,
    lat2: Double, lon2: Double
): Float {
    val results = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, results)
    return results[0]
}

@SuppressLint("DefaultLocale")
private fun calculateTravelTime(distanceMeters: Float): Pair<String, String> {
    // Walking speed: 5 km/h = 1.389 m/s
    val walkingSpeedMS = 1.389f
    // Average driving speed: 40 km/h = 11.111 m/s (considering urban areas)
    val drivingSpeedMS = 11.111f

    val walkingTimeSeconds = distanceMeters / walkingSpeedMS
    val drivingTimeSeconds = distanceMeters / drivingSpeedMS

    fun formatTime(seconds: Float): String {
        return when {
            seconds < 60 -> "${seconds.toInt()} seconds"
            seconds < 3600 -> "${(seconds / 60).toInt()} minutes"
            else -> String.format("%.1f hours", seconds / 3600)
        }
    }

    return Pair(formatTime(walkingTimeSeconds), formatTime(drivingTimeSeconds))
}

private fun getRelativeTimeSpanString(timeInMillis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timeInMillis
    
    return when {
        diff < 1000L -> "just now"
        diff < 60000L -> "${diff / 1000} seconds ago"
        diff < 3600000L -> "${diff / 60000} minutes ago"
        diff < 86400000L -> "${diff / 3600000} hours ago"
        diff < 604800000L -> "${diff / 86400000} days ago"
        diff < 2592000000L -> "${diff / 604800000} weeks ago"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timeInMillis))
    }
}