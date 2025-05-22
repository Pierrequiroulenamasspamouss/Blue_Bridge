package com.bluebridge.bluebridgeapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bluebridge.bluebridgeapp.data.model.WaterNeed
import com.bluebridge.bluebridgeapp.data.repository.WaterNeedsManager
import com.bluebridge.bluebridgeapp.ui.components.WaterNeedCard
import com.bluebridge.bluebridgeapp.ui.components.WaterNeedDialog
import com.bluebridge.bluebridgeapp.viewmodels.UserViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWaterNeedsScreen(
    userViewModel: UserViewModel,
    navController: NavController
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Initialize WaterNeedsManager
    val waterManager = remember {
        WaterNeedsManager(
            coroutineScope = coroutineScope,
            snackbarHostState = snackbarHostState,
            userViewModel = userViewModel,
            navController = navController
        )
    }

    // Load initial data
    LaunchedEffect(Unit) {
        waterManager.isLoading = true
        try {
            val userData = userViewModel.repository.getUserData().first()
            userData?.waterNeeds?.let { needs ->
                waterManager.waterNeeds.clear()
                waterManager.waterNeeds.addAll(needs)
                if (waterManager.waterNeeds.isEmpty()) {
                    waterManager.waterNeeds.add(WaterNeed(0, "General", "", 3))
                }
            }
        } catch (e: Exception) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Error loading water needs: ${e.message}")
            }
        } finally {
            waterManager.isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Water Needs") },
                colors = topAppBarColors(
                    containerColor = colorScheme.primaryContainer,
                    titleContentColor = colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        ) {
            if (waterManager.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Explanation card
                    InfoCard()

                    // Water needs list
                    if (waterManager.waterNeeds.isEmpty()) {
                        EmptyState()
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(waterManager.waterNeeds.toList()) { need ->
                                val index = waterManager.waterNeeds.indexOf(need)
                                WaterNeedCard(
                                    waterNeed = need,
                                    onEdit = { waterManager.prepareEdit(index) },
                                    onDelete = { waterManager.deleteWaterNeed(index) }
                                )
                            }
                        }
                    }

                    // Action buttons
                    ActionButtons(
                        onAddClick = { waterManager.showAddDialog = true },
                        onSaveClick = { waterManager.saveAll() }
                    )
                }
            }

            // Dialogs
            if (waterManager.showAddDialog) {
                WaterNeedDialog(
                    title = "Add Water Need",
                    state = waterManager,
                    onConfirm = { waterManager.addWaterNeed() },
                    onDismiss = { waterManager.showAddDialog = false }
                )
            }

            if (waterManager.showEditDialog) {
                WaterNeedDialog(
                    title = "Edit Water Need",
                    state = waterManager,
                    onConfirm = { waterManager.updateWaterNeed(waterManager.editingNeedIndex) },
                    onDismiss = { waterManager.showEditDialog = false }
                )
            }

            if (waterManager.showDeleteConfirmDialog) {
                DeleteConfirmationDialog(
                    onConfirm = { waterManager.confirmDelete() },
                    onDismiss = { waterManager.showDeleteConfirmDialog = false }
                )
            }
        }
    }
}

@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Water Needs Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Specify your water needs by type, amount, and priority. This helps well owners understand community needs.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No water needs added yet.\nTap the Add button to create one.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun ActionButtons(
    onAddClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(
            onClick = onAddClick,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Add Water Need")
        }

        Button(
            onClick = onSaveClick,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = "Save",
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Save All")
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Water Need") },
        text = { Text("Are you sure you want to delete this water need?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}