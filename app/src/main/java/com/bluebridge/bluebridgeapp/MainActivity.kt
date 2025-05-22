package com.bluebridge.bluebridgeapp


import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.bluebridge.bluebridgeapp.data.UserEvent
import com.bluebridge.bluebridgeapp.data.local.UserPreferences
import com.bluebridge.bluebridgeapp.data.local.WellPreferences
import com.bluebridge.bluebridgeapp.data.model.UserData
import com.bluebridge.bluebridgeapp.data.repository.NearbyUsersRepositoryImpl
import com.bluebridge.bluebridgeapp.data.repository.ServerRepositoryImpl
import com.bluebridge.bluebridgeapp.data.repository.UserRepositoryImpl
import com.bluebridge.bluebridgeapp.data.repository.WeatherRepository
import com.bluebridge.bluebridgeapp.data.repository.WellRepositoryImpl
import com.bluebridge.bluebridgeapp.network.RetrofitBuilder
import com.bluebridge.bluebridgeapp.ui.navigation.NavigationGraph
import com.bluebridge.bluebridgeapp.ui.theme.My_second_appTheme
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
import com.bluebridge.bluebridgeapp.viewmodels.ServerViewModel
import com.bluebridge.bluebridgeapp.viewmodels.SmsViewModel
import com.bluebridge.bluebridgeapp.viewmodels.UiState
import com.bluebridge.bluebridgeapp.viewmodels.UserViewModel
import com.bluebridge.bluebridgeapp.viewmodels.ViewModelFactory
import com.bluebridge.bluebridgeapp.viewmodels.WeatherViewModel
import com.bluebridge.bluebridgeapp.viewmodels.WellViewModel
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import kotlinx.serialization.InternalSerializationApi

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(InternalSerializationApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "Starting app initialization")
        splashScreen.setKeepOnScreenCondition { false }

        // Initialize dependencies
        val userPreferences = UserPreferences(applicationContext)
        val wellPreferences = WellPreferences(applicationContext)
        val api = RetrofitBuilder.getServerApi(applicationContext)
        // Initialize AdMob
        initializeAdMob()

        Log.d("MainActivity", "Initializing repositories and viewmodels")
        // Initialize repositories in correct order
        val userRepository = UserRepositoryImpl(
            api = api,
            preferences = userPreferences
        )

        val weatherRepository = WeatherRepository() //This is for standardization, weatherRepository does nothing.
        val wellRepository = WellRepositoryImpl(
            api = api,
            preferences = wellPreferences
        )
        val nearbyUsersRepository = NearbyUsersRepositoryImpl(api, userRepository)
        val serverRepository = ServerRepositoryImpl(api)

        // Create ViewModel factory
        val viewModelFactory = ViewModelFactory(
            context = applicationContext,
            userRepository = userRepository,
            wellRepository = wellRepository,
            serverRepository = serverRepository,
            smsApi = RetrofitBuilder.getSmsApi(applicationContext),
            nearbyUsersRepository = nearbyUsersRepository,
            weatherRepository = weatherRepository,
        )

        setContent {
            MyApp(viewModelFactory = viewModelFactory)
        }
    }

    private fun initializeAdMob() {
        try {
            Log.d("MainActivity", "Initializing AdMob")
            // Initialize with test ads first
            val configuration = RequestConfiguration.Builder()
                .setTestDeviceIds(listOf("ABCDEF012345"))
                .build()
            MobileAds.setRequestConfiguration(configuration)
            
            MobileAds.initialize(this) { initializationStatus ->
                val statusMap = initializationStatus.adapterStatusMap
                Log.d("MainActivity", "AdMob initialization complete: ${statusMap.size} adapters")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to initialize AdMob: ${e.message}", e)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun MyApp(
    viewModelFactory: ViewModelProvider.Factory

) {
    val userPreferences = UserPreferences(LocalContext.current)

    val userViewModel: UserViewModel = viewModel(factory = viewModelFactory)
    // Load user data when app starts
    LaunchedEffect(Unit) {
        Log.d("MainActivity", "Starting initial user data load")
        try {
            val userId = userViewModel.repository.getUserId()
            val isLoggedIn = userViewModel.repository.isLoggedIn()
            Log.d("MainActivity", "Initial state - userId: $userId, isLoggedIn: $isLoggedIn")
            
            if (userId != null && isLoggedIn) {
                Log.d("MainActivity", "Found valid user session, loading user data")
                userViewModel.handleEvent(UserEvent.LoadUser(userId = userId.toString()))
            } else {
                Log.d("MainActivity", "No valid user session found")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error during initial user data load", e)
        }
    }

    // Collect user state safely
    val userState by userViewModel.state

    // Determine theme preference
    val themePreference = remember(userState) {
        when (userState) {
            is UiState.Success -> (userState as UiState.Success<UserData>).data.themePreference
            else -> 0 // Default theme
        }
    }

    val isDarkTheme = when (themePreference) {
        0 -> isSystemInDarkTheme()
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }

    // Apply the selected theme based on themePreference
    when (themePreference) {
        0, 1, 2 -> { // System, Light, Dark - use default theme
            My_second_appTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App(
                        viewModelFactory = viewModelFactory
                    )
                }
            }
        }
        3 -> { // Green theme
            MaterialTheme(
                colorScheme = getGreenColorScheme(isDarkTheme),
                typography = Typography()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App(
                        viewModelFactory = viewModelFactory
                    )
                }
            }
        }
        4 -> { // Pink theme
            MaterialTheme(
                colorScheme = getPinkColorScheme(isDarkTheme),
                typography = Typography()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App(
                        viewModelFactory = viewModelFactory
                    )
                }
            }
        }
        5 -> { // Red theme
            MaterialTheme(
                colorScheme = getRedColorScheme(isDarkTheme),
                typography = Typography()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App(
                        viewModelFactory = viewModelFactory
                    )
                }
            }
        }
        6 -> { // Purple theme
            MaterialTheme(
                colorScheme = getPurpleColorScheme(isDarkTheme),
                typography = Typography()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App(
                        viewModelFactory = viewModelFactory
                    )
                }
            }
        }
        7 -> { // Yellow theme
            MaterialTheme(
                colorScheme = getYellowColorScheme(isDarkTheme),
                typography = Typography()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App(
                        viewModelFactory = viewModelFactory
                    )
                }
            }
        }
        8 -> { // Tan theme
            MaterialTheme(
                colorScheme = getTanColorScheme(isDarkTheme),
                typography = Typography()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App(
                        viewModelFactory = viewModelFactory
                    )
                }
            }
        }
        9 -> { // Orange theme
            MaterialTheme(
                colorScheme = getOrangeColorScheme(isDarkTheme),
                typography = Typography()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App(
                        viewModelFactory = viewModelFactory
                    )
                }
            }
        }
        10 -> { // Cyan theme
            MaterialTheme(
                colorScheme = getCyanColorScheme(isDarkTheme),
                typography = Typography()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App(
                        viewModelFactory = viewModelFactory
                    )
                }
            }
        }
        else -> { // Fallback to default theme
            My_second_appTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App(
                        viewModelFactory = viewModelFactory
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun App(
    viewModelFactory: ViewModelProvider.Factory,

) {
    Log.d("App", "App started")
    val userViewModel: UserViewModel = viewModel(factory = viewModelFactory)
    val wellViewModel: WellViewModel = viewModel(factory = viewModelFactory)
    val nearbyUsersViewModel: NearbyUsersViewModel = viewModel(factory = viewModelFactory)
    val weatherViewModel: WeatherViewModel = viewModel(factory = viewModelFactory)
    val smsViewModel: SmsViewModel = viewModel(factory = viewModelFactory)
    val serverViewModel: ServerViewModel = viewModel(factory = viewModelFactory)
    val navController = rememberNavController()
    val context = LocalContext.current
    var isOnline by remember { mutableStateOf(isNetworkAvailable(context)) }
    var hasCheckedServer by remember { mutableStateOf(false) }
    var showLocationPermissionDialog by remember { mutableStateOf(false) }
    var showNotificationPermissionDialog by remember { mutableStateOf(false) }
    var showServerUnreachableDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    val notificationsEnabled by userViewModel.notificationsEnabled
    // Check server status when online
    LaunchedEffect(isOnline) {
        if (isOnline && !hasCheckedServer) {
            serverViewModel.getServerStatus()
            hasCheckedServer = true

        }
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


    // Register network callback to monitor connectivity changes
    DisposableEffect(Unit) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOnline = true
            }

            override fun onLost(network: Network) {
                isOnline = isNetworkAvailable(context)
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
