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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.bluebridgeapp.bluebridge.data.model.ValidateAuthTokenRequest
import com.bluebridgeapp.bluebridge.events.AppEvent
import com.bluebridgeapp.bluebridge.events.AppEventChannel
import com.bluebridgeapp.bluebridge.events.UserEvent
import com.bluebridgeapp.bluebridge.network.RetrofitBuilder
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
import com.bluebridgeapp.bluebridge.utils.isInternetAvailable
import com.bluebridgeapp.bluebridge.viewmodels.NearbyUsersViewModel
import com.bluebridgeapp.bluebridge.viewmodels.ServerState
import com.bluebridgeapp.bluebridge.viewmodels.ServerViewModel
import com.bluebridgeapp.bluebridge.viewmodels.UserViewModel
import com.bluebridgeapp.bluebridge.viewmodels.WellViewModel
import org.json.JSONObject
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BlueBridgeApp(viewModelFactory: ViewModelProvider.Factory) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val userViewModel: UserViewModel = viewModel(factory = viewModelFactory)
    val languagePreference by userViewModel.currentLanguage.collectAsState(initial = "system")

    val currentLocale = remember(languagePreference) {
        if (languagePreference == "system") Locale.getDefault() else Locale(languagePreference)
    }

    val updatedConfig = remember(configuration, currentLocale) {
        Configuration(configuration).apply { setLocale(currentLocale) }
    }

    val localizedContext = remember(updatedConfig) {
        context.createConfigurationContext(updatedConfig)
    }

    CompositionLocalProvider(LocalContext provides localizedContext) {
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
    val navController = rememberNavController()
    val serverViewModel: ServerViewModel = viewModel(factory = viewModelFactory)
    val wellViewModel: WellViewModel = viewModel(factory = viewModelFactory)
    val nearbyUsersViewModel: NearbyUsersViewModel = viewModel(factory = viewModelFactory)

    val hasInternet by rememberInternetState(context)
    val isServerReachable by serverViewModel.isServerReachable.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    rememberCoroutineScope()
    val api = RetrofitBuilder.getServerApi(context)

    val themePreference by userViewModel.currentTheme.collectAsState()
    val isSystemDark = isSystemInDarkTheme()

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

    // Check server reachability when internet becomes available
    LaunchedEffect(hasInternet) {
        if (hasInternet) {
            serverViewModel.checkServerReachability()
        } else {
            serverViewModel.setServerUnreachable()
        }
    }

    // Validate token only when server is reachable and user is logged in
    LaunchedEffect(isServerReachable) {
        if (isServerReachable) {
            validateUserToken(userViewModel, api, navController, context)
        }
    }

    // Load user data if logged in
    LaunchedEffect(Unit) {
        userViewModel.getUserId()?.takeIf { userViewModel.isLoggedIn() }?.let { userId ->
            userViewModel.handleEvent(UserEvent.LoadUser(userId))
        }
    }

    MaterialTheme(colorScheme = colorScheme) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            NavigationGraph(
                navController = navController,
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                nearbyUsersViewModel = nearbyUsersViewModel,
                wellViewModel = wellViewModel,
                userViewModel = userViewModel,
                weatherViewModel = viewModel(factory = viewModelFactory),
                smsViewModel = viewModel(factory = viewModelFactory),
                chatViewModel = viewModel(factory = viewModelFactory)
            )
            ServerStatusDialogs(serverViewModel)
        }
    }
}

private suspend fun validateUserToken(
    userViewModel: UserViewModel,
    api: com.bluebridgeapp.bluebridge.network.ServerApi,
    navController: androidx.navigation.NavHostController,
    context: Context
) {
    val token = userViewModel.getLoginToken()
    val currentRole = userViewModel.getRole()

    if (token != null && currentRole != "guest") {
        try {
            val request = ValidateAuthTokenRequest(
                token = token,
                userId = userViewModel.getUserId().toString()
            )
            val authResponse = api.validateAuthToken(request)

            when {
                authResponse.isSuccessful && authResponse.body()?.status == "success" -> {
                    Log.d("Auth", "Token validation successful")
                }
                authResponse.code() == 401 -> {
                    handleTokenExpired(userViewModel, navController, context)
                }
                else -> {
                    handleTokenValidationError(authResponse, userViewModel, navController, context)
                }
            }
        } catch (e: Exception) {
            Log.e("Auth", "Token validation failed", e)
        }
    }
}

private suspend fun handleTokenExpired(
    userViewModel: UserViewModel,
    navController: androidx.navigation.NavHostController,
    context: Context
) {
    val sessionExpiredText = context.getString(R.string.session_expired)
    val pleaseLoginAgainText = context.getString(R.string.please_login_again)

    Log.w("Auth", "Token expired")
    userViewModel.logout()
    AppEventChannel.sendEvent(AppEvent.ShowError("$sessionExpiredText $pleaseLoginAgainText"))
    navController.navigate(Routes.LOGIN_SCREEN) { popUpTo(0) { inclusive = true } }
}

private suspend fun handleTokenValidationError(
    response: retrofit2.Response<*>,
    userViewModel: UserViewModel,
    navController: androidx.navigation.NavHostController,
    context: Context
) {
    val errorMessage = try {
        JSONObject(response.errorBody()?.string() ?: "").getString("message")
    } catch (_: Exception) {
        response.message() ?: context.getString(R.string.authentication_error)
    }

    Log.e("Auth", "Token validation failed: $errorMessage")
    userViewModel.logout()
    AppEventChannel.sendEvent(AppEvent.ShowError(errorMessage))
    navController.navigate(Routes.LOGIN_SCREEN) { popUpTo(0) { inclusive = true } }
}

@Composable
private fun rememberInternetState(context: Context): State<Boolean> {
    val hasInternet = remember { mutableStateOf(context.isInternetAvailable()) }

    DisposableEffect(Unit) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                hasInternet.value = true
            }
            override fun onLost(network: Network) {
                hasInternet.value = false
            }
        }

        cm.registerNetworkCallback(
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build(),
            callback
        )

        onDispose { cm.unregisterNetworkCallback(callback) }
    }

    return hasInternet
}

@Composable
private fun ServerStatusDialogs(serverViewModel: ServerViewModel) {
    val serverState by serverViewModel.serverState.collectAsState()
    var showServerUnreachableDialog by remember { mutableStateOf(serverState is ServerState.Error) }
    val needsUpdate by serverViewModel.needsUpdate.collectAsState()
    val context = LocalContext.current
    val needUpdateString = stringResource(R.string.update_url).toUri()
    if (showServerUnreachableDialog) {
        AlertDialog(
            onDismissRequest = { showServerUnreachableDialog = false },
            title = { Text(stringResource(R.string.server_unreachable)) },
            text = { Text(stringResource(R.string.server_unavailable_message)) },
            confirmButton = {
                Button({ showServerUnreachableDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    if (needsUpdate) {
        AlertDialog(
            onDismissRequest = { serverViewModel.resetUpdateState() },
            title = { Text(stringResource(R.string.update_available)) },
            text = { Text(stringResource(R.string.update_available_message)) },
            confirmButton = {
                Button({
                    context.startActivity(Intent(Intent.ACTION_VIEW, needUpdateString))
                    serverViewModel.resetUpdateState()
                }) { Text(stringResource(R.string.update)) }
            },
            dismissButton = {
                TextButton({ serverViewModel.resetUpdateState() }) {
                    Text(stringResource(R.string.later))
                }
            }
        )
    }
}