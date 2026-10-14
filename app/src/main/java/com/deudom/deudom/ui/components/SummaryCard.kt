package com.deudom.deudom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import com.deudom.deudom.util.CurrencyManager

@Composable
fun SummaryCard(title: String, amount: Double, color: Color, priv: Boolean = false, currency: String = "RD$") {
    val gradientColors = if (title == "Me deben") {
        listOf(color.copy(alpha = 0.08f), color.copy(alpha = 0.02f))
    } else {
        listOf(color.copy(alpha = 0.08f), color.copy(alpha = 0.02f))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Box(
            modifier = Modifier
                .background(Brush.verticalGradient(gradientColors))
                .padding(14.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    @Suppress("DEPRECATION")
                    Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    AutoResizedText(
                        text = if (priv) "****" else CurrencyManager.format(amount, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = color
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (title == "Me deben") Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    null,
                    tint = color.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
