@file:Suppress("DEPRECATION")

package com.bluebridgeapp.bluebridge.ui.components


import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SignalWifiOff
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import com.bluebridgeapp.bluebridge.R
import com.bluebridgeapp.bluebridge.data.model.Location
import com.bluebridgeapp.bluebridge.utils.PasswordStrength
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.Date


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
        label = { Text( "$label *")    },
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
                    contentDescription = if (isVisible) stringResource(id = R.string.hide_password) else stringResource(id = R.string.show_password)
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
                    stringResource(id = R.string.password_strength_very_weak) -> 0.2f
                    stringResource(id = R.string.password_strength_weak) -> 0.4f
                    stringResource(id = R.string.password_strength_medium) -> 0.6f
                    stringResource(id = R.string.password_strength_strong) -> 0.8f
                    stringResource(id = R.string.password_strength_very_strong) -> 1f
                    else -> 0f
                },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                color = it.color
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


@Composable
fun NameField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
    )
}

@Composable
fun EmailField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(id = R.string.email_label) + " *") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        )
    )
}

@Composable
fun PhoneField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.all { c -> c.isDigit() || c == '+' }) onValueChange(it) },
        label = { Text(stringResource(id = R.string.phone_number_label)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Next
        )
    )
}


@SuppressLint("ClickableViewAccessibility")
@Composable
fun MiniMap(
    currentLocation: Location? = null,
    selectedLocation: Location? = null,
    onLocationSelected: (Location) -> Unit,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    val dateFormat = remember {
        // Ensure this format is compatible with your backend or how you parse it elsewhere.
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(250.dp)) {
            AndroidView(
                factory = { ctx ->
                    Configuration.getInstance().load(context, context.getSharedPreferences("osm", 0))
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        minZoomLevel = 2.0
                        maxZoomLevel = 19.0
                        controller.setZoom(12.0)

                        currentLocation?.let { loc ->
                            Marker(this).apply {
                                position = GeoPoint(loc.latitude, loc.longitude)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                title = context.getString(R.string.your_location)
                                icon = context.getDrawable(R.drawable.ic_location_pin)?.let { drawable ->
                                    val bitmap = createBitmap(
                                        drawable.intrinsicWidth,
                                        drawable.intrinsicHeight
                                    )
                                    drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
                                    drawable.draw(android.graphics.Canvas(bitmap))
                                    bitmap.toDrawable(context.resources)
                                }
                                icon = icon
                            }//.also { addMarker(it) } //TODO: check if this removal was critical or not
                        }

                        setOnTouchListener { _, event ->
                            if (event.action == android.view.MotionEvent.ACTION_UP) {
                                val geoPoint = projection.fromPixels(
                                    event.x.toInt(),
                                    event.y.toInt()
                                ) as GeoPoint
                                onLocationSelected(
                                    Location(
                                        geoPoint.latitude,
                                        geoPoint.longitude,
                                        dateFormat.format(Date())
                                    )
                                )
                            }
                            false
                        }
                        mapView = this
                    }
                },
                update = { mapView ->
                    selectedLocation?.let { loc ->
                        mapView.overlays.removeIf { it is Marker && it.title == context.getString(R.string.selected_location) }
                        Marker(mapView).apply {
                            position = GeoPoint(loc.latitude, loc.longitude)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = context.getString(R.string.selected_location)
                            icon = context.getDrawable(R.drawable.ic_location_pin)?.let { drawable ->
                                val bitmap = createBitmap(
                                    drawable.intrinsicWidth,
                                    drawable.intrinsicHeight
                                )
                                drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
                                drawable.draw(android.graphics.Canvas(bitmap))
                                bitmap.toDrawable(context.resources)
                            }
                            icon = icon
                        }.also { mapView.overlays.add(it) }
                        mapView.invalidate()
                    }
                }
            )

            FloatingActionButton(
                onClick = {
                    currentLocation?.let { loc ->
                        mapView?.controller?.animateTo(GeoPoint(loc.latitude, loc.longitude))
                        onLocationSelected(loc.copy(
                            lastUpdated = dateFormat.format(Date())
                            )
                        )
                    }
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(40.dp),
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(Icons.Default.LocationOn, stringResource(id = R.string.current_location))
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { mapView?.onDetach() }
    }
}

// Extension function for Bitmap to Drawable conversion
fun Bitmap.toDrawable(resources: android.content.res.Resources): android.graphics.drawable.Drawable {
    return android.graphics.drawable.BitmapDrawable(resources, this)
}


@Composable
fun TextComponent(text: String, fontSize: androidx.compose.ui.unit.TextUnit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize),
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

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

@Composable
fun TopBar(topBarMessage: String,isIcon : Boolean = true,iconId : Int = R.drawable.app_logo) {
    Row(modifier = Modifier.fillMaxWidth().padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = topBarMessage,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 30.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.weight(1f)) // puts the following element on the extreme right

        if (isIcon) {
            Image(
                modifier = Modifier.size(80.dp),
                painter = painterResource(id = iconId),
                contentDescription = stringResource(id = R.string.logo_description)
            )
        }
    }
}

@Composable
fun OfflineBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.SignalWifiOff,
                contentDescription = stringResource(id = R.string.offline_icon_description),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                stringResource(id = R.string.offline_banner_message),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
