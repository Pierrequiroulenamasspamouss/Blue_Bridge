package com.bluebridge.bluebridgeapp


import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import com.bluebridge.bluebridgeapp.viewmodels.NearbyUsersViewModel
import com.bluebridge.bluebridgeapp.viewmodels.ServerViewModel
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

        val wellRepository = WellRepositoryImpl(
            api = api,
            preferences = wellPreferences
        )
        val nearbyUsersRepository = NearbyUsersRepositoryImpl(api, userRepository)
        val serverRepository = ServerRepositoryImpl(api)

        // Create ViewModel factory
        val viewModelFactory = ViewModelFactory(
            userRepository = userRepository,
            wellRepository = wellRepository,
            nearbyUsersRepository = nearbyUsersRepository,
            serverRepository = serverRepository,
        )

        setContent {
            MyApp(viewModelFactory = viewModelFactory)
        }
    }

    private fun initializeAdMob() {
        try {
            Log.d("MainActivity", "Initializing AdMob")
            MobileAds.initialize(this) { initializationStatus ->
                val statusMap = initializationStatus.adapterStatusMap
                Log.d("MainActivity", "AdMob initialization complete: ${statusMap.size} adapters")
                
                // Configure test devices if in development
                val testDeviceIds = listOf("ABCDEF012345") // Add your test device IDs here
                val configuration = RequestConfiguration.Builder()
                    .setTestDeviceIds(testDeviceIds)
                    .build()
                MobileAds.setRequestConfiguration(configuration)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to initialize AdMob: ${e.message}", e)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun MyApp(viewModelFactory: ViewModelProvider.Factory) {
    val userViewModel: UserViewModel = viewModel(factory = viewModelFactory)

    // Load user data when app starts
    LaunchedEffect(Unit) {
        userViewModel.handleEvent(UserEvent.LoadUser)
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
    val userViewModel: UserViewModel = viewModel(factory = viewModelFactory)
    val wellViewModel: WellViewModel = viewModel(factory = viewModelFactory)
    val nearbyUsersViewModel: NearbyUsersViewModel = viewModel(factory = viewModelFactory)
    val weatherViewModel: WeatherViewModel = viewModel(factory = viewModelFactory)
    val serverViewModel: ServerViewModel = viewModel(factory = viewModelFactory)
    val navController = rememberNavController()


    NavigationGraph(

        navController = navController,
        userViewModel = userViewModel,
        wellViewModel = wellViewModel,
        nearbyUsersViewModel = nearbyUsersViewModel,
        weatherViewModel = weatherViewModel,
        modifier = Modifier.fillMaxSize(),
        serverViewModel = serverViewModel
    )
}
