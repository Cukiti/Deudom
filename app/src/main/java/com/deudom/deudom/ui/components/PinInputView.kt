package com.deudom.deudom.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deudom.deudom.ui.theme.GoldPremium
import com.deudom.deudom.util.HapticManager

import androidx.compose.material.icons.filled.Fingerprint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PinInputView(
    title: String = "Ingresa tu PIN",
    onPinComplete: (String) -> Unit,
    onBiometricClick: (() -> Unit)? = null
) {
    var pin by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = GoldPremium)
        Spacer(Modifier.height(32.dp))

        // Dots
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { index ->
                val filled = index < pin.length
                val scale by animateFloatAsState(if (filled) 1.2f else 1f, label = "scale")
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(if (filled) GoldPremium else MaterialTheme.colorScheme.outline.copy(0.3f))
                )
            }
        }

        Spacer(Modifier.height(48.dp))

        // Keypad
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("BIO", "0", "DEL")
        )

        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))
                                .clickable {
                                    HapticManager.vibrateImpact(context)
                                    when (key) {
                                        "DEL" -> if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                        "BIO" -> onBiometricClick?.invoke()
                                        else -> {
                                            if (pin.length < 4) {
                                                val newPin = pin + key
                                                pin = newPin
                                                if (newPin.length == 4) {
                                                    scope.launch {
                                                        delay(150)
                                                        onPinComplete(newPin)
                                                        pin = ""
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when (key) {
                                "DEL" -> Icon(Icons.AutoMirrored.Filled.Backspace, null, tint = MaterialTheme.colorScheme.onSurface)
                                "BIO" -> if (onBiometricClick != null) Icon(androidx.compose.material.icons.Icons.Default.Fingerprint, null, tint = GoldPremium)
                                else -> Text(key, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}
