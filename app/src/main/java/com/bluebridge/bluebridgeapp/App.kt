package com.bluebridge.bluebridgeapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.bluebridge.bluebridgeapp.data.UserEvent
import com.bluebridge.bluebridgeapp.ui.navigation.NavigationGraph
import com.bluebridge.bluebridgeapp.ui.theme.getGreenColorScheme
import com.bluebridge.bluebridgeapp.utils.isNetworkAvailable
import com.bluebridge.bluebridgeapp.viewmodels.NearbyUsersViewModel
import com.bluebridge.bluebridgeapp.viewmodels.ServerState
import com.bluebridge.bluebridgeapp.viewmodels.ServerViewModel
import com.bluebridge.bluebridgeapp.viewmodels.UserViewModel
import com.bluebridge.bluebridgeapp.viewmodels.WellViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BlueBridgeApp(viewModelFactory: ViewModelProvider.Factory) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val isOnline by rememberNetworkState(context)

    // ViewModels
    val userViewModel: UserViewModel = viewModel(factory = viewModelFactory)
    val wellViewModel: WellViewModel = viewModel(factory = viewModelFactory)
    val nearbyUsersViewModel: NearbyUsersViewModel = viewModel(factory = viewModelFactory)
    val serverViewModel: ServerViewModel = viewModel(factory = viewModelFactory)

    // Theme handling
    val themePreference by userViewModel.currentTheme.collectAsState()
    val colorScheme = when (themePreference) {
        1 -> lightColorScheme()
        2 -> darkColorScheme()
        3 -> getGreenColorScheme(false)
        else -> if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    }

    // Effects
    LaunchedEffect(Unit) {
        userViewModel.repository.getUserId().takeIf { userViewModel.repository.isLoggedIn() }
            ?.let { userViewModel.handleEvent(UserEvent.LoadUser(it.toString())) }
    }

    LaunchedEffect(isOnline) { if (isOnline) serverViewModel.getServerStatus() }

    // UI
    MaterialTheme(colorScheme = colorScheme) {
        NavigationGraph(
            navController = navController,
            modifier = Modifier.fillMaxSize(),
            nearbyUsersViewModel = nearbyUsersViewModel,
            wellViewModel = wellViewModel,
            userViewModel = userViewModel,
            weatherViewModel = viewModel(factory = viewModelFactory),
            smsViewModel = viewModel(factory = viewModelFactory)
        )

        PermissionDialogs(userViewModel)
        ServerStatusDialogs(serverViewModel)
    }
}

@Composable
private fun rememberNetworkState(context: Context): State<Boolean> {
    val isOnline = remember { mutableStateOf(isNetworkAvailable(context)) }

    DisposableEffect(Unit) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { isOnline.value = true }
            override fun onLost(network: Network) { isOnline.value = isNetworkAvailable(context) }
        }

        cm.registerNetworkCallback(NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build(), callback)

        onDispose { cm.unregisterNetworkCallback(callback) }
    }

    return isOnline
}

@Composable
private fun PermissionDialogs(userViewModel: UserViewModel) {
    val context = LocalContext.current
    var showLocationDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showLocationDialog = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).any { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            showNotificationDialog = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        }
    }

    if (showLocationDialog) {
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { Text("Location Permission") },
            text = { Text("BlueBridge needs location access for nearby water sources") },
            confirmButton = { Button({ showLocationDialog = false }) { Text("Allow") } },
            dismissButton = { TextButton({ showLocationDialog = false }) { Text("Later") } }
        )
    }

    if (showNotificationDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationDialog = false },
            title = { Text("Notification Permission") },
            text = { Text("Enable notifications for water source updates") },
            confirmButton = { Button({ showNotificationDialog = false }) { Text("Allow") } },
            dismissButton = { TextButton({ showNotificationDialog = false }) { Text("Later") } }
        )
    }
}

@Composable
private fun ServerStatusDialogs(serverViewModel: ServerViewModel) {
    val serverState by serverViewModel.serverState.collectAsState()
    val needsUpdate by serverViewModel.needsUpdate.collectAsState()

    if (serverState is ServerState.Error) {
        AlertDialog(
            onDismissRequest = { /* Keep dialog open */ },
            title = { Text("Server Unreachable") },
            text = { Text("The server is currently unavailable. Please try again later.") },
            confirmButton = { Button({}) { Text("OK") } }
        )
    }

    if (needsUpdate) {
        AlertDialog(
            onDismissRequest = { serverViewModel.resetUpdateState() },
            title = { Text("Update Available") },
            text = { Text("A new version is available. Would you like to update?") },
            confirmButton = { Button({ serverViewModel.resetUpdateState() }) { Text("Update") } },
            dismissButton = { TextButton({ serverViewModel.resetUpdateState() }) { Text("Later") } }
        )
    }
}