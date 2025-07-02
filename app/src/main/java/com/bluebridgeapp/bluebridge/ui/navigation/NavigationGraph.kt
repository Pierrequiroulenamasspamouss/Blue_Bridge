package com.bluebridgeapp.bluebridge.ui.navigation

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.bluebridgeapp.bluebridge.data.model.UserData
import com.bluebridgeapp.bluebridge.ui.screens.CompassScreen
import com.bluebridgeapp.bluebridge.ui.screens.HomeScreen
import com.bluebridgeapp.bluebridge.ui.screens.UrgentSmsScreen
import com.bluebridgeapp.bluebridge.ui.screens.WeatherScreen
import com.bluebridgeapp.bluebridge.ui.screens.miscscreens.AdMobScreen
import com.bluebridgeapp.bluebridge.ui.screens.miscscreens.CreditsScreen
import com.bluebridgeapp.bluebridge.ui.screens.miscscreens.LanguageSelectionScreen
import com.bluebridgeapp.bluebridge.ui.screens.miscscreens.EasterEgg
import com.bluebridgeapp.bluebridge.ui.screens.miscscreens.FeatureNotImplementedScreen
import com.bluebridgeapp.bluebridge.ui.screens.miscscreens.LoadingScreen
import com.bluebridgeapp.bluebridge.ui.screens.navscreens.MapScreen
import com.bluebridgeapp.bluebridge.ui.screens.userscreens.EditWaterNeedsScreen
import com.bluebridgeapp.bluebridge.ui.screens.userscreens.LoginScreen
import com.bluebridgeapp.bluebridge.ui.screens.userscreens.NearbyUsersScreen
import com.bluebridgeapp.bluebridge.ui.screens.userscreens.ProfileScreen
import com.bluebridgeapp.bluebridge.ui.screens.userscreens.RegisterScreen
import com.bluebridgeapp.bluebridge.ui.screens.userscreens.SettingsScreen
import com.bluebridgeapp.bluebridge.ui.screens.wellscreens.BrowseWellsScreen
import com.bluebridgeapp.bluebridge.ui.screens.wellscreens.MonitorScreen
import com.bluebridgeapp.bluebridge.ui.screens.wellscreens.WellConfigScreen
import com.bluebridgeapp.bluebridge.ui.screens.wellscreens.WellDetailsScreen
import com.bluebridgeapp.bluebridge.viewmodels.NearbyUsersViewModel
import com.bluebridgeapp.bluebridge.viewmodels.SmsViewModel
import com.bluebridgeapp.bluebridge.viewmodels.UiState
import com.bluebridgeapp.bluebridge.viewmodels.UserViewModel
import com.bluebridgeapp.bluebridge.viewmodels.WeatherViewModel
import com.bluebridgeapp.bluebridge.viewmodels.WellViewModel

@SuppressLint("StateFlowValueCalledInComposition")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NavigationGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    nearbyUsersViewModel: NearbyUsersViewModel,
    wellViewModel: WellViewModel,
    userViewModel: UserViewModel,
    weatherViewModel: WeatherViewModel,
    smsViewModel: SmsViewModel,
    paddingValues: PaddingValues,
) {

    NavHost(
        navController = navController,
        startDestination = Routes.HOME_SCREEN,
        modifier = modifier
    ) {
        composable(Routes.FEATURE_NOT_IMPLEMENTED) {
            FeatureNotImplementedScreen(navController = navController)
        }
        composable(Routes.EDIT_WATER_NEEDS_SCREEN) {
            val userState = userViewModel.state.value
            val userData = (userState as? UiState.Success<UserData>)?.data
            
            // Check if user is guest
            if (userData == null || userData.role == "guest") {
                HomeScreen(
                    navController = navController, userViewModel = userViewModel,
                )
                // Show loading screen while redirecting
                LoadingScreen()
            } else {
                EditWaterNeedsScreen(
                    userViewModel = userViewModel,
                    navController = navController
                )
            }
        }

        // Home Screen
        composable(Routes.HOME_SCREEN) {
            HomeScreen(
                navController = navController,
                userViewModel = userViewModel,
            )
        }

        // Profile Screen
        composable(Routes.PROFILE_SCREEN) {
            val userState = userViewModel.state.value

            when (userState) {
                is UiState.Success<*> -> {
                    val userData = userState.data as? UserData
                    
                    // Check if user is guest
                    if (userData == null || userData.role == "guest") {
                        LaunchedEffect(Unit) {
                            navController.navigate(Routes.LOGIN_SCREEN) {
                                popUpTo(Routes.PROFILE_SCREEN) { inclusive = true }
                            }
                        }
                        // Show loading screen while redirecting
                        LoadingScreen()
                    } else {
                        ProfileScreen(
                            navController = navController,
                            userViewModel = userViewModel
                        )
                    }
                }

                is UiState.Error, UiState.Empty -> {
                    LaunchedEffect(Unit) {
                        navController.navigate(Routes.LOGIN_SCREEN) {
                            popUpTo(Routes.PROFILE_SCREEN) { inclusive = true }
                        }
                    }
                    // Show loading screen while redirecting
                    LoadingScreen()
                }

                UiState.Loading -> LoadingScreen()
            }
        }

        // Login Screen
        composable(Routes.LOGIN_SCREEN) {
            val userState = userViewModel.state.value

            when (userState) {
                is UiState.Success<*> -> {
                    LaunchedEffect(Unit) {
                        navController.navigate(Routes.HOME_SCREEN) {
                            popUpTo(Routes.LOGIN_SCREEN) { inclusive = true }
                        }
                    }
                }

                else -> LoginScreen(
                    navController = navController,
                    userViewModel = userViewModel,
                )
            }
        }

        // Monitoring Screen
        composable(Routes.MONITORING_SCREEN) {
            MonitorScreen(
                navController = navController,
                wellViewModel = wellViewModel,
                userViewModel = userViewModel
            )
        }

        // Well Picker Screen
        composable(Routes.BROWSE_WELLS_SCREEN) {
            val userState = userViewModel.state.value
            wellViewModel.wellsListState.value

            when (userState) {
                is UiState.Success<*> -> BrowseWellsScreen(
                    userData = userState.data as? UserData,
                    navController = navController
                )
                else -> {
                    LaunchedEffect(Unit) {
                        navController.navigate(Routes.LOGIN_SCREEN) {
                            popUpTo(Routes.BROWSE_WELLS_SCREEN) { inclusive = true }
                        }
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

            wellViewModel.loadWell(wellId)
            Log.d("WellDetailsScreen", "Navigating to details of well with wellId: $wellId")
            WellDetailsScreen(
                navController = navController,
                wellViewModel = wellViewModel,
                wellId = wellId,
                userViewModel = userViewModel
            )
        }

        composable(
            route = "${Routes.WELL_DETAILS_TEMP_SCREEN}/{espId}",
            arguments = listOf(navArgument("espId") {
                type = NavType.StringType
            })
        ) { backStackEntry ->
            // Use LaunchedEffect to perform navigation just once
            LaunchedEffect(Unit) {
                navController.navigate(Routes.FEATURE_NOT_IMPLEMENTED) {
                    // Pop the temp screen from back stack
                    popUpTo(Routes.WELL_DETAILS_TEMP_SCREEN) {
                        inclusive = true
                    }
                    // Prevent multiple instances of the feature screen
                    launchSingleTop = true
                }
            }

            // Show empty content while redirecting
            Box(modifier = Modifier.fillMaxSize())
        }

        // Well Config Screen
        composable(
            route = "${Routes.WELL_CONFIG_SCREEN}/{wellId}",
            arguments = listOf(navArgument("wellId") {
                type = NavType.StringType
            })
        ) { backStackEntry ->
            val wellIdParam = backStackEntry.arguments?.getString("wellId") ?: ""
            val userState = userViewModel.state.value
            val userData = (userState as? UiState.Success<UserData>)?.data

            // Check authentication first
            if (userData == null || userData.role == "guest") {
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.LOGIN_SCREEN) {
                        popUpTo(Routes.WELL_CONFIG_SCREEN) { inclusive = true }
                    }
                }
                LoadingScreen()
            }
            // Handle new well case
            else if (wellIdParam == Routes.WELL_CONFIG_NEW) {
                FeatureNotImplementedScreen(
                    navController = navController
                )
            }
            // Handle existing well case
            else {
                val wellId = wellIdParam.toIntOrNull() ?: 0
                WellConfigScreen(
                    navController = navController,
                    wellViewModel = wellViewModel,
                    wellId = wellId,
                    userViewModel = userViewModel
                )
            }
        }
        // Settings Screen
        composable(Routes.SETTINGS_SCREEN) {
            val userState = userViewModel.state.value

            SettingsScreen(
                userState = userState,
                userViewModel = userViewModel,
                navController = navController,
            )
        }

        // Nearby Users Screen
        composable(Routes.NEARBY_USERS_SCREEN) {

            val userState = userViewModel.state.value
            val userData = (userState as? UiState.Success<UserData>)?.data
            
            // Check if user is guest
            if (userData == null || userData.role == "guest") {
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.LOGIN_SCREEN) {
                        popUpTo(Routes.NEARBY_USERS_SCREEN) { inclusive = true }
                    }
                }
                // Show loading screen while redirecting
                LoadingScreen()
            } else {
                NearbyUsersScreen(
                    nearbyUsersViewModel = nearbyUsersViewModel
                )
            }
        }

        // Compass Screen
        composable(
            route = "${Routes.COMPASS_SCREEN}?lat={lat}&lon={lon}&name={name}",
            arguments = listOf(
                navArgument("lat") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("lon") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("name") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull()?.coerceIn(-90.0, 90.0)
            val lon = backStackEntry.arguments?.getString("lon")?.toDoubleOrNull()?.coerceIn(-180.0, 180.0)
            val name = backStackEntry.arguments?.getString("name") ?: "Destination"

            CompassScreen(
                navController = navController,
                latitude = lat,
                longitude = lon,
                locationName = name,
                wellViewModel = wellViewModel
            )
        }

        // Map Screen
        composable(
            route = "${Routes.MAP_SCREEN}?userLat={userLat}&userLon={userLon}&targetLat={targetLat}&targetLon={targetLon}",
            arguments = listOf(
                navArgument("userLat") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("userLon") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("targetLat") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("targetLon") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val userLat = backStackEntry.arguments?.getString("userLat")?.toDoubleOrNull()?.coerceIn(-90.0, 90.0)
            val userLon = backStackEntry.arguments?.getString("userLon")?.toDoubleOrNull()?.coerceIn(-180.0, 180.0)
            val targetLat = backStackEntry.arguments?.getString("targetLat")?.toDoubleOrNull()?.coerceIn(-90.0, 90.0)
            val targetLon = backStackEntry.arguments?.getString("targetLon")?.toDoubleOrNull()?.coerceIn(-180.0, 180.0)

            MapScreen(
                navController = navController,
                wellViewModel = wellViewModel,
                userLat = userLat,
                userLon = userLon,
                targetLat = targetLat,
                targetLon = targetLon
            )
        }

        // Credits Screen
        composable(Routes.CREDITS_SCREEN) {
            CreditsScreen(
            )
        }

        // Register Screen
        composable(Routes.REGISTER_SCREEN) {
            RegisterScreen(
                navController = navController,
                userViewModel = userViewModel
            )
        }
        
        // AdMob Screen
        composable(Routes.ADMOB_SCREEN) {
            AdMobScreen(
                navController = navController
            )
        }

        // Weather Screen
        composable(Routes.WEATHER_SCREEN) {
            WeatherScreen(
                userViewModel = userViewModel,
                weatherViewModel = weatherViewModel,
                modifier = modifier,
                navController = navController,
            )
        }

        // Urgent SMS Screen
        composable(Routes.URGENT_SMS_SCREEN) {
            val userState = userViewModel.state.value
            LocalContext.current
            val userData = (userState as? UiState.Success<UserData>)?.data

            if (userData == null || userData.role == "guest") {
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.LOGIN_SCREEN) {
                        popUpTo(Routes.URGENT_SMS_SCREEN) { this.inclusive = true }
                    }
                }
                LoadingScreen()
            } else {

                UrgentSmsScreen(
                    smsViewModel = smsViewModel,
                    modifier = modifier
                )
            }
        }

        // Easter Egg Screen
        composable(Routes.EASTER_EGG_SCREEN) {
            EasterEgg()
        }

        composable(Routes.LANGUAGE_SELECTION_SCREEN) {
            LanguageSelectionScreen(navController = navController, userViewModel)
        }


    }

}




