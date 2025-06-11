package com.bluebridge.bluebridgeapp.ui.navigation

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.bluebridge.bluebridgeapp.data.model.UserData
import com.bluebridge.bluebridgeapp.network.SmsApi
import com.bluebridge.bluebridgeapp.ui.screens.AdMobScreen
import com.bluebridge.bluebridgeapp.ui.screens.BrowseWellsScreen
import com.bluebridge.bluebridgeapp.ui.screens.CompassScreen
import com.bluebridge.bluebridgeapp.ui.screens.CreditsScreen
import com.bluebridge.bluebridgeapp.ui.screens.EasterEgg
import com.bluebridge.bluebridgeapp.ui.screens.EditWaterNeedsScreen
import com.bluebridge.bluebridgeapp.ui.screens.FeatureNotImplementedScreen
import com.bluebridge.bluebridgeapp.ui.screens.HomeScreen
import com.bluebridge.bluebridgeapp.ui.screens.LoadingScreen
import com.bluebridge.bluebridgeapp.ui.screens.LoginScreen
import com.bluebridge.bluebridgeapp.ui.screens.MapScreen
import com.bluebridge.bluebridgeapp.ui.screens.MonitorScreen
import com.bluebridge.bluebridgeapp.ui.screens.NearbyUsersScreen
import com.bluebridge.bluebridgeapp.ui.screens.ProfileScreen
import com.bluebridge.bluebridgeapp.ui.screens.RegisterScreen
import com.bluebridge.bluebridgeapp.ui.screens.SettingsScreen
import com.bluebridge.bluebridgeapp.ui.screens.UrgentSmsScreen
import com.bluebridge.bluebridgeapp.ui.screens.WeatherScreen
import com.bluebridge.bluebridgeapp.ui.screens.WellConfigScreen
import com.bluebridge.bluebridgeapp.ui.screens.WellDetailsScreen
import com.bluebridge.bluebridgeapp.viewmodels.NearbyUsersViewModel
import com.bluebridge.bluebridgeapp.viewmodels.SmsViewModel
import com.bluebridge.bluebridgeapp.viewmodels.UiState
import com.bluebridge.bluebridgeapp.viewmodels.UserViewModel
import com.bluebridge.bluebridgeapp.viewmodels.WeatherViewModel
import com.bluebridge.bluebridgeapp.viewmodels.WellViewModel

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
    smsViewModel: SmsViewModel
) {

    NavHost(
        navController = navController,
        startDestination = Routes.HOME_SCREEN,
        modifier = modifier
    ) {
        composable(Routes.FEATURE_NOT_IMPLEMENTED) {
            FeatureNotImplementedScreen(navController = navController) { navController.popBackStack() }
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

            WellDetailsScreen(
                navController = navController,
                wellViewModel = wellViewModel,
                wellId = wellId,
                userViewModel = userViewModel
            )
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
                    onBackClick = { navController.popBackStack() },
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
            val nearbyState = nearbyUsersViewModel.uiState.value
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
                    nearbyState = nearbyState,
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
            val context = LocalContext.current
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
                    navController = navController,
                    smsViewModel = smsViewModel,
                    userViewModel = userViewModel,
                    smsApi = SmsApi(context),
                    modifier = modifier
                )
            }
        }

        // Easter Egg Screen
        composable(Routes.EASTER_EGG_SCREEN) {
            EasterEgg()
        }
    }

}



