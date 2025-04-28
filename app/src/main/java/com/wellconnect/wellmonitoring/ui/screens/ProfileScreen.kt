package com.wellconnect.wellmonitoring.ui.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.wellconnect.wellmonitoring.data.Location
import com.wellconnect.wellmonitoring.data.UserData
import com.wellconnect.wellmonitoring.data.UserDataStoreImpl
import com.wellconnect.wellmonitoring.ui.components.TopBar
import com.wellconnect.wellmonitoring.viewmodels.WellViewModel
import kotlinx.coroutines.launch

private const val TAG = "NewProfileScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    userViewModel: WellViewModel
) {
    val context = LocalContext.current
    val userDataStore = remember { UserDataStoreImpl(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State for user data
    var userData by remember { mutableStateOf<UserData?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Form fields
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }

    // Load user data
    LaunchedEffect(Unit) {
        userDataStore.getUserData().collect { data ->
            userData = data

            // Initialize form fields if data exists
            data?.let {
                firstName = it.firstName
                lastName = it.lastName
                username = it.username
                email = it.email
                latitude = it.location.latitude.toString()
                longitude = it.location.longitude.toString()
            }

            isLoading = false
        }
    }

    // Check if user is logged in
    val isLoggedIn = userData != null && userData?.email?.isNotBlank() == true

    // Handle back navigation
    BackHandler {
        navController.navigate("settings") {
            popUpTo(navController.graph.findStartDestination().id)
            launchSingleTop = true
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
                        navController.navigate("settings") {
                            popUpTo(navController.graph.findStartDestination().id)
                            launchSingleTop = true
                        }
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

            // Save button
            Button(
                onClick = {
                    scope.launch {
                        try {
                            val updatedUserData = userData?.copy(
                                firstName = firstName,
                                lastName = lastName,
                                username = username,
                                location = Location(
                                    latitude = latitude.toDoubleOrNull() ?: 0.0,
                                    longitude = longitude.toDoubleOrNull() ?: 0.0
                                )
                            )

                            if (updatedUserData != null) {
                                userDataStore.saveUserData(updatedUserData)
                                snackbarHostState.showSnackbar("Profile updated successfully")
                                navController.navigate("settings") {
                                    popUpTo(navController.graph.findStartDestination().id)
                                    launchSingleTop = true
                                }
                            } else {
                                snackbarHostState.showSnackbar("Error: User data is not available")
                            }
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
        }
    }
}