package com.deudom.deudom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deudom.deudom.data.User
import com.deudom.deudom.ui.theme.GoldPremium
import com.deudom.deudom.util.ScoreCalculator

@Composable
fun ReliabilityDetailDialog(
    friendName: String,
    user: User,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Stars, null, tint = GoldPremium)
                Spacer(Modifier.width(8.dp))
                Text("Análisis de Confianza", fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Estás viendo el perfil financiero de $friendName. Estos datos son generados automáticamente por su historial en Deudom.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )

                // Big Score Meter
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("NIVEL DE FIABILIDAD", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = GoldPremium)
                        Text(
                            "${user.score}%", 
                            style = MaterialTheme.typography.headlineLarge, 
                            fontWeight = FontWeight.Black,
                            color = when {
                                user.score >= 80 -> Color(0xFF4CAF50)
                                user.score >= 60 -> GoldPremium
                                else -> Color(0xFFF44336)
                            }
                        )
                        Text(
                            ScoreCalculator.getScoreLabel(user.score).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Stats breakdown
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatRow(
                        icon = Icons.Default.CheckCircle,
                        label = "Pagos a tiempo o anticipados",
                        value = user.goodPayCount.toString(),
                        color = Color(0xFF4CAF50)
                    )
                    StatRow(
                        icon = Icons.Default.Error,
                        label = "Pagos fuera de fecha",
                        value = user.badPayCount.toString(),
                        color = Color(0xFFF44336)
                    )
                }

                // Jarvis Advice
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = GoldPremium.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPremium.copy(alpha = 0.2f))
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = GoldPremium, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = getJarvisAdvice(user.score),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = Color.Black)
            ) {
                Text("CERRAR", fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun StatRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
        Text(value, fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

fun getJarvisAdvice(score: Int): String {
    return when {
        score >= 90 -> "Sugerencia de Jarvis: Esta persona es extremadamente confiable. Es un honor prestarle."
        score >= 75 -> "Sugerencia de Jarvis: Un historial sólido. Cumple con sus compromisos regularmente."
        score >= 60 -> "Sugerencia de Jarvis: Usuario cumplidor, aunque ha tenido algún contratiempo menor."
        score >= 40 -> "Sugerencia de Jarvis: Precaución. Ha tenido varios retrasos en sus pagos."
        else -> "Sugerencia de Jarvis: Riesgo elevado. Su historial de cumplimiento es muy bajo."
    }
}