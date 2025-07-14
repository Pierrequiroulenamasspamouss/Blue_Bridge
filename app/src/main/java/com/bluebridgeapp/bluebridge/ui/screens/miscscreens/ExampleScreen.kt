package com.bluebridgeapp.bluebridge.ui.screens.miscscreens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.bluebridgeapp.bluebridge.viewmodels.UserViewModel


@Composable
fun ExampleScreen(userViewModel: UserViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var username by remember { mutableStateOf("") }

        LaunchedEffect(Unit) {
            userViewModel.getUserData().collect { userData ->
                username = userData?.username ?: "User"
            }
        }
        Text(text = "Hello $username")
    }
}