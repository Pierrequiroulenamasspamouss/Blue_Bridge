package com.wellconnect.wellmonitoring.ui.screens

import UserData
import WellData
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wellconnect.wellmonitoring.ui.components.WellCard
import com.wellconnect.wellmonitoring.ui.navigation.Routes
import com.wellconnect.wellmonitoring.viewmodels.ActionState
import com.wellconnect.wellmonitoring.viewmodels.UiState
import com.wellconnect.wellmonitoring.viewmodels.UserViewModel
import com.wellconnect.wellmonitoring.viewmodels.WellViewModel
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonitorScreen(wellViewModel: WellViewModel, userViewModel: UserViewModel, navController: NavController) {
    val wellsState = wellViewModel.wellsListState.value
    val wells: List<WellData> = when (wellsState) {
        is UiState.Success -> wellsState.data
        else -> emptyList()
    }
    
    val actionState = wellViewModel.actionState.value
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    // Dialog state for well deletion confirmation
    val (wellToDelete, setWellToDelete) = remember { mutableStateOf<WellData?>(null) }
    
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
                isWellOwner.value = userData.isWellOwner
                isAdmin.value = userData.role.equals("admin", ignoreCase = true)
            }
            
            // Check shared preferences for temporary admin mode
            context.getSharedPreferences("wellconnect", Context.MODE_PRIVATE).let { prefs ->
                tempAdminMode.value = prefs.getBoolean("temp_admin_mode", false)
            }
        }
    }

    // Force refresh the wells list when screen is displayed
    LaunchedEffect(Unit) {
        wellViewModel.loadWells()
    }
    
    // Show snackbar for action state updates
    LaunchedEffect(actionState) {
        when (actionState) {
            is ActionState.Success -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(actionState.message)
                }
            }
            is ActionState.Error -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(actionState.error)
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
                text = "Monitor Wells",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Button(
                onClick = { navController.navigate(Routes.WELL_PICKER_SCREEN) }
            ) {
                Text("Browse Wells")
            }
        }
        
        if (wells.isEmpty()) {
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
                        text = "No wells found",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Add a well to start monitoring",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    if (isWellOwner.value || isAdmin.value || tempAdminMode.value) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                val newWellId = 1
                                navController.navigate("${Routes.WELL_CONFIG_SCREEN}/$newWellId")
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
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(wells) { well ->
                    WellCard(
                        well = well,
                        onItemClick = { wellId ->
                            navController.navigate("${Routes.WELL_DETAILS_SCREEN}/$wellId")
                        },
                        showAdminActions = isWellOwner.value || isAdmin.value || tempAdminMode.value,
                        onDeleteClick = { setWellToDelete(well) }
                    )
                }
                
                // Add new well button for admins and well owners
                item {
                    if (isAdmin.value || tempAdminMode.value) { // TODO: use a role instead of the WellOwner data of the user
                        Button(
                            onClick = {
                                val newWellId = (wells.lastOrNull()?.id?.toInt() ?: 0) + 1
                                navController.navigate("${Routes.WELL_CONFIG_SCREEN}/$newWellId")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
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
        }
    }
    
    // Delete confirmation dialog
    wellToDelete?.let { well ->
        AlertDialog(
            onDismissRequest = { setWellToDelete(null) },
            title = { Text("Delete Well") },
            text = { Text("Are you sure you want to delete the well '${well.wellName}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        wellViewModel.deleteWell(well.espId)
                        setWellToDelete(null)
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { setWellToDelete(null) }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

