package com.deudom.deudom.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AchievementBadge(title: String, desc: String, isUnlocked: Boolean, color: Color) {
    Card(
        modifier = Modifier.width(140.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) color.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = if (isUnlocked) BorderStroke(1.dp, color) else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.WorkspacePremium,
                null,
                tint = if (isUnlocked) color else Color.Gray,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
            Text(desc, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.secondary)
        }
    }
}
