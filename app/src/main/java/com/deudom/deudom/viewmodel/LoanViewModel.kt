package com.deudom.deudom.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.deudom.deudom.data.FirestoreService
import com.deudom.deudom.data.Loan
import com.deudom.deudom.data.LoanStatus
import com.deudom.deudom.data.User
import com.deudom.deudom.data.PersonalExpense
import com.deudom.deudom.data.SavingGoal
import com.deudom.deudom.data.local.DeudomDatabase
import com.deudom.deudom.repository.DeudomRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import com.deudom.deudom.util.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class LoanViewModel(
    application: Application,
    private val repository: DeudomRepository
) : AndroidViewModel(application) {
    private var lenderListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var borrowerListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var userListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var personalExpensesListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var savingGoalsListener: com.google.firebase.firestore.ListenerRegistration? = null
    
    var lenderLoans by mutableStateOf<List<Loan>>(emptyList())
    var borrowerLoans by mutableStateOf<List<Loan>>(emptyList())
    // New: Friends from local DB
    var localFriends by mutableStateOf<List<com.deudom.deudom.data.local.FriendEntity>>(emptyList())
    var sentPendingRequests by mutableStateOf<List<com.deudom.deudom.data.FriendRequest>>(emptyList())
    
    private val _personalExpenses = MutableStateFlow<List<PersonalExpense>>(emptyList())
    val personalExpenses = _personalExpenses.asStateFlow()
    var savingGoals by mutableStateOf<List<SavingGoal>>(emptyList())
    var currentUser by mutableStateOf<User?>(null)
    var isLoading by mutableStateOf(true)
    var isPrivacyMode by mutableStateOf(true)

    // Filter States
    private val _selectedDay = MutableStateFlow("")
    val selectedDay = _selectedDay.asStateFlow()

    private val _selectedMonth = MutableStateFlow("")
    val selectedMonth = _selectedMonth.asStateFlow()

    private val _selectedYear = MutableStateFlow("")
    val selectedYear = _selectedYear.asStateFlow()

    // --- Performance Optimizations: Derived State Flows for Dashboard ---
    val weeklySpent = _personalExpenses.map { expenses ->
        val weekMillis = 7 * 24 * 60 * 60 * 1000L
        val weekAgo = System.currentTimeMillis() - weekMillis
        expenses.filter { it.date > weekAgo }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val topCategory = _personalExpenses.map { expenses ->
        val now = Calendar.getInstance()
        val currentMonth = now.get(Calendar.MONTH)
        val currentYear = now.get(Calendar.YEAR)
        
        expenses
            .filter { 
                val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
            }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { e -> e.amount } }
            .maxByOrNull { it.value }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    // -----------------------------------------------------------------

    val filteredPersonalExpenses = combine(
        _personalExpenses,
        _selectedDay,
        _selectedMonth,
        _selectedYear
    ) { expenses, day, month, year ->
        expenses.filter { expense ->
            val cal = Calendar.getInstance().apply { timeInMillis = expense.date }
            val dayMatch = day.isEmpty() || day == cal.get(Calendar.DAY_OF_MONTH).toString()
            val monthMatch = month.isEmpty() || month == SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time)
            val yearMatch = year.isEmpty() || year == cal.get(Calendar.YEAR).toString()
            dayMatch && monthMatch && yearMatch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setDayFilter(day: String) {
        _selectedDay.value = day
    }

    fun setMonthFilter(month: String) {
        _selectedMonth.value = month
    }

    fun setYearFilter(year: String) {
        _selectedYear.value = year
    }

    fun clearFilters() {
        _selectedDay.value = ""
        _selectedMonth.value = ""
        _selectedYear.value = ""
    }

    fun loadLoans(phone: String) {
        viewModelScope.launch {
            isLoading = true
            
            // Setup Real-time listeners for user data
            userListener?.remove()
            personalExpensesListener?.remove()
            savingGoalsListener?.remove()
            
            userListener = FirestoreService.listenToUser(phone) { user ->
                currentUser = user
                user?.let { 
                    Prefs.updateWidgetData(getApplication(), it.currentBalance, it.baseCurrency) 
                }
            }
            
            personalExpensesListener = FirestoreService.listenToPersonalExpenses(phone) { expenses ->
                _personalExpenses.value = expenses
            }
            
            savingGoalsListener = FirestoreService.listenToSavingGoals(phone) { goals ->
                savingGoals = goals
            }

            // Listen to sent pending requests to block lending to unaccepted verified users
            FirestoreService.listenToSentPendingRequests(phone) { list ->
                sentPendingRequests = list
            }
            
            // Local stream for UI responsiveness - Primary source of truth
            repository.getLoans(phone).collectLatest { all ->
                lenderLoans = all.filter { it.lenderPhone == phone }
                borrowerLoans = all.filter { it.borrowerPhone == phone }
                isLoading = false
            }
        }
        
        // Start friend sync in background
        viewModelScope.launch {
            repository.getFriends().collectLatest { friends ->
                localFriends = friends
            }
        }
        viewModelScope.launch {
            try {
                repository.refreshFriends(phone)
            } catch (e: Exception) { /* Offline */ }
        }
        
        // Setup Real-time listeners (Best effort sync)
        try {
            lenderListener?.remove()
            borrowerListener?.remove()
            
            lenderListener = FirestoreService.listenToLoansAsLender(phone) { loans ->
                viewModelScope.launch {
                    repository.insertLoans(loans)
                }
            }
            
            borrowerListener = FirestoreService.listenToLoansAsBorrower(phone) { loans ->
                viewModelScope.launch {
                    repository.insertLoans(loans)
                }
            }
        } catch (e: Exception) {
            // Ignore listener errors in offline mode
        }
    }

    // Offline & Hybrid Operations
    fun createOfflineLoan(loan: Loan) {
        viewModelScope.launch {
            repository.insertLoan(loan)
            com.deudom.deudom.worker.SyncWorker.scheduleNow(getApplication())
        }
    }

    fun registerPayment(loan: Loan, amount: Double, description: String? = null) {
        viewModelScope.launch {
            val updatedLoan = repository.addPayment(loan, amount, description)
            // Immediate cloud sync attempt
            try {
                if (FirestoreService.createLoan(updatedLoan)) {
                    repository.markAsSynced(loan.id)
                }
            } catch (e: Exception) {
                // Fail silently, Worker will retry
            }
            com.deudom.deudom.worker.SyncWorker.scheduleNow(getApplication())
        }
    }

    fun settleDebt(loan: Loan) {
        viewModelScope.launch {
            val updatedLoan = repository.settleLoan(loan)
            // Immediate cloud sync attempt
            try {
                if (FirestoreService.createLoan(updatedLoan)) {
                    repository.markAsSynced(loan.id)
                }
            } catch (e: Exception) {
                // Fail silently, Worker will retry
            }
            com.deudom.deudom.worker.SyncWorker.scheduleNow(getApplication())
        }
    }

    fun togglePrivacy() {
        isPrivacyMode = !isPrivacyMode
    }
    
    fun addManualFriend(name: String, phone: String) {
        viewModelScope.launch {
            val friend = com.deudom.deudom.data.local.FriendEntity(
                phone = phone,
                name = name,
                photoUrl = null
            )
            repository.insertFriend(friend)
        }
    }
    
    fun editManualFriend(oldPhone: String, newPhone: String, newName: String) {
        viewModelScope.launch {
            repository.updateManualFriend(oldPhone, newPhone, newName)
        }
    }
    
    fun deleteFriend(phone: String, firestoreId: String? = null) {
        viewModelScope.launch {
            repository.deleteFriend(phone, firestoreId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        lenderListener?.remove()
        borrowerListener?.remove()
        userListener?.remove()
        personalExpensesListener?.remove()
        savingGoalsListener?.remove()
    }

    fun updateStatus(loan: Loan, status: LoanStatus, phone: String) {
        viewModelScope.launch {
            // Optimistic update
            val updatedLoan = loan.copy(status = status)
            repository.updateLoan(updatedLoan)
            
            // Try remote update
            try {
                if (FirestoreService.updateLoanStatus(loan, status)) {
                   // Remote success
                }
            } catch (e: Exception) {
                // Failed to sync, but local is updated
            }
        }
    }

    fun addPersonalExpense(expense: PersonalExpense, context: android.content.Context) {
        viewModelScope.launch {
            if (FirestoreService.addPersonalExpense(expense)) {
                // Success
            }
            // Refresh
            loadLoans(expense.userPhone)
        }
    }

    fun logout(context: android.content.Context) {
        viewModelScope.launch {
            // Trigger an immediate sync before clearing to save pending changes
            try {
                val unsynced = repository.getUnsyncedLoansSync()
                if (unsynced.isNotEmpty()) {
                    FirestoreService.createMultipleLoans(unsynced)
                }
            } catch (e: Exception) {
                android.util.Log.e("LoanViewModel", "Final sync failed: ${e.message}")
            }

            // Clear data storage FIRST
            repository.clearAllData()
            Prefs.clearUserData(context)
            
            // Clear UI States LAST to avoid NullPointerExceptions during transition
            lenderLoans = emptyList()
            borrowerLoans = emptyList()
            localFriends = emptyList()
            _personalExpenses.value = emptyList()
            currentUser = null
        }
    }
}
