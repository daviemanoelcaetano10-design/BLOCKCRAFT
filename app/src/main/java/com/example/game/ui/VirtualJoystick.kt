package com.example.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun VirtualJoystick(
    onMove: (x: Float, y: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }
    val maxRadiusPx = 130f

    Box(
        modifier = modifier
            .size(150.dp)
            .testTag("virtual_joystick")
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val delta = offset - center
                        val dist = delta.getDistance()
                        val clamped = if (dist > maxRadiusPx) delta * (maxRadiusPx / dist) else delta
                        thumbOffset = clamped
                        onMove(clamped.x / maxRadiusPx, -clamped.y / maxRadiusPx)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = thumbOffset + dragAmount
                        val dist = newOffset.getDistance()
                        val clamped = if (dist > maxRadiusPx) newOffset * (maxRadiusPx / dist) else newOffset
                        thumbOffset = clamped
                        onMove(clamped.x / maxRadiusPx, -clamped.y / maxRadiusPx)
                    },
                    onDragEnd = {
                        thumbOffset = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDragCancel = {
                        thumbOffset = Offset.Zero
                        onMove(0f, 0f)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(150.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)

            // Outer Base Ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x33FFFFFF), Color(0x661A2035)),
                    center = center,
                    radius = maxRadiusPx + 15f
                ),
                radius = maxRadiusPx + 10f,
                center = center
            )
            drawCircle(
                color = Color(0x8800E5FF),
                radius = maxRadiusPx + 10f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Inner directional cross markers
            val crossLength = 15f
            drawLine(Color(0x55FFFFFF), center + Offset(-maxRadiusPx + 5f, 0f), center + Offset(-maxRadiusPx + 5f + crossLength, 0f), strokeWidth = 2f)
            drawLine(Color(0x55FFFFFF), center + Offset(maxRadiusPx - 5f, 0f), center + Offset(maxRadiusPx - 5f - crossLength, 0f), strokeWidth = 2f)
            drawLine(Color(0x55FFFFFF), center + Offset(0f, -maxRadiusPx + 5f), center + Offset(0f, -maxRadiusPx + 5f + crossLength), strokeWidth = 2f)
            drawLine(Color(0x55FFFFFF), center + Offset(0f, maxRadiusPx - 5f), center + Offset(0f, maxRadiusPx - 5f - crossLength), strokeWidth = 2f)

            // Thumb knob
            val thumbCenter = center + thumbOffset
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00E5FF), Color(0xFF0D47A1)),
                    center = thumbCenter,
                    radius = 35f
                ),
                radius = 32f,
                center = thumbCenter
            )
            drawCircle(
                color = Color.White,
                radius = 32f,
                center = thumbCenter,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
