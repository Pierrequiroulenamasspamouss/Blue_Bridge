package com.bluebridgeapp.bluebridge.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Base64
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.bluebridgeapp.bluebridge.R
import com.bluebridgeapp.bluebridge.data.model.ImageData
import com.bluebridgeapp.bluebridge.data.model.ShortenedWellData
import com.bluebridgeapp.bluebridge.data.model.WellData
import com.bluebridgeapp.bluebridge.events.AppEvent
import com.bluebridgeapp.bluebridge.events.AppEventChannel
import com.bluebridgeapp.bluebridge.viewmodels.WellViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.io.encoding.ExperimentalEncodingApi

@Composable
fun WellCard(
    well: WellData,
    isWellOwner: Boolean = false,
    showAdminActions: Boolean = false,
    showLastRefresh: Boolean = false,
    showLastUpdate: Boolean = false,
    onEdit: () -> Unit = {},
    onItemClick: (String) -> Unit = {},
    onNavigate: () -> Unit = {},
    onDeleteClick: () -> Unit
) {
    // Safe property access with fallbacks
    val wellName = well.wellName ?: "Unnamed Well"
    val wellStatus = well.wellStatus ?: "Unknown"
    val capacity = well.wellCapacity?.toFloatOrNull() ?: 0f
    val waterLevel = well.wellWaterLevel?.toFloatOrNull() ?: 0f
    val waterLevelRatio = if (capacity > 0) (waterLevel / capacity).coerceIn(0f, 1f) else 0f
    val waterLevelPercent = (waterLevelRatio * 100).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row - Safe with fallback name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = wellName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (isWellOwner || showAdminActions) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isWellOwner) {
                            IconButton(onClick = onEdit) {
                                Icon(Icons.Default.Edit, stringResource(R.string.edit_well))
                            }
                        }
                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                Icons.Default.Delete,
                                stringResource(R.string.delete_well),
                                tint = if (showAdminActions) MaterialTheme.colorScheme.error
                                else LocalContentColor.current
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Safe status display
            Text(
                text = stringResource(R.string.status_label, wellStatus),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            // Water level indicator - safe with calculated values
            LinearProgressIndicator(
                progress = { waterLevelRatio },
                modifier = Modifier
                    .height(8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.water_level_liters, waterLevel.toString()),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = stringResource(R.string.water_level_percentage, waterLevelPercent),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Safe time displays
            well.lastRefreshTime?.let { refreshTime ->
                if (showLastRefresh && refreshTime > 0) {
                    val date = Date(refreshTime)
                    val formatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date)
                    Text(
                        text = stringResource(R.string.last_refreshed_label, formatted),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            well.lastUpdated?.let { updated ->
                if (showLastUpdate) {
                    Text(
                        text = stringResource(R.string.last_update_label, formatDateTime(updated)),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { well.wellId?.let(onItemClick) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Navigation, stringResource(R.string.details))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.go_button))
            }
        }
    }
}

@Composable
fun EnhancedWellCard(
    well: ShortenedWellData,
    onClick: () -> Unit,
    onNavigateClick: () -> Unit
) {
    // Calculate water level percentage safely outside composable
    val waterLevelInfo = if (well.wellWaterLevel.toString().isNotBlank() && well.wellCapacity.isNotBlank()) {
        calculateWaterLevelInfo(well.wellWaterLevel, well.wellCapacity)
    } else null

    Card(
        Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(well.wellName, style = MaterialTheme.typography.titleMedium)
                StatusIndicator(well.wellStatus)
            }

            Text(
                text = stringResource(
                    R.string.location_coordinates,
                    well.wellLocation.latitude,
                    well.wellLocation.longitude
                ), style = MaterialTheme.typography.bodySmall
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.type_label, well.wellWaterType), style = MaterialTheme.typography.bodySmall)

                if (well.wellCapacity.isNotBlank()) {
                    Text(stringResource(R.string.capacity_liters, well.wellCapacity), style = MaterialTheme.typography.bodySmall)
                }
            }

            // Water level indicator using pre-calculated values
            waterLevelInfo?.let { (progress, percentage) ->
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    color = when {
                        percentage > 70 -> Color.Green
                        percentage > 30 -> Color.Yellow
                        else -> Color.Red
                    },
                )
                Text(
                    stringResource(R.string.water_level_percentage_label, percentage),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Navigate button
            Button(
                onClick = onNavigateClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.navigate_there))
            }
        }
    }
}

private fun formatDateTime(dateTime: String): String {
    return try {
        // Assuming the input dateTime string is in ISO 8601 format like "2023-10-27T10:15:30Z"
        // Adjust the inputFormat pattern if your dateTime string format is different.
        // Use SimpleDateFormat for compatibility with API 25 and earlier.
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'") // Adjust if needed
        inputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC") // Assuming input is UTC
        val date: Date? = inputFormat.parse(dateTime)
        val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm")
        outputFormat.timeZone = java.util.TimeZone.getDefault() // Format to local time zone
        date?.let { outputFormat.format(it) } ?: dateTime
    } catch (e: Exception) {
        Log.e("DateTimeFormat", "Error formatting date: $dateTime", e) // Log with specific error message
        dateTime
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ImageSection(
    wellData: WellData,
    wellViewModel: WellViewModel
) {
    var images by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var tapCounter by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Function to load images
    suspend fun loadImages(wellId: String?, wellViewModel: WellViewModel): List<Bitmap> {
        Log.d("ImageSection", "Loading images for well $wellId")
        if (wellId != null) {
            return try {
                val images = mutableListOf<Bitmap>()
                // Load all images (0-9) for the well
                for (i in 0..9) {
                    wellViewModel.getSingleWellImage(wellId, i)?.let { bitmap ->
                        images.add(bitmap)
                    }
                }
                images
            } catch (e: Exception) {
                Log.e("ImageSection", "Error loading images", e)
                AppEventChannel.sendEvent(AppEvent.ShowError("Error loading images: ${e.message}"))
                emptyList()
            }
        }
        return emptyList()
    }

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
                        val imageNumber = images.size
                        Log.d("ImageSection", "Uploading image for well ${wellData.wellId}, image number $imageNumber")

                        val success = wellViewModel.uploadWellPicture(wellData.wellId!!, imageNumber, selectedBitmap)

                        if (success) {
                            AppEventChannel.sendEvent(AppEvent.ShowSuccess("Image uploaded successfully!"))
                            // Reload images after upload
                            loadImages(
                                wellData.wellId!!,
                                wellViewModel = wellViewModel
                            )
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
                    Log.e("ImageSection", "Error uploading image", e)
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
                    val imageNumber = images.size

                    Log.d("ImageSection", "Uploading image from camera for well ${wellData.wellId}, image number $imageNumber")

                    val success = wellViewModel.uploadWellPicture(wellData.wellId, imageNumber, it)

                    if (success) {
                        AppEventChannel.sendEvent(AppEvent.ShowSuccess("Image uploaded successfully!"))
                        loadImages(
                            wellData.wellId,
                            wellViewModel = wellViewModel
                        )
                    } else {
                        error = "Failed to upload image"
                        AppEventChannel.sendEvent(AppEvent.ShowError("Failed to upload image"))
                    }
                } catch (e: Exception) {
                    error = e.message
                    Log.e("ImageSection", "Error uploading image from camera", e)
                    AppEventChannel.sendEvent(AppEvent.ShowError("Error uploading image from camera: ${e.message}"))
                } finally {
                    isUploading = false
                }
            }
        }
    }

    // Permission launchers
    val requestGalleryPermissionLauncher = rememberLauncherForActivityResult(
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

    // Function to load images
    fun loadImages(wellId: String?) {
        coroutineScope.launch {
            if (wellId != null) {
                try {
                    isLoading = true
                    error = null
                    images = wellViewModel.getAllImages(wellId)
                } catch (e: Exception) {
                    error = e.message
                    Log.e("ImageSection", "Error loading images", e)
                    AppEventChannel.sendEvent(AppEvent.ShowError("Error loading images: ${e.message}"))
                } finally {
                    isLoading = false
                }
            }
        }
    }
    // Load images initially
    LaunchedEffect(wellData.wellId) {
        if (wellData.wellId != null) {
            loadImages(wellData.wellId)
        }
    }

    Column {
        // Images display
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable {
                    tapCounter++
                    if (tapCounter % 3 == 0 && wellData.wellId != null) {
                        loadImages(wellData.wellId)
                    }
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(images) { index, image ->
                Box(modifier = Modifier) {
                    Image(
                        bitmap = image.asImageBitmap(),
                        contentDescription = "Well Image",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    // Remove button
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                wellViewModel.deleteWellImage(wellData.wellId!!, index)
                                loadImages(wellData.wellId)
                            }
                        },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove image",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Add image button
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                val galleryPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    Manifest.permission.READ_MEDIA_IMAGES
                                } else {
                                    Manifest.permission.READ_EXTERNAL_STORAGE
                                }

                                if (ContextCompat.checkSelfPermission(context, galleryPermission) == PackageManager.PERMISSION_GRANTED) {
                                    imagePickerLauncher.launch("image/*")
                                } else {
                                    requestGalleryPermissionLauncher.launch(galleryPermission)
                                }
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = stringResource(R.string.add_image),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.add_image),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // Camera button
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                val cameraPermission = Manifest.permission.CAMERA
                                if (ContextCompat.checkSelfPermission(context, cameraPermission) == PackageManager.PERMISSION_GRANTED) {
                                    cameraLauncher.launch(null)
                                } else {
                                    requestCameraPermissionLauncher.launch(cameraPermission)
                                }
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Take photo",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Take Photo",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        if (error != null) {
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp)
            )
        }

        if (images.isEmpty() && !isLoading) {
            Text(
                text = stringResource(R.string.no_images_yet_add_one),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Permission dialog
        if (showPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                title = { Text("Permission Required") },
                text = { Text("This app needs access to your photos to upload images. Please grant the permission in Settings.") },
                confirmButton = {
                    TextButton(
                        onClick = { showPermissionDialog = false }
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

