// ui/screens/NearbyUsersScreen.kt
package com.bluebridge.bluebridgeapp.ui.screens.userscreens

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
import androidx.compose.material3.AlertDialog
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
import com.bluebridge.bluebridgeapp.data.model.NearbyUser
import com.bluebridge.bluebridgeapp.data.model.NearbyUsersState
import com.bluebridge.bluebridgeapp.events.AppEvent
import com.bluebridge.bluebridgeapp.events.AppEventChannel
import com.bluebridge.bluebridgeapp.events.NearbyUserEvent
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch



@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NearbyUsersScreen(
    nearbyUsersViewModel: NearbyUsersViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val state by nearbyUsersViewModel.state.collectAsState()

    // State variables
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showLargeRadiusDialog by remember { mutableStateOf(false) }
    var radius by remember { mutableDoubleStateOf(50.0) }
    var radiusText by remember { mutableStateOf("50") }
    var initialSearchDone by remember { mutableStateOf(false) }

    // Location handling and initial search
    LaunchedEffect(Unit) {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            showPermissionDialog = true
            return@LaunchedEffect
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        // Get initial location and perform first search
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                currentLocation = it
                lastKnownLocation = it
                if (!initialSearchDone) {
                    performSearch(nearbyUsersViewModel, it, radius, coroutineScope)
                    initialSearchDone = true
                }
            }
        }

        // Setup location updates
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

    // Handle search with radius validation
    fun handleSearch() {
        currentLocation?.let { location ->
            if (radius > 100) {
                showLargeRadiusDialog = true
            } else {
                performSearch(nearbyUsersViewModel, location, radius, coroutineScope)
            }
        } ?: run {
            coroutineScope.launch {
                AppEventChannel.sendEvent(AppEvent.ShowError("Location not available"))
            }
        }
    }

    // Dialogs
    if (showPermissionDialog) {
        LocationPermissionDialog(
            onDismiss = { showPermissionDialog = false },
            onAllow = { showPermissionDialog = false }
        )
    }

    if (showLargeRadiusDialog) {
        AlertDialog(
            onDismissRequest = { showLargeRadiusDialog = false },
            title = { Text("Large Search Radius") },
            text = {
                Text("You're searching a very large area (${radius}km). " +
                        "This may return too much data for your device to handle. " +
                        "Do you want to proceed anyway?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLargeRadiusDialog = false
                        currentLocation?.let { location ->
                            performSearch(nearbyUsersViewModel, location, radius, coroutineScope)
                        }
                    }
                ) {
                    Text("Proceed")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showLargeRadiusDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Main content
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SearchControls(
            radiusText = radiusText,
            onRadiusChange = { newText ->
                if (newText.all { c -> c.isDigit() }) {
                    radiusText = newText
                    radius = newText.toDoubleOrNull()?.coerceIn(1.0, 400000.0) ?: 50.0
                }
            },
            onSearchClick = ::handleSearch,
            isLoading = state is NearbyUsersState.Loading,
            currentLocation = currentLocation
        )

        Spacer(modifier = Modifier.height(16.dp))

        when (state) {
            is NearbyUsersState.Loading -> LoadingState()
            is NearbyUsersState.Success -> UserListState((state as NearbyUsersState.Success).users)
            is NearbyUsersState.Error -> ErrorState(

                (state as NearbyUsersState.Error).message,
                { nearbyUsersViewModel.refreshUsers()

                }
            )
        }
    }
}

@Composable
private fun SearchControls(
    radiusText: String,
    onRadiusChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    isLoading: Boolean,
    currentLocation: Location?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Search Settings", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = radiusText,
                onValueChange = onRadiusChange,
                label = { Text("Radius (km)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Button(
                onClick = onSearchClick,
                modifier = Modifier.align(Alignment.End),
                enabled = !isLoading
            ) {
                if (isLoading) {
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
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Finding nearby users...")
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun UserListState(users: List<NearbyUser>) {
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

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = MaterialTheme.colorScheme.error)
            Log.e("NearbyUsersScreen", "Error: ${message}")
            Button(
                onClick = onRetry,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Retry")
            }
        }
    }
}

private fun handleSearch(
    viewModel: NearbyUsersViewModel,
    location: Location?,
    radius: Double,
    scope: CoroutineScope,
    showDialog: () -> Unit,
    setPending: () -> Unit
) {
    location?.let { loc ->
        if (radius > 100) {
            showDialog()
            setPending()
        } else {
            performSearch(viewModel, loc, radius, scope)
        }
    } ?: run {
        scope.launch {
            AppEventChannel.sendEvent(AppEvent.ShowError("Location not available"))
        }
    }
}

private fun performSearch(
    viewModel: NearbyUsersViewModel,
    location: Location,
    radius: Double,
    scope: CoroutineScope
) {
    try {
        viewModel.handleEvent(
            NearbyUserEvent.SearchUser(
                location.latitude,
                location.longitude,
                radius
            )
        )
    } catch (e: Exception) {
        scope.launch {
            AppEventChannel.sendEvent(
                AppEvent.ShowError("Search failed: ${e.message}")
            )
        }
    }
}