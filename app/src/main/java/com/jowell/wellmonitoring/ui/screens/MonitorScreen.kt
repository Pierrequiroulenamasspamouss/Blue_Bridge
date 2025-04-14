package com.jowell.wellmonitoring.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jowell.wellmonitoring.R
import com.jowell.wellmonitoring.data.WellData
import com.jowell.wellmonitoring.ui.TextComponent
import com.jowell.wellmonitoring.ui.TopBar
import com.jowell.wellmonitoring.ui.WellViewModel
import com.jowell.wellmonitoring.ui.theme.BackButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale


@Composable
fun WellDataDisplay(userViewModel: WellViewModel, navController: NavController) {
    val wells by userViewModel.wellList.collectAsState()
    val errorMessage by userViewModel.errorMessage
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isRefreshingAll = remember { mutableStateOf(false) }
    // Launch snackbar when there's an error
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
                userViewModel.clearErrorMessage()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {

            LazyColumn(
                modifier = Modifier
                    .padding(bottom = 60.dp)
                    .fillMaxSize()
            ) {
                item {
                    TopBar(topBarMessage = stringResource(id = R.string.Monitoring_top_bar_text))
                    BackButton(navController = navController, userViewModel = userViewModel) {
                        navController.popBackStack()
                    }
                    Button(onClick = {
                        scope.launch {
                            isRefreshingAll.value = true
                            try {
                                val (success, total) = userViewModel.refreshAllWells(context)
                                snackbarHostState.showSnackbar("Refreshed $success/$total wells")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Refresh failed: ${e.localizedMessage}")
                            } finally {
                                isRefreshingAll.value = false
                            }
                        }
                     } , modifier = Modifier.fillMaxWidth().height(60.dp), enabled = !isRefreshingAll.value  ) { Text("Refresh All Wells") }
                }
                itemsIndexed(wells) { index, well ->
                    DataAndButtons(well, index, navController, userViewModel, context, scope, snackbarHostState)

                }


                // Add new well button
                item {
                    Spacer(modifier = Modifier.padding(vertical = 12.dp))
                    Button(
                        onClick = {
                            val newWellId = (wells.lastOrNull()?.id ?: 0) + 1
                            val newWell = WellData(id = newWellId)
                            navController.navigate("${Routes.WELL_CONFIG_SCREEN}/${newWell.id}")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(id = R.string.create_new_well))
                    }
                }
            }
        }

        // Display error messages
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.padding(16.dp)
        )
    }
}


@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun DataAndButtons(
    well: WellData,
    index: Int,
    navController: NavController,
    userViewModel: WellViewModel,
    context: Context,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState
) {
    val expanded = remember { mutableStateOf(false) }
    val isThisWellRefreshing by remember {
        derivedStateOf { userViewModel.isRefreshing.value == well.id }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .border(1.dp, color = MaterialTheme.colorScheme.primary)
            .padding(16.dp)
    ) {
        Column {
            // Conditionally show only non-empty or non-zero fields
            if (well.wellName.isNotBlank()) TextComponent("Name: ${well.wellName}", 20.sp, textFont = FontWeight.Bold)
            if (well.lastRefreshTime > 0) {
                Text(
                    text = "Last refreshed: ${formatDateTime(well.lastRefreshTime)}",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            if (well.wellOwner.isNotBlank()) TextComponent("Owner: ${well.wellOwner}", 16.sp)
            if (well.wellLocation.isNotBlank()) TextComponent("Location: ${well.wellLocation}", 16.sp)
            if (well.wellWaterType.isNotBlank()) TextComponent("Type: ${well.wellWaterType}", 16.sp)
            if (well.wellCapacity > 0) TextComponent("Capacity: ${well.wellCapacity} L", 16.sp)
            if (well.wellWaterLevel > 0) TextComponent("Level: ${well.wellWaterLevel} L", 16.sp)
            if (well.wellWaterConsumption > 0) TextComponent("Consumption: ${well.wellWaterConsumption} L/day", 16.sp)
            if (well.ipAddress.isNotBlank()) TextComponent("IP: ${well.ipAddress}", 16.sp)
            well.extraData.forEach { (key, value) ->
                TextComponent("$key: ${value.toString().trim('"')}", 16.sp)
            }

        }

        // Triple-dot menu for actions
        Box(modifier = Modifier.align(Alignment.TopEnd)) {
            IconButton(
                onClick = { expanded.value = true },
                enabled = !isThisWellRefreshing
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = "Actions")
            }

            DropdownMenu(
                expanded = expanded.value && !isThisWellRefreshing,
                onDismissRequest = { expanded.value = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        navController.navigate("${Routes.WELL_CONFIG_SCREEN}/${well.id}")
                        expanded.value = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        userViewModel.deleteWell(index)
                        expanded.value = false
                    }
                )
                val isRefreshing = remember { mutableStateOf(false) }

                DropdownMenuItem(
                    text = {
                        if (isThisWellRefreshing) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Refreshing...")
                            }
                        } else {
                            Text("Refresh")
                        }
                    },
                    onClick = {
                        scope.launch {
                            expanded.value = false
                            val success = userViewModel.refreshSingleWell(well.id, context)
                            val message = if (success) {
                                "Successfully refreshed ${well.wellName}"
                            } else {
                                userViewModel.errorMessage.value ?: "Refresh failed"
                            }
                            snackbarHostState.showSnackbar(message)
                        }
                    },
                    leadingIcon = {
                        if (isThisWellRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh"
                            )
                        }
                    },
                    enabled = !isThisWellRefreshing
                )
            }
        }

        // Show refreshing indicator near the well name
        if (isThisWellRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(16.dp),
                strokeWidth = 2.dp
            )
        }
    }
}
fun formatDateTime(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}