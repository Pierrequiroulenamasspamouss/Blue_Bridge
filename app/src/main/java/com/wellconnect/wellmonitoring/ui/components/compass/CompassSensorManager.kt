package com.wellconnect.wellmonitoring.ui.components.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * Hook to handle compass sensor data
 */
@Composable
fun rememberCompassSensor(context: Context, smoothingFactor: Float = 0.03f): State<Float> {
    val azimuthState = remember { mutableStateOf(0f) }
    val filteredAzimuth = remember { mutableStateOf(0f) }
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    
    // Create smoothing buffer for additional filtering
    val azimuthBuffer = remember { FloatArray(15) { 0f } } // Buffer for last 15 readings
    var bufferIndex = remember { mutableStateOf(0) }
    
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
                    
                    // Step 1: Apply low-pass filter for initial smoothing
                    filteredAzimuth.value = smoothingFactor * newAzimuth + (1 - smoothingFactor) * filteredAzimuth.value
                    
                    // Step 2: Add to circular buffer for additional smoothing
                    azimuthBuffer[bufferIndex.value] = filteredAzimuth.value
                    bufferIndex.value = (bufferIndex.value + 1) % azimuthBuffer.size
                    
                    // Step 3: Calculate moving average from buffer
                    var sum = 0f
                    for (value in azimuthBuffer) {
                        sum += value
                    }
                    val finalAzimuth = sum / azimuthBuffer.size
                    
                    // Apply final value
                    azimuthState.value = finalAzimuth
                }
            }
        }
    }

    // Register and unregister sensors
    DisposableEffect(sensorManager) {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        
        if (accelerometer != null && magnetometer != null) {
            sensorManager.registerListener(
                sensorEventListener,
                accelerometer,
                SensorManager.SENSOR_DELAY_UI // Use slower UI rate instead of GAME for less jitter
            )
            
            sensorManager.registerListener(
                sensorEventListener,
                magnetometer,
                SensorManager.SENSOR_DELAY_UI // Use slower UI rate instead of GAME for less jitter
            )
        }
        
        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }
    
    return azimuthState
} 