package com.deudom.deudom.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.random.Random

data class ConfettiState(
    val color: Color,
    var x: Float,
    var y: Float,
    var velocityX: Float,
    var velocityY: Float,
    var rotation: Float,
    var rotationSpeed: Float,
    val size: Float
)

@Composable
fun ConfettiView(
    modifier: Modifier = Modifier,
    onFinished: () -> Unit
) {
    // Generate particles once
    val particles = remember {
        List(150) {
            ConfettiState(
                color = listOf(
                    Color(0xFFFFD700), // Gold
                    Color(0xFF4CAF50), // Green
                    Color(0xFF2196F3), // Blue
                    Color(0xFFE91E63), // Pink
                    Color(0xFF9C27B0)  // Purple
                ).random(),
                x = Random.nextFloat(), // 0..1 relative width
                y = -0.2f - Random.nextFloat() * 0.5f, // Start well above screen
                velocityX = (Random.nextFloat() - 0.5f) * 0.01f,
                velocityY = Random.nextFloat() * 0.015f + 0.005f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 15f,
                size = Random.nextFloat() * 15f + 8f
            )
        }
    }

    // This state drives the recomposition of Canvas
    var time by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        val startTime = withFrameMillis { it }
        while (true) {
            val currentTime = withFrameMillis { it }
            val elapsed = (currentTime - startTime)
            time = elapsed
            
            if (elapsed > 4000) { // End after 4 seconds
                onFinished()
                break
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // On each draw call (triggered by 'time' change or system draw), update physics
        particles.forEach { p ->
            // Simple physics update per frame
            p.x += p.velocityX
            p.y += p.velocityY
            p.velocityY += 0.0003f // Gravity
            p.rotation += p.rotationSpeed
            
            // Add some sway
            p.x += kotlin.math.sin(time / 200f + p.size) * 0.002f

            if (p.y < 1.2f) { // Only draw if within bounds (plus margin)
                withTransform({
                    translate(left = p.x * width, top = p.y * height)
                    rotate(p.rotation)
                }) {
                    drawRect(
                        color = p.color,
                        topLeft = Offset(-p.size / 2, -p.size / 2),
                        size = Size(p.size, p.size)
                    )
                }
            }
        }
    }
}
