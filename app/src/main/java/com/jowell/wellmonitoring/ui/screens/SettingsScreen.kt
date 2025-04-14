package com.jowell.wellmonitoring.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jowell.wellmonitoring.data.ThemePreference
import com.jowell.wellmonitoring.ui.theme.BackButton
import com.jowell.wellmonitoring.ui.RectangleButton
import com.jowell.wellmonitoring.ui.TextComponent
import com.jowell.wellmonitoring.ui.TopBar
import com.jowell.wellmonitoring.ui.WellViewModel

@Composable
fun SettingsScreen(
    navController: NavController,
    userViewModel: WellViewModel,
    isDarkTheme: Boolean,
    useSystemTheme: Boolean,
    onUpdateThemePref: (ThemePreference) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
    ) {
        item { TopBar(topBarMessage = "Settings") }
        item {
            BackButton(navController = navController, userViewModel) {
                navController.popBackStack()
            }
        }

        // Reset data
        item {
            var showDialog by remember { mutableStateOf(false) }

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Confirm Reset") },
                    text = { Text("This will delete all stored data.") },
                    confirmButton = {
                        TextButton(onClick = {
                            userViewModel.resetAllWells()
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
        }

        item {
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
        }

        item { Spacer(modifier = Modifier.height(100.dp)) }

        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                TextComponent(textValue = "Use System Theme", textSize = 18.sp)
                Switch(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    checked = useSystemTheme,
                    onCheckedChange = {
                        onUpdateThemePref(
                            if (it) ThemePreference.SYSTEM_DEFAULT
                            else if (isDarkTheme) ThemePreference.DARK else ThemePreference.LIGHT
                        )
                    }
                )
            }
        }

        if (!useSystemTheme) {
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    TextComponent(textValue = "Dark Mode", textSize = 18.sp)
                    Switch(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        checked = isDarkTheme,
                        onCheckedChange = {
                            onUpdateThemePref(
                                if (it) ThemePreference.DARK else ThemePreference.LIGHT
                            )
                        }
                    )
                }
            }
        }
    }
}

