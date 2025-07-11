package com.bluebridgeapp.bluebridge.ui.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.ExploreOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bluebridgeapp.bluebridge.R
import com.bluebridgeapp.bluebridge.data.model.UserData
import com.bluebridgeapp.bluebridge.ui.components.FeatureCard
import com.bluebridgeapp.bluebridge.ui.components.OfflineBanner
import com.bluebridgeapp.bluebridge.ui.components.WelcomeHeader
import com.bluebridgeapp.bluebridge.ui.dialogs.LogoutConfirmationDialog
import com.bluebridgeapp.bluebridge.ui.navigation.Routes
import com.bluebridgeapp.bluebridge.utils.isNetworkAvailable
import com.bluebridgeapp.bluebridge.viewmodels.UiState
import com.bluebridgeapp.bluebridge.viewmodels.UserViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.InternalSerializationApi


@OptIn(ExperimentalMaterial3Api::class, InternalSerializationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    userViewModel: UserViewModel,
) {
    val context = LocalContext.current
    val userState by userViewModel.state
    var currentUserRole by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) } // Added for loading state

    val scrollState = rememberScrollState()

    // Network connectivity state
    var isOnline by remember { mutableStateOf(isNetworkAvailable(context)) }

    // Define user roles
    val guestRole = 1
    val userRole = 2
    val wellOwnerRole = 3
    //Get the role's value for easy permissions
    LaunchedEffect(Unit) {
        currentUserRole = userViewModel.getRoleValue()
        // Simulate data loading delay
        isLoading = false // Data loaded
    }
    // Check if user is logged in
    val isLoggedIn = when (userState) {
        is UiState.Success -> true
        else -> false
    }

    // Extract user data if logged in
    val userData = if (userState is UiState.Success) {
        (userState as UiState.Success<UserData>).data
    } else null

    // Show loading indicator while user data and theme are loading
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            // Text(text = "Welcome to BlueBridge") //a blank screen is better than nothing
        }
        return // Do not show the screen until loaded
    }

    if (userData == null && isLoggedIn) { // Still loading user data after login
        // Potentially show a more specific loading state or skeleton UI
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Offline Banner - show only when offline
            if (!isOnline) {
                OfflineBanner()
            }

            // Welcome header
            WelcomeHeader(userData = userData, isLoggedIn = isLoggedIn)

            // Feature cards section
            Text(
                text = stringResource(R.string.main_features),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Features grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeatureCard(
                    icon = Icons.Default.WaterDrop,
                    title = stringResource(R.string.monitor_wells),
                    description = stringResource(R.string.track_water_levels),
                    onClick = { navController.navigate(Routes.MONITORING_SCREEN) },
                    modifier = Modifier.weight(1f)
                )

                FeatureCard(
                    icon = Icons.Default.Map,
                    title = stringResource(R.string.map_view),
                    description = stringResource(R.string.see_nearby_water_resources),
                    onClick = { navController.navigate(Routes.MAP_SCREEN) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeatureCard(
                    icon = Icons.Outlined.ExploreOff,
                    title = stringResource(R.string.navigation),
                    description = stringResource(R.string.find_your_way_to_nearest_well),
                    onClick = { navController.navigate("${Routes.COMPASS_SCREEN}?lat=90&lon=0&name=North") },
                    modifier = Modifier.weight(1f)
                )

                if (currentUserRole >= userRole) { // Show weather for logged-in users (user, well_owner, admin)
                    FeatureCard(
                        icon = Icons.Default.Cloud,
                        title = stringResource(R.string.weather),
                        description = stringResource(R.string.check_upcoming_weather_forecast),
                        onClick = { navController.navigate(Routes.WEATHER_SCREEN) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (currentUserRole >= userRole) { // Show for logged-in users (user, well_owner, admin)
                    FeatureCard(
                        icon = Icons.Default.Visibility,
                        title = stringResource(R.string.nearby_users),
                        description = stringResource(R.string.find_community_members_near_you),
                        onClick = { navController.navigate(Routes.NEARBY_USERS_SCREEN) },
                        modifier = Modifier.weight(1f)
                    )

                    if (currentUserRole >= wellOwnerRole) { // Show urgent SMS only for well_owner and admin
                        FeatureCard(
                            icon = Icons.Default.Emergency,
                            title = stringResource(R.string.urgent_sms),
                            description = stringResource(R.string.send_urgent_messages_to_contacts),
                            onClick = { navController.navigate(Routes.URGENT_SMS_SCREEN) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // removed Support section since not needed
            /*
            Text(
                text = "Support BlueBridge",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 4.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = "Help us improve BlueBridge",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = "BlueBridge is a community-driven app to help everyone access clean water. Your support keeps us running.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Button(
                        onClick = { navController.navigate(Routes.ADMOB_SCREEN) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.support_with_ads))
                    }
                }
            }
            */

            // Account section
            if (currentUserRole >= guestRole) { // Show account section for logged-in users (user, well_owner, admin)
                // User profile summary
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = "${userData?.firstName} ${userData?.lastName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = userData?.email ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                        if (currentUserRole > 1 ) { // to not have the option to edit your profile as a guest
                            OutlinedButton(
                                onClick = { navController.navigate(Routes.PROFILE_SCREEN) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.edit_profile))
                            }
                        }
                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    showLogoutDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.logout))
                            }

                            if (showLogoutDialog) {
                                LogoutConfirmationDialog(
                                    onConfirm = {
                                        userViewModel.logout()
                                        showLogoutDialog = false
                                        navController.navigate(Routes.HOME_SCREEN)},
                                    onDismiss = {
                                        showLogoutDialog = false
                                    }
                                )
                            }
                        }
                        Button(
                            onClick = { navController.navigate(Routes.SETTINGS_SCREEN) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings", modifier = Modifier.padding(end = 8.dp))
                            Text(stringResource(R.string.settings))
                        }
                    }
                }
            } else {
                // Login/Register buttons
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.sign_in_to_access_more_features),
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = { navController.navigate(Routes.LOGIN_SCREEN) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.login))
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedButton(
                                onClick = { navController.navigate(Routes.REGISTER_SCREEN) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.sign_up))
                            }
                        }

                        // Guest login button
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    userViewModel.loginAsGuest()
                                    navController.navigate(Routes.HOME_SCREEN) {
                                        popUpTo(Routes.HOME_SCREEN) { inclusive = true }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            ),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        ) {
                            Text(stringResource(R.string.login_as_guest))
                        }
                        Button(
                            onClick = { navController.navigate(Routes.SETTINGS_SCREEN) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(Icons.Outlined.Language, contentDescription = "Change Language", modifier = Modifier.padding(end = 8.dp))
                            Text(stringResource(R.string.change_language))
                        }


                    }
                }
            }
        }
    }
}