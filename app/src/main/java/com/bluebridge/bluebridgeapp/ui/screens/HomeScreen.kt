package com.bluebridge.bluebridgeapp.ui.screens
//TODO: unable to connect to the server message pops up each time you go back to the home screen. I want it to only do that each time the user opens the app, not each time he goes back to the HomeScreen
//TODO: have the UrgentSmsScreen button accessible to users with not the role "guest" or no role at all (minimum logged in) , with the other buttons like the SettingsScreen or the WeatherScreen. Don't do a direct navigation, use the NavigationGraph

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.ExploreOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.bluebridge.bluebridgeapp.R
import com.bluebridge.bluebridgeapp.data.model.UserData
import com.bluebridge.bluebridgeapp.ui.components.FeatureCard
import com.bluebridge.bluebridgeapp.ui.components.WelcomeHeader
import com.bluebridge.bluebridgeapp.ui.navigation.Routes
import com.bluebridge.bluebridgeapp.viewmodels.ServerState
import com.bluebridge.bluebridgeapp.viewmodels.ServerViewModel
import com.bluebridge.bluebridgeapp.viewmodels.UiState
import com.bluebridge.bluebridgeapp.viewmodels.UserViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.InternalSerializationApi

// Network utility object to check connectivity
object NetworkUtils {
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
    }
}

@Composable
fun OfflineBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.SignalWifiOff,
                contentDescription = "Offline",
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "You are currently offline. Some features may be limited.",
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, InternalSerializationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    userViewModel: UserViewModel,
    serverViewModel: ServerViewModel
) {
    val context = LocalContext.current
    val userState by userViewModel.state
    val notificationsEnabled by userViewModel.notificationsEnabled
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showLocationPermissionDialog by remember { mutableStateOf(false) }
    var showNotificationPermissionDialog by remember { mutableStateOf(false) }
    var showServerUnreachableDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Network connectivity state
    var isOnline by remember { mutableStateOf(NetworkUtils.isNetworkAvailable(context)) }

    // Server state
    val serverState by serverViewModel.serverState.collectAsState()
    val needsUpdate by serverViewModel.needsUpdate.collectAsState()
    val isServerReachable by serverViewModel.isServerReachable.collectAsState()

    // Check server status when online
    LaunchedEffect(isOnline) {
        if (isOnline) {
            serverViewModel.checkServerStatus()
        }
    }

    // Handle server state changes
    LaunchedEffect(serverState) {
        when (serverState) {
            is ServerState.Error -> {
                showServerUnreachableDialog = true
            }
            is ServerState.Success -> {
                if (needsUpdate) {
                    showUpdateDialog = true
                }
            }
            else -> {}
        }
    }

    // Register network callback to monitor connectivity changes
    DisposableEffect(Unit) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOnline = true
            }
            
            override fun onLost(network: Network) {
                isOnline = NetworkUtils.isNetworkAvailable(context)
            }
        }
        
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        
        onDispose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }

    // Permission request launchers
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (!allGranted) {
            Log.d("Permission", "Insufficient permissions")
            // Handle permission denial if needed
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Enable notifications in ViewModel
            userViewModel.setNotificationsEnabled(true)
        }
    }

    // Check if user is logged in
    val isLoggedIn = when (userState) {
        is UiState.Success -> true
        else -> false
    }

    // Extract user data if logged in
    val userData = if (userState is UiState.Success) {
        (userState as UiState.Success<UserData>).data
    } else null

    // Check if user is in guest mode
    var isGuestMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isGuestMode = userViewModel.isGuestMode()
    }

    // Check and request permissions
    LaunchedEffect(Unit) {
        // Location permissions
        val locationPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val hasLocationPermissions = locationPermissions.all {
            context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }
        if (!hasLocationPermissions) {
            showLocationPermissionDialog = true
        }

        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsEnabled) {
            val hasNotificationPermission =
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED

            if (!hasNotificationPermission) {
                showNotificationPermissionDialog = true
            }
        }
    }

    // Server unreachable dialog
    if (showServerUnreachableDialog) {
        AlertDialog(
            onDismissRequest = { showServerUnreachableDialog = false },
            title = { Text("Server Unreachable") },
            text = { Text("The BlueBridge server is currently unreachable. Please contact support for assistance.") },
            confirmButton = {
                Button(onClick = { showServerUnreachableDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Update available dialog
    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { 
                showUpdateDialog = false
                serverViewModel.resetUpdateState()
            },
            title = { Text("Update Available") },
            text = { Text("A new version of BlueBridge is available. Would you like to update now?") },
            confirmButton = {
                Button(
                    onClick = {
                        showUpdateDialog = false
                        serverViewModel.resetUpdateState()
                        // Download the new APK
                        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        val request = DownloadManager.Request("https://bluebridgehomeonthewater.com/download".toUri())
                            .setTitle("BlueBridge Update")
                            .setDescription("Downloading new version")
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "BlueBridge.apk")
                        downloadManager.enqueue(request)
                    }
                ) {
                    Text("Update Now")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showUpdateDialog = false
                        serverViewModel.resetUpdateState()
                    }
                ) {
                    Text("Later")
                }
            }
        )
    }

    // Location permission dialog
    if (showLocationPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showLocationPermissionDialog = false },
            title = { Text("Location Permission") },
            text = { Text("BlueBridge needs location access to show nearby water sources and users") },
            confirmButton = {
                Button(
                    onClick = {
                        showLocationPermissionDialog = false
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                ) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationPermissionDialog = false }) {
                    Text("Later")
                }
            }
        )
    }

    // Notification permission dialog
    if (showNotificationPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationPermissionDialog = false },
            title = { Text(stringResource(R.string.notification_permission_title)) },
            text = { Text(stringResource(R.string.notification_permission_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showNotificationPermissionDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            // For older Android versions, just enable without asking
                            userViewModel.setNotificationsEnabled(true)
                        }
                    }
                ) {
                    Text(stringResource(R.string.notification_permission_allow))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationPermissionDialog = false }) {
                    Text(stringResource(R.string.notification_permission_deny))
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Offline Banner - show only when offline
            if (!isOnline) {
                OfflineBanner()
            }
            
            // Welcome header
            WelcomeHeader(userData = userData, isLoggedIn = isLoggedIn)

            // Feature cards section
            Text(
                text = "Main Features",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Features grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeatureCard(
                    icon = Icons.Default.WaterDrop,
                    title = "Monitor Wells",
                    description = "Track water levels and quality data",
                    onClick = { navController.navigate(Routes.MONITORING_SCREEN) },
                    modifier = Modifier.weight(1f)
                )

                FeatureCard(
                    icon = Icons.Default.Map,
                    title = "Map View",
                    description = "See nearby water resources",
                    onClick = { navController.navigate(Routes.MAP_SCREEN) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeatureCard(
                    icon = Icons.Outlined.ExploreOff,
                    title = "Navigation",
                    description = "Find your way to the nearest well",
                    onClick = { navController.navigate("${Routes.COMPASS_SCREEN}?lat=90&lon=0&name=North") },
                    modifier = Modifier.weight(1f)
                )
                if(isLoggedIn && !isGuestMode){
                    FeatureCard(
                        icon = Icons.Default.Cloud,
                        title = "Weather",
                        description = "Check upcoming weather forecast",
                        onClick = { navController.navigate(Routes.WEATHER_SCREEN) },
                        modifier = Modifier.weight(1f)
                    )
                } else {

                    FeatureCard(
                        icon = Icons.Default.Settings,
                        title = "Settings",
                        description = "Configure app preferences",
                        onClick = { navController.navigate(Routes.SETTINGS_SCREEN) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isLoggedIn && !isGuestMode) {
                    FeatureCard(
                        icon = Icons.Default.Visibility,
                        title = "Nearby Users",
                        description = "Find community members near you",
                        onClick = { navController.navigate(Routes.NEARBY_USERS_SCREEN) },
                        modifier = Modifier.weight(1f)
                    )
                    
                    FeatureCard(
                        icon = Icons.Default.Settings,
                        title = "Settings",
                        description = "Configure app preferences",
                        onClick = { navController.navigate(Routes.SETTINGS_SCREEN) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Support section
            Text(
                text = "Support BlueBridge",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 4.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = "Help us improve BlueBridge",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = "BlueBridge is a community-driven app to help everyone access clean water. Your support keeps us running.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Button(
                        onClick = { navController.navigate(Routes.ADMOB_SCREEN) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Support with Ads")
                    }
                }
            }

            // Account section
            if (isLoggedIn) {
                // User profile summary
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = "${userData?.firstName} ${userData?.lastName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = userData?.email ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(
                                onClick = { navController.navigate(Routes.PROFILE_SCREEN) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Edit Profile")
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        userViewModel.logout()
                                        navController.navigate(Routes.HOME_SCREEN) {
                                            popUpTo(Routes.HOME_SCREEN) { inclusive = true }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Logout")
                            }
                        }
                    }
                }
            } else {
                // Login/Register buttons
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Sign in to access more features",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = { navController.navigate(Routes.LOGIN_SCREEN) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Login")
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedButton(
                                onClick = { navController.navigate(Routes.REGISTER_SCREEN) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Sign Up")
                            }
                        }

                        // Guest login button
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    userViewModel.loginAsGuest()
                                    navController.navigate(Routes.HOME_SCREEN) {
                                        popUpTo(Routes.HOME_SCREEN) { inclusive = true }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            ),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        ) {
                            Text(stringResource(R.string.login_as_guest))
                        }
                    }
                }
            }
        }
    }
}


