package com.bluebridge.bluebridgeapp.ui.components.compass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Composable that renders the compass with arrow pointing to target direction
 */
@Composable
fun CompassView(
    currentRotation: Float,
    isTargetMode: Boolean,
    isInTargetZone: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(200.dp)
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
                    oval = Rect(
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
                    oval = Rect(
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

            // Create filled area path
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
                    rect = Rect(
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
                    rect = Rect(
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
                style = Fill
            )

            // Draw the outline
            drawPath(
                path = indicatorPath,
                color = if (isInTargetZone) Color.Green.copy(alpha = 0.5f) else Color.Red.copy(alpha = 0.5f),
                style = Stroke(width = 2f)
            )

            // Draw direction arrow
            rotate(currentRotation) {
                // Draw arrow pointing to target
                val arrowLength = radius * 0.8f
                val arrowWidth = radius * 0.15f
                
                // Determine arrow color based on whether we're pointing north or to a destination
                val arrowColor = if (!isTargetMode) {
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