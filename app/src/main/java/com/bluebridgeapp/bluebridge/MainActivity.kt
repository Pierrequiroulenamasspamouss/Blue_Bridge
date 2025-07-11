package com.bluebridgeapp.bluebridge


import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.bluebridgeapp.bluebridge.events.AppEventChannel
import com.bluebridgeapp.bluebridge.data.local.UserPreferences
import com.bluebridgeapp.bluebridge.data.local.WellPreferences
import com.bluebridgeapp.bluebridge.events.AppEventHandler
import com.bluebridgeapp.bluebridge.data.repository.NearbyUsersRepositoryImpl
import com.bluebridgeapp.bluebridge.data.repository.ServerRepositoryImpl
import com.bluebridgeapp.bluebridge.data.repository.UserRepositoryImpl
import com.bluebridgeapp.bluebridge.data.repository.WeatherRepository
import com.bluebridgeapp.bluebridge.data.repository.WellRepositoryImpl
import com.bluebridgeapp.bluebridge.network.RetrofitBuilder
import com.bluebridgeapp.bluebridge.viewmodels.ViewModelFactory
import com.google.android.gms.ads.MobileAds



@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : ComponentActivity() {
    lateinit var viewModelFactory: ViewModelFactory
    private lateinit var snackbarHostState: SnackbarHostState // Remove initialization here

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        initializeAdMob()
        viewModelFactory = createViewModelFactory(applicationContext)

        setContent {
            // Initialize SnackbarHostState inside composable
            snackbarHostState = remember { SnackbarHostState() }
            val appEventHandler = remember { AppEventHandler(lifecycleScope, snackbarHostState) }
            AppEventChannel.initialize(appEventHandler)

            MaterialTheme {
                // Provide ActivityResultRegistryOwner to all composables
                CompositionLocalProvider(LocalActivityResultRegistryOwner provides this) {
                    // Box container to position Snackbar at top
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Your main content
                        Scaffold { padding ->
                            Surface(Modifier.fillMaxSize().padding(padding)) {
                                BlueBridgeApp(viewModelFactory)
                            }
                        }

                        // Custom SnackbarHost at top
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 48.dp) // Add some top padding if needed
                        )
                    }
                }
            }
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

    private fun createViewModelFactory(context: Context): ViewModelFactory {
        val userPreferences = UserPreferences(context)
        val wellPreferences = WellPreferences(context)
        val api = RetrofitBuilder.getServerApi(context)
        val smsApi = RetrofitBuilder.getSmsApi(context)

        return ViewModelFactory(
            context = context,
            userRepository = UserRepositoryImpl(api, userPreferences),
            wellRepository = WellRepositoryImpl(api, wellPreferences),
            serverRepository = ServerRepositoryImpl(api),
            smsApi = smsApi,
            nearbyUsersRepository = NearbyUsersRepositoryImpl(
                api,
                UserRepositoryImpl(api, userPreferences)
            ),
            weatherRepository = WeatherRepository(),
        )
    }
}