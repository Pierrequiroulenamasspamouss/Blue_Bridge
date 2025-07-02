package com.bluebridgeapp.bluebridge.ui.screens.userscreens

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.bluebridgeapp.bluebridge.R
import com.bluebridgeapp.bluebridge.data.model.Location
import com.bluebridgeapp.bluebridge.data.model.RegisterRequest
import com.bluebridgeapp.bluebridge.data.model.WaterNeed
import com.bluebridgeapp.bluebridge.events.AppEvent
import com.bluebridgeapp.bluebridge.events.AppEventChannel
import com.bluebridgeapp.bluebridge.ui.components.EmailField
import com.bluebridgeapp.bluebridge.ui.components.MiniMap
import com.bluebridgeapp.bluebridge.ui.components.NameField
import com.bluebridgeapp.bluebridge.ui.components.PasswordField
import com.bluebridgeapp.bluebridge.ui.components.PhoneField
import com.bluebridgeapp.bluebridge.ui.components.WaterNeedsSection
import com.bluebridgeapp.bluebridge.ui.dialogs.ScrollableEULADialog
import com.bluebridgeapp.bluebridge.ui.navigation.Routes
import com.bluebridgeapp.bluebridge.utils.encryptPassword
import com.bluebridgeapp.bluebridge.utils.getPasswordStrength
import com.bluebridgeapp.bluebridge.viewmodels.UiState
import com.bluebridgeapp.bluebridge.viewmodels.UserViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
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
    val coroutineScope = rememberCoroutineScope()

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
    val waterNeeds = remember { mutableStateListOf<WaterNeed>() }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var showEULADialog by remember { mutableStateOf(false) }
    var pendingRegistrationRequest by remember { mutableStateOf<RegisterRequest?>(null) }

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

                AppEventChannel.sendEvent(AppEvent.ShowError((userState as UiState.Error).message))
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


    val imeInsets = WindowInsets.ime.asPaddingValues()
    val navigationBarsInsets = WindowInsets.navigationBars.asPaddingValues()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = 16.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = with(LocalDensity.current) {
                        maxOf(
                            imeInsets.calculateBottomPadding(),
                            navigationBarsInsets.calculateBottomPadding()
                        ) + 16.dp
                    }
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Create Account", style = MaterialTheme.typography.headlineMedium)

            // Required fields
            SectionTitle(stringResource(R.string.required_information))
            EmailField(email) { email = it }
            //Password field (with strength)
            PasswordField(
                password, { password = it }, stringResource(R.string.password),
                isVisible = isPasswordVisible,
                onVisibilityChange = { isPasswordVisible = !isPasswordVisible },
                passwordStrength = getPasswordStrength(password)
            )
            //confirmation Password field (without strength)
            PasswordField(
                confirmPassword, { confirmPassword = it }, stringResource(R.string.confirm_password),
                isVisible = isConfirmPasswordVisible,
                onVisibilityChange = { isConfirmPasswordVisible = !isConfirmPasswordVisible }
            )

            NameField(firstName, { firstName = it }, stringResource(R.string.first_name))
            NameField(lastName, { lastName = it }, stringResource(R.string.last_name))

            // Optional fields
            SectionTitle(stringResource(R.string.optional_information))
            NameField(username, { username = it }, stringResource(R.string.username))
            PhoneField(phoneNumber) { phoneNumber = it }

            // Location
            Text(stringResource(R.string.your_location), style = MaterialTheme.typography.titleSmall)
            MiniMap(
                currentLocation = currentLocation,
                onLocationSelected = { selectedLocation = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            )

            // Water needs
            var showWaterNeeds = false
            var waterNeedAmount: String? = null
            var waterNeedType: String? = null
            var waterNeedDesc: String? = null
            var waterNeedPriority: Int? = null
            var customWaterType: String? = null
            WaterNeedsSection(
                showWaterNeeds = showWaterNeeds,
                onToggle = { showWaterNeeds = !showWaterNeeds },
                waterNeeds = waterNeeds,
                amount = waterNeedAmount.toString(),
                onAmountChange = { waterNeedAmount = it },
                type = waterNeedType.toString(),
                onTypeChange = { waterNeedType = it },
                desc = waterNeedDesc.toString(),
                onDescChange = { waterNeedDesc = it },
                priority = waterNeedPriority,
                onPriorityChange = { waterNeedPriority = it },
                customType = customWaterType.toString(),
                onCustomTypeChange = { customWaterType = it }
            )
            // Well owner toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.are_you_well_owner))
                Switch(
                    checked = isWellOwner,
                    onCheckedChange = { isWellOwner = it }
                )
            }

            // Register button
            Button(
                onClick = {
                    if (!validateForm(email, password, confirmPassword, firstName, lastName)) {
                        coroutineScope.launch {
                            AppEventChannel.sendEvent(AppEvent.ShowError("Please fill all required fields"))
                        }
                        return@Button
                    }

                    // Store the registration request
                    pendingRegistrationRequest = RegisterRequest(
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

                    // Show EULA dialog
                    showEULADialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(24.dp))
                else Text(stringResource(R.string.sign_up))
            }
            if (showEULADialog && pendingRegistrationRequest != null) {
                ScrollableEULADialog(
                    onDismiss = { showEULADialog = false },
                    onAccept = {
                        userViewModel.register(pendingRegistrationRequest!!)
                        showEULADialog = false
                    }
                )
            }

            TextButton(
                onClick = { navController.navigate(Routes.LOGIN_SCREEN) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.already_have_account))
            }
        }
    }
}

// Helper functions
@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
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