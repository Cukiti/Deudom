package com.deudom.deudom.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deudom.deudom.ui.theme.GoldPremium

@Composable
fun ReliabilityInfo(good: Int, bad: Int) {
    val total = good + bad
    val percentage = if (total == 0) 100 else (good.toFloat() / total * 100).toInt()
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            // Percentage Only
            Text("$percentage%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = if (percentage >= 80) Color(0xFF4CAF50) else if (percentage >= 50) GoldPremium else Color.Red)
        }
        Text("Tasa de Cumplimiento", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        
        Spacer(Modifier.height(12.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                Icon(Icons.Default.ThumbUp, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(text = "$good Pagos", style = MaterialTheme.typography.labelMedium, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
            }
            Box(Modifier.width(1.dp).height(12.dp).background(MaterialTheme.colorScheme.outlineVariant))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                Icon(Icons.Default.ThumbDown, null, tint = Color(0xFFF44336), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(text = "$bad Fallos", style = MaterialTheme.typography.labelMedium, color = Color(0xFFF44336), fontWeight = FontWeight.Bold)
            }
        }
    }
}
