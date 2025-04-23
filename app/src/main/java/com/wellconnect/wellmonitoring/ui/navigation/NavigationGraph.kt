package com.wellconnect.wellmonitoring.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.wellconnect.wellmonitoring.data.UserDataStoreImpl
import com.wellconnect.wellmonitoring.ui.WellViewModel
import com.wellconnect.wellmonitoring.ui.screens.CompassScreen
import com.wellconnect.wellmonitoring.ui.screens.CreditsScreen
import com.wellconnect.wellmonitoring.ui.screens.HomeScreen
import com.wellconnect.wellmonitoring.ui.screens.LoginScreen
import com.wellconnect.wellmonitoring.ui.screens.MonitorScreen
import com.wellconnect.wellmonitoring.ui.screens.SettingsScreen
import com.wellconnect.wellmonitoring.ui.screens.SignUpScreen
import com.wellconnect.wellmonitoring.ui.screens.WellConfigScreen

@SuppressLint("StateFlowValueCalledInComposition", "NewApi")
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

    NavHost(
        navController = navController,
        startDestination = Routes.HOME_SCREEN
    ) {
        composable(Routes.HOME_SCREEN) {
            HomeScreen(
                navController = navController,
                wellViewModel = userViewModel
            )
        }

        composable(Routes.MONITORING_SCREEN) {
            MonitorScreen(
                userViewModel = userViewModel,
                navController = navController
            )
        }

        composable(
            route = "${Routes.WELL_CONFIG_SCREEN}/{wellId}",
            arguments = listOf(navArgument("wellId") { type = NavType.IntType })
        ) { backStackEntry ->
            val wellId = backStackEntry.arguments?.getInt("wellId") ?: 0
            WellConfigScreen(
                wellId = wellId,
                userViewModel = userViewModel,
                navController = navController
            )
        }

        composable(Routes.CREDITS_SCREEN) {
            CreditsScreen(navController, userViewModel)
        }

        composable(Routes.SETTINGS_SCREEN) {
            SettingsScreen(
                navController = navController,
                userViewModel = userViewModel,
                isDarkTheme = isDarkTheme,
                useSystemTheme = useSystemTheme,
                onUpdateTheme = onUpdateTheme
            )
        }

        composable(Routes.LOGIN_SCREEN) {
            LoginScreen(
                navController = navController,
                userDataStore = userDataStore,
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(Routes.SIGNUP_SCREEN) {
            SignUpScreen(
                navController = navController
            )
        }

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
    }
}


