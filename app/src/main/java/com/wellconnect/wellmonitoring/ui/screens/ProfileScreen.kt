package com.wellconnect.wellmonitoring.ui.screens

import UserData
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.wellconnect.wellmonitoring.data.UserEvent
import com.wellconnect.wellmonitoring.data.model.Location
import com.wellconnect.wellmonitoring.ui.components.ProfileMapView
import com.wellconnect.wellmonitoring.ui.components.TopBar
import com.wellconnect.wellmonitoring.viewmodels.UiState
import com.wellconnect.wellmonitoring.viewmodels.UserViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    userViewModel: UserViewModel,

) {
    val currentTime = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    // State for user data
    val userState = userViewModel.state.value
    val userData = (userState as? UiState.Success<UserData>)?.data
    var isLoading by remember { mutableStateOf(userState is UiState.Loading) }

    // Form fields
    var firstName by remember { mutableStateOf(userData?.firstName ?: "") }
    var lastName by remember { mutableStateOf(userData?.lastName ?: "") }
    var username by remember { mutableStateOf(userData?.username ?: "") }
    var email by remember { mutableStateOf(userData?.email ?: "") }
    var latitude by remember { mutableStateOf(userData?.location?.latitude?.toString() ?: "") }
    var selectedLocation by remember {
        mutableStateOf(
            userData?.location ?: Location(0.0, 0.0)
        )
    }
    var longitude by remember { mutableStateOf(userData?.location?.longitude?.toString() ?: "") }

    // Check if user is logged in
    val isLoggedIn = userData != null && userData.email.isNotBlank()

    // Add easter egg counter
    var easterEggCount by remember { mutableStateOf(0) }

    // Handle back navigation
    BackHandler {
        navController.popBackStack()
    }
    // Get current location if permission is granted
    LaunchedEffect(Unit) {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = Location(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        lastUpdated = currentTime
                    )
                }
            }
            Log.d("ProfileScreen", "Recieved the current location: $currentLocation")
        }
        else{
            Log.e("ProfileScreen", "Permission to access precise location not granted")
        }
    }
    // Loading state
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // Guest user view
    if (!isLoggedIn) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = { TopBar(topBarMessage = "Profile") }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "You need to be logged in to edit your profile",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            "Please log in from the settings screen to access your profile information.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        navController.navigate("settings")
                    }
                ) {
                    Text("Go to Settings")
                }
            }
        }
        return
    }

    // Logged in user view
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopBar(topBarMessage = "Edit Profile") }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile header
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Your Profile",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Update your personal details and location",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Personal info
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false // Email cannot be changed
            )

            // Location section
            Text(
                text = "Your Location",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = latitude,
                    onValueChange = { latitude = it },
                    label = { Text("Latitude") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = longitude,
                    onValueChange = { longitude = it },
                    label = { Text("Longitude") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            
            // New map component
            ProfileMapView(
                userLocation = currentLocation,
                selectedLocation = selectedLocation,
                onLocationSelected = { location ->
                    selectedLocation = location
                    latitude = location.latitude.toString()
                    longitude = location.longitude.toString()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            // Save button
            Button(
                onClick = {
                    scope.launch {
                        try {
                            val updatedUserData = userData.copy(
                                firstName = firstName,
                                lastName = lastName,
                                username = username,
                                location = Location(
                                    latitude = selectedLocation.latitude,
                                    longitude = selectedLocation.longitude
                                )
                            )

                            userViewModel.handleEvent(UserEvent.UpdateProfile(updatedUserData))
                            snackbarHostState.showSnackbar(
                                "Profile updated successfully",
                                duration = SnackbarDuration.Short
                            )
                            navController.popBackStack()
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Failed to update profile: ${e.message}")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text("Save Changes")
            }
            
            // Easter egg - clickable card for role
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    easterEggCount++
                    if (easterEggCount >= 6) {
                        scope.launch {
                            val message = when (easterEggCount) {
                                6 -> "Easter egg mode activated! Click 5 more times..."
                                7 -> "Keep going... 4 more clicks!"
                                8 -> "Almost there... 3 more clicks!"
                                9 -> "So close... 2 more clicks!"
                                10 -> "One more click!"
                                11 -> "Congratulations! You've unlocked the secret game!"
                                else -> "Easter egg activated: ${easterEggCount - 5}"
                            }
                            snackbarHostState.showSnackbar(message)
                            if (easterEggCount >= 11) {
                                navController.navigate("easter_egg")
                            }
                        }
                    }
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "User role:",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = userData?.role ?: "Unknown",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}