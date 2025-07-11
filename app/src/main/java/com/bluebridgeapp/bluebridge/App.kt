package com.bluebridgeapp.bluebridge

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.bluebridgeapp.bluebridge.data.model.BugReportRequest
import com.bluebridgeapp.bluebridge.data.model.ValidateAuthTokenRequest
import com.bluebridgeapp.bluebridge.events.AppEvent
import com.bluebridgeapp.bluebridge.events.AppEventChannel
import com.bluebridgeapp.bluebridge.events.UserEvent
import com.bluebridgeapp.bluebridge.network.RetrofitBuilder
import com.bluebridgeapp.bluebridge.ui.dialogs.BugReportDialog
import com.bluebridgeapp.bluebridge.ui.navigation.NavigationGraph
import com.bluebridgeapp.bluebridge.ui.navigation.Routes
import com.bluebridgeapp.bluebridge.ui.theme.getCyanColorScheme
import com.bluebridgeapp.bluebridge.ui.theme.getGreenColorScheme
import com.bluebridgeapp.bluebridge.ui.theme.getOrangeColorScheme
import com.bluebridgeapp.bluebridge.ui.theme.getPinkColorScheme
import com.bluebridgeapp.bluebridge.ui.theme.getPurpleColorScheme
import com.bluebridgeapp.bluebridge.ui.theme.getRedColorScheme
import com.bluebridgeapp.bluebridge.ui.theme.getTanColorScheme
import com.bluebridgeapp.bluebridge.ui.theme.getYellowColorScheme
import com.bluebridgeapp.bluebridge.utils.isNetworkAvailable
import com.bluebridgeapp.bluebridge.viewmodels.NearbyUsersViewModel
import com.bluebridgeapp.bluebridge.viewmodels.ServerState
import com.bluebridgeapp.bluebridge.viewmodels.ServerViewModel
import com.bluebridgeapp.bluebridge.viewmodels.UserViewModel
import com.bluebridgeapp.bluebridge.viewmodels.WellViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BlueBridgeApp(viewModelFactory: ViewModelProvider.Factory) {
    // Get the context and configuration
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    // ViewModels
    val userViewModel: UserViewModel = viewModel(factory = viewModelFactory)

    // Language preference state
    val languagePreference by userViewModel.currentLanguage.collectAsState(initial = "system")

    // Apply language preference
    val currentLocale = remember(languagePreference) {
        if (languagePreference == "system") {
            Locale.getDefault()
        } else {
            Locale(languagePreference)
        }
    }

    // Create a new configuration with the selected locale
    val updatedConfig = remember(configuration, currentLocale) {
        Configuration(configuration).apply {
            setLocale(currentLocale)
        }
    }

    // Create a new context with the updated configuration
    val localizedContext = remember(updatedConfig) {
        context.createConfigurationContext(updatedConfig)
    }

    // Wrap the app in a CompositionLocalProvider to provide the localized context
    CompositionLocalProvider(
        LocalContext provides localizedContext
    ) {
        AppContent(viewModelFactory, localizedContext, userViewModel)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun AppContent(
    viewModelFactory: ViewModelProvider.Factory,
    context: Context,
    userViewModel: UserViewModel
) {
    // Rest of your app implementation...
    val navController = rememberNavController()
    val isOnline by rememberNetworkState(context)
    val role = remember { mutableStateOf("") }
    val showBugReportDialog = remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val api = RetrofitBuilder.getServerApi(context)
    // String resources, remembered to avoid recomposition issues
    val sessionExpiredText = stringResource(R.string.session_expired)
    val pleaseLoginAgainText = stringResource(R.string.please_login_again)
    val authenticationErrorText = stringResource(R.string.authentication_error)
    val networkErrorText = stringResource(R.string.network_error)
    // ViewModels
    val wellViewModel: WellViewModel = viewModel(factory = viewModelFactory)
    val nearbyUsersViewModel: NearbyUsersViewModel = viewModel(factory = viewModelFactory)
    val serverViewModel: ServerViewModel = viewModel(factory = viewModelFactory)

    // Theme handling (same as before)
    val isSystemDark = isSystemInDarkTheme()
    val themePreference by userViewModel.currentTheme.collectAsState()

    val colorScheme = when (themePreference) {
        0 -> if (isSystemDark) darkColorScheme() else lightColorScheme()
        1 -> lightColorScheme()
        2 -> darkColorScheme()
        3 -> getGreenColorScheme(isSystemDark)
        4 -> getPinkColorScheme(isSystemDark)
        5 -> getRedColorScheme(isSystemDark)
        6 -> getPurpleColorScheme(isSystemDark)
        7 -> getYellowColorScheme(isSystemDark)
        8 -> getTanColorScheme(isSystemDark)
        9 -> getOrangeColorScheme(isSystemDark)
        10 -> getCyanColorScheme(isSystemDark)
        else -> if (isSystemDark) darkColorScheme() else lightColorScheme()
    }


    // Authentication and initial setup
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                if (isOnline) {
                    val token = userViewModel.getLoginToken()
                    val currentRole = userViewModel.getRole()
                    role.value = currentRole ?: ""

                    if (token != null && currentRole != "guest") {
                        val request = ValidateAuthTokenRequest(
                            token = token,
                            userId = userViewModel.getUserId().toString()
                        )
                        val authResponse = api.validateAuthToken(request)

                        when {
                            authResponse.isSuccessful && authResponse.body()?.status == "success" -> {
                                // Token valid, proceed
                            }
                            authResponse.code() == 401 -> {
                                val errorMessage = authResponse.body()?.message ?: sessionExpiredText
                                AppEventChannel.sendEvent(AppEvent.ShowError("$errorMessage $pleaseLoginAgainText"))
                                userViewModel.logout()
                            }
                            else -> {
                                val errorMessage = try {
                                    JSONObject(authResponse.errorBody()?.string() ?: "").getString("message")
                                } catch (e: Exception) {
                                    authResponse.message() ?: "Unknown error"
                                }
                                AppEventChannel.sendEvent(AppEvent.ShowError("$authenticationErrorText: $errorMessage"))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("Auth", "Token validation failed", e)
                AppEventChannel.sendEvent(AppEvent.ShowError("$networkErrorText: ${e.message}"))
                userViewModel.logout()
                navController.navigate(Routes.LOGIN_SCREEN) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    // Load user data if logged in
    LaunchedEffect(Unit) {
        userViewModel.getUserId()
            .takeIf { userViewModel.isLoggedIn() }
            ?.let { userViewModel.handleEvent(UserEvent.LoadUser(it)) }
    }

    // Check server status when online
    LaunchedEffect(isOnline) {
        if (isOnline) serverViewModel.getServerStatus()
    }

    // UI Composition
    MaterialTheme(colorScheme = colorScheme) {
        Scaffold(

            floatingActionButton = {
                if (role.value == "admin") {
                    FloatingActionButton(onClick = { showBugReportDialog.value = true }) {
                        val reportBugText = stringResource(R.string.report_bug)
                        Icon(Icons.Default.BugReport, contentDescription = reportBugText)
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            NavigationGraph(
                navController = navController,
                modifier = Modifier.fillMaxSize(),
                nearbyUsersViewModel = nearbyUsersViewModel,
                wellViewModel = wellViewModel,
                userViewModel = userViewModel,
                weatherViewModel = viewModel(factory = viewModelFactory),
                smsViewModel = viewModel(factory = viewModelFactory),
                paddingValues = paddingValues
            )

            ServerStatusDialogs(serverViewModel)

            if (showBugReportDialog.value) {
                val bugReportThankYouText = stringResource(R.string.bug_report_thank_you)
                BugReportDialog(
                    showDialog = showBugReportDialog.value,
                    onDismiss = { showBugReportDialog.value = false },
                    onSubmit = { name, description, category, extra ->
                        coroutineScope.launch {
                            val bugReport = BugReportRequest(name, description, category, extra)
                            api.submitBugReport(bugReport) // Consider handling response
                            snackbarHostState.showSnackbar(bugReportThankYouText)
                        }
                        showBugReportDialog.value = false
                    }
                )
            }
        }
    }
}

@Composable
private fun rememberNetworkState(context: Context): State<Boolean> {
    val isOnline = remember { mutableStateOf(isNetworkAvailable(context)) }

    DisposableEffect(Unit) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { isOnline.value = true }
            override fun onLost(network: Network) { isOnline.value = isNetworkAvailable(context) }
        }

        cm.registerNetworkCallback(
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build(),
            callback
        )

        onDispose { cm.unregisterNetworkCallback(callback) }
    }

    return isOnline
}

@Composable
private fun ServerStatusDialogs(
    serverViewModel: ServerViewModel
) {
    val serverState by serverViewModel.serverState.collectAsState()
    var showServerUnreachableDialog by remember { mutableStateOf(serverState is ServerState.Error) }
    val needsUpdate by serverViewModel.needsUpdate.collectAsState()
    val context = LocalContext.current

    // Remember string resources
    val serverUnreachableTitle = stringResource(R.string.server_unreachable)
    val serverUnavailableMessage = stringResource(R.string.server_unavailable_message)
    val okText = stringResource(R.string.ok)
    val updateAvailableTitle = stringResource(R.string.update_available)
    val updateAvailableMessage = stringResource(R.string.update_available_message)
    val updateText = stringResource(R.string.update)
    val laterText = stringResource(R.string.later)
    val updateUrl = stringResource(R.string.update_url)

    LaunchedEffect(serverState) {
        showServerUnreachableDialog = serverState is ServerState.Error
    }

    if (showServerUnreachableDialog) {
        AlertDialog(
            onDismissRequest = { showServerUnreachableDialog = false },
            title = { Text(serverUnreachableTitle) },
            text = { Text(serverUnavailableMessage) },
            confirmButton = {
                Button({ showServerUnreachableDialog = false }) {
                    Text(okText)
                }
            }
        )
    }

    if (needsUpdate) {
        AlertDialog(
            onDismissRequest = { serverViewModel.resetUpdateState() },
            title = { Text(updateAvailableTitle) },
            text = { Text(updateAvailableMessage) },
            confirmButton = {
                Button({
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        updateUrl.toUri()
                    )
                    context.startActivity(intent)
                    serverViewModel.resetUpdateState()
                }) { Text(updateText) }
            },
            dismissButton = {
                TextButton({ serverViewModel.resetUpdateState() }) {
                    Text(laterText)
                }
            }
        )
    }
}