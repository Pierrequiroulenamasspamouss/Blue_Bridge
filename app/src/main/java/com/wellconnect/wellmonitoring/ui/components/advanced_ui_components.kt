@file:Suppress("DEPRECATION")

package com.wellconnect.wellmonitoring.ui.components


import android.annotation.SuppressLint
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.wellconnect.wellmonitoring.R
import com.wellconnect.wellmonitoring.data.model.Location
import com.wellconnect.wellmonitoring.utils.PasswordStrength
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun WellField(
    label: String,
    value: String,
    keyId: Int,
    onValueChange: (String) -> Unit,
    isNumeric: Boolean = false
) {
    Spacer(modifier = Modifier.size(10.dp))
    TextComponent(text = label, fontSize = 18.sp)

    key(keyId) {
        if (isNumeric) {
            AdvancedNumbersFieldComponent(
                initialValue = value,
                defaultInputMessage = label,
                onTextChanged = onValueChange
            )
        } else {
            AdvancedTextFieldComponent(
                initialValue = value,
                defaultInputMessage = label,
                onTextChanged = onValueChange
            )
        }
    }
}

@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isVisible: Boolean,
    onVisibilityChange: () -> Unit,
    passwordStrength: PasswordStrength? = null // Optional parameter
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        trailingIcon = {
            IconButton(onClick = onVisibilityChange) {
                Icon(
                    imageVector = if (isVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (isVisible) "Hide password" else "Show password"
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )

    // Display password strength if provided
    passwordStrength?.let {
        if (value.isNotEmpty()) {
            LinearProgressIndicator(
                progress = when (it.strength) {
                    "Very Weak" -> 0.2f
                    "Weak" -> 0.4f
                    "Medium" -> 0.6f
                    "Strong" -> 0.8f
                    "Very Strong" -> 1f
                    else -> 0f
                },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                color = it.color,
            )
            Text(
                text = "${it.strength}: ${it.message}",
                color = it.color,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * An improved MiniMap component that uses OSMDroid for OpenStreetMap
 */
@SuppressLint("ClickableViewAccessibility", "DefaultLocale")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MiniMap(
    currentLocation: Location? = null,
    selectedLocation: Location? = null,
    onLocationSelected: (Location) -> Unit,
    optionalMarker: Location? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tag = "MiniMap"

    // State to hold reference to the mapView
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    
    // Keep direct references to markers
    var userMarkerRef by remember { mutableStateOf<Marker?>(null) }
    var selectedMarkerRef by remember { mutableStateOf<Marker?>(null) }
    
    // State for zoom level
    var zoomLevel by remember { mutableFloatStateOf(12f) }
    
    // Card with the mini map
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp) // Default height, can be overridden by the modifier parameter
        ) {
            // Setup OpenStreetMap view
            AndroidView(
                factory = { ctx ->
                    try {
                        // Configure OSMDroid
                        Configuration.getInstance().apply {
                            load(context, PreferenceManager.getDefaultSharedPreferences(context))
                            userAgentValue = context.packageName
                        }
                        
                        // Create MapView
                        MapView(ctx).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            )
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            
                            // Set minimum zoom level
                            minZoomLevel = 2.0
                            maxZoomLevel = 19.0
                            
                            // Set initial zoom level
                            controller.setZoom(zoomLevel.toDouble())
                            
                            // Add user marker if location is available
                            currentLocation?.let { loc ->
                                val userPoint = GeoPoint(loc.latitude, loc.longitude)
                                val marker = Marker(this)
                                marker.position = userPoint
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                marker.title = "Your Location"
                                marker.icon = context.getDrawable(R.drawable.small_map_arrow) // TODO : orient it to the current phone orientation
                                overlays.add(marker)
                                controller.setCenter(userPoint)
                                userMarkerRef = marker
                            }
                            
                            // Add selected location marker if available
                            selectedLocation?.let { loc ->
                                val selectedPoint = GeoPoint(loc.latitude, loc.longitude)
                                val marker = Marker(this)
                                marker.position = selectedPoint
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                marker.title = "Selected Location"
                                marker.icon = context.getDrawable(R.drawable.ic_location_pin)
                                overlays.add(marker)
                                selectedMarkerRef = marker
                                
                                // If no user location, center on selected location
                                if (currentLocation == null) {
                                    controller.setCenter(selectedPoint)
                                }
                            }

                            // Add optional marker if available
                            optionalMarker?.let { loc ->
                                val optionalPoint = GeoPoint(loc.latitude, loc.longitude)
                                val marker = Marker(this)
                                marker.position = optionalPoint
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                marker.title = "Optional Location"
                                marker.icon = context.getDrawable(R.drawable.blue_location_pin)
                                overlays.add(marker)
                            }


                            // Set onTap listener to select location
                            setOnTouchListener { _, event ->
                                val action = event.action
                                if (action == android.view.MotionEvent.ACTION_UP) {
                                    val projection = projection
                                    val geoPoint = projection.fromPixels(event.x.toInt(), event.y.toInt()) as GeoPoint
                                    
                                    // Create a new selected location
                                    val location = Location(
                                        latitude = geoPoint.latitude,
                                        longitude = geoPoint.longitude,
                                        lastUpdated = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
                                    )
                                    
                                    // Update or create selected marker
                                    if (selectedMarkerRef != null) {
                                        selectedMarkerRef?.position = geoPoint
                                    } else {
                                        val marker = Marker(this)
                                        marker.position = geoPoint
                                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                        marker.title = "Selected Location"
                                        marker.icon = context.getDrawable(R.drawable.ic_location_pin)
                                        overlays.add(marker)
                                        selectedMarkerRef = marker
                                    }
                                    
                                    // Notify callback
                                    onLocationSelected(location)
                                    invalidate()
                                }
                                false
                            }
                            
                            // Set map instance for later updates
                            mapViewInstance = this
                            
                            // Return the MapView
                            this
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Error creating map view: ${e.message}")
                        // Return a dummy view in case of error
                        FrameLayout(ctx).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            )
                        }
                    }
                },
                update = { mapView ->
                    // Update markers if locations change
                    try {
                        // Update user marker if location changes
                        currentLocation?.let { loc ->
                            val userPoint = GeoPoint(loc.latitude, loc.longitude)
                            if (userMarkerRef != null) {
                                userMarkerRef?.position = userPoint
                            } else {
                                val marker = Marker(mapView as MapView?)
                                marker.position = userPoint
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                marker.title = "Your Location"
                                marker.icon = context.getDrawable(R.drawable.small_map_arrow)
                                mapView.overlays.add(marker)
                                userMarkerRef = marker
                            }
                        }
                        
                        // Update selected marker if location changes
                        selectedLocation?.let { loc ->
                            val selectedPoint = GeoPoint(loc.latitude, loc.longitude)
                            if (selectedMarkerRef != null) {
                                selectedMarkerRef?.position = selectedPoint
                            } else {
                                val marker = Marker(mapView as MapView?)
                                marker.position = selectedPoint
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                marker.title = "Selected Location"
                                marker.icon = context.getDrawable(R.drawable.ic_location_pin)
                                mapView.overlays.add(marker)
                                selectedMarkerRef = marker
                            }
                        }
                        
                        // Force redraw
                        mapView.invalidate()
                    } catch (e: Exception) {
                        Log.e(tag, "Error updating map: ${e.message}")
                    }
                }
            )
            
            // Information display for selected location
            selectedLocation?.let {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .fillMaxWidth(0.8f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ) {
                    Text(
                        text = "Lat: ${String.format("%.6f", it.latitude)}, Lon: ${String.format("%.6f", it.longitude)}",
                        modifier = Modifier.padding(8.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            // Current location button
            FloatingActionButton(
                onClick = {
                    currentLocation?.let {
                        // Set selected location to current location
                        onLocationSelected(it.copy(
                            lastUpdated = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
                        ))
                        
                        // Center map on current location
                        mapViewInstance?.let { mapView ->
                            val userPoint = GeoPoint(currentLocation.latitude, currentLocation.longitude)
                            mapView.controller.animateTo(userPoint)
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(40.dp),
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Use Current Location",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
    
    // Clean up map resources when the composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            try {
                // Clear references
                userMarkerRef = null
                selectedMarkerRef = null
                mapViewInstance?.onDetach()
                mapViewInstance = null
            } catch (e: Exception) {
                Log.e(tag, "Error cleaning up map resources: ${e.message}")
            }
        }
    }
}

// Helper composable functions needed for WellField
@Composable
fun TextComponent(text: String, fontSize: androidx.compose.ui.unit.TextUnit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize),
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

// Renamed to avoid conflict with ui_components.kt
@Composable
fun AdvancedTextFieldComponent(
    initialValue: String,
    defaultInputMessage: String,
    onTextChanged: (String) -> Unit
) {
    OutlinedTextField(
        value = initialValue,
        onValueChange = onTextChanged,
        label = { Text(defaultInputMessage) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

// Renamed to avoid conflict with ui_components.kt
@Composable
fun AdvancedNumbersFieldComponent(
    initialValue: String,
    defaultInputMessage: String,
    onTextChanged: (String) -> Unit
) {
    OutlinedTextField(
        value = initialValue,
        onValueChange = onTextChanged,
        label = { Text(defaultInputMessage) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

