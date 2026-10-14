package com.deudom.deudom.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deudom.deudom.util.ScoreCalculator
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ScoreGauge(
    score: Int,
    modifier: Modifier = Modifier.size(200.dp)
) {
    val animatedScore = remember { Animatable(0f) }
    
    LaunchedEffect(score) {
        animatedScore.animateTo(
            targetValue = score.toFloat(),
            animationSpec = tween(durationMillis = 1500)
        )
    }

    val maxScore = 100f
    val startAngle = 135f
    val sweepAngle = 270f
    
    val percentage = (animatedScore.value / maxScore).coerceIn(0f, 1f)
    
    // Color Logic
    val scoreColor = when {
        score >= 90 -> Color(0xFFFFD700) // Gold
        score >= 75 -> Color(0xFF4CAF50) // Green
        score >= 60 -> Color(0xFF2196F3) // Blue
        score >= 40 -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336) // Red
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val radius = width / 2.5f
            val center = Offset(width / 2, height / 2)
            
            // Background Arc
            drawArc(
                color = Color.LightGray.copy(alpha = 0.3f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = width * 0.1f, cap = StrokeCap.Round)
            )
            
            // Progress Arc
            drawArc(
                color = scoreColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle * percentage,
                useCenter = false,
                style = Stroke(width = width * 0.1f, cap = StrokeCap.Round)
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${animatedScore.value.toInt()}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = scoreColor
            )
            Text(
                text = ScoreCalculator.getScoreLabel(animatedScore.value.toInt()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
