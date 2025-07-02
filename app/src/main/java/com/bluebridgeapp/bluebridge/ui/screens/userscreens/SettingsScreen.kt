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
import androidx.compose.material.icons.filled.Language
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.bluebridgeapp.bluebridge.R
import com.bluebridgeapp.bluebridge.data.model.BugReportRequest
import com.bluebridgeapp.bluebridge.data.model.UserData
import com.bluebridgeapp.bluebridge.events.AppEvent
import com.bluebridgeapp.bluebridge.events.AppEventChannel
import com.bluebridgeapp.bluebridge.events.UserEvent
import com.bluebridgeapp.bluebridge.ui.navigation.Routes
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
    val currentLanguagePreference by userViewModel.currentLanguage.collectAsState()

    //Get the role's value for easy permissions
    LaunchedEffect(Unit) {
        currentUserRole = userViewModel.repository.getRoleValue()
    }

    val guestRole = 1

    // Log userData state for debugging
    val isGuest = userData == null
    Log.d("SettingsScreen", "UserData state: ${if (isGuest) "Guest/Null" else "Logged in as ${userData!!.email}"}")

    // Default values for guest mode
    val displayName = if (isGuest) "Guest" else "${userData!!.firstName} ${userData.lastName}"
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
                title = { Text(stringResource(R.string.settings)) },
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
            SettingsSection(title = stringResource(R.string.profile)) {
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = stringResource(R.string.account),
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
                    title = stringResource(R.string.email),
                    subtitle = email
                )

                SettingsItem(
                    icon = Icons.Default.Badge,
                    title = stringResource(R.string.role),
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
                    title = stringResource(R.string.register_to_notifications),
                    subtitle = if (currentUserRole <= guestRole){ stringResource(R.string.register_with_email_to_receive_notifications)}else stringResource(R.string.send_firebase_token_to_server),
                    onClick = {
                        if (currentUserRole > guestRole) {
                            showNotificationPermissionDialog = true
                        } else {
                            coroutineScope.launch {
                                AppEventChannel.sendEvent(AppEvent.ShowInfo(context.getString(R.string.need_to_be_registered_to_receive_notifications)))
                            }
                        }
                    }
                )
            }

            // Appearance Section
            SettingsSection(title = stringResource(R.string.appearance)) {
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = stringResource(R.string.theme),
                    subtitle = when (currentThemePreference) {
                        0 -> stringResource(R.string.system_default)
                        1 -> stringResource(R.string.light)
                        2 -> stringResource(R.string.dark)
                        3 -> stringResource(R.string.green)
                        4 -> stringResource(R.string.pink)
                        5 -> stringResource(R.string.red)
                        6 -> stringResource(R.string.purple)
                        7 -> stringResource(R.string.yellow)
                        8 -> stringResource(R.string.tan)
                        9 -> stringResource(R.string.orange)
                        10 -> stringResource(R.string.cyan)
                        else -> stringResource(R.string.system_default)
                    },
                    onClick = { showThemeDialog = true }
                )
                SettingsItem(
                    title = stringResource(R.string.language),
                    icon = Icons.Default.Language,
                    subtitle = stringResource(R.string.the_language_of_the_app),
                    onClick = {
                        navController.navigate(Routes.LANGUAGE_SELECTION_SCREEN)
                    }
                    )
            }

            if (currentUserRole > guestRole) {
                // Location Section
                SettingsSection(title = stringResource(R.string.location_section)) {
                    SettingsItem(
                        icon = Icons.Default.LocationOn,
                        title = stringResource(R.string.current_location),
                        subtitle = stringResource(R.string.lat_lon, latitude.toString(), longitude.toString()),
                        onClick = { navController.navigate(Routes.PROFILE_SCREEN) }
                    )
                }

                // Water Needs Section
                SettingsSection(title = stringResource(R.string.water_needs)) {
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
                            title = stringResource(R.string.no_water_needs_configured),
                            subtitle = stringResource(R.string.tap_below_to_add_water_needs)
                        )
                    }

                    TextButton(
                        onClick = { navController.navigate(Routes.EDIT_WATER_NEEDS_SCREEN) },
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        Text(stringResource(R.string.edit_water_needs))
                    }
                }

            }

            // Support Section
            SettingsSection(title = stringResource(R.string.support)) {
                SettingsItem(
                    icon = Icons.Default.MonetizationOn,
                    title = stringResource(R.string.support_bluebridge),
                    subtitle = stringResource(R.string.help_us_by_viewing_ads),
                    onClick = { navController.navigate(Routes.ADMOB_SCREEN) }
                )

                SettingsItem(
                    icon = Icons.Default.BugReport,
                    title = stringResource(R.string.send_bug_report),
                    subtitle = stringResource(R.string.report_a_bug_or_suggest_improvement),
                    onClick = { showBugReportDialog = true }
                )

                SettingsItem(
                    icon = Icons.Default.Badge,
                    title = stringResource(R.string.credits),
                    subtitle = stringResource(R.string.view_app_credits),
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
            SettingsSection(title = stringResource(R.string.account_actions_section)) {
                if (isGuest) {
                    SettingsItem(
                        icon = Icons.Default.Login,
                        title = stringResource(R.string.login),
                        subtitle = stringResource(R.string.sign_in_to_your_account),
                        onClick = { navController.navigate(Routes.LOGIN_SCREEN) }
                    )
                } else {
                    SettingsItem(
                        icon = Icons.Default.Logout,
                        title = stringResource(R.string.logout),
                        subtitle = stringResource(R.string.sign_out_of_your_account),
                        onClick = { showLogoutDialog = true },
                        tint = MaterialTheme.colorScheme.error
                    )

                    SettingsItem(
                        icon = Icons.Default.Delete,
                        title = stringResource(R.string.delete_account),
                        subtitle = stringResource(R.string.permanently_delete_account),
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
