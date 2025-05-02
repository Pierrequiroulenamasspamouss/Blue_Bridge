package com.wellconnect.wellmonitoring.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wellconnect.wellmonitoring.R
import com.wellconnect.wellmonitoring.data.UserDataStoreImpl
import com.wellconnect.wellmonitoring.ui.components.RectangleButton
import com.wellconnect.wellmonitoring.ui.navigation.Routes
import com.wellconnect.wellmonitoring.viewmodels.WellViewModel
import kotlinx.serialization.InternalSerializationApi

@OptIn(ExperimentalMaterial3Api::class, InternalSerializationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    wellViewModel: WellViewModel
) {
    val context = LocalContext.current
    val userDataStore = remember { UserDataStoreImpl(context) }
    val userData by userDataStore.getUserData().collectAsState(initial = null)
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Check and request location permissions
    LaunchedEffect(Unit) {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val hasPermissions = permissions.all {
            context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }
        if (!hasPermissions) {
            showPermissionDialog = true
        }
    }

    // Permission request dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Location Permission") },
            text = { Text("This app needs location permission to find wells near you and provide navigation services.") },
            confirmButton = {
                Button(onClick = {
                    showPermissionDialog = false
                    (context as? Activity)?.requestPermissions(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ),
                        1001
                    )
                }) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Not Now")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            userData?.let { user ->
                Text(
                    text = "Welcome, ${user.username}!",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                Text(
                    text = "Role: ${user.role}",
                    style = MaterialTheme.typography.bodyLarge
                )
            } ?: run {
                Text(
                    text = "Welcome!",
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            RectangleButton(
                textValue = "Monitor Wells",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                backgroundColor = MaterialTheme.colorScheme.primary,
                function = { navController.navigate(Routes.MONITORING_SCREEN) },
                functionName = "Navigate to Monitor Wells Screen",
                textColor = MaterialTheme.colorScheme.onPrimary
            )

            RectangleButton(
                textValue = "Map",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                backgroundColor = MaterialTheme.colorScheme.tertiary,
                function = { navController.navigate(Routes.MAP_SCREEN) },
                functionName = "Navigate to Map Screen",
                textColor = MaterialTheme.colorScheme.onTertiary
            )

            RectangleButton(
                textValue = "Navigation",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                backgroundColor = MaterialTheme.colorScheme.secondary,
                function = {
                    navController.navigate("${Routes.COMPASS_SCREEN}?lat=90&lon=0&name=North")
                },
                functionName = "Navigate to Compass Screen",
                textColor = MaterialTheme.colorScheme.onSecondary
            )

            if (userData != null) {
                RectangleButton(
                    textValue = "Nearby Users",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    backgroundColor = MaterialTheme.colorScheme.tertiary,
                    function = { navController.navigate(Routes.NEARBY_USERS_SCREEN) },
                    functionName = "Navigate to Nearby Users Screen",
                    textColor = MaterialTheme.colorScheme.onTertiary
                )
            }
            // Settings button - visible for all users (logged in or not)
            RectangleButton(
                textValue = "Settings",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                backgroundColor = colorResource(id = R.color.light_gray),
                function = { navController.navigate(Routes.SETTINGS_SCREEN) },
                functionName = "Navigate to Settings Screen",
                textColor = MaterialTheme.colorScheme.onPrimary
            )

            if (userData == null) {
                RectangleButton(
                    textValue = "Login",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    function = { navController.navigate(Routes.LOGIN_SCREEN) },
                    functionName = "Navigate to Login Screen",
                    textColor = MaterialTheme.colorScheme.onPrimary
                )

                RectangleButton(
                    textValue = "Sign Up",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    function = { navController.navigate(Routes.REGISTER_SCREEN) },
                    functionName = "Navigate to Sign Up Screen",
                    textColor = MaterialTheme.colorScheme.onPrimary
                )
            }

        }
    }
}


