package com.bluebridge.bluebridgeapp.ui.components


import android.annotation.SuppressLint
import android.os.Build
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bluebridge.bluebridgeapp.data.model.Location
import com.bluebridge.bluebridgeapp.data.model.ShortenedWellData
import kotlinx.coroutines.Job


@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapView(
    wells: List<ShortenedWellData>,
    userLocation: Location?
) {
    var webView: WebView? = null

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    webViewClient = WebViewClient()

                    // Load basic OpenStreetMap with markers for wells
                    val wellMarkers = wells.mapNotNull { well ->
                        val lat = well.getLatitude()
                        val lon = well.getLongitude()
                        if (lat != null && lon != null) {
                            """
                            L.marker([${lat}, ${lon}], {title: "${well.wellName}"})
                              .addTo(map)
                              .bindPopup("${well.wellName}<br>${well.wellWaterType}<br><button onclick='navigateToWell(${lat},${lon},\"${well.wellName}\")'>Navigate</button><button onclick='viewWellDetails(\"${well.espId}\")'>Details</button>");
                            """
                        } else null
                    }.joinToString("\n")

                    // Center point based on user location or first well
                    val centerLat = userLocation?.latitude ?: wells.firstOrNull()?.getLatitude() ?: 0.0
                    val centerLon = userLocation?.longitude ?: wells.firstOrNull()?.getLongitude() ?: 0.0

                    // Set up user location marker if available
                    val userMarker = if (userLocation != null) {
                        """
                        L.marker([${userLocation.latitude}, ${userLocation.longitude}], {
                            icon: L.divIcon({
                                className: 'user-location',
                                html: '<div style="background-color:blue;width:15px;height:15px;border-radius:50%;"></div>'
                            })
                        }).addTo(map).bindPopup("Your location");
                        """
                    } else ""

                    val html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.7.1/dist/leaflet.css" />
                        <script src="https://unpkg.com/leaflet@1.7.1/dist/leaflet.js"></script>
                        <style>
                            html, body { height: 100%; margin: 0; }
                            #map { height: 100%; width: 100%; }
                            .user-location { border-radius: 50%; }
                        </style>
                    </head>
                    <body>
                        <div id="map"></div>
                        <script>
                            var map = L.map('map').setView([${centerLat}, ${centerLon}], 10);
                            
                            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                            }).addTo(map);
                            
                            $wellMarkers
                            $userMarker
                            
                            // JavaScript function to communicate back to the app
                            function viewWellDetails(espId) {
                                window.AndroidInterface.onWellSelected(espId);
                            }
                            
                            function navigateToWell(lat, lon, name) {
                                window.AndroidInterface.navigateToWell(lat, lon, name);
                            }
                        </script>
                    </body>
                    </html>
                    """.trimIndent()

                    // Set up JavaScript interface for callback from WebView
//                    addJavascriptInterface(object : Any() {
//                        @JavascriptInterface
//                        fun onWellSelected(espId: String) {
//                            val well = wells.find { it.espId == espId }
//                            well?.let { onWellClicked(it as ShortenedWellData) }
//                        }

//                        @JavascriptInterface
//                        fun navigateToWell(lat: Double, lon: Double, name: String) {
//                            onNavigateToWell(lat, lon, name)
//                        }
//                    }, "AndroidInterface")

                    loadDataWithBaseURL("https://openstreetmap.org", html, "text/html", "UTF-8", null)
                    webView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}





// Helper functions to safely calculate water level information outside composables
fun calculateWaterLevelInfo(waterLevelStr: String, capacityStr: String): Pair<Float, Int>? {
    return try {
        val waterLevel = waterLevelStr.toFloatOrNull() ?: return null
        val capacity = capacityStr.toFloatOrNull() ?: return null
        if (capacity <= 0) return null

        val progress = (waterLevel / capacity).coerceIn(0f, 1f)
        val percentage = (waterLevel / capacity * 100).toInt()
        Pair(progress, percentage)
    } catch (_: Exception) {
        null
    }
}

fun calculateWaterLevelProgress(waterLevelStr: String, capacityStr: String): Float? {
    return try {
        val waterLevel = waterLevelStr.toFloatOrNull() ?: return null
        val capacity = capacityStr.toFloatOrNull() ?: return null
        if (capacity <= 0) return null

        (waterLevel / capacity).coerceIn(0f, 1f)
    } catch (_: Exception) {
        null
    }
}

@Composable
fun FiltersSection(
    selectedWaterType: String?,
    onWaterTypeSelected: (String?) -> Unit,
    selectedStatus: String?,
    onStatusSelected: (String?) -> Unit,
    capacityRange: ClosedRange<Int>?,
    onCapacityRangeChange: (ClosedRange<Int>?) -> Job,
    showNearbyOnly: Boolean,
    onNearbyOnlyChange: (Boolean) -> Unit
) {
    // Convert capacityRange to a float range for the RangeSlider
    val floatCapacityRange = capacityRange?.let { it.start.toFloat()..it.endInclusive.toFloat() } ?: 0f..0f
    Column(Modifier.padding(8.dp)) {
        Text("Filters", style = MaterialTheme.typography.titleMedium)
        // Water Type filters
        Row(Modifier.padding(vertical = 4.dp)) {
            FilterChip(
                selected = selectedWaterType == "Clean",
                onClick = { onWaterTypeSelected(if (selectedWaterType != "Clean") "Clean" else null)
                },
                label = { Text("Clean") }
            )
            Spacer(Modifier.width(4.dp))
            FilterChip(
                selected = selectedWaterType == "Grey",
                onClick = { onWaterTypeSelected(if (selectedWaterType != "Grey") "Grey" else null) },
                label = { Text("Grey") }
            )
        }

        // Status filters
        Row(Modifier.padding(vertical = 4.dp)) {
            FilterChip(
                selected = selectedStatus == "Active",
                onClick = { onStatusSelected(if (selectedStatus != "Active") "Active" else null) },
                label = { Text("Active") }
            )
            Spacer(Modifier.width(4.dp))
            FilterChip(
                selected = selectedStatus == "Maintenance",
                onClick = { onStatusSelected(if (selectedStatus != "Maintenance") "Maintenance" else null) },
                label = { Text("Maintenance") }
            )
            Spacer(Modifier.width(4.dp))
            FilterChip(
                selected = selectedStatus == "Inactive",
                onClick = { onStatusSelected(if (selectedStatus != "Inactive") "Inactive" else null) },
                label = { Text("Inactive") }
            )
        }

        //TODO: fix this: horrible type mismatch: fix
        // Capacity range
        Text("Capacity Range: ${capacityRange?.start?.toInt()} - ${capacityRange?.endInclusive?.toInt()}")
        RangeSlider(
            value = floatCapacityRange,
            onValueChange = { newRange -> onCapacityRangeChange(newRange.start.toInt()..newRange.endInclusive.toInt()) },
            valueRange = 0f..10000f, // Define the total possible range
            steps = 20
        )

        // Nearby filter
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = showNearbyOnly,
                onCheckedChange = onNearbyOnlyChange
            )
            Text("Show only nearby wells")
        }
    }
}

@Composable
fun StatusIndicator(status: String) {
    val color = when (status) {
        "Active" -> Color.Green
        "Maintenance" -> Color.Yellow
        "Inactive" -> Color.Red
        else -> Color.Gray
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(4.dp))
        Text(status, style = MaterialTheme.typography.bodySmall)
    }
}