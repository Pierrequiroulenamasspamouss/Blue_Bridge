package com.wellconnect.wellmonitoring.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wellconnect.wellmonitoring.data.NearbyUser
import com.wellconnect.wellmonitoring.data.NearbyUsersState
import com.wellconnect.wellmonitoring.viewmodels.NearbyUsersViewModel

@Composable
fun NearbyUsersScreen(
    viewModel: NearbyUsersViewModel = hiltViewModel(),
    onNavigateToCompass: (Double, Double, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

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

        IconButton(
            onClick = { viewModel.refreshNearbyUsers() },
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
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
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
                        text = user.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${user.distance} km away",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            IconButton(
                onClick = { onNavigateToCompass(user.latitude, user.longitude, user.name) }
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Navigate to ${user.name}"
                )
            }
        }
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