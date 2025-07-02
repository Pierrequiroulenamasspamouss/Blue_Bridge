package com.bluebridgeapp.bluebridge.ui.screens.userscreens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bluebridgeapp.bluebridge.R
import com.bluebridgeapp.bluebridge.data.model.Location
import com.bluebridgeapp.bluebridge.data.model.UserData
import com.bluebridgeapp.bluebridge.events.AppEvent
import com.bluebridgeapp.bluebridge.events.AppEventChannel
import com.bluebridgeapp.bluebridge.events.UserEvent
import com.bluebridgeapp.bluebridge.ui.components.MiniMap
import com.bluebridgeapp.bluebridge.ui.components.TopBar
import com.bluebridgeapp.bluebridge.viewmodels.UiState
import com.bluebridgeapp.bluebridge.viewmodels.UserViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    userViewModel: UserViewModel,

) {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    val currentTime = sdf.format(Date())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
                    Text(stringResource(R.string.go_to_settings))
                }
            }
        }
        return
    }

    // Logged in user view
    Scaffold(
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
                label = { Text(stringResource(R.string.first_name)) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text(stringResource(R.string.last_name)) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(stringResource(R.string.username)) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.email)) },
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
                    label = { Text(stringResource(R.string.latitude)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = longitude,
                    onValueChange = { longitude = it },
                    label = { Text(stringResource(R.string.longitude)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            // TODO : Use a minimap composable
            currentLocation?.let {
                MiniMap(
                    currentLocation = it,
                    selectedLocation = selectedLocation,
                    onLocationSelected = { location ->
                        selectedLocation = location
                        latitude = location.latitude.toString()
                        longitude = location.longitude.toString()
                    },
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            }


            // Save button
            Button(
                onClick = {
                    scope.launch {
                        try {
                            val updatedUserData = userData!!.copy(
                                firstName = firstName,
                                lastName = lastName,
                                username = username,
                                location = Location(
                                    latitude = selectedLocation.latitude,
                                    longitude = selectedLocation.longitude
                                )
                            )

                            userViewModel.handleEvent(UserEvent.UpdateProfile(updatedUserData))

                            navController.popBackStack()
                        } catch (e: Exception) {
                            AppEventChannel.sendEvent(AppEvent.ShowError("Failed to update profile: ${e.message}"))

                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text(stringResource(R.string.save_changes))
            }
            
            // Easter egg - clickable card for role
            Card(
                modifier = Modifier.fillMaxWidth()
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
                        text = userData!!.role,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

