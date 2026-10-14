package com.deudom.deudom.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f),
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    this.background(brush)
}

@Composable
fun DashboardSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.size(width = 150.dp, height = 32.dp).clip(RoundedCornerShape(8.dp)).shimmerEffect())
            Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(16.dp)).shimmerEffect())
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Summary Cards
        Box(modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(12.dp)).shimmerEffect())
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(12.dp)).shimmerEffect())
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Pulse Card
        Box(modifier = Modifier.size(width = 200.dp, height = 24.dp).clip(RoundedCornerShape(8.dp)).shimmerEffect())
        Spacer(modifier = Modifier.height(12.dp))
        Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(24.dp)).shimmerEffect())
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Quick Access
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).shimmerEffect())
            Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).shimmerEffect())
            Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).shimmerEffect())
            Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).shimmerEffect())
        }
    }
}
