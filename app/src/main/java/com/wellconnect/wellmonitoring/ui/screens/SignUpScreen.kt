package com.wellconnect.wellmonitoring.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.util.Patterns
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.wellconnect.wellmonitoring.data.UserDataStoreImpl
import com.wellconnect.wellmonitoring.network.LocationData
import com.wellconnect.wellmonitoring.network.RegisterRequest
import com.wellconnect.wellmonitoring.network.RetrofitBuilder
import com.wellconnect.wellmonitoring.network.WaterNeed
import com.wellconnect.wellmonitoring.ui.components.PasswordField
import com.wellconnect.wellmonitoring.ui.navigation.Routes
import com.wellconnect.wellmonitoring.utils.encryptPassword
import com.wellconnect.wellmonitoring.utils.getBaseApiUrl
import com.wellconnect.wellmonitoring.utils.getPasswordStrength
import kotlinx.coroutines.launch
import kotlinx.serialization.InternalSerializationApi
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, InternalSerializationApi::class)
@Composable
fun SignUpScreen(
    navController: NavController,
    initialLoginMode: Boolean = false
) {
    val currentTime = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
    var isLoginMode by remember { mutableStateOf(initialLoginMode) }
    val context = LocalContext.current
    val userDataStore = remember { UserDataStoreImpl(context) }
    val scope = rememberCoroutineScope()
    var currentPriority by remember { mutableStateOf(6) }  // Default priority for "Other"
    var customType by remember { mutableStateOf("") }  // For custom type input
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    val waterNeeds = remember { mutableStateOf(mutableListOf<WaterNeed>()) }
    var currentWaterNeedAmount by remember { mutableStateOf("") }
    var currentWaterNeedType by remember { mutableStateOf("") }
    var currentWaterNeedDescription by remember { mutableStateOf("") }
    var isWellOwner by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val baseUrl = getBaseApiUrl(context)
    val api = RetrofitBuilder.create(baseUrl)

    var locationInput by remember { mutableStateOf("") } // New state for location input

    val passwordStrength = getPasswordStrength(password)

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isLoginMode) "Login" else "Create Account",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Common fields: email & password
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            // Use PasswordField component for password
            PasswordField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                isVisible = passwordVisible,
                onVisibilityChange = { passwordVisible = !passwordVisible },
                passwordStrength = if (!isLoginMode) passwordStrength else null // Only show strength for signup
            )
            
            // Confirm password field for signup
            if (!isLoginMode) {
                PasswordField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Confirm Password",
                    isVisible = confirmPasswordVisible,
                    onVisibilityChange = { confirmPasswordVisible = !confirmPasswordVisible }
                    // No password strength indicator for confirm password
                )
                
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = locationInput,
                    onValueChange = { locationInput = it },
                    label = { Text("Location (format: Location:\nlat: 0.000000\nlon: 25.000000)") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Button(
                    onClick = {
                        scope.launch {
                            // Check if we have location permission
                            val hasLocationPermission = context.checkSelfPermission(
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            
                            if (hasLocationPermission) {
                                // Request current location
                                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                                fusedLocationClient.getCurrentLocation(
                                    Priority.PRIORITY_HIGH_ACCURACY,
                                    null
                                ).addOnSuccessListener { location ->
                                    if (location != null) {
                                        locationInput = "Location:\nlat: ${location.latitude}\nlon: ${location.longitude}"
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Unable to get current location")
                                        }
                                    }
                                }.addOnFailureListener {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Failed to get location: ${it.localizedMessage}")
                                    }
                                }
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Location permission needed")
                                }
                                // Request permission
                                (context as? Activity)?.requestPermissions(
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                    1002
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Use My Current Location")
                }

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number (Optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Water Needs")
                OutlinedTextField(
                    value = currentWaterNeedAmount,
                    onValueChange = {
                        if (it.isEmpty() || it.all(Char::isDigit)) currentWaterNeedAmount = it
                    },
                    label = { Text("Amount (liters)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                WaterUsageTypeSelector(
                    currentWaterNeedType = currentWaterNeedType,
                    onWaterNeedTypeChange = { currentWaterNeedType = it },
                    currentPriority = currentPriority,
                    onPriorityChange = { currentPriority = it },
                    customType = customType,
                    onCustomTypeChange = { customType = it }
                )
                OutlinedTextField(
                    value = currentWaterNeedDescription,
                    onValueChange = { currentWaterNeedDescription = it },
                    label = { Text("Description (Optional)") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        val amount = currentWaterNeedAmount.toIntOrNull()
                        if (amount != null && amount > 0) {
                            // Get priority based on type (or use the selected priority for "Other")
                            val priority = when (currentWaterNeedType) {
                                "Absolute emergency" -> 0
                                "Medical" -> 1
                                "Drinking" -> 2
                                "Farming" -> 3
                                "Industry" -> 4
                                else -> currentPriority // Use selected priority for "Other"
                            }

                            waterNeeds.value.add(
                                WaterNeed(
                                    amount = amount,
                                    usageType = if (currentWaterNeedType == "Other") customType else currentWaterNeedType,
                                    description = currentWaterNeedDescription,
                                    priority = priority
                                )
                            )
                            currentWaterNeedAmount = ""
                            currentWaterNeedType = ""
                            currentWaterNeedDescription = ""
                            customType = ""
                            currentPriority = 6 // Reset to default for next entry
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Water Need")
                }


                // Display added water needs
                waterNeeds.value.forEach { need ->
                    Text("Amount: ${need.amount} liters, Type: ${need.usageType}, Priority: P${need.priority}, Description: ${need.description ?: "N/A"}")
                }
            }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Are you a well owner?")
                    Switch(checked = isWellOwner, onCheckedChange = { isWellOwner = it })
            }



            Button(
                onClick = {
                    scope.launch {
                        try {
                            if (isLoginMode) {
                                // Handle login
                                if (email.isBlank() || password.isBlank()) {
                                    errorMessage = "Please enter email and password"
                                    snackbarHostState.showSnackbar(errorMessage!!)
                                    return@launch
                                }
                                
                                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                    errorMessage = "Please enter a valid email address"
                                    snackbarHostState.showSnackbar(errorMessage!!)
                                    return@launch
                                }
                                
                                val encrypted = encryptPassword(password)
                                val success = userDataStore.login(email.trim(), encrypted)
                                
                                if (success) {
                                    navController.navigate(Routes.HOME_SCREEN) {
                                        popUpTo(Routes.LOGIN_SCREEN) { inclusive = true }
                                    }
                                } else {
                                    errorMessage = "Login failed: Invalid credentials"
                                    snackbarHostState.showSnackbar(errorMessage!!)
                                }
                            } else {
                                // Handle registration
                                // Validate inputs
                                if (email.isBlank() || password.isBlank() || firstName.isBlank() || 
                                    lastName.isBlank() || username.isBlank()) {
                                    errorMessage = "Please fill in all required fields"
                                    snackbarHostState.showSnackbar(errorMessage!!)
                                    return@launch
                                }
                                
                                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                    errorMessage = "Please enter a valid email address"
                                    snackbarHostState.showSnackbar(errorMessage!!)
                                    return@launch
                                }
                                
                                if (password != confirmPassword) {
                                    errorMessage = "Passwords do not match"
                                    snackbarHostState.showSnackbar(errorMessage!!)
                                    return@launch
                                }
                                
                                // Parse location
                                var latitude = 0.0
                                var longitude = 0.0
                                
                                try {
                                    if (locationInput.contains("lat:") && locationInput.contains("lon:")) {
                                        val latStr = locationInput.substringAfter("lat:").substringBefore("\n").trim()
                                        val lonStr = locationInput.substringAfter("lon:").trim()
                                        latitude = latStr.toDoubleOrNull() ?: 0.0
                                        longitude = lonStr.toDoubleOrNull() ?: 0.0
                                    }
                                } catch (e: Exception) {
                                    Log.e("SignUpScreen", "Error parsing location", e)
                                }
                                
                                // Create request
                                val locationData = LocationData(
                                    latitude = latitude,
                                    longitude = longitude,
                                    lastUpdated = currentTime
                                )
                                
                                val encrypted = encryptPassword(password)
                                val request = RegisterRequest(
                                    email = email.trim(),
                                    password = encrypted,
                                    firstName = firstName.trim(),
                                    lastName = lastName.trim(),
                                    username = username.trim(),
                                    location = locationData,
                                    waterNeeds = waterNeeds.value,
                                    isWellOwner = isWellOwner,
                                    role = "user"
                                )
                                
                                // Send registration request
                                Log.d("SignUpScreen", "Sending registration request: $request")
                                val response = api.register(request)
                                
                                if (response.isSuccessful && response.body()?.status == "success") {
                                    // Registration successful, now login
                                    val loginSuccess = userDataStore.login(email.trim(), encrypted)
                                    
                                    if (loginSuccess) {
                                        snackbarHostState.showSnackbar("Account created successfully!")
                                        navController.navigate(Routes.HOME_SCREEN) {
                                            popUpTo(Routes.SIGNUP_SCREEN) { inclusive = true }
                                        }
                                    } else {
                                        snackbarHostState.showSnackbar("Account created! Please login.")
                                        isLoginMode = true
                                    }
                                } else {
                                    val msg = response.body()?.message ?: "Registration failed"
                                    errorMessage = msg
                                    snackbarHostState.showSnackbar(errorMessage!!)
                                    Log.e("SignUpScreen", "Registration failed: $msg")
                                }
                            }
                        } catch (e: Exception) {
                            errorMessage = "Authentication failed: ${e.message}"
                            snackbarHostState.showSnackbar(errorMessage!!)
                            Log.e("SignUpScreen", "Auth failed", e)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            ) {
                Text(if (isLoginMode) "Login" else "Sign Up")
            }

            TextButton(
                onClick = { isLoginMode = !isLoginMode },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Already have an account? Login")
            }
            Spacer(modifier = Modifier.height(20.dp))

        }



    }


    SnackbarHost(hostState = snackbarHostState)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterUsageTypeSelector(
    currentWaterNeedType: String,
    currentPriority: Int,
    onWaterNeedTypeChange: (String) -> Unit,
    onPriorityChange: (Int) -> Unit,
    customType: String,
    onCustomTypeChange: (String) -> Unit
) {
    val usageTypes = listOf("Absolute emergency", "Medical", "Drinking", "Farming", "Industry", "Other")
    var expanded by remember { mutableStateOf(false) }
    var showPrioritySelector by remember { mutableStateOf(false) }

    Column {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = currentWaterNeedType,
                onValueChange = {},
                label = { Text("Usage Type") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                usageTypes.forEach { usageType ->
                    DropdownMenuItem(
                        text = { Text(text = usageType) },
                        onClick = {
                            onWaterNeedTypeChange(usageType)
                            // Automatically set priority based on type
                            val priority = when (usageType) {
                                "Absolute emergency" -> 0
                                "Medical" -> 1
                                "Drinking" -> 2
                                "Farming" -> 3
                                "Industry" -> 4
                                else -> 6 // Default for "Other"
                            }
                            onPriorityChange(priority)
                            showPrioritySelector = usageType == "Other"
                            expanded = false
                        }
                    )
                }
            }
        }

        // If "Other" is selected, show custom type field and priority selector
        if (currentWaterNeedType == "Other") {
            OutlinedTextField(
                value = customType,
                onValueChange = { onCustomTypeChange(it) },
                label = { Text("Custom Usage Type") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (showPrioritySelector) {
                PrioritySelector(
                    currentPriority = currentPriority,
                    onPriorityChange = onPriorityChange
                )
            }
        }
    }
}

@Composable
fun PrioritySelector(
    currentPriority: Int,
    onPriorityChange: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = "Priority for Other Type",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(0, 1, 2, 3, 4, 5, 6).forEach { priority ->
                FilterChip(
                    selected = currentPriority == priority,
                    onClick = { onPriorityChange(priority) },
                    label = { Text("P$priority") },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }
    }
} 