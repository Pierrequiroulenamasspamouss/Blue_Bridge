@file:Suppress("DEPRECATION")

package com.bluebridgeapp.bluebridge.ui.screens.userscreens

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.bluebridgeapp.bluebridge.data.model.BugReportRequest
import com.bluebridgeapp.bluebridge.data.model.UserData
import com.bluebridgeapp.bluebridge.events.AppEvent
import com.bluebridgeapp.bluebridge.events.AppEventChannel
import com.bluebridgeapp.bluebridge.events.UserEvent
import com.bluebridgeapp.bluebridge.navigation.Routes
import com.bluebridgeapp.bluebridge.network.RetrofitBuilder
import com.bluebridgeapp.bluebridge.ui.components.SettingsItem
import com.bluebridgeapp.bluebridge.ui.components.SettingsSection
import com.bluebridgeapp.bluebridge.ui.dialogs.BugReportDialog
import com.bluebridgeapp.bluebridge.ui.dialogs.DeleteAccountDialog
import com.bluebridgeapp.bluebridge.ui.dialogs.LogoutConfirmationDialog
import com.bluebridgeapp.bluebridge.ui.dialogs.NotificationPermissionDialog
import com.bluebridgeapp.bluebridge.ui.dialogs.ThemeSelectionDialog
import com.bluebridgeapp.bluebridge.utils.encryptPassword
import com.bluebridgeapp.bluebridge.viewmodels.UiState
import com.bluebridgeapp.bluebridge.viewmodels.UserViewModel
import kotlinx.coroutines.launch


@SuppressLint("NewApi")
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userViewModel: UserViewModel,
    navController: NavController,
    userState: UiState<UserData>,
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var passwordForDeletion by remember { mutableStateOf("") }
    var showNotificationPermissionDialog by remember { mutableStateOf(false) }
    var showBugReportDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val userData = (userState as? UiState.Success<UserData>)?.data
    var easterCount by remember { mutableIntStateOf(0) }
    var currentUserRole by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    // Get current theme preference directly from ViewModel's theme state
    val currentThemePreference by userViewModel.currentTheme.collectAsState()

    //Get the role's value for easy permissions
    LaunchedEffect(Unit) {
        currentUserRole = userViewModel.repository.getRoleValue()
    }

    val guestRole = 1

    // Log userData state for debugging
    val isGuest = userData == null
    Log.d("SettingsScreen", "UserData state: ${if (isGuest) "Guest/Null" else "Logged in as ${userData.email}"}")

    // Default values for guest mode
    val displayName = if (isGuest) "Guest" else "${userData.firstName} ${userData.lastName}"
    val email = userData?.email ?: "Not logged in"
    val role = userData?.role?.replaceFirstChar { it.uppercase() } ?: "Guest"
    val latitude = userData?.location?.latitude ?: 0.0
    val longitude = userData?.location?.longitude ?: 0.0

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(navBackStackEntry) {
        // Always reload user data when entering the Settings screen or returning from EditWaterNeedsScreen
        userViewModel.loadUser()
    }

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
                    subtitle = role,
                    onClick = {
                        easterCount++
                        if (easterCount >= 6) {
                            coroutineScope.launch {
                                val message = when (easterCount) {
                                    6 -> "Easter egg mode activated! Click 5 more times..."
                                    7 -> "Keep going... 4 more clicks!"
                                    8 -> "Almost there... 3 more clicks!"
                                    9 -> "So close... 2 more clicks!"
                                    10 -> "One more click!"
                                    11 -> {
                                        userData?.role = "admin" //put the use as an admin for this session
                                        // Navigate to easter egg screen on the 11th click
                                        navController.navigate(Routes.EASTER_EGG_SCREEN)
                                        "Congratulations! You've unlocked the secret game!"
                                    }
                                    else -> "Easter egg activated: ${easterCount - 5}"
                                }
                                AppEventChannel.sendEvent(AppEvent.ShowInfo(message))
                            }
                        }
                    }
                )
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Register to notifications",
                    subtitle = if (currentUserRole <= guestRole){ "Register with a proper E-mail address to receive notifications"}else "Send your firebase push notification loginToken to the server",
                    onClick = {
                        if (currentUserRole > guestRole) {
                            showNotificationPermissionDialog = true

                        }
                        else {
                            coroutineScope.launch {
                                AppEventChannel.sendEvent(AppEvent.ShowInfo("You need to be registered to receive notifications"))
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
                    subtitle = when (currentThemePreference) {
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

            if (currentUserRole > guestRole) {
                // Location Section
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
                    if (userData?.waterNeeds?.isNotEmpty() == true) {
                        userData.waterNeeds.forEach { need ->
                            SettingsItem(
                                icon = Icons.Default.Opacity,
                                title = need.usageType.replaceFirstChar { it.uppercase() },
                                subtitle = "${need.amount}L - Priority: ${need.priority}"
                            )
                        }
                    } else {
                        SettingsItem(
                            icon = Icons.Default.Opacity,
                            title = "No water needs configured",
                            subtitle = "Tap below to add your water needs"
                        )
                    }

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
                    icon = Icons.Default.BugReport,
                    title = "Send Bug Report",
                    subtitle = "Report a bug or suggest an improvement",
                    onClick = { showBugReportDialog = true }
                )

                SettingsItem(
                    icon = Icons.Default.Badge,
                    title = "Credits",
                    subtitle = "View app credits",
                    onClick = { navController.navigate(Routes.CREDITS_SCREEN) }
                )
                /*
                //EULA
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "End User License Agreement",
                    subtitle = "What you agree to by using this app",
                    onClick = { navController.navigate(Routes.EULA_SCREEN) }
                )
                 */

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
        ThemeSelectionDialog(
            showDialog = showThemeDialog,
            currentThemePreference = currentThemePreference,
            userViewModel = userViewModel,
            onDismiss = { showThemeDialog = false }
        )

        // Logout Confirmation Dialog
        if (showLogoutDialog) {
            LogoutConfirmationDialog(
                onDismiss = { showLogoutDialog = false },
                onConfirm = {
                    coroutineScope.launch {
                    userViewModel.handleEvent(UserEvent.Logout)
                    }
                    navController.navigate(Routes.LOGIN_SCREEN) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        // Delete Account Dialog
        DeleteAccountDialog(
            showDialog = showDeleteAccountDialog,
            onDismiss = {
                showDeleteAccountDialog = false
                passwordForDeletion = ""
            },
            onConfirmFinal = { password ->
                coroutineScope.launch {
                    val encrypted = encryptPassword(password)
                    userViewModel.deleteAccount(userData?.email ?: "", encrypted)
                    // TODO: Handle deletion result
                    showDeleteAccountDialog = false
                    navController.navigate(Routes.HOME_SCREEN)
                }
            }
        )

        if (showBugReportDialog) {
            BugReportDialog(
                showDialog = showBugReportDialog,
                onDismiss = { showBugReportDialog = false },
                onSubmit = { name, description, category, extra ->
                    coroutineScope.launch {
                        try {
                            val api = RetrofitBuilder.getServerApi(context)
                            val response = api.submitBugReport(
                                BugReportRequest(
                                    name = name,
                                    description = description,
                                    category = category,
                                    extra = extra
                                )
                            )
                            if (response.isSuccessful && response.body()?.status == "success") {
                                AppEventChannel.sendEvent(AppEvent.ShowError("Bug report sent! Thank you."))
                            } else {
                                AppEventChannel.sendEvent(AppEvent.ShowError("Failed to send bug report: ${response.body()?.message ?: response.message()}"))
                               
                            }
                        } catch (e: Exception) {
                            AppEventChannel.sendEvent(AppEvent.ShowError("Error: ${e.localizedMessage}"))
                        }
                        showBugReportDialog = false
                    }
                }
            )
        }
    }

    if (showNotificationPermissionDialog) {
        NotificationPermissionDialog(
            onDismiss = { showNotificationPermissionDialog = false },
            onAllow = {
                userViewModel.registerForNotifications()
                showNotificationPermissionDialog = false
            }
        )
    }

}
