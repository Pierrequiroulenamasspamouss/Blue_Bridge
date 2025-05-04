@file:Suppress("REDUNDANT_ELSE_IN_WHEN")

package com.wellconnect.wellmonitoring.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.wellconnect.wellmonitoring.data.NearbyUserEvent.Refresh
import com.wellconnect.wellmonitoring.data.model.NearbyUser
import com.wellconnect.wellmonitoring.data.model.NearbyUsersState
import com.wellconnect.wellmonitoring.viewmodels.NearbyUsersViewModel

@Composable
fun NearbyUsersScreen(
    nearbyState: NearbyUsersState,
    nearbyUsersViewModel: NearbyUsersViewModel
) {
    val context = LocalContext.current
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    // Separate boolean state to track user-initiated refresh
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Get the current location when the screen is first shown
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            Log.d("NearbyUsersScreen", "Getting current location on initial load")
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { loc ->
                currentLocation = loc
                Log.d("NearbyUsersScreen", "Got location: $loc")
                // Auto-refresh with current location when first loaded
                loc?.let { 
                    Log.d("NearbyUsersScreen", "Auto-refreshing with location on initial load")
                    nearbyUsersViewModel.handleEvent(Refresh(it.latitude, it.longitude, 50.0))
                }
            }
        }
    }
    
    // Reset isRefreshing when the state is no longer Loading
    LaunchedEffect(nearbyState) {
        if (nearbyState !is NearbyUsersState.Loading && isRefreshing) {
            Log.d("NearbyUsersScreen", "State changed from Loading to ${nearbyState::class.simpleName}, resetting isRefreshing")
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
                            Log.d("NearbyUsersScreen", "Refresh button clicked, setting isRefreshing = true")
                            isRefreshing = true
                            currentLocation?.let { loc ->
                                Log.d("NearbyUsersScreen", "Refreshing with current location")
                                nearbyUsersViewModel.handleEvent(Refresh(loc.latitude, loc.longitude, radius))
                            } ?: run {
                                Log.d("NearbyUsersScreen", "Refreshing with default location (location is null)")
                                nearbyUsersViewModel.refreshNearbyUsers(0.0, 0.0, radius)
                            }
                        },
                        enabled = !isRefreshing && nearbyState !is NearbyUsersState.Loading
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Finding nearby users...",
                        style = MaterialTheme.typography.bodyMedium
                    )
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
                    ErrorMessage(message = state.message)
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

// Helper function to format doubles with specified decimal places
fun Double.format(digits: Int) = "%.${digits}f".format(this)

@Composable
fun NearbyUserCard(
    user: NearbyUser,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (user.isOnline) Color.Green else Color.Gray
                    )
                    Column {
                        Text(
                            text = "${user.firstName} ${user.lastName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${user.distance} km away",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (user.waterNeeds.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                Column {
                    Text(
                        text = "Water Needs",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    user.waterNeeds.forEach { need ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${need.amount} liters - ${need.usageType}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                need.description?.let { description ->
                                    Text(
                                        text = description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            PriorityChip(priority = need.priority)
                        }
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}


@Composable
fun PriorityChip(priority: Int) {
    val color = when (priority) {
        0 -> Color(red = 1.0f, green = 0.2f, blue = 0.2f)
        1 -> Color(red = 0.6f, green = 0.2f, blue = 0.2f)
        2 -> Color(red = 0.5f, green = 0.4f, blue = 0.2f)
        3 -> Color(red = 0.5f, green = 0.5f, blue = 0.2f)
        4 -> Color(red = 0.2f, green = 0.4f, blue = 0.2f)
        5 -> Color(red = 0.2f, green = 0.6f, blue = 0.2f)
        else -> MaterialTheme.colorScheme.outline
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(4.dp)
    ) {
        Text(
            text = "P$priority",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = color,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun ErrorMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No nearby users found",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun LocationPermissionDeniedMessage() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Location permission is required to find nearby users",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
    }
}
