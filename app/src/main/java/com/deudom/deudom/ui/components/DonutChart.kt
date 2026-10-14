package com.deudom.deudom.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap

@Composable
fun DonutChart(proportions: List<Float>, colors: List<Color>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        var startAngle = -90f
        proportions.forEachIndexed { index, prop ->
            val sweepAngle = prop * 360f
            drawArc(
                color = colors[index],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 40f, cap = StrokeCap.Round)
            )
            startAngle += sweepAngle
        }
    }
}
