package com.wellconnect.wellmonitoring.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.wellconnect.wellmonitoring.data.model.WaterNeed
import com.wellconnect.wellmonitoring.viewmodels.UserViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWaterNeedsScreen(navController: NavHostController, userViewModel: UserViewModel) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingNeedIndex by remember { mutableStateOf(-1) }
    
    // State for current water needs
    val waterNeeds = remember { mutableStateListOf<WaterNeed>() }
    
    // State for new water need
    var newAmount by remember { mutableStateOf("") }
    var newAmountSlider by remember { mutableFloatStateOf(50f) }
    var newUsageType by remember { mutableStateOf("") }
    var newDescription by remember { mutableStateOf("") }
    var newPriority by remember { mutableStateOf(3) }
    var customType by remember { mutableStateOf("") }
    
    // Add this state for delete confirmation
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var deletingNeedIndex by remember { mutableStateOf(-1) }
    
    // Load user data when screen is opened
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val userData = userViewModel.repository.getUserData().first()
            userData?.let {
                // Make a copy of the list to avoid modification issues
                waterNeeds.clear()
                waterNeeds.addAll(it.waterNeeds)
                if (waterNeeds.isEmpty()) {
                    // Add a default water need if none exist
                    waterNeeds.add(WaterNeed(0, "General", "", 3))
                }
            }
        } catch (e: Exception) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Error loading water needs: ${e.message}")
            }
        } finally {
            isLoading = false
        }
    }

    // Add water need function
    fun addWaterNeed() {
        val amount = newAmount.toIntOrNull() ?: 0
        if (amount > 0) {
            val usageType = if (newUsageType == "Other") customType else newUsageType
            if (usageType.isNotBlank()) {
                val newNeed = WaterNeed(
                    amount = amount,
                    usageType = usageType,
                    description = newDescription,
                    priority = newPriority
                )
                waterNeeds.add(newNeed)
                
                // Reset fields
                newAmount = ""
                newAmountSlider = 50f
                newUsageType = ""
                newDescription = ""
                newPriority = 3
                customType = ""
                showAddDialog = false
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Please select a usage type")
                }
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Please enter a valid amount")
            }
        }
    }
    
    // Update water need function
    fun updateWaterNeed(index: Int) {
        val amount = newAmount.toIntOrNull() ?: 0
        if (amount > 0) {
            val usageType = if (newUsageType == "Other") customType else newUsageType
            if (usageType.isNotBlank()) {
                val updatedNeed = WaterNeed(
                    amount = amount,
                    usageType = usageType,
                    description = newDescription,
                    priority = newPriority
                )
                waterNeeds[index] = updatedNeed
                
                // Reset fields
                editingNeedIndex = -1
                newAmount = ""
                newAmountSlider = 50f
                newUsageType = ""
                newDescription = ""
                newPriority = 3
                customType = ""
                showEditDialog = false
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Please select a usage type")
                }
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Please enter a valid amount")
            }
        }
    }
    
    // Delete water need function
    fun deleteWaterNeed(index: Int) {
        deletingNeedIndex = index
        showDeleteConfirmDialog = true
    }
    
    // Confirm delete function
    fun confirmDeleteWaterNeed() {
        waterNeeds.removeAt(deletingNeedIndex)
        showDeleteConfirmDialog = false
    }
    
    // Save all water needs function
    fun saveAllWaterNeeds() {
        isLoading = true
        coroutineScope.launch {
            try {
                if (waterNeeds.isEmpty()) {
                    waterNeeds.add(WaterNeed(0, "General", "", 3))
                }
                
                val success = userViewModel.repository.updateWaterNeedsOnServer(waterNeeds)
                if (success) {
                    snackbarHostState.showSnackbar("Water needs updated successfully")
                    navController.popBackStack()
                } else {
                    snackbarHostState.showSnackbar("Failed to update water needs")
                    isLoading = false
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error: ${e.message}")
                isLoading = false
            }
        }
    }
    
    // Prepare edit function
    fun prepareEdit(index: Int) {
        val need = waterNeeds[index]
        newAmount = need.amount.toString()
        newAmountSlider = need.amount.toFloat()
        newUsageType = if (need.usageType in listOf("Absolute emergency", "Medical", "Drinking", "Farming", "Industry")) {
            need.usageType
        } else {
            "Other"
        }
        newDescription = need.description
        newPriority = need.priority
        customType = if (need.usageType !in listOf("Absolute emergency", "Medical", "Drinking", "Farming", "Industry")) {
            need.usageType
        } else {
            ""
        }
        editingNeedIndex = index
        showEditDialog = true
    }
    
    // Show add/edit dialogs within the composable
    if (showAddDialog) {
        WaterNeedDialog(
            title = "Add Water Need",
            amount = newAmount,
            amountSlider = newAmountSlider,
            usageType = newUsageType,
            description = newDescription,
            priority = newPriority,
            customType = customType,
            onAmountChange = { newAmount = it; newAmountSlider = it.toFloatOrNull() ?: 0f },
            onAmountSliderChange = { newAmountSlider = it; newAmount = it.toInt().toString() },
            onUsageTypeChange = { newUsageType = it },
            onDescriptionChange = { newDescription = it },
            onPriorityChange = { newPriority = it },
            onCustomTypeChange = { customType = it },
            onConfirm = { addWaterNeed() },
            onDismiss = { showAddDialog = false }
        )
    }
    
    if (showEditDialog) {
        WaterNeedDialog(
            title = "Edit Water Need",
            amount = newAmount,
            amountSlider = newAmountSlider,
            usageType = newUsageType,
            description = newDescription,
            priority = newPriority,
            customType = customType,
            onAmountChange = { newAmount = it; newAmountSlider = it.toFloatOrNull() ?: 0f },
            onAmountSliderChange = { newAmountSlider = it; newAmount = it.toInt().toString() },
            onUsageTypeChange = { newUsageType = it },
            onDescriptionChange = { newDescription = it },
            onPriorityChange = { newPriority = it },
            onCustomTypeChange = { customType = it },
            onConfirm = { updateWaterNeed(editingNeedIndex) },
            onDismiss = { showEditDialog = false }
        )
    }
    
    // Add the delete confirmation dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Water Need") },
            text = { Text("Are you sure you want to delete this water need?") },
            confirmButton = {
                Button(
                    onClick = { confirmDeleteWaterNeed() },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Edit Water Needs") },
                colors = topAppBarColors(
                    containerColor = colorScheme.primaryContainer,
                    titleContentColor = colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        // Move the Add button to the topAppBar actions
        floatingActionButton = { TODO() }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Explanation card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
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
                    
                    // Water needs list
                    if (waterNeeds.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No water needs added yet.\nTap the Add button to create one.",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f)
                        ) {
                            items(waterNeeds.toList()) { need ->
                                val index = waterNeeds.indexOf(need)
                                WaterNeedCard(
                                    waterNeed = need,
                                    onEdit = { prepareEdit(index) },
                                    onDelete = { deleteWaterNeed(index) }
                                )
                            }
                        }
                    }
                    
                    // Add buttons in a row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Add Water Need button
                        Button(
                            onClick = { showAddDialog = true },
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
                        
                        // Save button
                        Button(
                            onClick = { saveAllWaterNeeds() },
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
            }
        }
    }
}

@Composable
fun WaterNeedCard(
    waterNeed: WaterNeed,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon and title in a Row with width constraint
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f) // This ensures the title takes only available space
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    
                    Column(modifier = Modifier.weight(1f)) {
                        // Truncate long titles
                        val displayUsageType = if (waterNeed.usageType.length > 15) {
                            waterNeed.usageType.take(15) + "..."
                        } else {
                            waterNeed.usageType
                        }
                        
                        Text(
                            text = "${waterNeed.amount} liters - $displayUsageType",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Text(
                            text = "Priority: P${waterNeed.priority}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        if (!waterNeed.description.isNullOrBlank()) {
                            Text(
                                text = waterNeed.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                // Fixed-width action buttons
                Row(
                    modifier = Modifier.width(100.dp), // Fixed width for buttons
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = colorScheme.primary
                        )
                    }
                    
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterNeedDialog(
    title: String,
    amount: String,
    amountSlider: Float,
    usageType: String,
    description: String,
    priority: Int,
    customType: String,
    onAmountChange: (String) -> Unit,
    onAmountSliderChange: (Float) -> Unit,
    onUsageTypeChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPriorityChange: (Int) -> Unit,
    onCustomTypeChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val usageTypes = listOf("Absolute emergency", "Medical", "Drinking", "Farming", "Industry", "Other")
    var expanded by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Amount input with slider
                Text(
                    text = "Amount (liters)",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                // Amount slider
                Text(
                    text = "${amountSlider.toInt()} liters",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                
                Slider(
                    value = amountSlider,
                    onValueChange = onAmountSliderChange,
                    valueRange = 0f..1000f,
                    steps = 0
                )
                
                // Manual amount input
                OutlinedTextField(
                    value = amount,
                    onValueChange = { 
                        val filtered = it.filter { char -> char.isDigit() }
                        if (filtered.length <= 4) {
                            onAmountChange(filtered)
                        }
                    },
                    label = { Text("Amount (liters)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Usage type dropdown
                Text(
                    text = "Usage Type",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = usageType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select usage type") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        usageTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    onUsageTypeChange(type)
                                    expanded = false
                                    
                                    // Set default priority based on type
                                    if (type != "Other") {
                                        val newPriority = when (type) {
                                            "Absolute emergency" -> 0
                                            "Medical" -> 1
                                            "Drinking" -> 2
                                            "Farming" -> 3
                                            "Industry" -> 4
                                            else -> 3
                                        }
                                        onPriorityChange(newPriority)
                                    }
                                }
                            )
                        }
                    }
                }
                
                // Custom type for "Other"
                if (usageType == "Other") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customType,
                        onValueChange = { 
                            // Limit to 15 characters
                            if (it.length <= 15) {
                                onCustomTypeChange(it)
                            }
                        },
                        label = { Text("Custom Usage Type (max 15 chars)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Priority selector
                Text(
                    text = "Priority (0 = Highest, 5 = Lowest)",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    (0..5).forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { onPriorityChange(p) },
                            label = { Text("P$p") }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
