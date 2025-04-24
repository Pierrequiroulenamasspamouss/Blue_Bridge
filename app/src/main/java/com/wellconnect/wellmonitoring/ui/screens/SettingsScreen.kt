package com.wellconnect.wellmonitoring.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.wellconnect.wellmonitoring.data.UserDataStoreImpl
import com.wellconnect.wellmonitoring.ui.components.RectangleButton
import com.wellconnect.wellmonitoring.ui.components.TextComponent
import com.wellconnect.wellmonitoring.ui.components.TopBar
import com.wellconnect.wellmonitoring.viewmodel.WellViewModel
import com.wellconnect.wellmonitoring.ui.navigation.Routes
import com.wellconnect.wellmonitoring.ui.components.BackButton
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    navController: NavController,
    userViewModel: WellViewModel,
    isDarkTheme: Boolean,
    useSystemTheme: Boolean,
    onUpdateTheme: (Int) -> Unit
) {
    val context = LocalContext.current
    val userDataStore = remember { UserDataStoreImpl(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            TopBar(topBarMessage = "Settings")
            BackButton(navController = navController, userViewModel) {
                navController.popBackStack()
            }

            // Reset data
            var showDialog by remember { mutableStateOf(false) }

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Confirm Reset") },
                    text = { Text("This will delete all stored data.") },
                    confirmButton = {
                        TextButton(onClick = {
                            userViewModel.clearAllWellData()
                            showDialog = false
                        }) { Text("Yes") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            RectangleButton(
                elevation = 0.dp,
                textValue = "Reset Data",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                backgroundColor = Color.Red,
                function = { showDialog = true }
            )

            RectangleButton(
                textValue = "Credits",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                backgroundColor = MaterialTheme.colorScheme.primary,
                function = { navController.navigate(Routes.CREDITS_SCREEN) },
                functionName = "Go to the credits",
                textColor = MaterialTheme.colorScheme.background
            )

            Spacer(modifier = Modifier.height(100.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                TextComponent(text = "Use System Theme", fontSize = 18.sp)
                Switch(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    checked = useSystemTheme,
                    onCheckedChange = {
                        onUpdateTheme(
                            if (it) 0 // System default
                            else if (isDarkTheme) 2 else 1 // 2 for Dark, 1 for Light
                        )
                    }
                )
            }

            if (!useSystemTheme) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    TextComponent(text = "Dark Mode", fontSize = 18.sp)
                    Switch(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        checked = isDarkTheme,
                        onCheckedChange = {
                            onUpdateTheme(
                                if (it) 2 else 1 // 2 for Dark, 1 for Light
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    scope.launch {
                        userDataStore.clearUserData()
                        snackbarHostState.showSnackbar("Logged out successfully")
                        navController.navigate(Routes.LOGIN_SCREEN) {
                            popUpTo(0) // Clear the back stack
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Logout")
            }
        }
    }
}

