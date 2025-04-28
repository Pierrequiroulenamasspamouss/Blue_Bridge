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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.wellconnect.wellmonitoring.data.NearbyUser
import com.wellconnect.wellmonitoring.viewmodels.NearbyUsersState
import com.wellconnect.wellmonitoring.viewmodels.NearbyUsersViewModel

@Composable
fun NearbyUsersScreen(
    navController: NavController,
    onNavigateToCompass: (Double, Double, String) -> Unit
) {
    val context = LocalContext.current
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { loc ->
                currentLocation = loc
            }
        }
    }
    val viewModel: NearbyUsersViewModel = viewModel { NearbyUsersViewModel(context) }
    val uiState by viewModel.uiState.collectAsState()
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

        IconButton(
            onClick = {
                currentLocation?.let { loc ->
                    viewModel.refreshNearbyUsers(loc.latitude, loc.longitude, radius)
                } ?: run {
                    viewModel.refreshNearbyUsers(0.0, 0.0, radius)
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh"
            )
        }

        when (val state = uiState) {
            is NearbyUsersState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is NearbyUsersState.Success -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.users) { user ->
                        NearbyUserCard(
                            user = user,
                            onNavigateToCompass = onNavigateToCompass
                        )
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
        }
    }
}
@Composable
fun NearbyUserCard(
    user: NearbyUser,
    onNavigateToCompass: (Double, Double, String) -> Unit
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
//                        Text(
//                            text = user.username,
//                            style = MaterialTheme.typography.titleMedium ,
//                            fontWeight = FontWeight.Bold)
                        Text(
                            text = "${user.distance} km away",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

//                IconButton(
//                    onClick = {
//                        onNavigateToCompass(
//                            user.location.latitude,
//                            user.location.longitude,
//                            "${user.firstName} ${user.lastName}"
//                        )
//                    }
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.LocationOn,
//                        contentDescription = "Navigate to ${user.firstName}"
//                    )
//                }
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
                            // Priority chip will now always be visible
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
