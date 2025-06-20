// ui/screens/NearbyUsersScreen.kt
package com.bluebridge.bluebridgeapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import com.bluebridge.bluebridgeapp.data.AppEvent
import com.bluebridge.bluebridgeapp.data.AppEventChannel
import com.bluebridge.bluebridgeapp.data.NearbyUserEvent
import com.bluebridge.bluebridgeapp.data.model.NearbyUsersState
import com.bluebridge.bluebridgeapp.ui.components.EmptyState
import com.bluebridge.bluebridgeapp.ui.components.NearbyUserCard
import com.bluebridge.bluebridgeapp.ui.components.format
import com.bluebridge.bluebridgeapp.ui.dialogs.LocationPermissionDialog
import com.bluebridge.bluebridgeapp.utils.lastKnownLocation
import com.bluebridge.bluebridgeapp.viewmodels.NearbyUsersViewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NearbyUsersScreen(
    nearbyUsersViewModel: NearbyUsersViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val state by nearbyUsersViewModel.state.collectAsState()

    // Location state
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Search parameters
    var radius by remember { mutableDoubleStateOf(50.0) }
    var radiusText by remember { mutableStateOf("50") }

    // Location handling
    LaunchedEffect(Unit) {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            showPermissionDialog = true
            return@LaunchedEffect
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                currentLocation = it
                lastKnownLocation = it
                nearbyUsersViewModel.handleEvent(
                    NearbyUserEvent.SearchUser(it.latitude, it.longitude, radius)
                )
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY, 5000
                ).build(),
                object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        result.lastLocation?.let {
                            currentLocation = it
                            lastKnownLocation = it
                        }
                    }
                },
                null
            )
        } catch (_: Exception) {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY, null
            ).addOnSuccessListener { location ->
                location?.let {
                    currentLocation = it
                    lastKnownLocation = it
                }
            }
        }
    }

    if (showPermissionDialog) {
        LocationPermissionDialog(
            onDismiss = { showPermissionDialog = false },
            onAllow = { showPermissionDialog = false }
        )
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
                        currentLocation?.let { location ->
                            nearbyUsersViewModel.handleEvent(
                                NearbyUserEvent.SearchUser(
                                    location.latitude,
                                    location.longitude,
                                    radius
                                )
                            )
                        } ?: run {
                            coroutineScope.launch {
                                AppEventChannel.sendEvent(
                                    AppEvent.ShowError("Location not available")
                                )
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.End),
                    enabled = state !is NearbyUsersState.Loading
                ) {
                    if (state is NearbyUsersState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Search")
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
        when (state) {
            is NearbyUsersState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Finding nearby users...")
                    }
                }
            }
            is NearbyUsersState.Success -> {
                val users = (state as NearbyUsersState.Success).users
                if (users.isEmpty()) {
                    EmptyState()
                } else {
                    LazyColumn {
                        item {
                            Text(
                                "Found ${users.size} users",
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(users) { user ->
                            NearbyUserCard(user = user)
                        }
                    }
                }
            }
            is NearbyUsersState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            (state as NearbyUsersState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(
                            onClick = { nearbyUsersViewModel.refreshUsers() },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}