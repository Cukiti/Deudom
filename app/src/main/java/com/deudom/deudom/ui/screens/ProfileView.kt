package com.deudom.deudom.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.* // Restore wildcard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.deudom.deudom.util.CurrencyAmountInputVisualTransformation
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import coil.compose.AsyncImage
import com.deudom.deudom.R
import com.deudom.deudom.data.FirestoreService
import com.deudom.deudom.data.User
import com.deudom.deudom.data.RecurringLoan
import com.deudom.deudom.data.SavingGoal
import com.deudom.deudom.ui.components.StatItem
import com.deudom.deudom.ui.components.PremiumTextField
import com.deudom.deudom.ui.components.AchievementBadge
import com.deudom.deudom.ui.theme.GoldPremium
import com.deudom.deudom.util.CategoryProvider
import com.deudom.deudom.util.Prefs
import com.deudom.deudom.util.formatPhone
import com.deudom.deudom.util.generateQRCode
import com.deudom.deudom.util.CurrencyManager
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

import com.deudom.deudom.util.ScoreCalculator
import android.widget.Toast
import com.deudom.deudom.util.BiometricManager
import com.deudom.deudom.ui.components.PremiumPullToRefresh
import com.deudom.deudom.util.IconProvider
import kotlinx.coroutines.delay

@Composable
fun ProfileView(
    userPhone: String,
    onLogout: () -> Unit,
    onSettingsClick: () -> Unit,
    onShowRewards: () -> Unit
) {
    var user by remember { mutableStateOf<User?>(null) }
    var qr by remember { mutableStateOf<Bitmap?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var recurringLoans by remember { mutableStateOf<List<RecurringLoan>>(emptyList()) }
    var savingGoals by remember { mutableStateOf<List<SavingGoal>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Dialog States
    var showWalletDialog by remember { mutableStateOf(false) }
    var showGoalsDialog by remember { mutableStateOf(false) }
    var showAchievementsDialog by remember { mutableStateOf(false) }
    var showRecurringDialog by remember { mutableStateOf(false) }
    var showExpandedQR by remember { mutableStateOf(false) }
    
    // Create Goal State
    var showNewGoalDialog by remember { mutableStateOf(false) }
    var goalName by remember { mutableStateOf("") }
    var goalTarget by remember { mutableStateOf("") }
    
    // Wallet States
    var inputWalletCash by remember { mutableStateOf("") }
    var inputBalance by remember { mutableStateOf("") }
    var inputReserve by remember { mutableStateOf("") }
    var selectedBaseCurrency by remember { mutableStateOf("RD$") }
    var categoryBudgets by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                isUploading = true
                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                }
                
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                val url = FirestoreService.uploadImage("profiles/${userPhone}.jpg", baos.toByteArray())
                if (url != null) {
                    FirestoreService.updatePhotoUrl(userPhone, url)
                    user = FirestoreService.getUser(userPhone)
                }
                isUploading = false
            }
        }
    }

    LaunchedEffect(user) {
        if (user != null) {
            qr = generateQRCode(userPhone)
            inputWalletCash = if (user?.walletCash == 0.0) "" else user?.walletCash?.let { java.text.DecimalFormat("#.##").format(it) } ?: ""
            inputBalance = if (user?.currentBalance == 0.0) "" else user?.currentBalance?.let { java.text.DecimalFormat("#.##").format(it) } ?: ""
            inputReserve = if (user?.emergencyReserve == 0.0) "" else user?.emergencyReserve?.let { java.text.DecimalFormat("#.##").format(it) } ?: ""
            selectedBaseCurrency = user?.baseCurrency ?: "RD$"
            categoryBudgets = user?.categoryBudgets?.mapValues { it.value.toString() } ?: emptyMap()
        }
    }
    
    DisposableEffect(userPhone) {
        if (userPhone.isNotBlank()) {
            val userListener = FirestoreService.listenToUser(userPhone) { u -> user = u }
            val recurringListener = FirestoreService.listenToRecurringLoans(userPhone) { l -> recurringLoans = l }
            val goalsListener = FirestoreService.listenToSavingGoals(userPhone) { g -> savingGoals = g }
            
            onDispose {
                userListener.remove()
                recurringListener.remove()
                goalsListener.remove()
            }
        } else {
            onDispose { }
        }
    }
    
    PremiumPullToRefresh(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                user = FirestoreService.getUser(userPhone)
                delay(1000) 
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- Header Actions ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logout button on the far left
                    IconButton(onClick = {
                        Prefs.removeCustomPin(context)
                        Prefs.setAppLockEnabled(context, false)
                        onLogout()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "Cerrar Sesión", tint = Color.Red.copy(0.7f))
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))

                    // QR Code button
                    IconButton(onClick = { showExpandedQR = true }) {
                        Icon(Icons.Default.QrCode2, "Código QR", tint = MaterialTheme.colorScheme.onSurface)
                    }

                    // Settings button
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, "Configuración", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // --- User Info ---
                Box(
                    modifier = Modifier.size(70.dp).clip(CircleShape)
                        .background(GoldPremium.copy(0.1f))
                        .clickable { launcher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploading) CircularProgressIndicator(color = GoldPremium, modifier = Modifier.size(24.dp))
                    else if (user?.photoUrl != null) AsyncImage(model = user?.photoUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                    else Text(user?.name?.take(1)?.uppercase() ?: "?", style = MaterialTheme.typography.titleLarge, color = GoldPremium)
                }
                Spacer(Modifier.height(8.dp))
                Text(user?.name ?: "Cargando...", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(formatPhone(userPhone), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)

                Spacer(Modifier.height(16.dp))
        
                // --- Stats Section ---
                val score = user?.score ?: 50 // Default
                val good = user?.goodPayCount ?: 0
                val bad = user?.badPayCount ?: 0

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "TU NIVEL DE FIABILIDAD", 
                            style = MaterialTheme.typography.labelMedium, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            com.deudom.deudom.ui.components.ScoreGauge(
                                score = score,
                                modifier = Modifier.size(120.dp)
                            )
                            
                            Box(Modifier.width(1.dp).height(80.dp).background(MaterialTheme.colorScheme.outline.copy(0.2f)))
                            
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$good", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color(0xFF4CAF50))
                                    Text("A Tiempo", style = MaterialTheme.typography.labelSmall)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$bad", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color(0xFFF44336))
                                    Text("A Destiempo", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
        
                Spacer(Modifier.height(16.dp))
        
                // --- Menu Grid Section ---
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MenuButton(
                            title = "Mi Cartera", 
                            icon = IconProvider.getIcon("account_balance_wallet"), 
                            color = GoldPremium, 
                            modifier = Modifier.weight(1f)
                        ) { showWalletDialog = true }
                        
                        MenuButton(
                            title = "Pagos Fijos", 
                            icon = IconProvider.getIcon("event_repeat"), 
                            color = Color(0xFF64B5F6), 
                            modifier = Modifier.weight(1f)
                        ) { showRecurringDialog = true }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MenuButton(
                            title = "Metas Ahorro", 
                            icon = IconProvider.getIcon("savings"), 
                            color = Color(0xFF81C784), 
                            modifier = Modifier.weight(1f)
                        ) { showGoalsDialog = true }
                        
                        MenuButton(
                            title = "Mercado Cukitis", 
                            icon = IconProvider.getIcon("diamond"), 
                            color = Color(0xFFFFB74D), 
                            modifier = Modifier.weight(1f)
                        ) { onShowRewards() }
                    }
                }
            }
        }
    }

    // --- Dialogs ---

    if (showGoalsDialog) {
        Dialog(onDismissRequest = { showGoalsDialog = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showGoalsDialog = false }) { Icon(Icons.Default.Close, null) }
                        Text("Mis Metas de Ahorro", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { showNewGoalDialog = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = GoldPremium)) {
                        Icon(Icons.Default.Add, null, tint = Color.Black); Spacer(Modifier.width(8.dp)); Text("NUEVA META", color = Color.Black)
                    }
                    Spacer(Modifier.height(16.dp))
                    if (savingGoals.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Sin metas aún.", color = Color.Gray) }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(savingGoals) { goal ->
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                    Column(Modifier.padding(16.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(goal.name, fontWeight = FontWeight.Bold)
                                            IconButton(onClick = { scope.launch { if (FirestoreService.deleteSavingGoal(goal.id)) savingGoals = FirestoreService.getSavingGoals(userPhone) } }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Delete, null, tint = Color.Red.copy(0.5f)) }
                                        }
                                        val progress = (user?.currentBalance ?: 0.0) / goal.targetAmount
                                        LinearProgressIndicator(progress = { progress.toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(8.dp).clip(CircleShape), color = GoldPremium)
                                        Text("${(progress * 100).toInt()}% completado", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.End))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showNewGoalDialog) {
        AlertDialog(
            onDismissRequest = { showNewGoalDialog = false },
            title = { Text("Nueva Meta") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { PremiumTextField(value = goalName, onValueChange = { goalName = it }, label = "Nombre", icon = Icons.Default.Star); PremiumTextField(value = goalTarget, onValueChange = { goalTarget = it }, label = "Monto", icon = Icons.Default.AttachMoney, keyboardType = KeyboardType.Decimal) } },
            confirmButton = { Button(onClick = { val t = goalTarget.toDoubleOrNull() ?: 0.0; if (t > 0 && goalName.isNotEmpty()) { scope.launch { if (FirestoreService.createSavingGoal(com.deudom.deudom.data.SavingGoal(userPhone = userPhone, name = goalName, targetAmount = t))) { savingGoals = FirestoreService.getSavingGoals(userPhone); showNewGoalDialog = false; goalName = ""; goalTarget = "" } } } }, colors = ButtonDefaults.buttonColors(containerColor = GoldPremium)) { Text("CREAR", color = Color.Black) } }
        )
    }

    if (showAchievementsDialog) {
        Dialog(onDismissRequest = { showAchievementsDialog = false }) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Tus Logros", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        AchievementBadge("Buen Pagador", "Pagos a tiempo", (user?.goodPayCount ?: 0) > 0, Color(0xFF4CAF50))
                        AchievementBadge("Billetera Sana", "Balance positivo", (user?.currentBalance ?: 0.0) >= 0, Color(0xFF64B5F6))
                    }
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { showAchievementsDialog = false }) { Text("Cerrar") }
                }
            }
        }
    }
    
    if (showWalletDialog) {
        Dialog(onDismissRequest = { showWalletDialog = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { showWalletDialog = false }) {
                            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(
                            "Mi Cartera",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = {
                                scope.launch {
                                    FirestoreService.updateFinancialInfo(
                                        phone = userPhone,
                                        walletCash = CurrencyManager.parseAmount(inputWalletCash) ?: 0.0,
                                        balance = CurrencyManager.parseAmount(inputBalance) ?: 0.0,
                                        reserve = CurrencyManager.parseAmount(inputReserve) ?: 0.0,
                                        baseCurrency = selectedBaseCurrency,
                                        budgets = categoryBudgets.mapValues { it.value.toDoubleOrNull() ?: 0.0 }
                                    )
                                    user = FirestoreService.getUser(userPhone)
                                    showWalletDialog = false
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = GoldPremium)
                        ) {
                            Text("GUARDAR", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    
                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        val visualTransformation = CurrencyAmountInputVisualTransformation()
                        WalletInputField(stringResource(R.string.wallet_cash), inputWalletCash, { inputWalletCash = it }, Icons.Default.Handyman, stringResource(R.string.wallet_cash_desc), visualTransformation = visualTransformation)
                        WalletInputField(stringResource(R.string.bank_cash), inputBalance, { inputBalance = it }, Icons.Default.AccountBalance, stringResource(R.string.bank_cash_desc), visualTransformation = visualTransformation)
                        WalletInputField(stringResource(R.string.emergency_reserve), inputReserve, { inputReserve = it }, Icons.Default.Lock, stringResource(R.string.emergency_reserve_desc), true, visualTransformation = visualTransformation)
                    }
                }
            }
        }
    }
    
    if (showRecurringDialog) {
        Dialog(onDismissRequest = { showRecurringDialog = false }) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().height(400.dp)) {
                Column(Modifier.padding(24.dp)) {
                    Text("Pagos Fijos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    if (recurringLoans.isEmpty()) Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Text("Sin pagos fijos.", color = Color.Gray) }
                    else LazyColumn(Modifier.weight(1f)) { items(recurringLoans) { r -> Text("• ${r.description} (${r.currency} ${r.amount}) - Día ${r.dayOfMonth}") } }
                    Button(onClick = { showRecurringDialog = false }, modifier = Modifier.fillMaxWidth()) { Text("Cerrar") }
                }
            }
        }
    }
    
    if (showExpandedQR) {
        Dialog(onDismissRequest = { showExpandedQR = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.size(300.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    qr?.let { 
                        androidx.compose.foundation.Image(
                            bitmap = it.asImageBitmap(), 
                            contentDescription = "QR Code Expanded",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        ) 
                    }
                }
            }
        }
    }
}

@Composable
fun MenuButton(title: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(100.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun WalletInputField(label: String, value: String, onValueChange: (String) -> Unit, icon: ImageVector, description: String, isEmergency: Boolean = false, visualTransformation: VisualTransformation = VisualTransformation.None) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (isEmergency) Color.Red else GoldPremium, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isEmergency) Color.Red else MaterialTheme.colorScheme.onBackground)
        }
        if (description.isNotEmpty()) Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(start = 32.dp))
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value, 
            onValueChange = onValueChange, 
            prefix = { Text("RD$ ", color = if (isEmergency) Color.Red else GoldPremium, fontWeight = FontWeight.Bold) }, 
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), 
            visualTransformation = visualTransformation,
            modifier = Modifier.fillMaxWidth().padding(start = 32.dp), 
            shape = RoundedCornerShape(12.dp), 
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = if (isEmergency) Color.Red else GoldPremium)
        )
    }
}
