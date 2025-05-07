@file:Suppress("REDUNDANT_ELSE_IN_WHEN")

package com.wellconnect.wellmonitoring.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.util.Log
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.wellconnect.wellmonitoring.data.NearbyUserEvent.Refresh
import com.wellconnect.wellmonitoring.data.model.NearbyUsersState
import com.wellconnect.wellmonitoring.ui.components.EmptyState
import com.wellconnect.wellmonitoring.ui.components.LocationPermissionDeniedMessage
import com.wellconnect.wellmonitoring.ui.components.NearbyUserCard
import com.wellconnect.wellmonitoring.ui.components.format
import com.wellconnect.wellmonitoring.viewmodels.NearbyUsersViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NearbyUsersScreen(
    nearbyState: NearbyUsersState,
    nearbyUsersViewModel: NearbyUsersViewModel
) {
    val context = LocalContext.current
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    // Add coroutine scope for launching inside Composables
    val coroutineScope = rememberCoroutineScope()
    
    // Separate boolean state to track user-initiated refresh
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Track whether initial location request has been completed
    var initialLocationRequestComplete by remember { mutableStateOf(false) }
    var locationPermissionGranted by remember { 
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
            == PackageManager.PERMISSION_GRANTED
        ) 
    }
    
    // Add timeout for location request
    var locationRequestTimedOut by remember { mutableStateOf(false) }
    
    // Get the current location when the screen is first shown
    LaunchedEffect(Unit) {
        if (locationPermissionGranted) {
            Log.d("NearbyUsersScreen", "Getting current location on initial load")
            try {
                // Start timeout counter
                delay(10000) // 10 seconds timeout
                if (!initialLocationRequestComplete) {
                    Log.d("NearbyUsersScreen", "Location request timed out after 10 seconds")
                    locationRequestTimedOut = true
                    initialLocationRequestComplete = true
                    // If we're still in loading state, use default coordinates
                    if (nearbyState is NearbyUsersState.Loading) {
                        nearbyUsersViewModel.handleEvent(Refresh(0.0, 0.0, 50.0))
                    }
                }
                
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).addOnSuccessListener { loc ->
                    if (!locationRequestTimedOut) {
                        currentLocation = loc
                        initialLocationRequestComplete = true
                        Log.d("NearbyUsersScreen", "Got location: $loc")
                        // Auto-refresh with current location when first loaded
                        loc?.let { 
                            Log.d("NearbyUsersScreen", "Auto-refreshing with location on initial load")
                            nearbyUsersViewModel.handleEvent(Refresh(it.latitude, it.longitude, 50.0))
                        } ?: run {
                            // Handle null location case (show error or use default coords)
                            Log.d("NearbyUsersScreen", "Location is null, using default coordinates")
                            nearbyUsersViewModel.handleEvent(Refresh(0.0, 0.0, 50.0))
                        }
                    } else {
                        // We already timed out but we'll update the location for future refreshes
                        Log.d("NearbyUsersScreen", "Location received after timeout, storing for future use")
                        currentLocation = loc
                    }
                }.addOnFailureListener { e ->
                    if (!locationRequestTimedOut) {
                        initialLocationRequestComplete = true
                        Log.e("NearbyUsersScreen", "Failed to get location", e)
                        // Make sure we don't stay in loading state
                        if (nearbyState is NearbyUsersState.Loading) {
                            nearbyUsersViewModel.handleEvent(Refresh(0.0, 0.0, 50.0))
                        }
                    }
                }
            } catch (e: Exception) {
                if (!locationRequestTimedOut) {
                    initialLocationRequestComplete = true
                    Log.e("NearbyUsersScreen", "Exception getting location", e)
                    // Handle exception
                    if (nearbyState is NearbyUsersState.Loading) {
                        nearbyUsersViewModel.handleEvent(Refresh(0.0, 0.0, 50.0))
                    }
                }
            }
        } else {
            initialLocationRequestComplete = true
            // Handle permission not granted
            if (nearbyState is NearbyUsersState.Loading) {
                nearbyUsersViewModel.handleEvent(Refresh(0.0, 0.0, 50.0))
            }
        }
    }
    
    // Reset isRefreshing when the state is no longer Loading
    LaunchedEffect(nearbyState) {
        if (nearbyState !is NearbyUsersState.Loading && isRefreshing) {
            val stateName = when(nearbyState) {
                is NearbyUsersState.Success -> "Success (${nearbyState.users.size} users)"
                is NearbyUsersState.Error -> "Error (${nearbyState.message})"
                is NearbyUsersState.NoUsers -> "NoUsers"
                is NearbyUsersState.LocationPermissionDenied -> "LocationPermissionDenied"
                else -> nearbyState::class.simpleName
            }
            Log.d("NearbyUsersScreen", "💬 State changed from Loading to $stateName - RESETTING refresh indicator")
            isRefreshing = false
        }
    }

    val uiState = nearbyState
    var radiusText by remember { mutableStateOf("50") }
    val radius = radiusText.toDoubleOrNull() ?: 50.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Nearby Users",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Improved layout with labeled sections
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Search Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = radiusText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '.' }) radiusText = input
                    },
                    label = { Text("Radius (km)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            Log.d("NearbyUsersScreen", "Refresh button clicked")
                            isRefreshing = true
                            
                            // Add safety timer to reset refreshing state after 15 seconds
                            coroutineScope.launch {
                                delay(15000) // 15 seconds timeout
                                if (isRefreshing) {
                                    Log.w("NearbyUsersScreen", "⚠️ Refresh button timeout - Force resetting isRefreshing")
                                    isRefreshing = false
                                }
                            }
                            
                            currentLocation?.let { loc ->
                                Log.d("NearbyUsersScreen", "Refreshing with current location")
                                nearbyUsersViewModel.handleEvent(Refresh(loc.latitude, loc.longitude, radius))
                            } ?: run {
                                Log.d("NearbyUsersScreen", "Refreshing with default location (location is null)")
                                nearbyUsersViewModel.refreshNearbyUsers(0.0, 0.0, radius)
                            }
                        },
                        enabled = !isRefreshing
                    ) {
                        if (isRefreshing || nearbyState is NearbyUsersState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        Text(if (isRefreshing || nearbyState is NearbyUsersState.Loading) "Loading..." else "Refresh")
                    }
                }
                
                // Show current location if available
                currentLocation?.let { loc ->
                    Text(
                        text = "Your location: ${loc.latitude.format(6)}, ${loc.longitude.format(6)}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        
        // Display a clear loading indicator when in Loading state
        if (nearbyState is NearbyUsersState.Loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (locationRequestTimedOut) {
                        Text(
                            text = "Location request timed out. Using default coordinates.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    } else {
                        Text(
                            text = "Finding nearby users...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    
                    Button(
                        onClick = {
                            initialLocationRequestComplete = true
                            locationRequestTimedOut = true
                            nearbyUsersViewModel.handleEvent(Refresh(0.0, 0.0, radius))
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("Skip location request")
                    }
                }
            }
            
            // If there are last known users, show them with some visual indication they're not fresh
            if (nearbyUsersViewModel.lastKnownUsers.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            item {
                                Text(
                                    text = "Last known users (refreshing...)",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            items(nearbyUsersViewModel.lastKnownUsers) { user ->
                                NearbyUserCard(user = user)
                            }
                        }
                    }
                }
            }
        } else {
            // Show results based on state
            when (val state = uiState) {
                is NearbyUsersState.Success -> {
                    if (state.users.isEmpty()){
                        EmptyState()
                    } else {
                        Text(
                            text = "Found ${state.users.size} ${if (state.users.size == 1) "user" else "users"} within ${radius}km",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                        )
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(state.users) { user ->
                                NearbyUserCard(user = user)
                            }
                        }
                    }
                }
                is NearbyUsersState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Button(
                                onClick = {
                                    isRefreshing = true
                                    currentLocation?.let { loc ->
                                        nearbyUsersViewModel.handleEvent(Refresh(loc.latitude, loc.longitude, radius))
                                    } ?: run {
                                        nearbyUsersViewModel.refreshNearbyUsers(0.0, 0.0, radius)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Retry",
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text("Retry")
                            }
                        }
                    }
                    Log.e("NearbyUsersScreen", "Error fetching nearby users: ${state.message}")
                }
                is NearbyUsersState.NoUsers -> {
                    EmptyState()
                }
                is NearbyUsersState.LocationPermissionDenied -> {
                    LocationPermissionDeniedMessage()
                }
                else -> {
                    // This covers any other state including Loading which we've already handled above
                }
            }
        }
    }
}

