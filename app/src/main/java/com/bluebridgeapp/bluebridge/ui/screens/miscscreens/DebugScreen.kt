package com.bluebridgeapp.bluebridge.ui.screens.miscscreens

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.bluebridgeapp.bluebridge.events.AppEvent
import com.bluebridgeapp.bluebridge.events.AppEventChannel
import com.bluebridgeapp.bluebridge.ui.navigation.Routes
import com.bluebridgeapp.bluebridge.viewmodels.UserViewModel
import com.bluebridgeapp.bluebridge.viewmodels.WellViewModel
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DebugScreen(
    userViewModel: UserViewModel, 
    wellViewModel: WellViewModel,
    navController: NavController? = null
) {
    var username by remember { mutableStateOf("") }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var wellImages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingAllImages by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var wellIdInput by remember { mutableStateOf("001") }
    var showAllImages by remember { mutableStateOf(false) }
    var imageNumberInput by remember { mutableStateOf("0") }
    var descriptionInput by remember { mutableStateOf("") }
    var showPermissionDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    var tapcounter = 0

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                try {
                    isUploading = true
                    error = null
                    
                    val inputStream = context.contentResolver.openInputStream(it)
                    val selectedBitmap = BitmapFactory.decodeStream(inputStream)
                    
                    if (selectedBitmap != null) {
                        val imageNumber = imageNumberInput.toIntOrNull() ?: 0
                        val description = descriptionInput.ifBlank { "Image $imageNumber" }
                        
                        Log.d("DebugScreen", "Uploading image for well $wellIdInput, image number $imageNumber")
                        
                        val success = wellViewModel.uploadWellPicture(wellIdInput, imageNumber, selectedBitmap)

                        Log.d("DebugScreen", "Image upload result: $success")
                        
                        if (success) {
                            Log.d("DebugScreen", "Image uploaded successfully")
                            AppEventChannel.sendEvent(AppEvent.ShowSuccess("Image uploaded successfully!"))
                            
                            // Reload the current image to show the uploaded one
                            bitmap = wellViewModel.getSingleWellImage(wellIdInput, imageNumber)
                        } else {
                            error = "Failed to upload image"
                            AppEventChannel.sendEvent(AppEvent.ShowError("Failed to upload image"))
                        }
                    } else {
                        error = "Failed to load selected image"
                        AppEventChannel.sendEvent(AppEvent.ShowError("Failed to load selected image"))
                    }
                } catch (e: Exception) {
                    error = e.message
                    Log.e("DebugScreen", "Error uploading image", e)
                    AppEventChannel.sendEvent(AppEvent.ShowError("Error uploading image: ${e.message}"))
                } finally {
                    isUploading = false
                }
            }
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { capturedBitmap: Bitmap? ->
        capturedBitmap?.let {
            coroutineScope.launch {
                try {
                    isUploading = true
                    error = null

                    val imageNumber = imageNumberInput.toIntOrNull() ?: 0
                    val description = descriptionInput.ifBlank { "Image $imageNumber" }

                    Log.d("DebugScreen", "Uploading image from camera for well $wellIdInput, image number $imageNumber")

                    val success = wellViewModel.uploadWellPicture(wellIdInput, imageNumber, it)

                    Log.d("DebugScreen", "Image upload result: $success")

                    if (success) {
                        Log.d("DebugScreen", "Image uploaded successfully")
                        AppEventChannel.sendEvent(AppEvent.ShowSuccess("Image uploaded successfully!"))

                        // Reload the current image to show the uploaded one
                        bitmap = wellViewModel.getSingleWellImage(wellIdInput, imageNumber)
                    } else {
                        error = "Failed to upload image"
                        AppEventChannel.sendEvent(AppEvent.ShowError("Failed to upload image"))
                    }
                } catch (e: Exception) {
                    error = e.message
                    Log.e("DebugScreen", "Error uploading image from camera", e)
                    AppEventChannel.sendEvent(AppEvent.ShowError("Error uploading image from camera: ${e.message}"))
                } finally {
                    isUploading = false
                }
            }
        }
    }

    // Permission launcher
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            imagePickerLauncher.launch("image/*")
        } else {
            showPermissionDialog = true
        }
    }

    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            coroutineScope.launch {
                AppEventChannel.sendEvent(AppEvent.ShowError("Camera permission denied. Cannot take photo."))
            }
        }
    }

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
            val imageNumber = imageNumberInput.toIntOrNull() ?: 0
            bitmap = wellViewModel.getSingleWellImage(wellIdInput, imageNumber)
        } catch (e: Exception) {
            error = e.message
            Log.e("DebugScreen", "Error loading image", e)
            AppEventChannel.sendEvent(AppEvent.ShowError("Issue loading the image : ${e}"))
        } finally {
            isLoading = false
        }
    }

    // Function to load all images for a well
    val loadAllWellImages = suspend {
        try {
            isLoadingAllImages = true
            error = null
            val images = mutableListOf<Bitmap>()
            for (i in 0..9) {
                wellViewModel.getSingleWellImage(wellIdInput, i)?.let {
                    images.add(it)
                }
            }
            wellImages = images
        } catch (e: Exception) {
            error = e.message
            Log.e("DebugScreen", "Error loading all well images", e)
            AppEventChannel.sendEvent(AppEvent.ShowError("Issue loading all well images: ${e}"))
        } finally {
            isLoadingAllImages = false
        }
    }

    // Load image initially
    LaunchedEffect(Unit) {
        if (showAllImages) {loadAllWellImages}
        else {
            loadImage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Hello $username")

        OutlinedTextField(
            value = wellIdInput,
            onValueChange = { wellIdInput = it },
            label = { Text("Well ID") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardActions = KeyboardActions(
                onDone = {
                    coroutineScope.launch {
                        if (showAllImages) {loadAllWellImages}
                        else {
                            loadImage()
                        }
                    }
                    keyboardController?.hide()
                }
            )
        )

        OutlinedTextField(
            value = imageNumberInput,
            onValueChange = { imageNumberInput = it },
            label = { Text("Image Number") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = androidx.compose.ui.text.input.ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardActions = KeyboardActions(
                onDone = {
                    coroutineScope.launch {
                        if (showAllImages) {loadAllWellImages}
                        else {
                            loadImage()
                        }
                    }
                    keyboardController?.hide()
                }
            )
        )

        // Upload section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Upload Image")
                
                OutlinedTextField(
                    value = descriptionInput,
                    onValueChange = { descriptionInput = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Manifest.permission.READ_MEDIA_IMAGES
                        } else {
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        }
                        
                        if (ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            imagePickerLauncher.launch("image/*")
                        } else {
                            requestPermissionLauncher.launch(permission)
                        }
                    },
                    enabled = !isUploading
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Upload, contentDescription = null)
                    }
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
                    Text(if (isUploading) "Uploading..." else "Upload Image")
                }

                Button(
                    onClick = {
                        val cameraPermission = Manifest.permission.CAMERA
                        if (ContextCompat.checkSelfPermission(context, cameraPermission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            cameraLauncher.launch(null)
                        } else {
                            requestCameraPermissionLauncher.launch(cameraPermission)
                        }
                    },
                    enabled = !isUploading
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null)
                    }
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
                    Text(if (isUploading) "Taking Photo..." else "Take Photo & Upload")
                }


            }
        }

        // FCM Debug section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "FCM Image Chunks Debug")
                
                Button(
                    onClick = {
                        navController?.navigate(Routes.NOTIFICATION_DEBUG_SCREEN)
                    },
                    enabled = navController != null
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null)
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
                    Text("Test FCM Image Chunks")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                tapcounter++
                Log.d("DebugScreen", "Tap counter: $tapcounter")

                // Only trigger reload every 3 taps
                if (tapcounter % 3 == 0) {
                    Log.d("DebugScreen", "Reloading images (counter reached 3)")
                    coroutineScope.launch {
                        if (showAllImages) {
                            loadAllWellImages()
                            Log.d("DebugScreen", "Reloading all images")
                        } else {
                            loadImage()
                            Log.d("DebugScreen", "Reloading single image")
                        }
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

                if (showAllImages) {
                    when {
                        isLoadingAllImages -> CircularProgressIndicator()
                        error != null -> Text("Error: $error", color = Color.Red)
                        wellImages.isNotEmpty() -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                wellImages.forEachIndexed { index, img ->
                                    Image(
                                        bitmap = img.asImageBitmap(),
                                        contentDescription = "Well Image $index",
                                        modifier = Modifier.size(100.dp) // Adjust size as needed
                                    )
                                }
                            }
                        }
                        else -> Text("No images available for this well")
                    }
                } else {
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

                Button(onClick = {
                    if (!isLoadingAllImages) {
                        showAllImages = !showAllImages
                        if (showAllImages && wellImages.isEmpty()) { // Load only if not already loaded
                            coroutineScope.launch {
                                loadAllWellImages()
                            }
                        }
                    }
                }) {
                    Text(if (showAllImages) "Show Single Image" else "Show All Images of that Well (0-9)")
                }
            }
        }

        // Permission dialog
        if (showPermissionDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                title = { Text("Permission Required") },
                text = { Text("This app needs access to your photos to upload images. Please grant the permission in Settings.") },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = { showPermissionDialog = false }
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }
}