package com.wellconnect.wellmonitoring

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.wellconnect.wellmonitoring.data.UserDataStoreImpl
import com.wellconnect.wellmonitoring.data.WellDataStore
import com.wellconnect.wellmonitoring.ui.WellViewModel
import com.wellconnect.wellmonitoring.ui.WellViewModelFactory
import com.wellconnect.wellmonitoring.ui.navigation.NavigationGraph
import com.wellconnect.wellmonitoring.ui.theme.My_second_appTheme
import kotlinx.coroutines.launch

/*TODO :
    - Add a screen to pick from a list of given (with a search bar)
        - Server side too
    - Be able to connect to other servers. Currently data is accessible for 192.168.0.98 only
    - Change app icon and boot screen
    -Map implementation


*/


class MainActivity : ComponentActivity() {

    private lateinit var userDataStore: UserDataStoreImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userDataStore = UserDataStoreImpl(applicationContext)

        // Create instance of WellDataStore
        val wellDataStore = WellDataStore(applicationContext)

        // Create the WellViewModelFactory with the necessary data store dependency
        val factory = WellViewModelFactory(wellDataStore)
        val userViewModel = ViewModelProvider(this, factory)[WellViewModel::class.java]

        setContent {
            var themePreference by remember { mutableStateOf(0) } // 0: System, 1: Light, 2: Dark

            // Load theme preference from UserDataStore
            LaunchedEffect(Unit) {
                userDataStore.getUserData().collect { userData ->
                    themePreference = userData?.themePreference ?: 0
                }
            }

            // Compute dark mode based on theme preference
            val systemIsDark = isSystemInDarkTheme()
            val isDarkTheme = when (themePreference) {
                0 -> systemIsDark  // System default
                1 -> false        // Light theme
                2 -> true         // Dark theme
                else -> systemIsDark
            }
            val useSystemTheme = themePreference == 0

            My_second_appTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App(
                        userViewModel = userViewModel,
                        isDarkTheme = isDarkTheme,
                        useSystemTheme = useSystemTheme,
                        onUpdateTheme = { newTheme ->
                            themePreference = newTheme
                            lifecycleScope.launch {
                                userDataStore.updateThemePreference(newTheme)
                            }
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun App(
        userViewModel: WellViewModel,
        isDarkTheme: Boolean,
        useSystemTheme: Boolean,
        onUpdateTheme: (Int) -> Unit
    ) {
        val navController = rememberNavController()
        
        NavigationGraph(
            userViewModel = userViewModel,
            isDarkTheme = isDarkTheme,
            useSystemTheme = useSystemTheme,
            onUpdateTheme = onUpdateTheme,
            navController = navController
        )
    }
}


