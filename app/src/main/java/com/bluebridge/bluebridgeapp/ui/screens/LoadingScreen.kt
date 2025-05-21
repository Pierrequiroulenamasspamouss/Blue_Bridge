package com.bluebridge.bluebridgeapp.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp


@Composable
fun LoadingScreen() {
    Box(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            androidx.compose.material3.CircularProgressIndicator()
            androidx.compose.material3.Text(
                text = "Loading...",
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                modifier = androidx.compose.ui.Modifier.padding(top = 16.dp)
            )
        }
    }
}

