package com.deudom.deudom.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.koin.androidx.compose.koinViewModel
import coil.compose.AsyncImage
import com.deudom.deudom.util.ReminderTone
import com.deudom.deudom.util.ReminderUtils
import com.deudom.deudom.R
import com.deudom.deudom.data.Loan
import com.deudom.deudom.data.LoanStatus
import com.deudom.deudom.viewmodel.LoanViewModel
import com.deudom.deudom.ui.components.OutlinedCardCustom
import com.deudom.deudom.ui.components.ChatPanel
import com.deudom.deudom.ui.components.AutoResizedText
import com.deudom.deudom.ui.theme.GoldPremium
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import com.deudom.deudom.util.ReviewPrompt
import com.deudom.deudom.util.ReviewDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(
    loan: Loan, 
    userPhone: String, 
    privacy: Boolean, 
    onDismiss: () -> Unit, 
    onRef: () -> Unit,
    viewModel: LoanViewModel = koinViewModel()
) {
    val isLender = loan.lenderPhone == userPhone
    var showPaymentScreen by remember { mutableStateOf(false) }
    var showRemind by remember { mutableStateOf(false) }
    var previewImage by remember { mutableStateOf<String?>(null) }
    var showChat by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val statusColor = when (loan.status) {
        LoanStatus.PAID -> Color(0xFF4CAF50)
        LoanStatus.OVERDUE -> Color(0xFFF44336)
        LoanStatus.REQUESTED -> Color(0xFF2196F3)
        else -> GoldPremium
    }
    
    val statusText = when (loan.status) {
        LoanStatus.PAID -> "PAGADO"
        LoanStatus.OVERDUE -> "ATRASADO"
        LoanStatus.REQUESTED -> "SOLICITADO"
        else -> "ACTIVO"
    }

    if (showPaymentScreen) {
        PaymentScreen(
            loan = loan,
            onDismiss = { showPaymentScreen = false },
            onConfirmPayment = { amt, desc ->
                viewModel.registerPayment(loan, amt, desc)
                onRef()
                showPaymentScreen = false
                
                ReviewPrompt.incrementInteraction(context)
                scope.launch { snackbarHostState.showSnackbar("¡Pago Registrado! +50 Cukitis \uD83D\uDC8E") }
                
                val remaining = (loan.amount - loan.paidAmount - amt).coerceAtLeast(0.0)
                var receiptText = "Comprobante de Pago - Deudom\n\nMonto: ${loan.currency} ${String.format("%.2f", amt)}\nFecha: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}\n\nSaldo Restante: ${loan.currency} ${String.format("%.2f", remaining)}"
                if (!desc.isNullOrBlank()) receiptText += "\nNota: $desc"
                
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, receiptText)
                }
                context.startActivity(Intent.createChooser(sendIntent, "Compartir comprobante"))
            },
            onSettleFull = {
                viewModel.settleDebt(loan)
                onRef()
                showPaymentScreen = false
                
                ReviewPrompt.incrementInteraction(context)
                if (ReviewPrompt.shouldShow(context)) {
                    showReviewDialog = true
                } else {
                    scope.launch { snackbarHostState.showSnackbar("¡Deuda Saldada! +100 Cukitis \uD83D\uDC8E\uD83D\uDD25") }
                }
                
                val receiptText = "¡Deuda Saldada! - Deudom\n\nTotal Pagado: ${loan.currency} ${String.format("%.2f", loan.amount - loan.paidAmount)}\nFecha: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}\n\nLa deuda ha sido cerrada exitosamente."
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, receiptText)
                }
                context.startActivity(Intent.createChooser(sendIntent, "Compartir liquidación"))
            }
        )
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Detalles", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                    },
                    actions = {
                        if (isLender && loan.status != LoanStatus.PAID && loan.status != LoanStatus.REQUESTED) {
                            IconButton(onClick = { showRemind = true }) {
                                Icon(Icons.Default.NotificationsActive, null, tint = GoldPremium)
                            }
                        }
                        IconButton(onClick = { showChat = !showChat }) {
                            BadgedBox(badge = { if (!showChat) Badge(containerColor = GoldPremium) }) {
                                Icon(Icons.Default.Chat, null, tint = if (showChat) GoldPremium else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                if (!showChat && loan.status != LoanStatus.PAID && loan.status != LoanStatus.REQUESTED && isLender) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                        shadowElevation = 24.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Button(
                            onClick = { showPaymentScreen = true },
                            modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = Color.Black)
                        ) {
                            Icon(Icons.Default.AddTask, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Registrar Pago Recibido", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (showChat) {
                    ChatPanel(loanId = loan.id, currentUserPhone = userPhone)
                } else {
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                        
                        // Premium Main Card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(32.dp),
                            shadowElevation = 12.dp,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(
                                modifier = Modifier.background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                                        )
                                    )
                                )
                            ) {
                                Column(modifier = Modifier.padding(24.dp)) {
                                    // Header: Target & Status
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isLender) "PRESTADO A" else "ADEUDADO A",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                                letterSpacing = 1.sp
                                            )
                                            Text(
                                                text = if (isLender) loan.borrowerName else loan.lenderName,
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                        }
                                        Surface(
                                            color = statusColor.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                                        ) {
                                            Text(
                                                text = statusText,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = statusColor
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(32.dp))

                                    // Main Amount
                                    Text(
                                        text = "MONTO TOTAL",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        letterSpacing = 1.sp
                                    )
                                    AutoResizedText(
                                        text = if (privacy) "****" else "${loan.currency} ${String.format(Locale.US, "%,.2f", loan.amount)}",
                                        style = MaterialTheme.typography.displayMedium,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Progress Bar
                                    val prog = if (loan.amount > 0) (loan.paidAmount / loan.amount).toFloat().coerceIn(0f, 1f) else 0f
                                    LinearProgressIndicator(
                                        progress = { prog },
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                        color = statusColor,
                                        trackColor = MaterialTheme.colorScheme.outline.copy(0.2f)
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Stats Row
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column {
                                            Text("PAGADO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                            Text(
                                                text = if (privacy) "****" else "${loan.currency} ${String.format(Locale.US, "%,.2f", loan.paidAmount)}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = statusColor
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("RESTANTE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                            val remainingAmount = if (loan.status == LoanStatus.PAID) 0.0 else (loan.amount - loan.paidAmount)
                                            Text(
                                                text = if (privacy) "****" else "${loan.currency} ${String.format(Locale.US, "%,.2f", remainingAmount)}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (loan.status == LoanStatus.PAID) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                        
                        // Metadata Card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Default.Notes, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text("Concepto", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                        Text(loan.description.ifBlank { "Préstamo General" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    }
                                }
                                
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.3f))
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text("Fecha de Creación", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                        Text(SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(loan.date)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        if (loan.installments.isNotEmpty()) {
                            Spacer(Modifier.height(32.dp))
                            Text("PLAN DE CUOTAS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.sp)
                            Spacer(Modifier.height(16.dp))
                            
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    loan.installments.forEachIndexed { idx, inst ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier.size(32.dp).background(if (inst.isPaid) Color(0xFF4CAF50).copy(0.2f) else MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (inst.isPaid) {
                                                        Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                                    } else {
                                                        Text("${idx + 1}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                                    }
                                                }
                                                Spacer(Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = "${loan.currency} ${String.format(Locale.US, "%,.2f", inst.amount)}",
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (inst.isPaid) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(inst.dueDate)),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.secondary
                                                    )
                                                }
                                            }
                                            
                                            Surface(
                                                color = if (inst.isPaid) Color(0xFF4CAF50).copy(0.1f) else GoldPremium.copy(0.1f),
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, if (inst.isPaid) Color(0xFF4CAF50).copy(0.3f) else GoldPremium.copy(0.3f))
                                            ) {
                                                Text(
                                                    text = if (inst.isPaid) "PAGADA" else "PENDIENTE",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (inst.isPaid) Color(0xFF4CAF50) else GoldPremium
                                                )
                                            }
                                        }
                                        if (idx < loan.installments.size - 1) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.2f))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(32.dp))
                        Text("HISTORIAL DE PAGOS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.sp)
                        Spacer(Modifier.height(16.dp))
                        
                        if (loan.payments.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(100.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.MoneyOff, null, tint = MaterialTheme.colorScheme.outline)
                                    Spacer(Modifier.height(8.dp))
                                    Text("No hay pagos registrados aún", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        } else {
                            // Timeline layout for payments
                            Column(modifier = Modifier.fillMaxWidth()) {
                                loan.payments.sortedByDescending { it.date }.forEachIndexed { index, p ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                        // Timeline visual
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
                                            Box(modifier = Modifier.size(12.dp).background(GoldPremium, CircleShape).border(2.dp, MaterialTheme.colorScheme.background, CircleShape))
                                            if (index != loan.payments.size - 1) {
                                                Box(modifier = Modifier.width(2.dp).height(60.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(0.5f)))
                                            }
                                        }
                                        
                                        Spacer(Modifier.width(12.dp))
                                        
                                        // Payment content
                                        Surface(
                                            modifier = Modifier.weight(1f).clickable { if (p.proofUrl != null) previewImage = p.proofUrl },
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.3f))
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "${loan.currency} ${String.format(Locale.US, "%,.2f", p.amount)}",
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 18.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (p.proofUrl != null) {
                                                        Icon(Icons.Default.Image, null, tint = GoldPremium, modifier = Modifier.size(20.dp))
                                                    }
                                                }
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(p.date)),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                                if (!p.description.isNullOrBlank()) {
                                                    Spacer(Modifier.height(8.dp))
                                                    Text(
                                                        text = "Nota: ${p.description}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(100.dp)) // Space for bottom bar
                    }
                }
            }
        }
    }

    if (showRemind) {
        AlertDialog(
            onDismissRequest = { showRemind = false },
            title = { Text("Recordatorio de Cobro", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Elige el tono del mensaje para enviarlo por WhatsApp o SMS:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    val tones = listOf(
                        ReminderTone.FRIENDLY to "😇 Amable",
                        ReminderTone.FORMAL to "👔 Formal",
                        ReminderTone.FUNNY to "🤡 Gracioso",
                        ReminderTone.URGENT to "🚨 Urgente"
                    )
                    tones.forEach { (tone, label) ->
                        Surface(
                            onClick = {
                                val msg = ReminderUtils.generateMessage(loan, tone)
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, msg)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Enviar cobro por..."))
                                showRemind = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(label, modifier = Modifier.padding(16.dp).fillMaxWidth(), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (previewImage != null) {
        Dialog(onDismissRequest = { previewImage = null }) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AsyncImage(model = previewImage, contentDescription = null, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)))
                IconButton(
                    onClick = { previewImage = null },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }
        }
    }
    
    if (showReviewDialog) {
        ReviewDialog(
            onDismiss = { showReviewDialog = false },
            onRate = {
                showReviewDialog = false
                ReviewPrompt.openPlayStore(context)
            }
        )
    }
}
