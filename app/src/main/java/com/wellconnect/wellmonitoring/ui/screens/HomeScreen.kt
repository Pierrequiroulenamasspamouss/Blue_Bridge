package com.wellconnect.wellmonitoring.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.wellconnect.wellmonitoring.R
import com.wellconnect.wellmonitoring.data.UserDataStoreImpl
import com.wellconnect.wellmonitoring.ui.components.RectangleButton
import com.wellconnect.wellmonitoring.ui.navigation.Routes
import com.wellconnect.wellmonitoring.viewmodel.WellViewModel
import kotlinx.serialization.InternalSerializationApi

@OptIn(ExperimentalMaterial3Api::class, InternalSerializationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    wellViewModel: WellViewModel
) {
    val context = LocalContext.current
    val userDataStore = remember { UserDataStoreImpl(context) }
    val userData by userDataStore.getUserData().collectAsState(initial = null)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            userData?.let { user ->
                Text(
                    text = "Welcome, ${user.username}!",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                Text(
                    text = "Role: ${user.role}",
                    style = MaterialTheme.typography.bodyLarge
                )
            } ?: run {
                Text(
                    text = "Welcome!",
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            RectangleButton(
                textValue = "Monitor Wells",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                backgroundColor = MaterialTheme.colorScheme.primary,
                function = { navController.navigate(Routes.MONITORING_SCREEN) },
                functionName = "Navigate to Monitor Wells Screen",
                textColor = MaterialTheme.colorScheme.onPrimary
            )

            RectangleButton(
                textValue = "Compass\nFind the nearest well",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                backgroundColor = MaterialTheme.colorScheme.secondary,
                function = {
                    navController.navigate("${Routes.COMPASS_SCREEN}?lat=90&lon=0&name=North")
                },
                functionName = "Navigate to Compass Screen",
                textColor = MaterialTheme.colorScheme.onSecondary
            )

            if (userData == null) {
                RectangleButton(
                    textValue = "Login",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    function = { navController.navigate(Routes.LOGIN_SCREEN) },
                    functionName = "Navigate to Login Screen",
                    textColor = MaterialTheme.colorScheme.onPrimary
                )

                RectangleButton(
                    textValue = "Sign Up",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    function = { navController.navigate(Routes.SIGNUP_SCREEN) },
                    functionName = "Navigate to Sign Up Screen",
                    textColor = MaterialTheme.colorScheme.onPrimary
                )
            }

            RectangleButton(
                textValue = "Settings",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                backgroundColor = colorResource(id = R.color.light_gray),
                function = { navController.navigate(Routes.SETTINGS_SCREEN) },
                functionName = "Navigate to Settings Screen",
                textColor = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}


