package com.bluebridge.bluebridgeapp.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold

import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bluebridge.bluebridgeapp.data.model.UserData
import com.bluebridge.bluebridgeapp.ui.components.EmptyWeatherState
import com.bluebridge.bluebridgeapp.ui.components.WeatherContent
import com.bluebridge.bluebridgeapp.ui.navigation.Routes.LOGIN_SCREEN
import com.bluebridge.bluebridgeapp.viewmodels.UiState
import com.bluebridge.bluebridgeapp.viewmodels.UserViewModel
import com.bluebridge.bluebridgeapp.viewmodels.WeatherViewModel


@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun WeatherScreen(
    userViewModel: UserViewModel,
    weatherViewModel: WeatherViewModel,
    modifier: Modifier = Modifier,
    navController : NavController
) {
    val userState by userViewModel.state // Observe the state directly for recomposition
    // Observe login state


    var showInvalidTokenDialog by remember { mutableStateOf(false) }

    val colorScheme = MaterialTheme.colorScheme
    LocalContext.current
    
    // Get user data for location
    val userData = (userState as? UiState.Success<UserData>)?.data // userState is already observed

    // Weather data state
    val weatherState = weatherViewModel.weatherState.value
    
    // Group weather data by date
    val groupedWeather = remember(weatherState) {
        if (weatherState is UiState.Success) {
            weatherState.data.groupBy { it.date }
        } else {
            emptyMap()
        }
    }
    
    // Initialize with user location if available
    LaunchedEffect(userData) {
        userData?.let {
            weatherViewModel.setLocation(it.location.latitude, it.location.longitude)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weather Forecast") },
                colors = topAppBarColors(
                    containerColor = colorScheme.primaryContainer,
                    titleContentColor = colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { weatherViewModel.refreshWeather() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (weatherState) {
                is UiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading weather data...")
                    }
                }
                
                is UiState.Success -> {

                    if (weatherState.data.isEmpty()) {
                        EmptyWeatherState(onRefresh = { weatherViewModel.refreshWeather() })
                    } else {
                        WeatherContent(
                            groupedWeather = groupedWeather,
                            userData = userData
                        )
                    }
                }
                
                is UiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Failed to load weather data",
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            weatherState.message,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { weatherViewModel.refreshWeather() }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Retry",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Retry")
                        }

                        if (weatherState.message.contains("Invalid token")) {
                            LaunchedEffect(Unit) {
                                showInvalidTokenDialog = true
                                userViewModel.logout()
                            }
                        }

                        if (showInvalidTokenDialog) {
                            AlertDialog(
                                onDismissRequest = {
                                    showInvalidTokenDialog = false
                                    navController.navigate(LOGIN_SCREEN) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            inclusive = true
                                        }
                                    }
                                },
                                title = { Text("Session Expired") },
                                text = { Text("You have been logged out. This might be because you logged in on another device.") },
                                confirmButton = {
                                    Button(onClick = {
                                        showInvalidTokenDialog = false
                                        navController.navigate(LOGIN_SCREEN) {
                                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                        }
                                    }) { Text("OK") }
                                }
                            )
                        }
                    }
                }
                
                else -> {
                    // Get user location if available
                    userData?.let {
                        LaunchedEffect(Unit) {
                            weatherViewModel.setLocation(it.location.latitude, it.location.longitude)
                        }
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "No weather data available",
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { weatherViewModel.refreshWeather() }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Load",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Load Weather")
                        }
                    }
                }
            }
        }
    }
}
