package com.wellconnect.wellmonitoring

import UserData
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.wellconnect.wellmonitoring.data.local.UserPreferences
import com.wellconnect.wellmonitoring.data.local.WellPreferences
import com.wellconnect.wellmonitoring.data.repository.NearbyUsersRepositoryImpl
import com.wellconnect.wellmonitoring.data.repository.UserRepositoryImpl
import com.wellconnect.wellmonitoring.data.repository.WellRepositoryImpl
import com.wellconnect.wellmonitoring.network.RetrofitBuilder
import com.wellconnect.wellmonitoring.ui.navigation.NavigationGraph
import com.wellconnect.wellmonitoring.ui.theme.My_second_appTheme
import com.wellconnect.wellmonitoring.viewmodels.NearbyUsersViewModel
import com.wellconnect.wellmonitoring.viewmodels.UiState
import com.wellconnect.wellmonitoring.viewmodels.UserViewModel
import com.wellconnect.wellmonitoring.viewmodels.ViewModelFactory
import com.wellconnect.wellmonitoring.viewmodels.WellViewModel
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

        val wellRepository = WellRepositoryImpl(wellPreferences)
        val nearbyUsersRepository = NearbyUsersRepositoryImpl(api, userRepository)

        // Create ViewModel factory
        val viewModelFactory = ViewModelFactory(
            userRepository = userRepository,
            wellRepository = wellRepository,
            nearbyUsersRepository = nearbyUsersRepository
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
    val wellViewModel: WellViewModel = viewModel(factory = viewModelFactory)
    val nearbyUsersViewModel: NearbyUsersViewModel = viewModel(factory = viewModelFactory)

    // Load user data when app starts
    androidx.compose.runtime.LaunchedEffect(Unit) {
        userViewModel.handleEvent(com.wellconnect.wellmonitoring.data.UserEvent.LoadUser)
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

    My_second_appTheme(darkTheme = isDarkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            App(
                userViewModel = userViewModel,
                wellViewModel = wellViewModel,
                nearbyUsersViewModel = nearbyUsersViewModel,
                viewModelFactory = viewModelFactory
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun App(
    userViewModel: UserViewModel,
    wellViewModel: WellViewModel,
    nearbyUsersViewModel: NearbyUsersViewModel,
    viewModelFactory: ViewModelProvider.Factory
) {
    val navController = rememberNavController()

    NavigationGraph(
        navController = navController,
        userViewModel = userViewModel,
        wellViewModel = wellViewModel,
        nearbyUsersViewModel = nearbyUsersViewModel,
        modifier = Modifier.fillMaxSize(),
        viewModelFactory = viewModelFactory
    )
}