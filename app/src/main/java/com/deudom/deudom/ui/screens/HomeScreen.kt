package com.deudom.deudom.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.deudom.deudom.data.Loan
import com.deudom.deudom.data.LoanStatus
import com.deudom.deudom.data.PersonalExpense
import com.deudom.deudom.data.FirestoreService
import com.deudom.deudom.util.Prefs
import com.deudom.deudom.viewmodel.LoanViewModel
import com.deudom.deudom.viewmodel.AIAssistantViewModel
import com.deudom.deudom.viewmodel.AIUiState
import com.deudom.deudom.viewmodel.AIAction
import com.deudom.deudom.ui.components.AIVoiceDialog
import com.deudom.deudom.ui.theme.GoldPremium
import org.koin.androidx.compose.koinViewModel
import java.util.UUID
import android.widget.Toast
import kotlinx.coroutines.launch

enum class HomeTab {
    FRIENDS, HISTORY, DASHBOARD, ANALYTICS, PROFILE
}

@Composable
fun HomeScreen(
    userPhone: String,
    onLogout: () -> Unit,
    currentTheme: Prefs.ThemeMode,
    onThemeChange: (Prefs.ThemeMode) -> Unit,
    onThemeIdChange: (String) -> Unit,
    onIconPackChange: () -> Unit,
    initialLoanId: String? = null
) {
    val viewModel: LoanViewModel = koinViewModel()
    val aiViewModel: AIAssistantViewModel = koinViewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val tabs = HomeTab.entries
    val pagerState = rememberPagerState(initialPage = 2) { tabs.size }
    
    var showPersonalHistory by remember { mutableStateOf(false) }
    var showLendScreen by remember { mutableStateOf(false) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var showRewardsScreen by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var showAIVoiceDialog by remember { mutableStateOf(false) }
    
    // NEW: Loan Selection states for detail and payment
    var selectedLoanForDetail by remember { mutableStateOf<Loan?>(null) }
    var selectedLoanForPayment by remember { mutableStateOf<Loan?>(null) }
    
    var historyInitialTab by remember { mutableIntStateOf(0) }
    var lendInitialPhone by remember { mutableStateOf("") }
    var lendInitialName by remember { mutableStateOf("") }

    val user = viewModel.currentUser
    val lenderLoans = viewModel.lenderLoans
    val borrowerLoans = viewModel.borrowerLoans
    val personalExpenses by viewModel.personalExpenses.collectAsState()
    val loading = viewModel.isLoading
    val privacy = viewModel.isPrivacyMode
    val aiUiState by aiViewModel.uiState.collectAsState()

    LaunchedEffect(userPhone) {
        viewModel.loadLoans(userPhone)
    }

    // Deep link handling logic (restored)
    LaunchedEffect(initialLoanId, lenderLoans, borrowerLoans) {
        if (initialLoanId != null) {
            val loan = (lenderLoans + borrowerLoans).find { it.id == initialLoanId }
            if (loan != null) {
                selectedLoanForDetail = loan
            }
        }
    }

    LaunchedEffect(aiUiState) {
        if (aiUiState is AIUiState.Success) {
            val success = aiUiState as AIUiState.Success
            if (success.isFinal) {
                val action = success.action
                scope.launch {
                    when (action) {
                        is AIAction.AddExpense -> {
                            val exp = PersonalExpense(userPhone = userPhone, amount = action.amount, description = action.description, category = action.category)
                            FirestoreService.addPersonalExpense(exp)
                            viewModel.loadLoans(userPhone)
                        }
                        is AIAction.AddLoan -> {
                            val loan = Loan(
                                id = UUID.randomUUID().toString(),
                                lenderPhone = if (action.isLender) userPhone else "",
                                lenderName = if (action.isLender) (user?.name ?: "Yo") else action.friendName,
                                borrowerPhone = if (action.isLender) "" else userPhone,
                                borrowerName = if (action.isLender) action.friendName else (user?.name ?: "Yo"),
                                amount = action.amount,
                                description = action.description,
                                date = System.currentTimeMillis(),
                                category = action.category,
                                status = LoanStatus.PENDING,
                                currency = "RD$",
                                dueDate = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L)
                            )
                            FirestoreService.createLoan(loan)
                            viewModel.loadLoans(userPhone)
                        }
                        else -> {}
                    }
                    aiViewModel.resetState()
                }
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            if (pagerState.currentPage == 2 && !showPersonalHistory && selectedLoanForDetail == null && selectedLoanForPayment == null) { 
                FloatingActionButton(
                    onClick = { showAIVoiceDialog = true },
                    containerColor = GoldPremium,
                    contentColor = Color.Black,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.AutoAwesome, "Jarvis")
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = { 
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        icon = { 
                            val icon = when(tab) {
                                HomeTab.FRIENDS -> Icons.Default.People
                                HomeTab.HISTORY -> Icons.Default.History
                                HomeTab.DASHBOARD -> Icons.Default.Home
                                HomeTab.ANALYTICS -> Icons.Default.BarChart
                                HomeTab.PROFILE -> Icons.Default.Person
                            }
                            
                            val badgeCount = when(tab) {
                                HomeTab.HISTORY -> borrowerLoans.count { it.status == LoanStatus.REQUESTED }
                                else -> 0
                            }

                            if (badgeCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = GoldPremium,
                                            contentColor = Color.Black
                                        ) {
                                            Text(badgeCount.toString(), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                        }
                                    }
                                ) {
                                    Icon(icon, null)
                                }
                            } else {
                                Icon(icon, null)
                            }
                        },
                        label = { 
                            val label = when(tab) {
                                HomeTab.FRIENDS -> "Amigos"
                                HomeTab.HISTORY -> "Deudas"
                                HomeTab.DASHBOARD -> "Home"
                                HomeTab.ANALYTICS -> "Análisis"
                                HomeTab.PROFILE -> "Perfil"
                            }
                            Text(label)
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(innerPadding),
            beyondViewportPageCount = 2
        ) { page ->
            when (tabs[page]) {
                HomeTab.FRIENDS -> FriendsView(
                    userPhone = userPhone,
                    loans = lenderLoans + borrowerLoans,
                    localFriends = viewModel.localFriends,
                    onNewLoanClick = { phone, name ->
                        lendInitialPhone = phone
                        lendInitialName = name
                        showLendScreen = true
                    },
                    onAddManualFriend = { name, phone ->
                        viewModel.addManualFriend(name, phone)
                    }
                )
                HomeTab.HISTORY -> HistoryView(
                    userPhone = userPhone,
                    lender = lenderLoans,
                    borrower = borrowerLoans,
                    privacy = privacy,
                    onLoan = { selectedLoanForDetail = it },
                    onPayment = { selectedLoanForPayment = it },
                    onRef = { viewModel.loadLoans(userPhone) }
                )
                HomeTab.DASHBOARD -> DashboardView(
                    user = user,
                    lender = lenderLoans,
                    borrower = borrowerLoans,
                    personalExpenses = personalExpenses,
                    savingGoals = viewModel.savingGoals,
                    loading = loading,
                    privacy = privacy,
                    onPriv = { viewModel.togglePrivacy() },
                    onLend = { 
                        lendInitialPhone = ""
                        lendInitialName = ""
                        showLendScreen = true 
                    },
                    onLoan = { selectedLoanForDetail = it },
                    onPersonalHistoryClick = { 
                        showPersonalHistory = true 
                    },
                    onRef = { 
                        viewModel.loadLoans(userPhone)
                    },
                    showExpenseDialog = showExpenseDialog,
                    onShowExpenseDialogChange = { showExpenseDialog = it }
                )
                HomeTab.ANALYTICS -> AnalyticsView(
                    user = user,
                    lender = lenderLoans,
                    borrower = borrowerLoans,
                    goals = viewModel.savingGoals,
                    onRef = { viewModel.loadLoans(userPhone) }
                )
                HomeTab.PROFILE -> ProfileView(
                    userPhone = userPhone,
                    onLogout = {
                        onLogout()
                        viewModel.logout(context)
                    },
                    onSettingsClick = { showSettingsScreen = true },
                    onShowRewards = { showRewardsScreen = true }
                )
            }
        }
    }

    // --- FULL SCREEN OVERLAYS ---

    if (showPersonalHistory) {
        PersonalHistoryScreen(
            expenses = personalExpenses,
            privacy = privacy,
            onClose = { showPersonalHistory = false }
        )
    }

    if (selectedLoanForDetail != null) {
        // Find the most up-to-date version of this loan from the lists
        val currentLoan = (lenderLoans + borrowerLoans).find { it.id == selectedLoanForDetail?.id } ?: selectedLoanForDetail!!
        
        LoanDetailScreen(
            loan = currentLoan,
            userPhone = userPhone,
            privacy = privacy,
            onDismiss = { selectedLoanForDetail = null },
            onRef = { viewModel.loadLoans(userPhone) }
        )
    }

    if (selectedLoanForPayment != null) {
        PaymentScreen(
            loan = selectedLoanForPayment!!,
            onDismiss = { selectedLoanForPayment = null },
            onConfirmPayment = { amt, desc ->
                viewModel.registerPayment(selectedLoanForPayment!!, amt, desc)
                selectedLoanForPayment = null
            },
            onSettleFull = {
                viewModel.settleDebt(selectedLoanForPayment!!)
                selectedLoanForPayment = null
            }
        )
    }

    if (showAIVoiceDialog) {
        val aiResponse = if (aiUiState is AIUiState.Success) {
            val action = (aiUiState as AIUiState.Success).action
            if (action is AIAction.ChatResponse) action.message else null
        } else if (aiUiState is AIUiState.Error) {
            (aiUiState as AIUiState.Error).message
        } else null

        val isProcessing = aiUiState is AIUiState.Loading
        val isFinal = if (aiUiState is AIUiState.Success) (aiUiState as AIUiState.Success).isFinal else false

        com.deudom.deudom.ui.components.AIVoiceDialog(
            onDismiss = { 
                showAIVoiceDialog = false
                aiViewModel.resetState()
                aiViewModel.clearChat()
            },
            onResult = { text ->
                val friendsNames = viewModel.localFriends.map { it.name }
                aiViewModel.processInput(text, friendsNames)
            },
            aiResponse = aiResponse,
            isProcessing = isProcessing,
            isFinalAction = isFinal
        )
    }

    if (showLendScreen) {
        LendScreen(
            userPhone = userPhone,
            initialPhone = lendInitialPhone,
            initialName = lendInitialName,
            onDismiss = { showLendScreen = false },
            onLoanCreated = { 
                showLendScreen = false
                viewModel.loadLoans(userPhone)
            }
        )
    }

    if (showSettingsScreen) {
        SettingsScreen(
            currentTheme = currentTheme,
            onThemeChange = onThemeChange,
            onThemeIdChange = onThemeIdChange,
            onClose = { showSettingsScreen = false },
            onShowDeleteDialog = { /* Handle delete account */ }
        )
    }

    if (showRewardsScreen) {
        RewardsScreen(
            userPhone = userPhone,
            onDismiss = { showRewardsScreen = false },
            onSettingsChange = { type, id ->
                if (type == "theme") onThemeIdChange(id)
                else if (type == "icon") {
                    Prefs.setIconPack(context, id)
                    onIconPackChange()
                }
                showRewardsScreen = false
            }
        )
    }
}