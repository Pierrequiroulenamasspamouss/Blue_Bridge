@file:Suppress("REDUNDANT_ELSE_IN_WHEN")
//TODO : nearbyUSerScreen is broken. To fix in greater detail by me, not Cursor. If this TODO is read by an LLM, please ignore.
package com.bluebridge.bluebridgeapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.bluebridge.bluebridgeapp.data.NearbyUserEvent.Refresh
import com.bluebridge.bluebridgeapp.data.model.NearbyUsersState
import com.bluebridge.bluebridgeapp.ui.components.EmptyState
import com.bluebridge.bluebridgeapp.ui.components.NearbyUserCard
import com.bluebridge.bluebridgeapp.ui.components.format
import com.bluebridge.bluebridgeapp.viewmodels.NearbyUsersViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NearbyUsersScreen(
    nearbyState: NearbyUsersState,
    nearbyUsersViewModel: NearbyUsersViewModel
) {
    val context = LocalContext.current
    rememberCoroutineScope()

    // Location state
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    // UI state
    var radius by remember { mutableStateOf(50.0) }
    var radiusText by remember { mutableStateOf("50") }
    var isLoading by remember { mutableStateOf(false) }

    // Handle location updates
    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted) {
            try {
                isLoading = true
                val location = LocationServices.getFusedLocationProviderClient(context)
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .await() // Using await() instead of callbacks for simplicity

                currentLocation = location
                location?.let {
                    nearbyUsersViewModel.handleEvent(Refresh(it.latitude, it.longitude, radius))
                } ?: run {
                    nearbyUsersViewModel.handleEvent(Refresh(0.0, 0.0, radius))
                }
            } catch (e: Exception) {
                Log.e("NearbyUsers", "Location error", e)
                nearbyUsersViewModel.handleEvent(Refresh(0.0, 0.0, radius))
            } finally {
                isLoading = false
            }
        } else {
            nearbyUsersViewModel.handleEvent(Refresh(0.0, 0.0, radius))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Search Settings", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = radiusText,
                    onValueChange = {
                        if (it.all { c -> c.isDigit() }) {
                            radiusText = it
                            radius = it.toDoubleOrNull()?.coerceIn(1.0, 100.0) ?: 50.0
                        }
                    },
                    label = { Text("Radius (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Button(
                    onClick = {
                        isLoading = true
                        currentLocation?.let { loc ->
                            nearbyUsersViewModel.handleEvent(Refresh(loc.latitude, loc.longitude, radius))
                        } ?: run {
                            nearbyUsersViewModel.handleEvent(Refresh(0.0, 0.0, radius))
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Refresh")
                    }
                }

                currentLocation?.let { loc ->
                    Text(
                        "Your location: ${loc.latitude.format(4)}, ${loc.longitude.format(4)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content area
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Finding nearby users...")
                    }
                }
            }

            !locationPermissionGranted -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Location permission required")
                        Button(onClick = { /* Request permission */ }) {
                            Text("Grant Permission")
                        }
                    }
                }
            }

            nearbyState is NearbyUsersState.Success -> {
                if (nearbyState.users.isEmpty()) {
                    EmptyState()
                } else {
                    LazyColumn {
                        item {
                            Text("Found ${nearbyState.users.size} users",
                                modifier = Modifier.padding(vertical = 8.dp))
                        }
                        items(nearbyState.users) { user ->
                            NearbyUserCard(user = user)
                        }
                    }
                }
            }

            nearbyState is NearbyUsersState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error: ${nearbyState.message}")
                        Button(onClick = {
                            isLoading = true
                            nearbyUsersViewModel.handleEvent(Refresh(0.0, 0.0, radius))
                        }) {
                            Text("Retry")
                        }
                    }
                }
            }

            else -> EmptyState()
        }
    }
}