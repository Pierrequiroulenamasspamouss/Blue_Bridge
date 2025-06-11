package com.bluebridge.bluebridgeapp.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.util.Patterns
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.bluebridge.bluebridgeapp.data.model.Location
import com.bluebridge.bluebridgeapp.data.model.RegisterRequest
import com.bluebridge.bluebridgeapp.data.model.WaterNeed
import com.bluebridge.bluebridgeapp.ui.components.MiniMap
import com.bluebridge.bluebridgeapp.ui.components.PasswordField
import com.bluebridge.bluebridgeapp.ui.components.WaterNeedsSection
import com.bluebridge.bluebridgeapp.ui.navigation.Routes
import com.bluebridge.bluebridgeapp.utils.encryptPassword
import com.bluebridge.bluebridgeapp.utils.getPasswordStrength
import com.bluebridge.bluebridgeapp.viewmodels.UiState
import com.bluebridge.bluebridgeapp.viewmodels.UserViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.text.SimpleDateFormat
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.Locale

@SuppressLint("MissingPermission")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RegisterScreen(
    navController: NavController,
    userViewModel: UserViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Form state
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var isWellOwner by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var selectedLocation by remember { mutableStateOf<Location?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Water needs state
    var showWaterNeeds by remember { mutableStateOf(false) }
    var waterNeedAmount by remember { mutableStateOf("") }
    var waterNeedType by remember { mutableStateOf("") }
    var waterNeedDesc by remember { mutableStateOf("") }
    var waterNeedPriority by remember { mutableIntStateOf(6) }
    var customWaterType by remember { mutableStateOf("") }
    val waterNeeds = remember { mutableStateListOf<WaterNeed>() }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    // Handle registration state
    val userState by userViewModel.state
    LaunchedEffect(userState) {
        when (userState) {
            is UiState.Success -> {
                navController.navigate(Routes.HOME_SCREEN) {
                    popUpTo(Routes.LOGIN_SCREEN) { inclusive = true }
                }
            }
            is UiState.Error -> {
                isLoading = false
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = (userState as UiState.Error).message,
                        duration = SnackbarDuration.Long
                    )
                }
            }
            is UiState.Loading -> isLoading = true
            else -> isLoading = false
        }
    }

    // Get current location
    LaunchedEffect(Unit) {
        if (context.hasLocationPermission()) {
            currentLocation = context.getCurrentLocation()?.let {
                val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                Location(it.latitude, it.longitude, formatter.format(Date()))
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Create Account", style = MaterialTheme.typography.headlineMedium)

            // Required fields
            SectionTitle("Required Information")
            EmailField(email) { email = it }
            //Password field (with strength)
            PasswordField(
                password, { password = it }, "Password",
                isVisible = isPasswordVisible,
                onVisibilityChange = { isPasswordVisible = !isPasswordVisible },
                passwordStrength = getPasswordStrength(password)
            )
            //confirmation Password field (without strength)
            PasswordField(
                confirmPassword, { confirmPassword = it }, "Confirm Password",
                isVisible = isConfirmPasswordVisible,
                onVisibilityChange = { isPasswordVisible = !isPasswordVisible }
            )

            NameField(firstName, { firstName = it }, "First Name")
            NameField(lastName, { lastName = it }, "Last Name")

            // Optional fields
            SectionTitle("Optional Information")
            NameField(username, { username = it }, "Username")
            PhoneField(phoneNumber) { phoneNumber = it }

            // Location
            Text("Your Location", style = MaterialTheme.typography.titleSmall)
            MiniMap(
                currentLocation = currentLocation,
                selectedLocation = selectedLocation,
                onLocationSelected = { selectedLocation = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            )

            // Water needs
            WaterNeedsSection(
                showWaterNeeds = showWaterNeeds,
                onToggle = { showWaterNeeds = !showWaterNeeds },
                waterNeeds = waterNeeds,
                amount = waterNeedAmount,
                onAmountChange = { waterNeedAmount = it },
                type = waterNeedType,
                onTypeChange = { waterNeedType = it },
                desc = waterNeedDesc,
                onDescChange = { waterNeedDesc = it },
                priority = waterNeedPriority,
                onPriorityChange = { waterNeedPriority = it },
                customType = customWaterType,
                onCustomTypeChange = { customWaterType = it }
            )

            // Well owner toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Are you a well owner?")
                Switch(
                    checked = isWellOwner,
                    onCheckedChange = { isWellOwner = it }
                )
            }

            // Register button
            Button(
                onClick = {
                    if (!validateForm(email, password, confirmPassword, firstName, lastName)) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please fill all required fields")
                        }
                        return@Button
                    }

                    val request = RegisterRequest(
                        email = email.trim(),
                        password = encryptPassword(password),
                        firstName = firstName.trim(),
                        lastName = lastName.trim(),
                        username = username.ifBlank { firstName.lowercase() },
                        location = selectedLocation ?: currentLocation ?: Location(0.0, 0.0),
                        waterNeeds = waterNeeds,
                        role = if (isWellOwner) "well_owner" else "user",
                        phoneNumber = phoneNumber.trim().takeIf { it.isNotBlank() }
                    )

                    userViewModel.register(request)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(24.dp))
                else Text("Sign Up")
            }

            TextButton(
                onClick = { navController.navigate(Routes.LOGIN_SCREEN) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Already have an account? Login")
            }
        }
    }
}

// Helper functions
@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun NameField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
    )
}

@Composable
private fun EmailField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Email *") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        )
    )
}

@Composable
private fun PhoneField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.all { c -> c.isDigit() || c == '+' }) onValueChange(it) },
        label = { Text("Phone") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Next
        )
    )
}

private fun validateForm(
    email: String,
    password: String,
    confirmPassword: String,
    firstName: String,
    lastName: String
): Boolean {
    return email.isNotBlank() &&
            Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
            password.isNotBlank() &&
            password == confirmPassword &&
            firstName.isNotBlank() &&
            lastName.isNotBlank()
}

// Extension functions for location
fun Context.hasLocationPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
suspend fun Context.getCurrentLocation(): android.location.Location? {
    return try {
        LocationServices.getFusedLocationProviderClient(this)
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .await()
    } catch (e: Exception) {
        Log.e("Location", "Error fetching location", e)
        null
    }
}