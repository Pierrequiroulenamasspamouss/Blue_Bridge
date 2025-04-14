package com.jowell.wellmonitoring

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
import com.jowell.wellmonitoring.data.ThemePreference
import com.jowell.wellmonitoring.data.UserPreferencesStore
import com.jowell.wellmonitoring.data.WellDataStore
import com.jowell.wellmonitoring.ui.WellViewModel
import com.jowell.wellmonitoring.ui.WellViewModelFactory
import com.jowell.wellmonitoring.ui.screens.NavigationGraph
import com.jowell.wellmonitoring.ui.theme.My_second_appTheme
import kotlinx.coroutines.launch

/*TODO :
    - Add "last updated: time+day"
    - Add a screen to pick from a list of given (with a search bar)
        - Server side too
    - Be able to connect to other servers. Currently data is accessible for 192.168.0.98 only
    - Change app icon and boot screen
    - Change app name
    - Ask for more user information
    - Don't ask for overwrite able data in the settings (Well name) or the field should only be displayed if the user wants to overwrite the name.
    -Map implementation



*/


class MainActivity : ComponentActivity() {

    private lateinit var userPreferencesStore: UserPreferencesStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userPreferencesStore = UserPreferencesStore(applicationContext)

        // Create instance of WellDataStore (replace this with actual data store object)
        val wellDataStore = WellDataStore(applicationContext)

        // Create the WellViewModelFactory with the necessary data store dependency
        val factory = WellViewModelFactory(wellDataStore)
        val userViewModel = ViewModelProvider(this, factory)[WellViewModel::class.java]

        setContent {
            var themePref by remember { mutableStateOf(ThemePreference.SYSTEM_DEFAULT) }

            // Load theme pref from DataStore
            LaunchedEffect(Unit) {
                userPreferencesStore.userPreferencesFlow.collect {
                    themePref = it.themePreference
                }
            }

            // Compute dark mode + system theme
            val systemIsDark = isSystemInDarkTheme()
            val isDarkTheme = when (themePref) {
                ThemePreference.SYSTEM_DEFAULT -> systemIsDark
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }
            val useSystemTheme = themePref == ThemePreference.SYSTEM_DEFAULT

            My_second_appTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App(
                        userViewModel = userViewModel,
                        isDarkTheme = isDarkTheme,
                        useSystemTheme = useSystemTheme,
                        onUpdateThemePref = { newPref ->
                            themePref = newPref
                            lifecycleScope.launch {
                                userPreferencesStore.saveThemePreference(newPref)
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
        onUpdateThemePref: (ThemePreference) -> Unit
    ) {
        NavigationGraph(
            userViewModel = userViewModel,
            isDarkTheme = isDarkTheme,
            useSystemTheme = useSystemTheme,
            onUpdateThemePref = onUpdateThemePref
        )
    }
}


