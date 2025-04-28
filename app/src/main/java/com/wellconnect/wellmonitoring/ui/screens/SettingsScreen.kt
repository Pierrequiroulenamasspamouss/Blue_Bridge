package com.wellconnect.wellmonitoring.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wellconnect.wellmonitoring.data.UserData
import com.wellconnect.wellmonitoring.data.UserDataStore
import com.wellconnect.wellmonitoring.ui.navigation.Routes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userData: UserData?,
    userDataStore: UserDataStore,
    navController: NavController
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // Log userData state for debugging
    val isGuest = userData == null
    Log.d("SettingsScreen", "UserData state: ${if (isGuest) "Guest/Null" else "Logged in as ${userData?.email}"}")
    
    // Default values for guest mode
    val displayName = if (isGuest) "Guest" else "${userData?.firstName} ${userData?.lastName}"
    val email = userData?.email ?: "Not logged in"
    val role = userData?.role?.capitalize() ?: "Guest"
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
                            title = need.usageType.capitalize(),
                            subtitle = "${need.amount}L - Priority: ${need.priority}"
                        )
                    }
                }
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
                                        userDataStore.updateThemePreference(0)
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
                                        userDataStore.updateThemePreference(1)
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
                                        userDataStore.updateThemePreference(2)
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
                                userDataStore.clearUserData()
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

