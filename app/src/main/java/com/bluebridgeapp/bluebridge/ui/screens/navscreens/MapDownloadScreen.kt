package com.bluebridgeapp.bluebridge.ui.screens.navscreens

import OfflineMapDownloader
import android.location.Location
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bluebridgeapp.bluebridge.ui.components.compass.MiniMapCard
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import kotlin.math.pow
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MapDownloadScreen(
    navController: NavController,
    userLat: Double?,
    userLon: Double?
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var radius by remember { mutableFloatStateOf(3f) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var downloadStatus by remember { mutableStateOf("") }
    var downloadJob by remember { mutableStateOf<Job?>(null) }
    var miniMapZoom by remember { androidx.compose.runtime.mutableDoubleStateOf(16.0) }

    // OSMDroid config
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    // Fake Location for MiniMapCard
    val fakeLocation = remember(userLat, userLon) {
        Location("").apply {
            latitude = userLat ?: 0.0
            longitude = userLon ?: 0.0
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Download Offline Map") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // MiniMapCard with radius overlay
            Box(
                modifier = Modifier
                    .height(200.dp)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
            ) {
                if (userLat != null && userLon != null) {
                    MiniMapCard(
                        userLocation = fakeLocation,
                        targetLat = null,
                        targetLon = null,
                        azimuth = 0f,
                        onZoomChanged = { zoom -> miniMapZoom = zoom }
                    )
                    // Draw a circle overlay to show the download radius
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val earthCircumference = 40075016.686 // meters
                        val metersPerPixel = (earthCircumference * kotlin.math.cos(Math.toRadians(userLat))) / (2.0.pow(miniMapZoom + 8))
                        val radiusPx = (radius * 1000) / metersPerPixel
                        drawCircle(
                            color = Color(0x5500BFFF),
                            radius = radiusPx.toFloat(),
                            center = center,
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }
                } else {
                    Text("Location unavailable", modifier = Modifier.align(Alignment.Center))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Select download radius (km):", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Slider(
                value = radius,
                onValueChange = { radius = it },
                valueRange = 1f..10f,
                steps = 9,
                enabled = !isDownloading
            )
            Text("${radius.toInt()} km", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            if (isDownloading) {
                LinearProgressIndicator(
                    progress = { downloadProgress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text("Downloading: $downloadProgress%", style = MaterialTheme.typography.bodyMedium)
                if (downloadStatus.isNotBlank()) {
                    Text(downloadStatus, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    downloadJob?.cancel()
                    isDownloading = false
                    downloadStatus = "Canceled"
                }) {
                    Text("Cancel")
                }
            } else {
                Button(
                    onClick = {
                        if (userLat != null && userLon != null) {
                            isDownloading = true
                            downloadProgress = 0
                            downloadStatus = ""
                            val downloader = OfflineMapDownloader(context)
                            downloadJob = coroutineScope.launch {
                                val result = downloader.downloadArea(
                                    center = GeoPoint(userLat, userLon),
                                    radiusKm = radius.toInt(),
                                    onProgress = { progress ->
                                        downloadProgress = progress
                                    }
                                )
                                isDownloading = false
                                downloadStatus = if (result.isSuccess) "Download complete!" else "Failed: ${result.exceptionOrNull()?.message}"
                            }
                        }
                    },
                    enabled = !isDownloading && userLat != null && userLon != null
                ) {
                    Text("Download Map")
                }
            }
        }
    }
}

