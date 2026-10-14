package com.deudom.deudom.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deudom.deudom.data.Loan
import com.deudom.deudom.data.LoanStatus
import com.deudom.deudom.data.FirestoreService
import com.deudom.deudom.ui.theme.GoldPremium
import com.deudom.deudom.util.CurrencyManager
import com.deudom.deudom.util.CategoryProvider
import com.deudom.deudom.ui.components.PremiumTextField
import kotlinx.coroutines.launch
import java.util.*
import java.text.SimpleDateFormat
import androidx.compose.ui.draw.scale
import android.widget.Toast
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.fragment.app.FragmentActivity
import com.deudom.deudom.data.local.FriendEntity
import com.deudom.deudom.viewmodel.LoanViewModel
import org.koin.androidx.compose.koinViewModel
import com.deudom.deudom.util.BiometricManager
import com.deudom.deudom.ui.components.PinInputView
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.delay
import com.deudom.deudom.R

import com.deudom.deudom.util.ThousandsSeparatorTransformation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LendScreen(
    userPhone: String,
    initialPhone: String = "",
    initialName: String = "",
    onDismiss: () -> Unit,
    onLoanCreated: () -> Unit
) {
    val viewModel: LoanViewModel = koinViewModel()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // State
    var step by remember { mutableIntStateOf(1) }
    val isLender = true // Fixed as per new requirement: user is always the lender in this flow
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedFriend by remember { mutableStateOf<FriendEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf(initialName) }
    var customPhone by remember { mutableStateOf(initialPhone) }
    var selectedCategory by remember { mutableStateOf("Otros") }
    var selectedCurrency by remember { mutableStateOf("RD$") }
    var isVerified by remember { mutableStateOf(false) }
    var installmentsCount by remember { mutableIntStateOf(1) }
    var installmentsFreq by remember { mutableStateOf("Mensual") }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var showSuccess by remember { mutableStateOf(false) }

    val lottieComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.checkloan))

    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(2500)
            onLoanCreated()
        }
    }

    // Auto-select friend if coming from FriendsView
    LaunchedEffect(initialPhone, viewModel.localFriends) {
        if (initialPhone.isNotBlank()) {
            val friend = viewModel.localFriends.find { it.phone == initialPhone }
            if (friend != null) {
                selectedFriend = friend
                step = 2
            } else if (initialName.isNotBlank()) {
                // If not in local DB yet, create a temporary manual entry to allow immediate lending
                customName = initialName
                customPhone = initialPhone
                step = 2
            }
        }
    }

    // Logic: Search friends
    val filteredFriends = remember(viewModel.localFriends, searchQuery) {
        viewModel.localFriends.filter { 
            it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
        }
    }

    // Logic: Check if selected friend is pending acceptance
    val isSelectionPending = remember(selectedFriend, viewModel.sentPendingRequests) {
        selectedFriend?.let { friend ->
            viewModel.sentPendingRequests.any { it.receiverPhone == friend.phone }
        } ?: false
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        when(step) {
                            1 -> "Seleccionar Amigo"
                            2 -> "Detalles del Préstamo"
                            3 -> "Seguridad"
                            else -> "Confirmación Final"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { if (step > 1) step-- else onDismiss() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            if (step < 3) { // Hide default bottom bar in security and confirm steps for custom logic
                Surface(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (step > 1) {
                            OutlinedButton(
                                onClick = { step-- },
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Atrás")
                            }
                        }
                        
                        Button(
                            onClick = {
                                when(step) {
                                    1 -> {
                                        if (isSelectionPending) {
                                            Toast.makeText(context, "Debes esperar a que acepte tu solicitud de amistad", Toast.LENGTH_LONG).show()
                                        } else if (selectedFriend != null || (customName.isNotBlank() && customPhone.length >= 10)) {
                                            if (selectedFriend == null && customPhone.isNotBlank()) {
                                                scope.launch {
                                                    val isRegistered = FirestoreService.getUser(customPhone) != null
                                                    if (isRegistered) {
                                                        Toast.makeText(context, "Este usuario está en Deudom. Agrégalo como amigo primero para prestarle en vivo.", Toast.LENGTH_LONG).show()
                                                    } else {
                                                        step++
                                                    }
                                                }
                                            } else {
                                                step++
                                            }
                                        } else {
                                            Toast.makeText(context, "Selecciona un amigo o ingresa datos válidos", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    2 -> {
                                        if (amount.isNotBlank()) step++
                                        else Toast.makeText(context, "Ingresa el monto", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(2f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = Color.Black)
                        ) {
                            Text("Continuar", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
            LinearProgressIndicator(
                progress = { step / 4f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = GoldPremium,
                trackColor = GoldPremium.copy(alpha = 0.1f)
            )

            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                        } else {
                            slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                        }
                    },
                    label = "stepAnim"
                ) { currentStep ->
                    Column(Modifier.fillMaxSize().padding(24.dp)) {
                        when(currentStep) {
                            1 -> StepFriendSelection(
                                searchQuery = searchQuery,
                                onSearchChange = { searchQuery = it },
                                friends = filteredFriends,
                                sentPendingRequests = viewModel.sentPendingRequests,
                                selectedFriend = selectedFriend,
                                onFriendSelect = { selectedFriend = it },
                                customName = customName,
                                onCustomNameChange = { customName = it },
                                customPhone = customPhone,
                                onCustomPhoneChange = { if (it.length <= 10) customPhone = it }
                            )
                            2 -> StepAmountDetails(
                                amount = amount,
                                onAmountChange = { amount = it },
                                description = description,
                                onDescChange = { description = it },
                                category = selectedCategory,
                                onCategoryChange = { selectedCategory = it },
                                currency = selectedCurrency,
                                onCurrencyChange = { selectedCurrency = it },
                                installmentsCount = installmentsCount,
                                onInstallmentsCountChange = { installmentsCount = it },
                                installmentsFreq = installmentsFreq,
                                onInstallmentsFreqChange = { installmentsFreq = it },
                                dueDate = dueDate,
                                onDueDateChange = { dueDate = it }
                            )
                            3 -> StepSecurityVerification(
                                onVerified = { 
                                    isVerified = true
                                    step++
                                }
                            )
                            4 -> {
                                val isBorrowerVerified = selectedFriend?.isRegistered == true
                                StepFinalConfirm(
                                    isLender = isLender,
                                    amount = amount,
                                    currency = selectedCurrency,
                                    friendName = selectedFriend?.name ?: customName,
                                    friendPhone = selectedFriend?.phone ?: customPhone,
                                    description = description,
                                    category = selectedCategory,
                                    requiresAcceptance = isBorrowerVerified,
                                    installmentsCount = installmentsCount,
                                    installmentsFreq = installmentsFreq,
                                    dueDate = dueDate,
                                    onConfirm = {
                                        val amt = CurrencyManager.parseAmount(amount) ?: 0.0
                                        val fName = selectedFriend?.name ?: customName
                                        val fPhone = selectedFriend?.phone ?: customPhone
                                        
                                        // "Live Acceptance" Logic:
                                        val initialStatus = if (isBorrowerVerified) LoanStatus.REQUESTED else LoanStatus.PENDING

                                        val instList = mutableListOf<com.deudom.deudom.data.Installment>()
                                        if (installmentsCount > 1) {
                                            val instAmount = amt / installmentsCount
                                            var currentDueDate = System.currentTimeMillis()
                                            for (i in 1..installmentsCount) {
                                                val daysToAdd = when(installmentsFreq) {
                                                    "Semanal" -> 7
                                                    "Quincenal" -> 15
                                                    else -> 30
                                                }
                                                currentDueDate += daysToAdd * 24L * 60 * 60 * 1000L
                                                instList.add(
                                                    com.deudom.deudom.data.Installment(
                                                        id = UUID.randomUUID().toString(),
                                                        amount = instAmount,
                                                        dueDate = currentDueDate,
                                                        isPaid = false
                                                    )
                                                )
                                            }
                                        }

                                        val finalDueDate = if (instList.isNotEmpty()) instList.last().dueDate else (dueDate ?: (System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L)))

                                        val newLoan = Loan(
                                            id = UUID.randomUUID().toString(),
                                            lenderPhone = userPhone,
                                            lenderName = viewModel.currentUser?.name ?: "Usuario",
                                            borrowerPhone = fPhone,
                                            borrowerName = fName,
                                            amount = amt,
                                            description = description,
                                            category = selectedCategory,
                                            status = initialStatus,
                                            currency = selectedCurrency,
                                            dueDate = finalDueDate,
                                            installments = instList
                                        )
                                        
                                        scope.launch {
                                            if (FirestoreService.createLoan(newLoan)) {
                                                showSuccess = true
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                if (showSuccess) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f)),
                        contentAlignment = Alignment.Center
                    ) {
                        LottieAnimation(
                            composition = lottieComposition,
                            iterations = 1,
                            modifier = Modifier.size(250.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StepFriendSelection(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    friends: List<FriendEntity>,
    sentPendingRequests: List<com.deudom.deudom.data.FriendRequest>,
    selectedFriend: FriendEntity?,
    onFriendSelect: (FriendEntity?) -> Unit,
    customName: String,
    onCustomNameChange: (String) -> Unit,
    customPhone: String,
    onCustomPhoneChange: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Verificados", "Manual")

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("¿A quién le prestarás dinero?") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = GoldPremium) },
            shape = RoundedCornerShape(24.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldPremium,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = GoldPremium,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = GoldPremium
                )
            },
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = if(selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (selectedTab == 0) {
                val verifiedFriends = friends.filter { it.isRegistered }
                if (verifiedFriends.isEmpty()) {
                    EmptyState("No hay amigos verificados", Icons.Default.PersonSearch)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(verifiedFriends) { friend ->
                            val isPending = sentPendingRequests.any { it.receiverPhone == friend.phone }
                            FriendItem(
                                friend = friend, 
                                isSelected = selectedFriend?.phone == friend.phone,
                                isPendingAcceptance = isPending,
                                onClick = { if (!isPending) onFriendSelect(friend) }
                            )
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val manualFriends = friends.filter { !it.isRegistered }
                    if (manualFriends.isNotEmpty()) {
                        Text("Contactos Guardados", style = MaterialTheme.typography.labelLarge, color = GoldPremium)
                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(manualFriends) { friend ->
                                FriendItem(
                                    friend = friend, 
                                    isSelected = selectedFriend?.phone == friend.phone,
                                    onClick = { onFriendSelect(friend) }
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    }
                    
                    Text("Agregar Nuevo Manual", style = MaterialTheme.typography.labelLarge, color = GoldPremium)
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            PremiumTextField(value = customName, onValueChange = onCustomNameChange, label = "Nombre Completo", icon = Icons.Default.Person)
                            PremiumTextField(value = customPhone, onValueChange = onCustomPhoneChange, label = "Número de Teléfono", icon = Icons.Default.Phone, keyboardType = KeyboardType.Phone)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepAmountDetails(
    amount: String,
    onAmountChange: (String) -> Unit,
    description: String,
    onDescChange: (String) -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit,
    currency: String,
    onCurrencyChange: (String) -> Unit,
    installmentsCount: Int,
    onInstallmentsCountChange: (Int) -> Unit,
    installmentsFreq: String,
    onInstallmentsFreqChange: (String) -> Unit,
    dueDate: Long?,
    onDueDateChange: (Long?) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dueDate ?: System.currentTimeMillis()
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Currency Selector (Compact)
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(modifier = Modifier.padding(2.dp)) {
                listOf("RD$", "USD", "EUR").forEach { curr ->
                    val isSel = currency == curr
                    Surface(
                        onClick = { onCurrencyChange(curr) },
                        color = if (isSel) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shape = RoundedCornerShape(20.dp),
                        shadowElevation = if (isSel) 2.dp else 0.dp,
                        modifier = Modifier.padding(2.dp)
                    ) {
                        Text(
                            text = curr,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSel) FontWeight.Black else FontWeight.Medium,
                            color = if (isSel) GoldPremium else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 2. Hero Amount Input (Takes available space)
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Monto a prestar",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 1.sp
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = currency,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.width(8.dp))
                
                androidx.compose.foundation.text.BasicTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    visualTransformation = ThousandsSeparatorTransformation(),
                    textStyle = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        fontSize = 48.sp
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f, fill = false).widthIn(min = 60.dp),
                    decorationBox = { innerTextField ->
                        if (amount.isEmpty()) {
                            Text(
                                text = "0",
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 48.sp),
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                        innerTextField()
                    }
                )
                
                Spacer(Modifier.width(8.dp))
                
                IconButton(
                    onClick = { onAmountChange("") },
                    modifier = Modifier
                        .size(40.dp)
                        .alpha(if (amount.isNotEmpty()) 1f else 0f)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Backspace, "Borrar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            }

            // Quick Add Buttons
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(10, 50, 100, 500, 1000).forEach { valAdd ->
                    Surface(
                        onClick = {
                            val current = CurrencyManager.parseAmount(amount) ?: 0.0
                            onAmountChange(String.format(Locale.US, "%.0f", current + valAdd))
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "+$valAdd",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 3. Compact Details Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            shadowElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = onDescChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Concepto...", style = MaterialTheme.typography.bodySmall) },
                        leadingIcon = { Icon(Icons.Default.EditNote, null, tint = GoldPremium, modifier = Modifier.size(20.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPremium,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        onClick = { showDatePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = if (dueDate != null) GoldPremium.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (dueDate != null) GoldPremium else MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Event, null, tint = if (dueDate != null) GoldPremium else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (dueDate != null) SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(dueDate)) else "Límite",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (dueDate != null) GoldPremium else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Category Selector (Compact)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryProvider.categories.forEach { cat ->
                        val isSel = category == cat.name
                        Surface(
                            onClick = { onCategoryChange(cat.name) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) GoldPremium.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, if (isSel) GoldPremium else Color.Transparent)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(cat.emoji, fontSize = 14.sp)
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) GoldPremium else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // Installments toggle (Compact)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, null, tint = GoldPremium, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Pago en Cuotas", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Switch(
                        checked = installmentsCount > 1,
                        onCheckedChange = { if (it) onInstallmentsCountChange(2) else onInstallmentsCountChange(1) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = GoldPremium),
                        modifier = Modifier.scale(0.8f)
                    )
                }
                
                AnimatedVisibility(visible = installmentsCount > 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Cuotas: $installmentsCount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            Slider(
                                value = installmentsCount.toFloat(),
                                onValueChange = { onInstallmentsCountChange(it.toInt()) },
                                valueRange = 2f..12f,
                                steps = 9,
                                colors = SliderDefaults.colors(thumbColor = GoldPremium, activeTrackColor = GoldPremium),
                                modifier = Modifier.weight(1f).padding(start = 16.dp)
                            )
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Semanal", "Quincenal", "Mensual").forEach { freq ->
                                val isSel = installmentsFreq == freq
                                Surface(
                                    onClick = { onInstallmentsFreqChange(freq) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) GoldPremium.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, if (isSel) GoldPremium else Color.Transparent)
                                ) {
                                    Text(
                                        text = freq,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) GoldPremium else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(80.dp)) // Padding for bottom bar
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { 
                    onDueDateChange(datePickerState.selectedDateMillis)
                    showDatePicker = false 
                }) { Text("Aceptar", color = GoldPremium, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar", color = MaterialTheme.colorScheme.secondary) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepSecurityVerification(onVerified: () -> Unit) {
    val context = LocalContext.current
    var isSuccess by remember { mutableStateOf(false) }
    var showPinSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        if (BiometricManager.checkSecurityStatus(context) == BiometricManager.SecurityStatus.READY) {
            BiometricManager.showBiometricPrompt(
                activity = context as? FragmentActivity,
                onAuthenticated = { isSuccess = true }
            )
        }
    }

    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            kotlinx.coroutines.delay(1000)
            onVerified()
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        if (isSuccess) {
            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(100.dp), tint = Color(0xFF4CAF50))
            Spacer(Modifier.height(24.dp))
            Text("¡Identidad Verificada!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color(0xFF4CAF50))
        } else {
            Text("Verificación de Seguridad", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text("Confirma tu identidad para autorizar", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
            
            Spacer(Modifier.height(48.dp))

            val biometricReady = BiometricManager.checkSecurityStatus(context) == BiometricManager.SecurityStatus.READY
            
            Surface(
                onClick = { 
                    if (biometricReady) {
                        BiometricManager.showBiometricPrompt(
                            activity = context as? FragmentActivity,
                            onAuthenticated = { isSuccess = true }
                        )
                    } else {
                        showPinSheet = true
                    }
                },
                modifier = Modifier.size(140.dp),
                shape = RoundedCornerShape(32.dp),
                color = GoldPremium.copy(alpha = 0.1f),
                border = BorderStroke(2.dp, GoldPremium)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Default.Fingerprint, 
                        contentDescription = "Huella", 
                        modifier = Modifier.size(80.dp), 
                        tint = GoldPremium
                    )
                }
            }

            Spacer(Modifier.height(48.dp))

            Button(
                onClick = { 
                    if (biometricReady) {
                        BiometricManager.showBiometricPrompt(
                            activity = context as? FragmentActivity,
                            onAuthenticated = { isSuccess = true }
                        )
                    } else {
                        showPinSheet = true 
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(50.dp)
            ) {
                Icon(Icons.Default.Lock, null)
                Spacer(Modifier.width(8.dp))
                Text(if (biometricReady) "Verificar Identidad" else "Verificar con PIN", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showPinSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPinSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(bottom = 32.dp).fillMaxWidth()) {
                PinInputView(
                    onPinComplete = { 
                        isSuccess = true
                        showPinSheet = false 
                    },
                    onBiometricClick = null
                )
            }
        }
    }
}

@Composable
fun StepFinalConfirm(
    isLender: Boolean,
    amount: String,
    currency: String,
    friendName: String,
    friendPhone: String,
    description: String,
    category: String,
    requiresAcceptance: Boolean,
    installmentsCount: Int,
    installmentsFreq: String,
    dueDate: Long?,
    onConfirm: () -> Unit
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val formattedAmount = CurrencyManager.format(CurrencyManager.parseAmount(amount) ?: 0.0, currency)
                Text("RESUMEN DEL PRÉSTAMO", style = MaterialTheme.typography.labelLarge, color = GoldPremium, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Spacer(Modifier.height(24.dp))
                
                Text(formattedAmount, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text("Para: $friendName", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(friendPhone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                
                Spacer(Modifier.height(32.dp))
                HorizontalDivider(alpha = 0.2f)
                Spacer(Modifier.height(24.dp))
                
                ConfirmRow("Concepto", if(description.isBlank()) "Préstamo General" else description)
                ConfirmRow("Categoría", category)
                ConfirmRow("Fecha Emisión", SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()))
                
                val dueDateStr = if (dueDate != null) SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dueDate)) else "Indefinido"
                ConfirmRow("Fecha Límite", dueDateStr)
                
                if (installmentsCount > 1) {
                    ConfirmRow("Plan de Pago", "$installmentsCount cuotas (${installmentsFreq.lowercase()})")
                }
                
                Spacer(Modifier.height(24.dp))
                
                if (requiresAcceptance) {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(GoldPremium.copy(alpha = 0.1f)).padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.CloudSync, null, tint = GoldPremium)
                            Text(
                                text = "El préstamo debe ser aceptado por el usuario para que la deuda sea oficial.",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldPremium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF4CAF50).copy(0.1f)).padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.TrendingUp, null, tint = Color(0xFF4CAF50))
                            Text(
                                text = "Este monto se descontará de tu balance una vez registrado.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = Color.Black)
        ) {
            Icon(Icons.Default.CheckCircle, null)
            Spacer(Modifier.width(12.dp))
            Text("Confirmar y Enviar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun FriendItem(
    friend: FriendEntity, 
    isSelected: Boolean, 
    isPendingAcceptance: Boolean = false,
    onClick: () -> Unit
) {
    val containerColor = if (isPendingAcceptance) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        else if (isSelected) GoldPremium.copy(alpha = 0.15f) 
                        else MaterialTheme.colorScheme.surface
    
    val borderColor = if (isPendingAcceptance) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                     else if (isSelected) GoldPremium 
                     else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .alpha(if (isPendingAcceptance) 0.6f else 1f),
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        shadowElevation = if (isSelected) 8.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Section
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (friend.isRegistered) GoldPremium.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (friend.isRegistered) {
                    Text(
                        text = friend.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = GoldPremium
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // Info Section
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = friend.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )

                    if (friend.isRegistered) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2196F3)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check,
                                null,
                                modifier = Modifier.size(10.dp),
                                tint = Color.White
                            )
                        }
                    }
                }                
                Spacer(Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Default.Phone,
                        null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = friend.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Status Chip
                Surface(
                    color = if (isPendingAcceptance) Color.Gray.copy(alpha = 0.1f)
                            else if (friend.isRegistered) GoldPremium.copy(alpha = 0.1f) 
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isPendingAcceptance) "ESPERANDO ACEPTACIÓN"
                               else if (friend.isRegistered) "USUARIO DEUDOM" 
                               else "CONTACTO MANUAL",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isPendingAcceptance) Color.Gray
                                else if (friend.isRegistered) GoldPremium 
                                else MaterialTheme.colorScheme.secondary,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Selection Indicator
            if (isPendingAcceptance) {
                Icon(
                    Icons.Default.HourglassTop,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                AnimatedVisibility(
                    visible = isSelected,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = GoldPremium,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                if (!isSelected) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(message: String, icon: ImageVector) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(12.dp))
        Text(message, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun ConfirmRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun SelectionCard(title: String, desc: String, icon: ImageVector, selected: Boolean, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = if (selected) color.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, if (selected) color else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(56.dp), shape = RoundedCornerShape(16.dp), color = if (selected) color else MaterialTheme.colorScheme.surfaceVariant) {
                Icon(icon, null, modifier = Modifier.padding(14.dp), tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Text(title, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
fun HorizontalDivider(
    modifier: Modifier = Modifier,
    thickness: androidx.compose.ui.unit.Dp = 1.dp,
    color: Color = MaterialTheme.colorScheme.outlineVariant,
    alpha: Float = 1f
) {
    androidx.compose.material3.HorizontalDivider(
        modifier = modifier,
        thickness = thickness,
        color = color.copy(alpha = alpha)
    )
}
