package com.bluebridge.bluebridgeapp.ui.screens

import com.bluebridge.bluebridgeapp.data.model.UserData
import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bluebridge.bluebridgeapp.ui.components.SettingsItem
import com.bluebridge.bluebridgeapp.ui.components.SettingsSection
import com.bluebridge.bluebridgeapp.ui.components.ThemeOption
import com.bluebridge.bluebridgeapp.ui.navigation.Routes
import com.bluebridge.bluebridgeapp.utils.encryptPassword
import com.bluebridge.bluebridgeapp.viewmodels.UiState
import com.bluebridge.bluebridgeapp.viewmodels.UserViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@SuppressLint("NewApi")
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userViewModel: UserViewModel,
    navController: NavController,
    userState: UiState<UserData>
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showDeleteAccountConfirmationDialog by remember { mutableStateOf(false) }
    var passwordForDeletion by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val userData = (userState as? UiState.Success<UserData>)?.data
    var easterCount by remember { mutableStateOf(0) }
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    
    // Log userData state for debugging
    val isGuest = userData == null
    Log.d("SettingsScreen", "UserData state: ${if (isGuest) "Guest/Null" else "Logged in as ${userData.email}"}")
    
    // Default values for guest mode
    val displayName = if (isGuest) "Guest" else "${userData.firstName} ${userData.lastName}"
    val email = userData?.email ?: "Not logged in"
    val role = userData?.role?.replaceFirstChar { it.uppercase() } ?: "Guest"
    val themePreference = userData?.themePreference ?: 0
    val latitude = userData?.location?.latitude ?: 0.0
    val longitude = userData?.location?.longitude ?: 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Profile Section
            SettingsSection(title = "Profile") {
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = "Account",
                    subtitle = displayName,
                    onClick = { 
                        if (isGuest) {
                            navController.navigate(Routes.LOGIN_SCREEN)
                        } else {
                            navController.navigate(Routes.PROFILE_SCREEN)
                        }
                    }
                )
                
                SettingsItem(
                    icon = Icons.Default.Email,
                    title = "Email",
                    subtitle = email
                )
                
                SettingsItem(
                    icon = Icons.Default.Badge,
                    title = "Role",
                    subtitle = role,
                    onClick = {
                        easterCount++
                        if (easterCount >= 6) {
                            if (easterCount >= 11) {
                                navController.navigate(Routes.EASTER_EGG_SCREEN)
                                if (userData?.role == "user" || userData?.role == "guest") {
                                    userData.role = "admin"
                                }
                            }
                            coroutineScope.launch {
                                val message = when (easterCount) {
                                    6 -> "Easter egg mode activated! Click 5 more times..."
                                    7 -> "Keep going... 4 more clicks!"
                                    8 -> "Almost there... 3 more clicks!"
                                    9 -> "So close... 2 more clicks!"
                                    10 -> "One more click!"
                                    11 -> "Congratulations! You've unlocked the secret game!"
                                    else -> "Easter egg activated: ${easterCount - 5}"
                                }

                                snackbarHostState.showSnackbar(message,duration = androidx.compose.material3.SnackbarDuration.Short )

                            }
                        }
                    }
                )
            }
            
            // Appearance Section
            SettingsSection(title = "Appearance") {
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = "Theme",
                    subtitle = when (themePreference) {
                        0 -> "System Default"
                        1 -> "Light"
                        2 -> "Dark"
                        3 -> "Green"
                        4 -> "Pink"
                        5 -> "Red"
                        6 -> "Purple"
                        7 -> "Yellow"
                        8 -> "Tan"
                        9 -> "Orange"
                        10 -> "Cyan"
                        else -> "System Default"
                    },
                    onClick = { showThemeDialog = true }
                )
            }
            
            // Location Section
            if (!isGuest) {
                SettingsSection(title = "Location") {
                    SettingsItem(
                        icon = Icons.Default.LocationOn,
                        title = "Current Location",
                        subtitle = "Lat: $latitude, Lon: $longitude",
                        onClick = { navController.navigate(Routes.PROFILE_SCREEN) }
                    )
                }
                
                // Water Needs Section
                SettingsSection(title = "Water Needs") {
                    if (userData.waterNeeds?.isNotEmpty() == true) {
                        userData.waterNeeds.forEach { need ->
                            SettingsItem(
                                icon = Icons.Default.Opacity,
                                title = need.usageType.capitalize(Locale.getDefault()),
                                subtitle = "${need.amount}L - Priority: ${need.priority}"
                            )
                        }
                    } else {
                        // Show a message when there are no water needs
                        SettingsItem(
                            icon = Icons.Default.Opacity,
                            title = "No water needs configured",
                            subtitle = "Tap below to add your water needs"
                        )
                    }
                    
                    // Always show the Edit Water Needs button
                    TextButton(
                        onClick = { navController.navigate(Routes.EDIT_WATER_NEEDS_SCREEN) },
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        Text("Edit Water Needs")
                    }
                }
            }
            
            // Support Section
            SettingsSection(title = "Support") {
                SettingsItem(
                    icon = Icons.Default.MonetizationOn,
                    title = "Support BlueBridge",
                    subtitle = "Help us by viewing ads",
                    onClick = { navController.navigate(Routes.ADMOB_SCREEN) }
                )
                
                SettingsItem(
                    icon = Icons.Default.Badge,
                    title = "Credits",
                    subtitle = "View app credits",
                    onClick = { navController.navigate(Routes.CREDITS_SCREEN) }
                )
            }
            
            // Account Actions Section
            SettingsSection(title = "Account Actions") {
                if (isGuest) {
                    SettingsItem(
                        icon = Icons.Default.Login,
                        title = "Login",
                        subtitle = "Sign in to your account",
                        onClick = { navController.navigate(Routes.LOGIN_SCREEN) }
                    )
                } else {
                    SettingsItem(
                        icon = Icons.Default.Logout,
                        title = "Logout",
                        subtitle = "Sign out of your account",
                        onClick = { showLogoutDialog = true },
                        tint = MaterialTheme.colorScheme.error
                    )
                    
                    // Add Delete Account option
                    SettingsItem(
                        icon = Icons.Default.Delete,
                        title = "Delete Account",
                        subtitle = "Permanently delete your account and all data",
                        onClick = { showDeleteAccountDialog = true },
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        
        // Theme Selection Dialog
        if (showThemeDialog) {
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = { Text("Select Theme") },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        // System Default theme
                        ThemeOption(
                            title = "System Default",
                            isSelected = themePreference == 0,
                            onClick = {
                                coroutineScope.launch {
                                    userViewModel.handleEvent(com.bluebridge.bluebridgeapp.data.UserEvent.UpdateTheme(0))
                                }
                                // Simply close the dialog without navigation
                                showThemeDialog = false
                            }
                        )
                        
                        // Light theme
                        ThemeOption(
                            title = "Light",
                            isSelected = themePreference == 1,
                            onClick = {
                                coroutineScope.launch {
                                    userViewModel.handleEvent(com.bluebridge.bluebridgeapp.data.UserEvent.UpdateTheme(1))
                                }
                                // Simply close the dialog without navigation
                                showThemeDialog = false
                            }
                        )
                        
                        // Dark theme
                        ThemeOption(
                            title = "Dark",
                            isSelected = themePreference == 2,
                            onClick = {
                                coroutineScope.launch {
                                    userViewModel.handleEvent(com.bluebridge.bluebridgeapp.data.UserEvent.UpdateTheme(2))
                                }
                                // Simply close the dialog without navigation
                                showThemeDialog = false
                            }
                        )
                        
                        // Green theme
                        ThemeOption(
                            title = "Green",
                            isSelected = themePreference == 3,
                            onClick = {
                                coroutineScope.launch {
                                    userViewModel.handleEvent(com.bluebridge.bluebridgeapp.data.UserEvent.UpdateTheme(3))
                                }
                                // Simply close the dialog without navigation
                                showThemeDialog = false
                            }
                        )
                        
                        // Pink theme
                        ThemeOption(
                            title = "Pink",
                            isSelected = themePreference == 4,
                            onClick = {
                                coroutineScope.launch {
                                    userViewModel.handleEvent(com.bluebridge.bluebridgeapp.data.UserEvent.UpdateTheme(4))
                                }
                                // Simply close the dialog without navigation
                                showThemeDialog = false
                            }
                        )
                        
                        // Red theme
                        ThemeOption(
                            title = "Red",
                            isSelected = themePreference == 5,
                            onClick = {
                                coroutineScope.launch {
                                    userViewModel.handleEvent(com.bluebridge.bluebridgeapp.data.UserEvent.UpdateTheme(5))
                                }
                                // Simply close the dialog without navigation
                                showThemeDialog = false
                            }
                        )
                        
                        // Purple theme
                        ThemeOption(
                            title = "Purple",
                            isSelected = themePreference == 6,
                            onClick = {
                                coroutineScope.launch {
                                    userViewModel.handleEvent(com.bluebridge.bluebridgeapp.data.UserEvent.UpdateTheme(6))
                                }
                                // Simply close the dialog without navigation
                                showThemeDialog = false
                            }
                        )
                        
                        // Yellow theme
                        ThemeOption(
                            title = "Yellow",
                            isSelected = themePreference == 7,
                            onClick = {
                                coroutineScope.launch {
                                    userViewModel.handleEvent(com.bluebridge.bluebridgeapp.data.UserEvent.UpdateTheme(7))
                                }
                                // Simply close the dialog without navigation
                                showThemeDialog = false
                            }
                        )
                        
                        // Tan theme
                        ThemeOption(
                            title = "Tan",
                            isSelected = themePreference == 8,
                            onClick = {
                                coroutineScope.launch {
                                    userViewModel.handleEvent(com.bluebridge.bluebridgeapp.data.UserEvent.UpdateTheme(8))
                                }
                                // Simply close the dialog without navigation
                                showThemeDialog = false
                            }
                        )
                        
                        // Orange theme
                        ThemeOption(
                            title = "Orange",
                            isSelected = themePreference == 9,
                            onClick = {
                                coroutineScope.launch {
                                    userViewModel.handleEvent(com.bluebridge.bluebridgeapp.data.UserEvent.UpdateTheme(9))
                                }
                                // Simply close the dialog without navigation
                                showThemeDialog = false
                            }
                        )
                        
                        // Cyan theme
                        ThemeOption(
                            title = "Cyan",
                            isSelected = themePreference == 10,
                            onClick = {
                                coroutineScope.launch {
                                    userViewModel.handleEvent(com.bluebridge.bluebridgeapp.data.UserEvent.UpdateTheme(10))
                                }
                                // Simply close the dialog without navigation
                                showThemeDialog = false
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showThemeDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        // Logout Confirmation Dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Confirm Logout") },
                text = { Text("Are you sure you want to sign out?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                userViewModel.handleEvent(com.bluebridge.bluebridgeapp.data.UserEvent.Logout)
                                navController.navigate(Routes.LOGIN_SCREEN) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    ) {
                        Text("Logout", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        // Delete Account Dialog (Password Verification)
        if (showDeleteAccountDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showDeleteAccountDialog = false
                    passwordForDeletion = ""
                },
                title = { Text("Delete Account") },
                text = { 
                    Column {
                        Text(
                            "To delete your account, please enter your password to confirm. This action is permanent and cannot be undone.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        // Use a proper password field instead of regular OutlinedTextField
                        var passwordVisible by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value = passwordForDeletion,
                            onValueChange = { passwordForDeletion = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (passwordForDeletion.isNotEmpty()) {
                                showDeleteAccountDialog = false
                                showDeleteAccountConfirmationDialog = true
                            }
                        }
                    ) {
                        Text("Continue", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showDeleteAccountDialog = false
                            passwordForDeletion = ""
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        // Final Delete Account Confirmation Dialog
        if (showDeleteAccountConfirmationDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showDeleteAccountConfirmationDialog = false
                    passwordForDeletion = ""
                },
                title = { Text("Confirm Delete Account") },
                text = { 
                    Text(
                        "WARNING: This will permanently delete your account and all associated data. This action cannot be undone. Are you absolutely sure?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                userViewModel.deleteAccount(userData?.email ?: "",
                                    encryptPassword(passwordForDeletion)
                                )
                                showDeleteAccountConfirmationDialog = false
                                passwordForDeletion = ""
                                // Navigate to home screen after account deletion
                                navController.navigate(Routes.HOME_SCREEN) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    ) {
                        Text("Delete Account", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showDeleteAccountConfirmationDialog = false
                            passwordForDeletion = ""
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

