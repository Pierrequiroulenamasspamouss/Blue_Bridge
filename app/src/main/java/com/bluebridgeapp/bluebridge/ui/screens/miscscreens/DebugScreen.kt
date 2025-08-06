package com.bluebridgeapp.bluebridge.ui.screens.miscscreens

import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.bluebridgeapp.bluebridge.viewmodels.UserViewModel
import com.bluebridgeapp.bluebridge.viewmodels.WellViewModel
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DebugScreen(userViewModel: UserViewModel, wellViewModel: WellViewModel) {
    var username by remember { mutableStateOf("") }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Load username when screen appears
    LaunchedEffect(Unit) {
        userViewModel.getUserData().collect { userData ->
            username = userData?.username ?: "User"
        }
    }

    // Function to load image
    val loadImage = suspend {
        try {
            isLoading = true
            error = null
            bitmap = wellViewModel.getSingleWellImage("001", 0)
        } catch (e: Exception) {
            error = e.message
            Log.e("DebugScreen", "Error loading image", e)
        } finally {
            isLoading = false
        }
    }

    // Load image initially
    LaunchedEffect(Unit) {
        loadImage()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val coroutineScope = rememberCoroutineScope()
        Text(text = "Hello $username")

        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                // Only trigger reload if not already loading
                if (!isLoading) { // This LaunchedEffect should be outside the onClick lambda
                    coroutineScope.launch {
                        loadImage()
                    }
                }
            }
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Well Images Demo")

                when {
                    isLoading -> CircularProgressIndicator()
                    error != null -> Text("Error: $error", color = Color.Red)
                    bitmap != null -> {
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = "Well Image",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> Text("No image available")
                }
            }
        }
    }
}