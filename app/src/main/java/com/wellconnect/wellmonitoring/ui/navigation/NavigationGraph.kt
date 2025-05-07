package com.wellconnect.wellmonitoring.ui.navigation

import UserData
import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.wellconnect.wellmonitoring.ui.screens.AdMobScreen
import com.wellconnect.wellmonitoring.ui.screens.CompassScreen
import com.wellconnect.wellmonitoring.ui.screens.CreditsScreen
import com.wellconnect.wellmonitoring.ui.screens.EasterEgg
import com.wellconnect.wellmonitoring.ui.screens.EditWaterNeedsScreen
import com.wellconnect.wellmonitoring.ui.screens.HomeScreen
import com.wellconnect.wellmonitoring.ui.screens.LoadingScreen
import com.wellconnect.wellmonitoring.ui.screens.LoginScreen
import com.wellconnect.wellmonitoring.ui.screens.MapScreen
import com.wellconnect.wellmonitoring.ui.screens.MonitorScreen
import com.wellconnect.wellmonitoring.ui.screens.NearbyUsersScreen
import com.wellconnect.wellmonitoring.ui.screens.ProfileScreen
import com.wellconnect.wellmonitoring.ui.screens.RegisterScreen
import com.wellconnect.wellmonitoring.ui.screens.SettingsScreen
import com.wellconnect.wellmonitoring.ui.screens.WeatherScreen
import com.wellconnect.wellmonitoring.ui.screens.WellConfigScreen
import com.wellconnect.wellmonitoring.ui.screens.WellDetailsScreen
import com.wellconnect.wellmonitoring.ui.screens.WellPickerScreen
import com.wellconnect.wellmonitoring.viewmodels.NearbyUsersViewModel
import com.wellconnect.wellmonitoring.viewmodels.UiState
import com.wellconnect.wellmonitoring.viewmodels.UserViewModel
import com.wellconnect.wellmonitoring.viewmodels.WeatherViewModel
import com.wellconnect.wellmonitoring.viewmodels.WellViewModel


@SuppressLint("StateFlowValueCalledInComposition")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NavigationGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    nearbyUsersViewModel: NearbyUsersViewModel,
    wellViewModel: WellViewModel,
    userViewModel: UserViewModel,
    weatherViewModel: WeatherViewModel
) {
    
    NavHost(
        navController = navController,
        startDestination = Routes.HOME_SCREEN,
        modifier = modifier
    ) {

        composable(Routes.EDIT_WATER_NEEDS_SCREEN) {
            val userState = userViewModel.state.value
            val userData = (userState as? UiState.Success<UserData>)?.data
            
            // Check if user is guest
            if (userData == null || userData.role == "guest") {
                HomeScreen(navController = navController, userViewModel = userViewModel)
                // Show loading screen while redirecting
                LoadingScreen()
            } else {
                EditWaterNeedsScreen(
                    navController = navController,
                    userViewModel = userViewModel
                )
            }
        }

        // Home Screen
        composable(Routes.HOME_SCREEN) {
            HomeScreen(
                navController = navController,
                userViewModel = userViewModel
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
                    modifier = Modifier.fillMaxSize(),
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
        composable(Routes.WELL_PICKER_SCREEN) {
            val userState = userViewModel.state.value
            val wellsState = wellViewModel.wellsListState.value

            when (userState) {
                is UiState.Success<*> -> WellPickerScreen(
                    userData = userState.data as? UserData,
                    navController = navController,
                    wellRepository = wellViewModel.repository
                )
                else -> {
                    LaunchedEffect(Unit) {
                        navController.navigate(Routes.LOGIN_SCREEN) {
                            popUpTo(Routes.WELL_PICKER_SCREEN) { inclusive = true }
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
            arguments = listOf(navArgument("wellId") { type = NavType.IntType })
        ) { backStackEntry ->
            val wellId = backStackEntry.arguments?.getInt("wellId") ?: 0
            wellViewModel.loadWell(wellId)

            WellConfigScreen(
                wellViewModel = wellViewModel,
                navController = navController,
                wellId = wellId,
                userViewModel = userViewModel,
            )
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
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull()
            val lon = backStackEntry.arguments?.getString("lon")?.toDoubleOrNull()
            val name = backStackEntry.arguments?.getString("name")

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
            val userLat = backStackEntry.arguments?.getString("userLat")?.toDoubleOrNull()
            val userLon = backStackEntry.arguments?.getString("userLon")?.toDoubleOrNull()
            val targetLat = backStackEntry.arguments?.getString("targetLat")?.toDoubleOrNull()
            val targetLon = backStackEntry.arguments?.getString("targetLon")?.toDoubleOrNull()

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
                weatherViewModel = weatherViewModel
            )
        }

        // Easter Egg Screen
        composable(Routes.EASTER_EGG_SCREEN) {
            EasterEgg()
        }
    }

}



