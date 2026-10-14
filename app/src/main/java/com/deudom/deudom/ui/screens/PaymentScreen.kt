package com.deudom.deudom.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deudom.deudom.data.Loan
import com.deudom.deudom.ui.theme.GoldPremium
import com.deudom.deudom.ui.components.OutlinedCardCustom
import com.deudom.deudom.util.CurrencyManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    loan: Loan,
    onDismiss: () -> Unit,
    onConfirmPayment: (Double, String?) -> Unit,
    onSettleFull: () -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var descriptionStr by remember { mutableStateOf("") }
    val remaining = loan.amount - loan.paidAmount
    val amountVal = CurrencyManager.parseAmount(amountStr) ?: 0.0
    val isSettleMode = amountVal >= (remaining - 0.01) && remaining > 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar Abono", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = GoldPremium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 16.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(modifier = Modifier.navigationBarsPadding().padding(16.dp)) {
                    Button(
                        onClick = {
                            if (isSettleMode || amountVal >= remaining) onSettleFull()
                            else onConfirmPayment(amountVal, descriptionStr.takeIf { it.isNotBlank() })
                        },
                        enabled = amountVal > 0,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSettleMode) GoldPremium else MaterialTheme.colorScheme.primary,
                            contentColor = if (isSettleMode) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (isSettleMode) "SALDAR DEUDA COMPLETA" else "CONFIRMAR ABONO",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Info
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Pendiente Actual", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        Text("${loan.currency} ${String.format("%.2f", remaining)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    if (isSettleMode) {
                        Icon(Icons.Default.CheckCircle, null, tint = GoldPremium)
                    }
                }
            }

            Spacer(Modifier.height(48.dp))

            // Big Input
            Text("Monto a abonar", style = MaterialTheme.typography.labelMedium, color = GoldPremium)
            Spacer(Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text(loan.currency, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                Spacer(Modifier.width(12.dp))
                BasicTextField(
                    value = amountStr,
                    onValueChange = { input -> 
                        if (input.length <= 10) {
                            val filtered = input.filter { it.isDigit() || it == '.' }
                            amountStr = filtered
                        }
                    },
                    textStyle = TextStyle(
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isSettleMode) GoldPremium else MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    cursorBrush = SolidColor(GoldPremium),
                    modifier = Modifier.width(IntrinsicSize.Min).defaultMinSize(minWidth = 100.dp)
                )
                if (amountStr.isNotEmpty()) {
                    IconButton(onClick = { amountStr = "" }) {
                        Icon(Icons.AutoMirrored.Filled.Backspace, null, tint = MaterialTheme.colorScheme.error.copy(0.7f))
                    }
                } else {
                    Text("0.00", fontSize = 56.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(0.1f))
                }
            }

            Spacer(Modifier.height(32.dp))

            // Description Input
            OutlinedTextField(
                value = descriptionStr,
                onValueChange = { descriptionStr = it },
                label = { Text("Descripción (Opcional)", color = MaterialTheme.colorScheme.secondary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPremium,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                singleLine = true
            )

            Spacer(Modifier.height(32.dp))

            // Settle Button (Quick Action)
            if (!isSettleMode) {
                OutlinedCardCustom(
                    onClick = { amountStr = String.format(java.util.Locale.US, "%.2f", remaining) },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, GoldPremium)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✨ Saldar Todo (${loan.currency} ${String.format("%.2f", remaining)})", fontWeight = FontWeight.Bold, color = GoldPremium)
                    }
                }
                Spacer(Modifier.height(24.dp))
            } else {
                Text("¡Estás pagando el total!", color = GoldPremium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))
            }

            // Quick Amounts
            Text("Agregar rápido", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val amounts = listOf(100, 200, 500, 1000, 2000, 5000)
                amounts.forEach { amt ->
                    Surface(
                        onClick = { 
                            val current = CurrencyManager.parseAmount(amountStr) ?: 0.0
                            val newVal = (current + amt).coerceAtMost(remaining)
                            amountStr = String.format(java.util.Locale.US, "%.2f", newVal)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            "+$amt", 
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}
