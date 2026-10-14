package com.deudom.deudom.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deudom.deudom.ui.theme.GoldPremium

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun QuickAccessSection(onLendClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "")

    Button(
        onClick = onLendClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale),
        colors = ButtonDefaults.buttonColors(
            containerColor = GoldPremium,
            contentColor = Color.Black
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            text = "NUEVO PRÉSTAMO",
            style = TextStyle(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                letterSpacing = 1.sp
            )
        )
    }
}
