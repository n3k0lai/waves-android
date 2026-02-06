package sh.comfy.waves.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun FocalPointPicker(
    focalX: Float,
    focalY: Float,
    onFocalPointChanged: (x: Float, y: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val crosshairColor = MaterialTheme.colorScheme.primary

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val x = (offset.x / size.width).coerceIn(0f, 1f)
                        val y = (offset.y / size.height).coerceIn(0f, 1f)
                        onFocalPointChanged(x, y)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val x = (change.position.x / size.width).coerceIn(0f, 1f)
                        val y = (change.position.y / size.height).coerceIn(0f, 1f)
                        onFocalPointChanged(x, y)
                    }
                }
        ) {
            val centerX = focalX * size.width
            val centerY = focalY * size.height
            val radius = 16.dp.toPx()
            val lineLength = 24.dp.toPx()
            val strokeWidth = 2.dp.toPx()

            // Outer circle
            drawCircle(
                color = crosshairColor,
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = strokeWidth)
            )

            // Inner dot
            drawCircle(
                color = crosshairColor,
                radius = 3.dp.toPx(),
                center = Offset(centerX, centerY),
            )

            // Crosshair lines
            drawLine(
                color = crosshairColor,
                start = Offset(centerX - lineLength, centerY),
                end = Offset(centerX - radius, centerY),
                strokeWidth = strokeWidth,
            )
            drawLine(
                color = crosshairColor,
                start = Offset(centerX + radius, centerY),
                end = Offset(centerX + lineLength, centerY),
                strokeWidth = strokeWidth,
            )
            drawLine(
                color = crosshairColor,
                start = Offset(centerX, centerY - lineLength),
                end = Offset(centerX, centerY - radius),
                strokeWidth = strokeWidth,
            )
            drawLine(
                color = crosshairColor,
                start = Offset(centerX, centerY + radius),
                end = Offset(centerX, centerY + lineLength),
                strokeWidth = strokeWidth,
            )

            // Semi-transparent overlay to show crop region won't include edges
            // (visual hint that content outside the viewport is cropped)
            drawRect(
                color = Color.Black.copy(alpha = 0.15f),
                size = size,
            )
        }
    }
}
