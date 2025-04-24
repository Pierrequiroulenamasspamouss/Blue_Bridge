package com.wellconnect.wellmonitoring.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.wellconnect.wellmonitoring.data.WellData
import com.wellconnect.wellmonitoring.data.getLatitude
import com.wellconnect.wellmonitoring.data.getLongitude
import com.wellconnect.wellmonitoring.data.hasValidCoordinates
import com.wellconnect.wellmonitoring.ui.WellViewModel
import com.wellconnect.wellmonitoring.ui.navigation.Routes
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CompassScreen(
    navController: NavController,
    latitude: Double? = null,
    longitude: Double? = null,
    locationName: String? = null,
    wellViewModel: WellViewModel? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showNearestWellsDialog by remember { mutableStateOf(false) }
    var nearestWells by remember { mutableStateOf<List<WellData>>(emptyList()) }

    var currentLocation by remember { mutableStateOf(lastKnownLocation) }
    var targetLatitude by remember { mutableStateOf(latitude?.toString() ?: "") }
    var targetLongitude by remember { mutableStateOf(longitude?.toString() ?: "") }
    var azimuth by remember { mutableFloatStateOf(0f) }
    var distance by remember { mutableStateOf<Float?>(null) }
    var bearing by remember { mutableStateOf<Float?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Decode the location name properly
    val decodedLocationName = remember(locationName) {
        locationName?.replace("+", " ")?.let { java.net.URLDecoder.decode(it, "UTF-8") }
    }

    // Increase smoothing by reducing alpha value
    val alpha = 0.08f // More smoothing (was 0.15f)
    var filteredAzimuth by remember { mutableFloatStateOf(0f) }

    // Smooth arrow rotation
    var targetRotation by remember { mutableFloatStateOf(0f) }
    var currentRotation by remember { mutableFloatStateOf(0f) }
    var currentVelocity by remember { mutableFloatStateOf(0f) }
    val maxRotationSpeed = 360f // doubled from 180 degrees per second
    val baseAcceleration = 720f // doubled from 360 degrees per second squared
    var lastUpdateTime by remember { mutableLongStateOf(System.nanoTime()) }

    // Calculate bearing and distance whenever location changes
    fun updateBearingAndDistance() {
        val targetLat = targetLatitude.toDoubleOrNull()
        val targetLon = targetLongitude.toDoubleOrNull()
        
        if (currentLocation != null && targetLat != null && targetLon != null) {
            val results = FloatArray(2)
            Location.distanceBetween(
                currentLocation!!.latitude,
                currentLocation!!.longitude,
                targetLat,
                targetLon,
                results
            )
            distance = results[0]
            bearing = results[1]
        }
    }

    // Initialize target coordinates if provided
    LaunchedEffect(latitude, longitude) {
        if (latitude != null && longitude != null) {
            targetLatitude = latitude.toString()
            targetLongitude = longitude.toString()
            updateBearingAndDistance()
        }
    }

    // Sensor event listener for compass
    val sensorEventListener = remember {
        object : SensorEventListener {
            private val rotationMatrix = FloatArray(9)
            private val orientationAngles = FloatArray(3)
            private val accelerometerReading = FloatArray(3)
            private val magnetometerReading = FloatArray(3)
            private var lastAccelerometerSet = false
            private var lastMagnetometerSet = false
            
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
                    lastAccelerometerSet = true
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
                    lastMagnetometerSet = true
                }

                if (lastAccelerometerSet && lastMagnetometerSet) {
                    SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    val newAzimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                    
                    // Apply low-pass filter
                    filteredAzimuth = alpha * newAzimuth + (1 - alpha) * filteredAzimuth
                    azimuth = filteredAzimuth
                }
            }
        }
    }

    // Register sensor listeners
    DisposableEffect(sensorManager) {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        
        sensorManager.registerListener(
            sensorEventListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME
        )
        
        sensorManager.registerListener(
            sensorEventListener,
            magnetometer,
            SensorManager.SENSOR_DELAY_GAME
        )
        
        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }

    // Request location updates only if we don't have a last known location
    LaunchedEffect(Unit) {
        if (currentLocation == null && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = location
                    lastKnownLocation = location
                    updateBearingAndDistance()
                }
            }
        }
    }

    // Check and request location permissions
    LaunchedEffect(Unit) {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val hasPermissions = permissions.all {
            context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }
        if (!hasPermissions) {
            showPermissionDialog = true
        }
    }

    // Calculate distance if we have both locations
    var currentDistance by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(currentLocation, latitude, longitude) {
        if (currentLocation != null && latitude != null && longitude != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                currentLocation!!.latitude,
                currentLocation!!.longitude,
                latitude,
                longitude,
                results
            )
            currentDistance = results[0]
        }
    }

    LaunchedEffect(bearing, azimuth) {
        if (bearing != null) {
            targetRotation = bearing!! - azimuth
        }
    }

    // Update rotation with variable acceleration
    LaunchedEffect(targetRotation) {
        while (true) {
            val currentTime = System.nanoTime()
            val deltaTime = (currentTime - lastUpdateTime) / 1_000_000_000f // Convert to seconds
            lastUpdateTime = currentTime

            // Calculate shortest rotation direction
            var angleDiff = ((targetRotation - currentRotation + 180) % 360) - 180
            if (angleDiff <= -180) angleDiff += 360

            // Calculate acceleration based on distance
            val distanceFactor = kotlin.math.abs(angleDiff) / 180f // 0 to 1 based on distance
            val acceleration = baseAcceleration * distanceFactor

            // Apply acceleration to velocity
            val targetVelocity = if (angleDiff > 0) maxRotationSpeed else -maxRotationSpeed
            if (currentVelocity < targetVelocity) {
                currentVelocity = (currentVelocity + acceleration * deltaTime).coerceAtMost(targetVelocity)
            } else if (currentVelocity > targetVelocity) {
                currentVelocity = (currentVelocity - acceleration * deltaTime).coerceAtLeast(targetVelocity)
            }

            // Apply deceleration when approaching target
            val stoppingDistance = (currentVelocity * currentVelocity) / (2 * acceleration)
            if (kotlin.math.abs(angleDiff) <= stoppingDistance) {
                currentVelocity *= 0.8f // Gradual deceleration
            }

            // Update current rotation with velocity
            currentRotation = (currentRotation + currentVelocity * deltaTime + 360) % 360

            withFrameNanos { frameTime -> }  // Wait for next frame
        }
    }

    // Permission dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Location Permission Required") },
            text = { Text("The compass needs location permission to show directions accurately. Please grant the permission in the next dialog.") },
            confirmButton = {
                Button(onClick = {
                    showPermissionDialog = false
                    (context as? Activity)?.requestPermissions(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ),
                        1001
                    )
                }) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showPermissionDialog = false
                    navController.navigateUp()
                }) {
                    Text("Go Back")
                }
            }
        )
    }

    // Nearest wells dialog
    if (showNearestWellsDialog && nearestWells.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showNearestWellsDialog = false },
            title = { Text("Nearest Wells") },
            text = {
                Column {
                    nearestWells.forEach { well ->
                        TextButton(
                            onClick = {
                                showNearestWellsDialog = false
                                val encodedName = java.net.URLEncoder.encode(well.wellName, "UTF-8")
                                navController.navigate(
                                    "${Routes.COMPASS_SCREEN}?lat=${well.getLatitude()}&lon=${well.getLongitude()}&name=$encodedName"
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(well.wellName)
                                Text(
                                    "Distance: ${formatDistance(calculateDistance(currentLocation, well))}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNearestWellsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Show appropriate text based on whether a destination is selected
        if (decodedLocationName != null && decodedLocationName != "North") {
            Text(
                text = "Navigating to: $decodedLocationName",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        } else {
            Text(
                text = "Point to North",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Tap 'Find Nearest Wells' to locate wells near you",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Compass
        Box(
            modifier = Modifier
                .size(250.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width.coerceAtMost(size.height) / 2

                // Draw compass circle
                drawCircle(
                    color = Color.Gray.copy(alpha = 0.3f),
                    radius = radius,
                    center = center
                )

                // Draw direction indicator zone (red/green arc near the top)
                val indicatorPath = Path().apply {
                    val innerRadius = radius * 0.5f
                    // Draw outer arc
                    addArc(
                        oval = androidx.compose.ui.geometry.Rect(
                            left = center.x - radius,
                            top = center.y - radius,
                            right = center.x + radius,
                            bottom = center.y + radius
                        ),
                        startAngleDegrees = -20f - 90,
                        sweepAngleDegrees = 40f
                    )
                    // Draw inner arc
                    addArc(
                        oval = androidx.compose.ui.geometry.Rect(
                            left = center.x - innerRadius,
                            top = center.y - innerRadius,
                            right = center.x + innerRadius,
                            bottom = center.y + innerRadius
                        ),
                        startAngleDegrees = -20f - 90,
                        sweepAngleDegrees = 40f
                    )
                    // Draw connecting lines
                    val startAngleRadians = Math.toRadians(-110.0) // -20 - 90 degrees
                    val endAngleRadians = Math.toRadians(-70.0)   // 20 - 90 degrees
                    
                    // Start side line
                    moveTo(
                        center.x + innerRadius * cos(startAngleRadians).toFloat(),
                        center.y + innerRadius * sin(startAngleRadians).toFloat()
                    )
                    lineTo(
                        center.x + radius * cos(startAngleRadians).toFloat(),
                        center.y + radius * sin(startAngleRadians).toFloat()
                    )
                    
                    // End side line
                    moveTo(
                        center.x + innerRadius * cos(endAngleRadians).toFloat(),
                        center.y + innerRadius * sin(endAngleRadians).toFloat()
                    )
                    lineTo(
                        center.x + radius * cos(endAngleRadians).toFloat(),
                        center.y + radius * sin(endAngleRadians).toFloat()
                    )
                }

                // Check if arrow is within the target zone
                val isInTargetZone = bearing?.let { targetBearing ->
                    // Use currentRotation instead of raw compass reading
                    val normalizedRotation = (currentRotation + 360) % 360
                    normalizedRotation in 340f..360f || normalizedRotation in 0f..20f
                } == true

                // Create and draw the filled area first
                val fillPath = Path().apply {
                    val innerRadius = radius * 0.5f
                    // Start at the inner radius at the start angle
                    val startAngleRadians = Math.toRadians(-110.0)
                    moveTo(
                        center.x + innerRadius * cos(startAngleRadians).toFloat(),
                        center.y + innerRadius * sin(startAngleRadians).toFloat()
                    )
                    // Draw inner arc
                    arcTo(
                        rect = androidx.compose.ui.geometry.Rect(
                            left = center.x - innerRadius,
                            top = center.y - innerRadius,
                            right = center.x + innerRadius,
                            bottom = center.y + innerRadius
                        ),
                        startAngleDegrees = -20f - 90,
                        sweepAngleDegrees = 40f,
                        forceMoveTo = false
                    )
                    // Line to outer radius
                    val endAngleRadians = Math.toRadians(-70.0)
                    lineTo(
                        center.x + radius * cos(endAngleRadians).toFloat(),
                        center.y + radius * sin(endAngleRadians).toFloat()
                    )
                    // Draw outer arc counter-clockwise
                    arcTo(
                        rect = androidx.compose.ui.geometry.Rect(
                            left = center.x - radius,
                            top = center.y - radius,
                            right = center.x + radius,
                            bottom = center.y + radius
                        ),
                        startAngleDegrees = -20f - 90 + 40f,
                        sweepAngleDegrees = -40f,
                        forceMoveTo = false
                    )
                    close()
                }

                // Draw the filled area
                drawPath(
                    path = fillPath,
                    color = if (isInTargetZone) Color.Green.copy(alpha = 0.25f) else Color.Red.copy(alpha = 0.25f),
                    style = androidx.compose.ui.graphics.drawscope.Fill
                )

                // Draw the outline
                drawPath(
                    path = indicatorPath,
                    color = if (isInTargetZone) Color.Green.copy(alpha = 0.5f) else Color.Red.copy(alpha = 0.5f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 2f
                    )
                )

                // Draw direction to target if available
                bearing?.let {
                    // Use currentRotation instead of direct calculation
                    rotate(currentRotation) {
                        // Draw arrow pointing to target
                        val arrowLength = radius * 0.8f
                        val arrowWidth = radius * 0.15f
                        
                        // Determine arrow color based on whether we're pointing north or to a destination
                        val arrowColor = if (decodedLocationName == "North") {
                            Color(0xFFFF69B4) // Hot pink color for north
                        } else {
                            Color.Blue
                        }
                        
                        // Main line
                        drawLine(
                            color = arrowColor,
                            start = center,
                            end = Offset(center.x, center.y - arrowLength),
                            strokeWidth = 8f
                        )
                        
                        // Arrow head
                        val arrowPath = Path().apply {
                            moveTo(center.x, center.y - arrowLength)
                            lineTo(center.x - arrowWidth, center.y - arrowLength + arrowWidth)
                            lineTo(center.x + arrowWidth, center.y - arrowLength + arrowWidth)
                            close()
                        }
                        drawPath(
                            path = arrowPath,
                            color = arrowColor
                        )
                    }
                }
            }
        }

        // Show distance and estimated times if available AND not pointing north
        if (currentDistance != null && decodedLocationName != "North") {
            Text(
                text = when {
                    currentDistance!! >= 1000 -> "%.1f km".format(currentDistance!! / 1000)
                    else -> "${currentDistance!!.toInt()} m"
                },
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Add estimated travel times
            val walkingMinutes = (currentDistance!! / 1.389f) / 60 // Convert to minutes (5 km/h = 1.389 m/s)
            Column(
                modifier = Modifier.padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🚶 Walking: ${formatTime(walkingMinutes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Only show driving time if walking takes more than 30 minutes
                if (walkingMinutes > 30) {
                    val drivingMinutes = (currentDistance!! / 11.111f) / 60 // 40 km/h = 11.111 m/s
                    Text(
                        text = "🚗 Driving: ${formatTime(drivingMinutes)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Add "Find Nearest Wells" button when no specific well destination or pointing north
        if ((latitude == null && longitude == null) || decodedLocationName == "North") {
            Spacer(modifier = Modifier.weight(1f))
            ExtendedFloatingActionButton(
                onClick = {
                    scope.launch {
                        if (currentLocation != null) {
                            nearestWells = findNearestWells(currentLocation!!, wellViewModel!!)
                            showNearestWellsDialog = true
                        }
                    }
                },
                modifier = Modifier.padding(bottom = 16.dp),
                icon = { Icon(Icons.Default.NearMe, contentDescription = null) },
                text = { Text("Find Nearest Wells") }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

private var lastKnownLocation: Location? = null

private fun calculateDistance(location: Location?, well: WellData): Float {
    if (location == null || !well.hasValidCoordinates()) return Float.POSITIVE_INFINITY
    val results = FloatArray(1)
    Location.distanceBetween(
        location.latitude,
        location.longitude,
        well.getLatitude()!!,
        well.getLongitude()!!,
        results
    )
    return results[0]
}

private fun formatDistance(meters: Float): String {
    return when {
        meters == Float.POSITIVE_INFINITY -> "Unknown distance"
        meters >= 1000 -> "%.1f km".format(meters / 1000)
        else -> "${meters.toInt()} m"
    }
}

private fun findNearestWells(location: Location, wellViewModel: WellViewModel): List<WellData> {
    return wellViewModel.wellList.value
        .filter { it.hasValidCoordinates() }
        .map { well -> well to calculateDistance(location, well) }
        .sortedBy { it.second }
        .take(3)
        .map { it.first }
}

@SuppressLint("DefaultLocale")
private fun formatTime(minutes: Float): String {
    return when {
        minutes < 1 -> "less than a minute"
        minutes < 60 -> "${minutes.toInt()} minutes"
        else -> String.format("%.1f hours", minutes / 60)
    }
} 