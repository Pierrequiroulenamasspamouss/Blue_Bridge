package com.bluebridge.bluebridgeapp.ui.screens

import android.content.Context
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.bluebridge.bluebridgeapp.data.AppEvent
import com.bluebridge.bluebridgeapp.data.AppEventChannel
import com.bluebridge.bluebridgeapp.data.model.UserData
import com.bluebridge.bluebridgeapp.data.model.WellData
import com.bluebridge.bluebridgeapp.ui.components.EnhancedWellCard
import com.bluebridge.bluebridgeapp.ui.navigation.Routes
import com.bluebridge.bluebridgeapp.viewmodels.ActionState
import com.bluebridge.bluebridgeapp.viewmodels.UiState
import com.bluebridge.bluebridgeapp.viewmodels.UserViewModel
import com.bluebridge.bluebridgeapp.viewmodels.WellViewModel
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonitorScreen(wellViewModel: WellViewModel, userViewModel: UserViewModel, navController: NavController) {
    // Observe the wells list state from ViewModel
    val wellsState = wellViewModel.wellsListState.value
    val wellsList = when (wellsState) {
        is UiState.Success -> wellsState.data
        else -> emptyList()
    }

    val actionState = wellViewModel.actionState.value
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    // Dialog state for well deletion confirmation
    var wellToDelete by remember { mutableStateOf<WellData?>(null) }

    // Get current user data to check if well owner or admin
    val userState = userViewModel.state.value
    val isWellOwner = remember { mutableStateOf(false) }
    val isAdmin = remember { mutableStateOf(false) }
    val tempAdminMode = remember { mutableStateOf(false) }

    // Get user data from UserViewModel
    LaunchedEffect(userState) {
        if (userState is UiState.Success<*>) {
            val userData = userState.data as? UserData
            if (userData != null) {
                isWellOwner.value = userData.role.equals("well_owner", ignoreCase = true)
                isAdmin.value = userData.role.equals("admin", ignoreCase = true)
            }

            // Check shared preferences for temporary admin mode
            context.getSharedPreferences("bluebridge", Context.MODE_PRIVATE).let { prefs ->
                tempAdminMode.value = prefs.getBoolean("temp_admin_mode", false)
            }
        }
    }

    // Load locally saved wells when the screen is displayed
    LaunchedEffect(Unit) {
        wellViewModel.getSavedWells()
        Log.d("MonitorScreen", "got saved wells: ")
    }

    // Show snackbar for action state updates
    LaunchedEffect(actionState) {
        when (actionState) {
            is ActionState.Success -> {
                coroutineScope.launch {
                    AppEventChannel.sendEvent(AppEvent.ShowError(actionState.message))

                }
            }
            is ActionState.Error -> {
                coroutineScope.launch {
                    AppEventChannel.sendEvent(AppEvent.ShowError(actionState.error))
                }
            }
            else -> { /* Do nothing */ }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Wells",
                style = MaterialTheme.typography.headlineMedium
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { wellViewModel.getSavedWells() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh")
                }

                Button(
                    onClick = { navController.navigate(Routes.BROWSE_WELLS_SCREEN) }
                ) {
                    Text("Browse Wells")
                }
            }
        }

        when (wellsState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Error: ${wellsState.message}")
                }
            }
            is UiState.Success -> {
                if (wellsList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No wells saved",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Browse wells to add them to your monitoring list",
                                style = MaterialTheme.typography.bodyLarge
                            )

                            if (isWellOwner.value || isAdmin.value || tempAdminMode.value) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = {
                                        navController.navigate("${Routes.WELL_CONFIG_SCREEN}/new")
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Well",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add New Well")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(wellsList.size) { index ->
                            val well = wellsList[index]
                            EnhancedWellCard(
                                well = well,
                                onClick = {
                                    navController.navigate("${Routes.WELL_DETAILS_SCREEN}/${well.id}")
                                },
                                onNavigateClick = {
                                    navController.navigate(
                                        "${Routes.COMPASS_SCREEN}?" +
                                                "lat=${well.wellLocation?.latitude}" +
                                                "&lon=${well.wellLocation?.longitude}" +
                                                "&name=${well.wellName}"
                                    )
                                },
                                onDeleteClick = { wellToDelete = well }
                            )
                        }
                    }
                }
            }
            UiState.Empty -> {
                // Handle empty state if needed
            }
        }
    }

    // Well deletion confirmation dialog
    wellToDelete?.let { well ->
        AlertDialog(
            onDismissRequest = { wellToDelete = null },
            title = { Text("Delete Well") },
            text = { Text("Are you sure you want to delete this well? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        well.id?.let { id ->
                            wellViewModel.deleteWell(id.toString())
                        }
                        wellToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { wellToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}