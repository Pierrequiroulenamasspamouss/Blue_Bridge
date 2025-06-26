package com.bluebridgeapp.bluebridge.ui.screens.wellscreens

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.bluebridgeapp.bluebridge.data.model.UserData
import com.bluebridgeapp.bluebridge.data.model.WellData
import com.bluebridgeapp.bluebridge.data.model.getLatitude
import com.bluebridgeapp.bluebridge.data.model.getLongitude
import com.bluebridgeapp.bluebridge.events.AppEvent
import com.bluebridgeapp.bluebridge.events.AppEventChannel
import com.bluebridgeapp.bluebridge.navigation.Routes
import com.bluebridgeapp.bluebridge.ui.components.WellCard
import com.bluebridgeapp.bluebridge.viewmodels.ActionState
import com.bluebridgeapp.bluebridge.viewmodels.UiState
import com.bluebridgeapp.bluebridge.viewmodels.UserViewModel
import com.bluebridgeapp.bluebridge.viewmodels.WellViewModel
import kotlinx.coroutines.launch
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonitorScreen(
    wellViewModel: WellViewModel,
    userViewModel: UserViewModel,
    navController: NavController
) {
    // State variables
    val wellsState = wellViewModel.wellsListState.value
    val wellsList = if (wellsState is UiState.Success) wellsState.data else emptyList()
    val actionState = wellViewModel.actionState.value
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var wellToDelete by remember { mutableStateOf<WellData?>(null) }
    var expanded by remember { mutableStateOf(false) }

    // User role states
    val (isWellOwner, setIsWellOwner) = remember { mutableStateOf(false) }
    val (isAdmin, setIsAdmin) = remember { mutableStateOf(false) }
    val (tempAdminMode, setTempAdminMode) = remember { mutableStateOf(false) }

    // Effects
    LaunchedEffect(userViewModel.state.value) {
        (userViewModel.state.value as? UiState.Success<UserData>)?.data?.let { userData ->
            setIsWellOwner(userData.role.equals("well_owner", ignoreCase = true))
            setIsAdmin(userData.role.equals("admin", ignoreCase = true))
            setTempAdminMode(
                context.getSharedPreferences("bluebridge", Context.MODE_PRIVATE)
                    .getBoolean("temp_admin_mode", false)
            )
        }
    }

    LaunchedEffect(Unit) {
        wellViewModel.getSavedWells()
        Log.d("MonitorScreen", "Loaded saved wells")
    }

    LaunchedEffect(actionState) {
        when (actionState) {
            is ActionState.Success -> {
            }
            is ActionState.Error -> {
                coroutineScope.launch {
                    AppEventChannel.sendEvent(AppEvent.ShowError(actionState.error))
                }
            }
            else -> Unit
        }
    }

    // Main UI
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Wells") },
                actions = {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, "More options")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Refresh, "Refresh", Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Refresh")
                                }
                            },
                            onClick = {
                                wellViewModel.getSavedWells()
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Browse Wells") },
                            onClick = {
                                navController.navigate(Routes.BROWSE_WELLS_SCREEN)
                                expanded = false
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (isWellOwner || isAdmin || tempAdminMode) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate("${Routes.WELL_CONFIG_SCREEN}/new") },
                    icon = { Icon(Icons.Default.Add, "Add Well") },
                    text = { Text("Add New Well") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (wellsState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is UiState.Error -> {
                    Text(
                        "Error: ${wellsState.message}",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is UiState.Success -> {
                    if (wellsList.isEmpty()) {
                        EmptyWellsState(
                            isWellOwner = isWellOwner,
                            isAdmin = isAdmin,
                            navController = navController
                        )
                    } else {
                        WellsList(
                            wellsList = wellsList,
                            isWellOwner = isWellOwner,
                            isAdmin = isAdmin,
                            tempAdminMode = tempAdminMode,
                            navController = navController,
                            onDeleteClick = { wellToDelete = it }
                        )
                    }
                }
                UiState.Empty -> {
                    EmptyWellsState(
                        isWellOwner = isWellOwner,
                        isAdmin = isAdmin,
                        navController = navController
                    )
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
                            well.id.let { id ->
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
}

@Composable
private fun EmptyWellsState(
    isWellOwner: Boolean,
    isAdmin: Boolean,
    navController: NavController
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("No wells saved", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Text(
            "Browse wells to add them to your monitoring list",
            style = MaterialTheme.typography.bodyLarge
        )

        if (isWellOwner || isAdmin ) {
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { navController.navigate(Routes.BROWSE_WELLS_SCREEN) }
            ) {
                Icon(Icons.Default.Add, "Add Well", Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Browse existing Wells")
            }
        }
    }
}
@Composable
private fun WellsList(
    wellsList: List<WellData>,
    isWellOwner: Boolean,
    isAdmin: Boolean,
    tempAdminMode: Boolean,
    navController: NavController,
    onDeleteClick: (WellData) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(wellsList.size) { index ->
            val well = wellsList[index]

            WellCard(
                well = well,
                isWellOwner = isWellOwner,
                showAdminActions = isAdmin || tempAdminMode,
                showLastRefresh = true,
                showLastUpdate = true,
                onEdit = {
                    navController.navigate("${Routes.WELL_CONFIG_SCREEN}/${well.id}")
                },
                onItemClick = {
                    val encodedName = URLEncoder.encode(well.wellName, "UTF-8")
                    val lat = well.getLatitude()
                    val lon = well.getLongitude()
                    navController.navigate("${Routes.COMPASS_SCREEN}?lat=$lat&lon=$lon&name=$encodedName")
                },
                onNavigate = {
                    navController.navigate("${Routes.WELL_DETAILS_SCREEN}/${well.id}")
                },
                onDeleteClick = { onDeleteClick(well) }
            )
        }
    }
}