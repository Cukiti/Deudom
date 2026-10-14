package com.deudom.deudom.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.graphicsLayer
import com.deudom.deudom.R
import com.deudom.deudom.data.FirestoreService
import com.deudom.deudom.data.Loan
import com.deudom.deudom.data.LoanStatus
import com.deudom.deudom.data.User
import com.deudom.deudom.data.PersonalExpense
import com.deudom.deudom.data.SavingGoal
import com.deudom.deudom.ui.components.LoanItem
import com.deudom.deudom.ui.components.QuickAccessSection
import com.deudom.deudom.ui.components.SummaryCard
import com.deudom.deudom.ui.components.PremiumTextField
import com.deudom.deudom.ui.theme.GoldPremium
import com.deudom.deudom.util.CurrencyManager
import com.deudom.deudom.util.CategoryProvider
import kotlinx.coroutines.launch
import java.util.*

import com.deudom.deudom.util.HapticManager

import android.Manifest
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.core.content.FileProvider
import com.deudom.deudom.util.ReceiptScanner
import com.deudom.deudom.ui.components.FinancialPulseCard
import com.deudom.deudom.ui.components.AutoResizedText
import java.io.File
import androidx.compose.material.icons.filled.CameraAlt

import com.deudom.deudom.ui.components.PremiumPullToRefresh
import com.deudom.deudom.ui.components.DashboardSkeleton
import com.deudom.deudom.ui.components.ConfettiView
import kotlinx.coroutines.delay

import android.speech.RecognizerIntent
import android.app.Activity
import android.content.Intent
import android.widget.Toast
import java.util.Locale

@Composable
fun DashboardView(
    user: User?,
    lender: List<Loan>,
    borrower: List<Loan>,
    personalExpenses: List<PersonalExpense>,
    savingGoals: List<SavingGoal> = emptyList(),
    loading: Boolean,
    privacy: Boolean,
    onPriv: () -> Unit,
    onLend: () -> Unit,
    onLoan: (Loan) -> Unit,
    onPersonalHistoryClick: () -> Unit,
    onRef: () -> Unit,
    showExpenseDialog: Boolean,
    onShowExpenseDialogChange: (Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var filterState by remember { mutableIntStateOf(0) }
    var expenseAmt by remember { mutableStateOf("") }
    var expenseDesc by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Otros") }
    var showConfetti by remember { mutableStateOf(false) }
    var isOffline by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var infoDialogTitle by remember { mutableStateOf("") }
    var infoDialogMessage by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    // Scanner State
    var isScanning by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val tempFile = remember { File(context.cacheDir, "receipt_temp.jpg") }
    val tempUri = remember { FileProvider.getUriForFile(context, "com.deudom.deudom.provider", tempFile) }

    // Wallet Dialog States
    var showUpdateWalletDialog by remember { mutableStateOf(false) }
    var walletCashInput by remember { mutableStateOf("") }
    var showAddWalletCashDialog by remember { mutableStateOf(false) }
    var addWalletCashInput by remember { mutableStateOf("") }

    fun resetExpenseDialog() {
        onShowExpenseDialogChange(false)
        expenseAmt = ""
        expenseDesc = ""
        selectedCategory = "Otros"
    }

    LaunchedEffect(user) {
        user?.let {
            val today = Calendar.getInstance()
            val lastUpdate = Calendar.getInstance().apply { timeInMillis = it.lastWalletUpdateTimestamp }
            if (today.get(Calendar.DAY_OF_YEAR) != lastUpdate.get(Calendar.DAY_OF_YEAR) || today.get(Calendar.YEAR) != lastUpdate.get(Calendar.YEAR)) {
                showUpdateWalletDialog = true
            }
        }
    }
    
    LaunchedEffect(Unit) {
        while(true) {
            isOffline = !com.deudom.deudom.util.NetworkUtils.isInternetAvailable(context)
            delay(5000)
        }
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            isScanning = true
            scope.launch {
                try {
                    val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                    val result = ReceiptScanner.scanReceipt(context, bitmap)
                    
                    if (result.amount != null) expenseAmt = result.amount.toString()
                    if (result.merchant != null) expenseDesc = result.merchant
                    HapticManager.vibrateSuccess(context)
                } catch (e: Exception) {
                    HapticManager.vibrateError(context)
                } finally {
                    isScanning = false
                }
            }
        }
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            cameraLauncher.launch(tempUri)
        }
    }

    val initialLoading = loading && lender.isEmpty() && borrower.isEmpty() && user == null

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                PremiumPullToRefresh(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        scope.launch {
                            isRefreshing = true
                            onRef()
                            delay(1000)
                            isRefreshing = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (initialLoading) {
                        DashboardSkeleton()
                    } else {
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).animateContentSize()) {
                            if (isOffline) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CloudOff, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Modo Offline - Los datos pueden no estar actualizados", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                    }
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(R.string.summary), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = onPriv) { Icon(if (privacy) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) }
                                }
                            }
                            
                            Spacer(Modifier.height(8.dp))
                            
                            val baseCurrency = user?.baseCurrency ?: "RD$"
                            val pendingLender = lender.filter { it.status == LoanStatus.PENDING || it.status == LoanStatus.OVERDUE }
                            val pendingBorrower = borrower.filter { it.status == LoanStatus.PENDING || it.status == LoanStatus.OVERDUE }
                            val totalLentInBase = pendingLender.sumOf { CurrencyManager.convert(it.amount - it.paidAmount, it.currency, baseCurrency) }
                            val totalBorrowedInBase = pendingBorrower.sumOf { CurrencyManager.convert(it.amount - it.paidAmount, it.currency, baseCurrency) }
                            
                            // Removed Incoming Loan Requests Section

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    SummaryCard(stringResource(R.string.me_deben), totalLentInBase, Color(0xFF4CAF50), privacy, baseCurrency)
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    SummaryCard(stringResource(R.string.debo), totalBorrowedInBase, Color(0xFFF44336), privacy, baseCurrency)
                                }
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            
                            Text("Tu Pulso Financiero", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp))
                            
                            FinancialPulseCard(
                                user = user,
                                privacy = privacy,
                                onHistoryClick = onPersonalHistoryClick,
                                onAddExpenseClick = { onShowExpenseDialogChange(true) },
                                onEditWalletClick = { showAddWalletCashDialog = true }
                            )

                            Spacer(Modifier.height(20.dp))
                            
                            // --- Original Dynamic Insights Section ---
                            val weeklySpent = remember(personalExpenses) {
                                val weekMillis = 7 * 24 * 60 * 60 * 1000L
                                personalExpenses.filter { it.date > System.currentTimeMillis() - weekMillis }.sumOf { it.amount }
                            }
                            
                            val topCategory = remember(personalExpenses) {
                                personalExpenses
                                    .filter { 
                                        val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                                        val now = Calendar.getInstance()
                                        cal.get(Calendar.MONTH) == now.get(Calendar.MONTH) && cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                                    }
                                    .groupBy { it.category }
                                    .mapValues { it.value.sumOf { e -> e.amount } }
                                    .maxByOrNull { it.value }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(0.3f)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Gasto Semanal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = {
                                                    infoDialogTitle = "Gasto Semanal"
                                                    infoDialogMessage = "Suma de todos tus gastos personales registrados en los últimos 7 días. Te ayuda a ver tu ritmo de gasto a corto plazo."
                                                    showInfoDialog = true
                                                },
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Icon(Icons.Default.Info, contentDescription = "Info Gasto Semanal", tint = MaterialTheme.colorScheme.secondary)
                                            }
                                        }
                                        AutoResizedText(
                                            text = if (privacy) "****" else CurrencyManager.format(weeklySpent, baseCurrency),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(0.2f)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Mayor Gasto", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = {
                                                    infoDialogTitle = "Mayor Gasto (del Mes)"
                                                    infoDialogMessage = "La categoría en la que más has gastado durante el mes actual. Sirve para identificar a dónde se va la mayor parte de tu dinero."
                                                    showInfoDialog = true
                                                },
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Icon(Icons.Default.Info, contentDescription = "Info Mayor Gasto", tint = MaterialTheme.colorScheme.secondary)
                                            }
                                        }
                                        Text(
                                            text = if (topCategory != null) "${CategoryProvider.getEmoji(topCategory.key)} ${topCategory.key}" else "N/A",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                val safetyNet = (user?.currentBalance ?: 0.0) + (user?.emergencyReserve ?: 0.0)
                                Card(
                                    modifier = Modifier.weight(1.1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(0.2f))
                                ) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Shield, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Respaldo Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                IconButton(
                                                    onClick = {
                                                        infoDialogTitle = "Respaldo Total"
                                                        infoDialogMessage = "Es la suma de tu dinero en el banco y tu reserva de emergencia. Representa tu seguridad financiera total ante imprevistos."
                                                        showInfoDialog = true
                                                    },
                                                    modifier = Modifier.size(18.dp)
                                                ) {
                                                    Icon(Icons.Default.Info, contentDescription = "Info Respaldo Total", tint = MaterialTheme.colorScheme.secondary)
                                                }
                                            }
                                            AutoResizedText(
                                                text = if (privacy) "****" else CurrencyManager.format(safetyNet, baseCurrency),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF4CAF50)
                                            )
                                        }
                                    }
                                }

                                Card(
                                    modifier = Modifier.weight(0.9f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, GoldPremium.copy(0.2f))
                                ) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("💎", fontSize = 14.sp)
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Mis Cukitis", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                IconButton(
                                                    onClick = {
                                                        infoDialogTitle = "Mis Cukitis"
                                                        infoDialogMessage = "Son puntos de recompensa que ganas por usar la app y mantener buenos hábitos financieros. ¡Pronto podrás canjearlos por beneficios!"
                                                        showInfoDialog = true
                                                    },
                                                    modifier = Modifier.size(18.dp)
                                                ) {
                                                    Icon(Icons.Default.Info, contentDescription = "Info Mis Cukitis", tint = MaterialTheme.colorScheme.secondary)
                                                }
                                            }
                                            Text("${user?.cukitis ?: 0}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black, color = GoldPremium)
                                        }
                                    }
                                }
                            }

                            val upcoming = (pendingLender + pendingBorrower)
                                .filter { it.dueDate != null && it.dueDate!! > System.currentTimeMillis() && it.dueDate!! < System.currentTimeMillis() + (3 * 24 * 60 * 60 * 1000L) }
                                .sortedBy { it.dueDate }
                            
                            if (upcoming.isNotEmpty()) {
                                Spacer(Modifier.height(20.dp))
                                Text("Alertas de Vencimiento", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                                Spacer(Modifier.height(8.dp))
                                upcoming.take(2).forEach { loan ->
                                    val isLender = loan.lenderPhone == user?.phone
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFFF9800).copy(0.1f),
                                        border = BorderStroke(1.dp, Color(0xFFFF9800).copy(0.2f))
                                    ) {
                                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.PriorityHigh, null, tint = Color(0xFFFF9800), modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(12.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(if (isLender) "Cobrar a ${loan.borrowerName}" else "Pagar a ${loan.lenderName}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                                Text("Vence pronto", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                            }
                                            AutoResizedText(
                                                text = if (privacy) "****" else "${loan.currency} ${String.format("%.0f", loan.amount - loan.paidAmount)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFFFF9800)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(20.dp))
                            QuickAccessSection(onLend)
                            Spacer(Modifier.height(20.dp))
                        }
                    }
                }
                
                if (showConfetti) {
                    ConfettiView(
                        modifier = Modifier.align(Alignment.Center),
                        onFinished = { showConfetti = false }
                    )
                }
            }
        }
    )

    InfoDialog(
        show = showInfoDialog,
        title = infoDialogTitle,
        message = infoDialogMessage,
        onDismiss = { showInfoDialog = false }
    )

    if (showUpdateWalletDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("¿Dinero en Mano?") },
            text = {
                Column {
                    Text("Para darte un mejor pulso de tus gastos, dinos cuánto dinero tienes en tu cartera ahora mismo.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    PremiumTextField(
                        value = walletCashInput,
                        onValueChange = { walletCashInput = it },
                        label = "Monto en Cartera",
                        icon = Icons.Default.AccountBalanceWallet,
                        keyboardType = KeyboardType.Decimal,
                        visualTransformation = com.deudom.deudom.util.CurrencyAmountInputVisualTransformation()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = CurrencyManager.parseAmount(walletCashInput)
                        if (amount != null && user != null) {
                            showUpdateWalletDialog = false
                            scope.launch {
                                try {
                                    if (FirestoreService.updateFinancialInfo(
                                        phone = user.phone,
                                        walletCash = amount,
                                        balance = user.currentBalance,
                                        reserve = user.emergencyReserve,
                                        baseCurrency = user.baseCurrency,
                                        lastWalletUpdateTimestamp = System.currentTimeMillis()
                                    )) {
                                        snackbarHostState.showSnackbar("¡Cartera actualizada!")
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Error al actualizar")
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPremium)
                ) {
                    Text("ACTUALIZAR", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showAddWalletCashDialog) {
        AlertDialog(
            onDismissRequest = { showAddWalletCashDialog = false },
            title = { Text("Agregar Dinero a Cartera") },
            text = {
                Column {
                    Text("¿Cuánto dinero quieres agregar a tu cartera?", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    PremiumTextField(
                        value = addWalletCashInput,
                        onValueChange = { addWalletCashInput = it },
                        label = "Monto a Agregar",
                        icon = Icons.Default.Add,
                        keyboardType = KeyboardType.Decimal,
                        visualTransformation = com.deudom.deudom.util.CurrencyAmountInputVisualTransformation()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountToAdd = CurrencyManager.parseAmount(addWalletCashInput)
                        if (amountToAdd != null && user != null) {
                            val currentPhone = user.phone
                            val currentBalance = user.currentBalance
                            val currentReserve = user.emergencyReserve
                            val currentCurrency = user.baseCurrency
                            val newWalletCash = user.walletCash + amountToAdd
                            
                            showAddWalletCashDialog = false
                            addWalletCashInput = ""
                            
                            scope.launch {
                                try {
                                    if (FirestoreService.updateFinancialInfo(
                                        phone = currentPhone,
                                        walletCash = newWalletCash,
                                        balance = currentBalance,
                                        reserve = currentReserve,
                                        baseCurrency = currentCurrency
                                    )) {
                                        showConfetti = true
                                        snackbarHostState.showSnackbar("¡Cartera actualizada con éxito!")
                                    } else {
                                        snackbarHostState.showSnackbar("Error al conectar con el servidor.")
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Error inesperado: ${e.localizedMessage}")
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPremium)
                ) {
                    Text("AGREGAR", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWalletCashDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showExpenseDialog) {
        com.deudom.deudom.ui.components.AddExpenseBottomSheet(
            user = user,
            personalExpenses = personalExpenses,
            onDismiss = { onShowExpenseDialogChange(false) },
            onSave = { amount, description, category ->
                scope.launch {
                    val exp = com.deudom.deudom.data.PersonalExpense(
                        userPhone = user?.phone ?: "",
                        amount = amount,
                        description = description,
                        category = category
                    )
                    if (com.deudom.deudom.data.FirestoreService.addPersonalExpense(exp)) {
                        onRef()
                        onShowExpenseDialogChange(false)
                    }
                }
            },
            onScanReceipt = { permissionLauncher.launch(android.Manifest.permission.CAMERA) },
            isScanning = isScanning
        )
    }
}

@Composable
fun InfoDialog(
    show: Boolean,
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title, fontWeight = FontWeight.Bold) },
            text = { Text(message) },
            confirmButton = {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPremium)
                ) {
                    Text("ENTENDIDO", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
