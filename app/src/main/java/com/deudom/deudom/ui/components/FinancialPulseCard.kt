package com.deudom.deudom.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deudom.deudom.R
import com.deudom.deudom.data.User
import com.deudom.deudom.ui.theme.GoldPremium
import com.deudom.deudom.util.CurrencyManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

@Composable
fun FinancialPulseCard(
    user: User?,
    privacy: Boolean,
    onHistoryClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onEditWalletClick: () -> Unit // New parameter
) {
    if (user == null) return

    // 1. Calculations
    val currency = user.baseCurrency
    val walletCash = user.walletCash
    
    // Health Color Logic - Simple version based on wallet amount
    val healthColor = when {
        walletCash < 1000 -> Color(0xFFF44336) // Red
        walletCash < 5000 -> GoldPremium // Yellow/Gold
        else -> Color(0xFF4CAF50) // Green
    }
    
    val healthLabel = when {
        walletCash < 1000 -> "Crítico"
        walletCash < 5000 -> "Regular"
        else -> "Saludable"
    }

    var showFullAmount by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, GoldPremium.copy(0.2f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header: Balance Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).clickable { showFullAmount = !showFullAmount }) {
                    Text(
                        "DINERO DISPONIBLE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = 0.5.sp
                    )
                    
                    val displayAmount = if (privacy) "****" else {
                        if (showFullAmount) {
                            CurrencyManager.format(walletCash, currency)
                        } else {
                            CurrencyManager.formatAbbreviated(walletCash, currency)
                        }
                    }
                    
                    AutoResizedText(
                        text = displayAmount,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Status Badge
                Surface(
                    color = healthColor.copy(0.1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, healthColor.copy(0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(healthColor))
                        Spacer(Modifier.width(6.dp))
                        Text(healthLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = healthColor)
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Health indicator bar
            val progress = (walletCash / 10000.0).toFloat().coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = healthColor,
                trackColor = healthColor.copy(0.1f)
            )
            
            Spacer(Modifier.height(16.dp))

            // Actions
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Edit Wallet Button
                OutlinedButton(
                    onClick = onEditWalletClick,
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(0.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(0.2f))
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Editar Cartera", tint = GoldPremium, modifier = Modifier.size(20.dp))
                }
                
                // History Button (Icon Only to prevent text overflow)
                OutlinedButton(
                    onClick = onHistoryClick,
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(0.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(0.2f))
                ) {
                    Icon(Icons.Default.History, contentDescription = "Ver Historial", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                }
                
                // Add Expense Button (Primary Action)
                Button(
                    onClick = onAddExpenseClick,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPremium)
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Registrar Gasto", color = Color.Black, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                }
            }
        }
    }
}
