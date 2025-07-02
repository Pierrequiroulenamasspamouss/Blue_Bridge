package com.bluebridgeapp.bluebridge.ui.screens.wellscreens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ScrollState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bluebridgeapp.bluebridge.R
import com.bluebridgeapp.bluebridge.data.model.UserData
import com.bluebridgeapp.bluebridge.data.model.WellData
import com.bluebridgeapp.bluebridge.data.model.getLatitude
import com.bluebridgeapp.bluebridge.data.model.getLongitude
import com.bluebridgeapp.bluebridge.data.model.hasValidCoordinates
import com.bluebridgeapp.bluebridge.events.AppEvent
import com.bluebridgeapp.bluebridge.events.AppEventChannel
import com.bluebridgeapp.bluebridge.ui.navigation.Routes
import com.bluebridgeapp.bluebridge.viewmodels.UiState
import com.bluebridgeapp.bluebridge.viewmodels.UserViewModel
import com.bluebridgeapp.bluebridge.viewmodels.WellViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellDetailsScreen(
    navController: NavController,
    wellViewModel: WellViewModel,
    wellId: Int,
    userViewModel: UserViewModel
) {
    // State management
    val userData by rememberUserData(userViewModel)
    val (well, isLoading) = rememberWellState(wellViewModel, wellId)

    // UI Scaffold
    Scaffold(
        topBar = { WellDetailsTopBar(navController, well, wellId, userData) },
    ) { padding ->
        WellDetailsContent(
            padding = padding,
            isLoading = isLoading,
            well = well,
            wellId = wellId,
            onRetry = { wellViewModel.loadWell(wellId) },
            navController = navController
        )
    }
}

@Composable
private fun rememberUserData(userViewModel: UserViewModel): State<UserData?> {
    val userState by userViewModel.state
    return remember(userState) {
        derivedStateOf {
            (userState as? UiState.Success<UserData>)?.data
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun rememberWellState(
    wellViewModel: WellViewModel,
    wellId: Int
): Pair<WellData?, Boolean> {
    val wellState by wellViewModel.currentWellState

    // Load data only once when screen appears
    LaunchedEffect(wellId) {
        wellViewModel.loadWell(wellId)
    }

    return remember(wellState) {
        when (wellState) {
            is UiState.Success -> (wellState as UiState.Success<WellData>).data to false
            is UiState.Loading -> null to true
            else -> null to false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WellDetailsTopBar(
    navController: NavController,
    well: WellData?,
    wellId: Int,
    userData: UserData?
) {
    TopAppBar(
        title = { Text(well?.wellName ?: "Well Details") },
        navigationIcon = {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
        },
        actions = {
            if (userData?.role == "admin") {
                IconButton(onClick = { navController.navigate("${Routes.WELL_CONFIG_SCREEN}/$wellId") }) {
                    Icon(Icons.Default.Edit, "Edit")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun WellDetailsContent(
    padding: PaddingValues,
    isLoading: Boolean,
    well: WellData?,
    wellId: Int,
    onRetry: () -> Unit,
    navController: NavController
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    when {
        isLoading -> FullScreenLoading()
        well == null -> WellNotFound(wellId, onRetry)
        else -> WellDetails(
            padding = padding,
            scrollState = scrollState,
            well = well,
            navController = navController,
            scope = scope
        )
    }
}

@Composable
private fun FullScreenLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun WellNotFound(wellId: Int, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.well_not_found, wellId))
            Text(stringResource(R.string.error_loading_data), style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun WellDetails(
    padding: PaddingValues,
    scrollState: ScrollState,
    well: WellData,
    navController: NavController,
    scope: CoroutineScope
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        WellBasicInfoCard(well)
        WellWaterSpecsCard(well)
        WellLocationCard(well, navController, scope)
    }
}

@Composable
private fun WellBasicInfoCard(well: WellData) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(well.wellName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            InfoRow("Owner:", well.wellOwner.ifBlank { "Not specified" })
            InfoRow("Status:", well.wellStatus)
        }
    }
}

@Composable
private fun WellWaterSpecsCard(well: WellData) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionTitle("Water Specifications")
            InfoRow("Water Type:", well.wellWaterType.ifBlank { "Not specified" })
            InfoRow("Water Level:", if (well.wellWaterLevel.isBlank()) "Not specified" else "${well.wellWaterLevel}L")
            InfoRow("Capacity:", if (well.wellCapacity.isBlank()) "Not specified" else "${well.wellCapacity}L")
            InfoRow("Consumption:", if (well.wellWaterConsumption.isBlank()) "Not specified" else "${well.wellWaterConsumption}L/day")
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun WellLocationCard(
    well: WellData,
    navController: NavController,
    scope: CoroutineScope
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionTitle("Location")

            Text(
                text = if (well.hasValidCoordinates())
                    "${well.wellLocation.latitude}, ${well.wellLocation.longitude}"
                else "Location not specified",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LocationButton(
                    icon = Icons.Default.LocationOn,
                    text = "Show directions",
                    enabled = well.hasValidCoordinates(),
                    onClick = { navigateToCompass(well, navController, scope) }
                )

                LocationButton(
                    icon = Icons.Default.Map,
                    text = "View on Map",
                    enabled = well.hasValidCoordinates(),
                    onClick = { navigateToMap(well, navController, scope) }
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Divider(modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text("$label ", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun LocationButton(
    icon: ImageVector,
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.padding(4.dp))
        Text(text)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun navigateToCompass(well: WellData, navController: NavController, scope: CoroutineScope) {
    if (!well.hasValidCoordinates()) {
        scope.launch { AppEventChannel.sendEvent(AppEvent.ShowError("No valid coordinates")) }
        return
    }

    val lat = well.getLatitude()
    val lon = well.getLongitude()
    if (lat != null && lon != null) {
        val encodedName = URLEncoder.encode(well.wellName, StandardCharsets.UTF_8.toString())
        navController.navigate("${Routes.COMPASS_SCREEN}?lat=$lat&lon=$lon&name=$encodedName")
    } else {
        scope.launch { AppEventChannel.sendEvent(AppEvent.ShowError("Invalid coordinates")) }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun navigateToMap(well: WellData, navController: NavController, scope: CoroutineScope) {
    if (!well.hasValidCoordinates()) {
        scope.launch { AppEventChannel.sendEvent(AppEvent.ShowError("No valid coordinates")) }
        return
    }

    val lat = well.getLatitude()
    val lon = well.getLongitude()
    if (lat != null && lon != null) {
        navController.navigate("${Routes.MAP_SCREEN}?targetLat=$lat&targetLon=$lon")
    } else {
        scope.launch { AppEventChannel.sendEvent(AppEvent.ShowError("Invalid coordinates")) }
    }
}