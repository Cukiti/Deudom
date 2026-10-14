package com.deudom.deudom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.deudom.deudom.ui.theme.GoldPremium

@Composable
fun UserAvatar(name: String, photoUrl: String?, size: Dp = 40.dp) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(GoldPremium.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = name.take(1).uppercase(),
                color = GoldPremium,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.4).sp
            )
        }
    }
}
