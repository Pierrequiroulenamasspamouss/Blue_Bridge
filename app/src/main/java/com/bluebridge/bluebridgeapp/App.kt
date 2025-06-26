package com.bluebridge.bluebridgeapp

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.bluebridge.bluebridgeapp.data.model.BugReportRequest
import com.bluebridge.bluebridgeapp.data.model.ValidateAuthTokenRequest
import com.bluebridge.bluebridgeapp.events.UserEvent
import com.bluebridge.bluebridgeapp.navigation.NavigationGraph
import com.bluebridge.bluebridgeapp.navigation.Routes
import com.bluebridge.bluebridgeapp.network.RetrofitBuilder
import com.bluebridge.bluebridgeapp.ui.dialogs.BugReportDialog
import com.bluebridge.bluebridgeapp.ui.theme.getCyanColorScheme
import com.bluebridge.bluebridgeapp.ui.theme.getGreenColorScheme
import com.bluebridge.bluebridgeapp.ui.theme.getOrangeColorScheme
import com.bluebridge.bluebridgeapp.ui.theme.getPinkColorScheme
import com.bluebridge.bluebridgeapp.ui.theme.getPurpleColorScheme
import com.bluebridge.bluebridgeapp.ui.theme.getRedColorScheme
import com.bluebridge.bluebridgeapp.ui.theme.getTanColorScheme
import com.bluebridge.bluebridgeapp.ui.theme.getYellowColorScheme
import com.bluebridge.bluebridgeapp.utils.isNetworkAvailable
import com.bluebridge.bluebridgeapp.viewmodels.NearbyUsersViewModel
import com.bluebridge.bluebridgeapp.viewmodels.ServerState
import com.bluebridge.bluebridgeapp.viewmodels.ServerViewModel
import com.bluebridge.bluebridgeapp.viewmodels.UserViewModel
import com.bluebridge.bluebridgeapp.viewmodels.WellViewModel
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BlueBridgeApp(viewModelFactory: ViewModelProvider.Factory) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val isOnline by rememberNetworkState(context)
    val showBugReportDialog = remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val api = RetrofitBuilder.getServerApi(context)

    // ViewModels
    val userViewModel: UserViewModel = viewModel(factory = viewModelFactory)
    val wellViewModel: WellViewModel = viewModel(factory = viewModelFactory)
    val nearbyUsersViewModel: NearbyUsersViewModel = viewModel(factory = viewModelFactory)
    val serverViewModel: ServerViewModel = viewModel(factory = viewModelFactory)

    // Theme handling
    val isSystemDark = isSystemInDarkTheme()
    val themePreference by userViewModel.currentTheme.collectAsState()
    Log.d("BlueBridgeApp", "Theme preference: $themePreference")
    val colorScheme = when (themePreference) {
        0 -> if (isSystemDark) darkColorScheme() else lightColorScheme() // System default
        1 -> lightColorScheme() // Light
        2 -> darkColorScheme() // Dark
        3 -> getGreenColorScheme(isSystemDark) // Green
        4 -> getPinkColorScheme(isSystemDark) // Pink
        5 -> getRedColorScheme(isSystemDark) // Red
        6 -> getPurpleColorScheme(isSystemDark) // Purple
        7 -> getYellowColorScheme(isSystemDark) // Yellow
        8 -> getTanColorScheme(isSystemDark) // Tan
        9 -> getOrangeColorScheme(isSystemDark) // Orange
        10 -> getCyanColorScheme(isSystemDark) // Cyan
        else -> if (isSystemDark) darkColorScheme() else lightColorScheme() // Fallback
    }

    // Authenticate on startup
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val token = userViewModel.getLoginToken()
                if (token.isNullOrBlank()) {
                    userViewModel.logout()
                    navController.navigate(Routes.LOGIN_SCREEN) { popUpTo(0) { inclusive = true } }
                } else {
                    // Try a simple authenticated request (e.g., get profile or weather)
                    val response = api.getServerStatus()
                    if (!response.isSuccessful || response.body()?.status != "success") {
                        userViewModel.logout()
                        navController.navigate(Routes.LOGIN_SCREEN) { popUpTo(0) { inclusive = true } }
                    } else {
                        // Validate token with a dedicated endpoint
                        val request = ValidateAuthTokenRequest(token = token, userId = userViewModel.getUserId()
                            .toString())
                        val authResponse = api.validateAuthToken(request)
                        if (!authResponse.isSuccessful || authResponse.body()?.status == "error" && authResponse.body()?.message == "invalid token") {
                            userViewModel.logout()
                            navController.navigate(Routes.LOGIN_SCREEN) { popUpTo(0) { inclusive = true } }
                        }
                    }
                }
            } catch (e: Exception) {
                userViewModel.logout()
                navController.navigate(Routes.LOGIN_SCREEN) { popUpTo(0) { inclusive = true } }
            }
        }
    }

    // Effects
    LaunchedEffect(Unit) {
        userViewModel.repository.getUserId().takeIf { userViewModel.repository.isLoggedIn() }
            ?.let { userViewModel.handleEvent(UserEvent.LoadUser(it.toString())) }
    }

    LaunchedEffect(isOnline) { if (isOnline) serverViewModel.getServerStatus() }

    // UI
    MaterialTheme(colorScheme = colorScheme) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = { showBugReportDialog.value = true }) {
                    Icon(Icons.Default.BugReport, contentDescription = "Report a Bug")
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            NavigationGraph(
                navController = navController,
                modifier = Modifier.fillMaxSize(),
                nearbyUsersViewModel = nearbyUsersViewModel,
                wellViewModel = wellViewModel,
                userViewModel = userViewModel,
                weatherViewModel = viewModel(factory = viewModelFactory),
                smsViewModel = viewModel(factory = viewModelFactory),
                paddingValues = paddingValues
            )

            ServerStatusDialogs(serverViewModel)

            if (showBugReportDialog.value) {
                BugReportDialog(
                    showDialog = showBugReportDialog.value,
                    onDismiss = { showBugReportDialog.value = false },
                    onSubmit = { name, description, category, extra ->
                        coroutineScope.launch {
                            val bugreport = BugReportRequest(name, description, category, extra)
                            api.submitBugReport(bugreport)
                            snackbarHostState.showSnackbar("Bug report sent! Thank you.")
                        }
                        showBugReportDialog.value = false
                    }
                )
            }
        }
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
private fun ServerStatusDialogs(serverViewModel: ServerViewModel) {
    val serverState by serverViewModel.serverState.collectAsState()
    var showServerUnreachableDialog by remember { mutableStateOf(serverState is ServerState.Error) }
    val context = LocalContext.current
    val needsUpdate by serverViewModel.needsUpdate.collectAsState()

    LaunchedEffect(serverState) {
        showServerUnreachableDialog = serverState is ServerState.Error
    }

    if (showServerUnreachableDialog) {
        AlertDialog(
            onDismissRequest = { showServerUnreachableDialog = false },
            title = { Text("Server Unreachable") },
            text = { Text("The server is currently unavailable. Please try again later.") },
            confirmButton = { Button({ showServerUnreachableDialog = false }) { Text("OK") } }
        )
    }

    if (needsUpdate) {
        AlertDialog(
            onDismissRequest = { serverViewModel.resetUpdateState() },
            title = { Text("Update Available") },
            text = { Text("A new version is available. Would you like to update?") },
            confirmButton = {
                Button({
                    val intent = Intent(Intent.ACTION_VIEW,
                        "http://bluebridge.homeonthewater.com/home".toUri())
                    context.startActivity(intent)
                    serverViewModel.resetUpdateState()
                }) { Text("Update") }
            },
            dismissButton = { TextButton({ serverViewModel.resetUpdateState() }) { Text("Later") } }
        )
    }
}

@Preview
@Composable
fun BlueBridgeAppPreview() {
    // This is a simplified preview and won't have actual ViewModel logic
    // BlueBridgeApp(viewModelFactory = /* Provide a mock ViewModelProvider.Factory here if needed */)
}