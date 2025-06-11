package com.bluebridge.bluebridgeapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.bluebridge.bluebridgeapp.data.UserEvent
import com.bluebridge.bluebridgeapp.data.local.UserPreferences
import com.bluebridge.bluebridgeapp.data.local.WellPreferences
import com.bluebridge.bluebridgeapp.data.repository.NearbyUsersRepositoryImpl
import com.bluebridge.bluebridgeapp.data.repository.ServerRepositoryImpl
import com.bluebridge.bluebridgeapp.data.repository.UserRepositoryImpl
import com.bluebridge.bluebridgeapp.data.repository.WeatherRepository
import com.bluebridge.bluebridgeapp.data.repository.WellRepositoryImpl
import com.bluebridge.bluebridgeapp.network.RetrofitBuilder
import com.bluebridge.bluebridgeapp.ui.navigation.NavigationGraph
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
import com.bluebridge.bluebridgeapp.viewmodels.SmsViewModel
import com.bluebridge.bluebridgeapp.viewmodels.UserViewModel
import com.bluebridge.bluebridgeapp.viewmodels.ViewModelFactory
import com.bluebridge.bluebridgeapp.viewmodels.WeatherViewModel
import com.bluebridge.bluebridgeapp.viewmodels.WellViewModel
import com.google.android.gms.ads.MobileAds
import kotlinx.serialization.InternalSerializationApi

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(InternalSerializationApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        initializeAdMob()

        setContent {
            BlueBridgeApp()
        }
    }

    private fun initializeAdMob() {
        try {
            MobileAds.initialize(this) { status ->
                Log.d("MainActivity", "AdMob initialized: ${status.adapterStatusMap.size} adapters")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "AdMob initialization failed", e)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun BlueBridgeApp() {
    val context = LocalContext.current
    val viewModelFactory = remember { createViewModelFactory(context) }
    val userViewModel: UserViewModel = viewModel(factory = viewModelFactory)

    // Load user data on startup
    LaunchedEffect(Unit) {
        val userId = userViewModel.repository.getUserId()
        if (userViewModel.repository.isLoggedIn()) {
            userViewModel.handleEvent(UserEvent.LoadUser(userId = userId.toString()))
        }
    }

    // Get theme preference directly from theme StateFlow
    val themePreference by userViewModel.currentTheme.collectAsState()

    // Apply theme dynamically
    AppTheme(themePreference = themePreference) {
        AppContent(viewModelFactory = viewModelFactory)
    }
}

@Composable
private fun AppTheme(
    themePreference: Int,
    content: @Composable () -> Unit
) {
    val isDarkTheme = when (themePreference) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when (themePreference) {
        3 -> getGreenColorScheme(isDarkTheme)
        4 -> getPinkColorScheme(isDarkTheme)
        5 -> getRedColorScheme(isDarkTheme)
        6 -> getPurpleColorScheme(isDarkTheme)
        7 -> getYellowColorScheme(isDarkTheme)
        8 -> getTanColorScheme(isDarkTheme)
        9 -> getOrangeColorScheme(isDarkTheme)
        10 -> getCyanColorScheme(isDarkTheme)
        else -> if (isDarkTheme) darkColorScheme() else lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun AppContent(viewModelFactory: ViewModelProvider.Factory) {
    val context = LocalContext.current

    // ViewModels
    val userViewModel: UserViewModel = viewModel(factory = viewModelFactory)
    val wellViewModel: WellViewModel = viewModel(factory = viewModelFactory)
    val nearbyUsersViewModel: NearbyUsersViewModel = viewModel(factory = viewModelFactory)
    val weatherViewModel: WeatherViewModel = viewModel(factory = viewModelFactory)
    val smsViewModel: SmsViewModel = viewModel(factory = viewModelFactory)
    val serverViewModel: ServerViewModel = viewModel(factory = viewModelFactory)

    val navController = rememberNavController()

    // Network monitoring
    val isOnline by rememberNetworkState(context)

    // Check server status when online
    LaunchedEffect(isOnline) {
        if (isOnline) {
            serverViewModel.getServerStatus()
        }
    }

    // Handle permissions
    PermissionHandler(userViewModel)

    // Server status handling
    ServerStatusHandler(serverViewModel)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavigationGraph(
            navController = navController,
            modifier = Modifier.fillMaxSize(),
            nearbyUsersViewModel = nearbyUsersViewModel,
            wellViewModel = wellViewModel,
            userViewModel = userViewModel,
            weatherViewModel = weatherViewModel,
            smsViewModel = smsViewModel,
        )
    }
}

@Composable
private fun rememberNetworkState(context: Context): State<Boolean> {
    val isOnline = remember { mutableStateOf(isNetworkAvailable(context)) }

    DisposableEffect(Unit) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOnline.value = true
            }
            override fun onLost(network: Network) {
                isOnline.value = isNetworkAvailable(context)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        onDispose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    return isOnline
}

@Composable
private fun PermissionHandler(userViewModel: UserViewModel) {
    val context = LocalContext.current
    var showLocationDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        Log.d("Permission", "Location permissions granted: $allGranted")
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) userViewModel.setNotificationsEnabled(true)
    }

    // Check permissions on startup
    LaunchedEffect(Unit) {
        val locationPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        showLocationDialog = locationPermissions.any {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            showNotificationDialog = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        }
    }

    // Permission dialogs
    if (showLocationDialog) {
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { Text("Location Permission") },
            text = { Text("BlueBridge needs location access to show nearby water sources and users") },
            confirmButton = {
                Button(onClick = {
                    showLocationDialog = false
                    locationLauncher.launch(arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ))
                }) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationDialog = false }) {
                    Text("Later")
                }
            }
        )
    }

    if (showNotificationDialog && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        AlertDialog(
            onDismissRequest = { showNotificationDialog = false },
            title = { Text("Notification Permission") },
            text = { Text("Enable notifications to receive updates about water sources") },
            confirmButton = {
                Button(onClick = {
                    showNotificationDialog = false
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationDialog = false }) {
                    Text("Later")
                }
            }
        )
    }
}

@Composable
private fun ServerStatusHandler(serverViewModel: ServerViewModel) {
    val serverState by serverViewModel.serverState.collectAsState()
    val needsUpdate by serverViewModel.needsUpdate.collectAsState()

    var showServerError by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(serverState) {
        showServerError = serverState is ServerState.Error
        showUpdateDialog = needsUpdate
    }

    if (showServerError) {
        AlertDialog(
            onDismissRequest = { showServerError = false },
            title = { Text("Server Unreachable") },
            text = { Text("The BlueBridge server is currently unreachable. Please contact support.") },
            confirmButton = {
                Button(onClick = { showServerError = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("Update Available") },
            text = { Text("A new version of BlueBridge is available. Would you like to update?") },
            confirmButton = {
                Button(onClick = {
                    showUpdateDialog = false
                    serverViewModel.resetUpdateState()
                    // TODO: Implement update logic
                }) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showUpdateDialog = false
                    serverViewModel.resetUpdateState()
                }) {
                    Text("Later")
                }
            }
        )
    }
}

private fun createViewModelFactory(context: Context): ViewModelFactory {
    val userPreferences = UserPreferences(context)
    val wellPreferences = WellPreferences(context)
    val api = RetrofitBuilder.getServerApi(context)
    val smsApi = RetrofitBuilder.getSmsApi(context)

    val userRepository = UserRepositoryImpl(api, userPreferences)
    val wellRepository = WellRepositoryImpl(api, wellPreferences)
    val nearbyUsersRepository = NearbyUsersRepositoryImpl(api, userRepository)
    val serverRepository = ServerRepositoryImpl(api)
    val weatherRepository = WeatherRepository()

    return ViewModelFactory(
        context = context,
        userRepository = userRepository,
        wellRepository = wellRepository,
        serverRepository = serverRepository,
        smsApi = smsApi,
        nearbyUsersRepository = nearbyUsersRepository,
        weatherRepository = weatherRepository,
    )
}