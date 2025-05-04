package com.wellconnect.wellmonitoring.ui.screens

import UserData
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wellconnect.wellmonitoring.ui.navigation.Routes
import com.wellconnect.wellmonitoring.viewmodels.UiState
import com.wellconnect.wellmonitoring.viewmodels.UserViewModel
import kotlinx.coroutines.launch
import java.util.Locale

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
        }
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
                    subtitle = role
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
                        else -> "System Default"
                    },
                    onClick = { showThemeDialog = true }
                )
            }
            
            // Location Section
            SettingsSection(title = "Location") {
                SettingsItem(
                    icon = Icons.Default.LocationOn,
                    title = "Current Location",
                    subtitle = "Lat: $latitude, Lon: $longitude"
                )
            }
            
            // Water Needs Section
            if (!isGuest && userData?.waterNeeds?.isNotEmpty() == true) {
                SettingsSection(title = "Water Needs") {
                    userData.waterNeeds.forEach { need ->
                        SettingsItem(
                            icon = Icons.Default.Opacity,
                            title = need.usageType.capitalize(Locale.getDefault()),
                            subtitle = "${need.amount}L - Priority: ${need.priority}"
                        )
                    }
                    // Edit Water Needs Button
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
                    title = "Support WellConnect",
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
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = themePreference == 0,
                                onClick = { 
                                    coroutineScope.launch {
                                        userViewModel.handleEvent(com.wellconnect.wellmonitoring.data.UserEvent.UpdateTheme(0))
                                    }
                                    showThemeDialog = false
                                }
                            )
                            Text("System Default")
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = themePreference == 1,
                                onClick = { 
                                    coroutineScope.launch {
                                        userViewModel.handleEvent(com.wellconnect.wellmonitoring.data.UserEvent.UpdateTheme(1))
                                    }
                                    showThemeDialog = false
                                }
                            )
                            Text("Light")
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = themePreference == 2,
                                onClick = { 
                                    coroutineScope.launch {
                                        userViewModel.handleEvent(com.wellconnect.wellmonitoring.data.UserEvent.UpdateTheme(2))
                                    }
                                    showThemeDialog = false
                                }
                            )
                            Text("Dark")
                        }
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
                                userViewModel.handleEvent(com.wellconnect.wellmonitoring.data.UserEvent.Logout)
                                navController.navigate(com.wellconnect.wellmonitoring.ui.navigation.Routes.LOGIN_SCREEN) {
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
                        OutlinedTextField(
                            value = passwordForDeletion,
                            onValueChange = { passwordForDeletion = it },
                            label = { Text("Password") },
                            singleLine = true,
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
                                // TODO: Call delete account API
                                userViewModel.deleteAccount(userData?.email ?: "", passwordForDeletion)
                                showDeleteAccountConfirmationDialog = false
                                passwordForDeletion = ""
                                // Navigate to login screen after account deletion
                                navController.navigate(Routes.LOGIN_SCREEN) {
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

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
        Divider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = tint
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (onClick != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

