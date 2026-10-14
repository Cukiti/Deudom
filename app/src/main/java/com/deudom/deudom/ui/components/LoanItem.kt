package com.deudom.deudom.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deudom.deudom.data.Loan
import com.deudom.deudom.data.LoanStatus
import com.deudom.deudom.ui.theme.GoldPremium
import com.deudom.deudom.util.CategoryProvider
import java.text.SimpleDateFormat
import java.util.*
import com.deudom.deudom.util.ReminderUtils
import com.deudom.deudom.util.ReminderTone
import kotlinx.coroutines.launch

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.ui.text.style.TextOverflow
import com.deudom.deudom.ui.components.AutoResizedText

@Composable
fun LoanItem(
    loan: Loan,
    isLender: Boolean,
    privacy: Boolean = false,
    onClick: () -> Unit = {},
    onAction: (() -> Unit)? = null
) {
    val paid = loan.status == LoanStatus.PAID
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = if (paid) MaterialTheme.colorScheme.surfaceVariant.copy(0.3f) else MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (paid) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(
                    name = if (isLender) loan.borrowerName else loan.lenderName,
                    photoUrl = if (isLender) loan.borrowerPhotoUrl else loan.lenderPhotoUrl,
                    size = 50.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isLender) loan.borrowerName else loan.lenderName,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = if (paid) Color.Gray else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(CategoryProvider.getEmoji(loan.category), fontSize = 14.sp)
                    }
                    Text(
                        loan.description, 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(0.4f)) {
                    AutoResizedText(
                        text = if (privacy) "****" else "${loan.currency} ${String.format("%.2f", loan.amount - loan.paidAmount)}",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                        color = if (paid) Color.Gray else if (isLender) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                    if (paid) {
                        Text("PAGADO TODO", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress and Status Row
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                val statusText: String
                val statusColor: Color
                
                when (loan.status) {
                    LoanStatus.PAID -> {
                        statusText = "PAGADO"
                        statusColor = Color(0xFF4CAF50)
                    }
                    LoanStatus.REQUESTED -> {
                        statusText = if (isLender) "ESPERANDO ACEPTACIÓN" else "SOLICITUD PENDIENTE"
                        statusColor = Color(0xFF2196F3)
                    }
                    LoanStatus.REJECTED -> {
                        statusText = "RECHAZADO"
                        statusColor = Color(0xFF9E9E9E)
                    }
                    else -> {
                        statusText = if (isLender) "A MI FAVOR" else "POR PAGAR"
                        statusColor = if (isLender) Color(0xFF4CAF50) else Color(0xFFF44336)
                    }
                }
                
                Surface(
                    color = statusColor.copy(0.12f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, statusColor.copy(0.2f))
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = statusColor,
                        letterSpacing = 0.5.sp
                    )
                }

                if (!paid && loan.paidAmount > 0 && loan.status != LoanStatus.REQUESTED && loan.status != LoanStatus.REJECTED) {
                    LinearProgressIndicator(
                        progress = { (loan.paidAmount / loan.amount).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                            .height(6.dp)
                            .clip(CircleShape),
                        color = GoldPremium,
                        trackColor = GoldPremium.copy(0.1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                if (loan.dueDate != null && !paid && loan.status != LoanStatus.REQUESTED && loan.status != LoanStatus.REJECTED) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Event, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(loan.dueDate!!)),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (loan.dueDate!! < System.currentTimeMillis()) Color.Red else MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            // Action Buttons
            if (!paid && loan.status == LoanStatus.PENDING) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLender) {
                        val context = LocalContext.current
                        TextButton(
                            onClick = {
                                val msg = ReminderUtils.generateMessage(loan, ReminderTone.FRIENDLY)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, msg)
                                }
                                context.startActivity(Intent.createChooser(intent, "Recordar pago"))
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Message, null, modifier = Modifier.size(16.dp), tint = Color(0xFF25D366))
                            Spacer(Modifier.width(4.dp))
                            Text("RECORDAR", fontSize = 11.sp, color = Color(0xFF25D366), fontWeight = FontWeight.Bold)
                        }
                        
                        val interactionAbonar = remember { MutableInteractionSource() }
                        val isPressedAbonar by interactionAbonar.collectIsPressedAsState()
                        val scaleAbonar by animateFloatAsState(if (isPressedAbonar) 0.94f else 1f, label = "")

                        Button(
                            onClick = { onAction?.invoke() },
                            interactionSource = interactionAbonar,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPremium),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
                            modifier = Modifier.height(36.dp).graphicsLayer(scaleX = scaleAbonar, scaleY = scaleAbonar),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("ABONAR", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.ExtraBold)
                        }
                    } else {
                        val interactionVer = remember { MutableInteractionSource() }
                        val isPressedVer by interactionVer.collectIsPressedAsState()
                        val scaleVer by animateFloatAsState(if (isPressedVer) 0.94f else 1f, label = "")

                        Button(
                            onClick = { onClick() },
                            interactionSource = interactionVer,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPremium.copy(0.1f), contentColor = GoldPremium),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
                            modifier = Modifier.height(36.dp).graphicsLayer(scaleX = scaleVer, scaleY = scaleVer),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, GoldPremium.copy(0.5f))
                        ) {
                            Text("VER ABONOS", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            } else if (!isLender && loan.status == LoanStatus.REQUESTED) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                if (com.deudom.deudom.data.FirestoreService.updateLoanStatus(loan, LoanStatus.REJECTED)) {
                                    // Successfully rejected
                                }
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) {
                        Text("RECHAZAR", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                if (com.deudom.deudom.data.FirestoreService.updateLoanStatus(loan, LoanStatus.PENDING)) {
                                    // Successfully accepted
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp)
                    ) {
                        Text("ACEPTAR", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
