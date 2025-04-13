package com.jowell.wellmonitoring.ui.screens

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jowell.wellmonitoring.data.ThemePreference
import com.jowell.wellmonitoring.ui.WellViewModel



@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun NavigationGraph(
    userViewModel: WellViewModel,
    isDarkTheme: Boolean,
    useSystemTheme: Boolean,
    onUpdateThemePref: (ThemePreference) -> Unit
) {
    val navController = rememberNavController()
    val wellId = "0"
    NavHost(
        navController = navController,
        startDestination = Routes.HOME_SCREEN

    ) {
        composable(Routes.HOME_SCREEN) {
            HomeScreen(navController)
        }

        composable(Routes.MONITORING_SCREEN) {
            WellDataDisplay(userViewModel, navController)
        }

        composable("${Routes.WELL_CONFIG_SCREEN}/{wellId}") { backStackEntry ->
            val wellId = backStackEntry.arguments?.getString("wellId")?.toInt() ?: 0
            WellConfigScreen(userViewModel, navController, wellId)
        }



        composable(Routes.CREDITS_SCREEN) {
            CreditsScreen(navController)
        }

        composable(Routes.SETTINGS_SCREEN) {
            SettingsScreen(
                navController = navController,
                userViewModel = userViewModel,
                isDarkTheme = isDarkTheme,
                useSystemTheme = useSystemTheme,
                onUpdateThemePref = onUpdateThemePref
            )
        }
    }
}


