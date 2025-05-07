package com.wellconnect.wellmonitoring.ui.screens

import UserData
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wellconnect.wellmonitoring.ui.components.EmptyWeatherState
import com.wellconnect.wellmonitoring.ui.components.WeatherContent
import com.wellconnect.wellmonitoring.viewmodels.UiState
import com.wellconnect.wellmonitoring.viewmodels.UserViewModel
import com.wellconnect.wellmonitoring.viewmodels.WeatherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun WeatherScreen(
    userViewModel: UserViewModel,
    weatherViewModel: WeatherViewModel
) {

    val snackbarHostState = remember { SnackbarHostState() }
    val colorScheme = MaterialTheme.colorScheme
    
    // Get user data for location
    val userState = userViewModel.state.value
    val userData = (userState as? UiState.Success<UserData>)?.data
    
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
            if (it.location.latitude != null && it.location.longitude != null) {
                weatherViewModel.setLocation(it.location.latitude, it.location.longitude)
            }
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
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
                    }
                }
                
                else -> {
                    // Get user location if available
                    userData?.let {
                        if (it.location.latitude != null && it.location.longitude != null) {
                            LaunchedEffect(Unit) {
                                weatherViewModel.setLocation(it.location.latitude, it.location.longitude)
                            }
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

