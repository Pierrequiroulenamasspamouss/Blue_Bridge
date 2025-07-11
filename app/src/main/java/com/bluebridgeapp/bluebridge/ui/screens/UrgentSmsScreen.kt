package com.bluebridgeapp.bluebridge.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.bluebridgeapp.bluebridge.R
import com.bluebridgeapp.bluebridge.events.AppEvent
import com.bluebridgeapp.bluebridge.events.AppEventChannel
import com.bluebridgeapp.bluebridge.network.Location
import com.bluebridgeapp.bluebridge.viewmodels.ActionState
import com.bluebridgeapp.bluebridge.viewmodels.SmsViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrgentSmsScreen(
    modifier: Modifier = Modifier,
    smsViewModel: SmsViewModel
) {
    val context = LocalContext.current
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    val actionState by smsViewModel.actionState
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getCurrentLocation(context) { location ->
                currentLocation = location
            }
        }
    }

    // SMS permission launcher
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Retry the last action
            when (actionState) {
                is ActionState.Error -> {
                    val lastCommand = (actionState as ActionState.Error).error
                    if (lastCommand.contains("GNW")) {
                        smsViewModel.sendSms("GNW", currentLocation)
                    } else if (lastCommand.contains("SH")) {
                        smsViewModel.sendSms("SH", currentLocation)
                    }
                }
                else -> {}
            }
        } else {
            showPermissionDialog = true
        }
    }

    // Check permissions
    LaunchedEffect(Unit) {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocation(context) { location ->
                    currentLocation = location
                }
            }
            else -> {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        // Check SMS permission
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED) {
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        }
    }

    // Show snackbar when action state changes
    LaunchedEffect(actionState) {
        when (actionState) {
            is ActionState.Success -> {
                AppEventChannel.sendEvent(AppEvent.ShowError((actionState as ActionState.Success).message))
            }
            is ActionState.Error -> {
                val error = (actionState as ActionState.Error).error
                if (error.contains("SEND_SMS")) {
                    smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                } else {
                    AppEventChannel.sendEvent(AppEvent.ShowError(error))
                }
            }
            else -> {}
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.emergency_services),
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = stringResource(R.string.use_these_buttons_to_request_emergency_assistance_or_find_the_nearest_water_source),
            style = MaterialTheme.typography.bodyMedium
        )

        // Get Nearest Water Button
        Button(
            onClick = {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.SEND_SMS
                    ) == PackageManager.PERMISSION_GRANTED) {
                    smsViewModel.sendSms("GNW", currentLocation)
                } else {
                    smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = actionState !is ActionState.Loading && currentLocation != null
        ) {
            Icon(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = stringResource(R.string.get_nearest_water)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.find_nearest_water_source))
        }

        // Send Help Button
        Button(
            onClick = {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.SEND_SMS
                    ) == PackageManager.PERMISSION_GRANTED) {
                    smsViewModel.sendSms("SH", currentLocation)
                } else {
                    smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = actionState !is ActionState.Loading && currentLocation != null
        ) {
            Icon(
                imageVector = Icons.Default.Emergency,
                contentDescription = stringResource(R.string.send_help)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.request_emergency_assistance))
        }
    }

    // Permission denied dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text(stringResource(R.string.permission_required)) },
            text = { Text(stringResource(R.string.sms_permission_required)) },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        // Open app settings
                        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = android.net.Uri.fromParts("package", context.packageName, null)
                        context.startActivity(intent)
                    }
                ) {
                    Text(stringResource(R.string.open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

}

@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
private fun getCurrentLocation(context: Context, onLocationReceived: (Location) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    fusedLocationClient.getCurrentLocation(
        Priority.PRIORITY_HIGH_ACCURACY,
        null
    ).addOnSuccessListener { location ->
        if (location != null) {
            onLocationReceived(
                Location(
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            )
        }
    }
}
