package com.wellconnect.wellmonitoring.ui.navigation

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.wellconnect.wellmonitoring.data.UserDataStoreImpl
import com.wellconnect.wellmonitoring.data.WellDataStore
import com.wellconnect.wellmonitoring.ui.screens.CompassScreen
import com.wellconnect.wellmonitoring.ui.screens.CreditsScreen
import com.wellconnect.wellmonitoring.ui.screens.HomeScreen
import com.wellconnect.wellmonitoring.ui.screens.LoginScreen
import com.wellconnect.wellmonitoring.ui.screens.MapScreen
import com.wellconnect.wellmonitoring.ui.screens.MonitorScreen
import com.wellconnect.wellmonitoring.ui.screens.NearbyUsersScreen
import com.wellconnect.wellmonitoring.ui.screens.ProfileScreen
import com.wellconnect.wellmonitoring.ui.screens.SettingsScreen
import com.wellconnect.wellmonitoring.ui.screens.RegisterScreen
import com.wellconnect.wellmonitoring.ui.screens.WellConfigScreen
import com.wellconnect.wellmonitoring.ui.screens.WellDetailsScreen
import com.wellconnect.wellmonitoring.ui.screens.WellPickerScreen
import com.wellconnect.wellmonitoring.viewmodels.WellViewModel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.InternalSerializationApi
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(InternalSerializationApi::class)
@SuppressLint("StateFlowValueCalledInComposition", "NewApi")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NavigationGraph(
    userViewModel: WellViewModel,
    isDarkTheme: Boolean,
    useSystemTheme: Boolean,
    onUpdateTheme: (Int) -> Unit,
    navController: NavHostController
) {
    val context = LocalContext.current
    val userDataStore = remember { UserDataStoreImpl(context) }
    
    // Always log the login status for debugging but don't use it for navigation
    val isLoggedIn = runBlocking {
        try {
            userDataStore.isLoggedIn()
        } catch (e: Exception) {
            Log.e("NavigationGraph", "Error checking login state", e)
            false
        }
    }
    
    Log.d("NavigationGraph", "User logged in status: $isLoggedIn (but always starting with HOME_SCREEN)")

    NavHost(
        navController = navController,
        startDestination = Routes.HOME_SCREEN
    ) {
        // Home Screen
        composable(Routes.HOME_SCREEN) {
            HomeScreen(
                navController = navController,
                wellViewModel = userViewModel
            )
        }

        // Monitoring Screen
        composable(Routes.MONITORING_SCREEN) {
            MonitorScreen(
                wellViewModel = userViewModel,
                navController = navController
            )
        }

        // Well Picker Screen
        composable(Routes.WELL_PICKER_SCREEN) {
            val userData = userDataStore.getUserData().collectAsState(initial = null).value
            WellPickerScreen(
                userData = userData,
                wellDataStore = remember { WellDataStore(context) },
                navController = navController
            )
        }

        // Profile Screen
        composable(Routes.PROFILE_SCREEN) {
            val userData = userDataStore.getUserData().collectAsState(initial = null).value
            if (userData != null) {
                ProfileScreen(
                    navController = navController,
                    userViewModel = userViewModel
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.LOGIN_SCREEN) {
                        popUpTo(Routes.PROFILE_SCREEN) { inclusive = true }
                    }
                }
            }
        }

        // Well Details Screen
        composable(
            route = "${Routes.WELL_DETAILS_SCREEN}/{wellId}",
            arguments = listOf(navArgument("wellId") { type = NavType.IntType })
        ) { backStackEntry ->
            val wellId = backStackEntry.arguments?.getInt("wellId") ?: 0
            WellDetailsScreen(
                navController = navController,
                wellViewModel = userViewModel,
                wellId = wellId
            )
        }

        // Well Config Screen
        composable(
            route = "${Routes.WELL_CONFIG_SCREEN}/{wellId}",
            arguments = listOf(navArgument("wellId") { type = NavType.IntType })
        ) { backStackEntry ->
            val wellId = backStackEntry.arguments?.getInt("wellId") ?: 0
            WellConfigScreen(
                wellId = wellId,
                navController = navController,
                wellViewModel = userViewModel
            )
        }

        // Credits Screen
        composable(Routes.CREDITS_SCREEN) {
            CreditsScreen(navController, userViewModel)
        }

        // Settings Screen
        composable(Routes.SETTINGS_SCREEN) {
            val userData = userDataStore.getUserData().collectAsState(initial = null).value
            SettingsScreen(
                userData = userData,
                userDataStore = userDataStore,
                navController = navController
            )
        }

        // Login Screen
        composable(Routes.LOGIN_SCREEN) {
            // Check if already logged in, if so, navigate to home
            val userData = userDataStore.getUserData().collectAsState(initial = null).value
            if (userData != null) {
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.HOME_SCREEN) {
                        popUpTo(Routes.LOGIN_SCREEN) { inclusive = true }
                    }
                }
            } else {
                LoginScreen(
                    navController = navController,
                    userDataStore = userDataStore,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Signup Screen
        composable(Routes.REGISTER_SCREEN) {
            RegisterScreen(
                navController = navController
            )
        }

        // Compass Screen
        composable(
            route = "${Routes.COMPASS_SCREEN}?lat={lat}&lon={lon}&name={name}",
            arguments = listOf(
                navArgument("lat") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("lon") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("name") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull()
            val lon = backStackEntry.arguments?.getString("lon")?.toDoubleOrNull()
            val name = backStackEntry.arguments?.getString("name")
            
            CompassScreen(
                navController = navController,
                latitude = lat,
                longitude = lon,
                locationName = name,
                wellViewModel = userViewModel
            )
        }

        // Nearby Users Screen
        composable(Routes.NEARBY_USERS_SCREEN) {
            NearbyUsersScreen(
                onNavigateToCompass = { lat, lon, name ->
                    val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
                    navController.navigate(
                        "${Routes.COMPASS_SCREEN}?lat=$lat&lon=$lon&name=$encodedName"
                    )
                },
                navController = navController
            )
        }
        
        // Map Screen
        composable(
            route = "${Routes.MAP_SCREEN}?userLat={userLat}&userLon={userLon}&targetLat={targetLat}&targetLon={targetLon}",
            arguments = listOf(
                navArgument("userLat") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("userLon") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("targetLat") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("targetLon") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val userLat = backStackEntry.arguments?.getString("userLat")?.toDoubleOrNull()
            val userLon = backStackEntry.arguments?.getString("userLon")?.toDoubleOrNull()
            val targetLat = backStackEntry.arguments?.getString("targetLat")?.toDoubleOrNull()
            val targetLon = backStackEntry.arguments?.getString("targetLon")?.toDoubleOrNull()
            
            MapScreen(
                navController = navController,
                wellViewModel = userViewModel,
                userLat = userLat,
                userLon = userLon,
                targetLat = targetLat,
                targetLon = targetLon
            )
        }
    }
}


