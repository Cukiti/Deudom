package com.deudom.deudom.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.deudom.deudom.R
import com.deudom.deudom.data.FirestoreService
import com.deudom.deudom.data.Group
import com.deudom.deudom.data.GroupExpense
import com.deudom.deudom.data.GroupMember
import com.deudom.deudom.ui.theme.GoldPremium
import com.deudom.deudom.ui.components.PremiumTextField
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import com.deudom.deudom.ui.components.PremiumPullToRefresh
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(group: Group, userPhone: String, onDismiss: () -> Unit) {
    var expenses by remember { mutableStateOf<List<GroupExpense>>(emptyList()) }
    var showAddExpense by remember { mutableStateOf(false) }
    var expenseAmt by remember { mutableStateOf("") }
    var expenseDesc by remember { mutableStateOf("") }
    var selectedParticipants by remember { mutableStateOf(group.members.map { it.phone }.toSet()) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Simplification State
    var showSimplifyDialog by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    DisposableEffect(group.id) {
        val listener = FirestoreService.listenToGroupExpenses(group.id) { 
            expenses = it 
        }
        onDispose {
            listener.remove()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column {
                    Text(group.name, fontWeight = FontWeight.Bold)
                    Text("${group.members.size} miembros", style = MaterialTheme.typography.bodySmall)
                } },
                navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = GoldPremium) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddExpense = true }, containerColor = GoldPremium) {
                Icon(Icons.Default.Add, null, tint = Color.Black)
            }
        }
    ) { p ->
        PremiumPullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    delay(1000)
                    isRefreshing = false
                }
            },
            modifier = Modifier.fillMaxSize().padding(p)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Balance Summary Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Balances del grupo", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                    TextButton(onClick = { showSimplifyDialog = true }) {
                        Text("Simplificar Deudas", color = MaterialTheme.colorScheme.primary)
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                GroupBalancesContent(group, expenses, userPhone)
                
                Spacer(Modifier.height(24.dp))
                Text("Gastos recientes", fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(12.dp))
                
                if (expenses.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Aún no hay gastos en este grupo.", color = MaterialTheme.colorScheme.secondary)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(expenses.sortedByDescending { it.date }) { exp ->
                            ExpenseItem(exp)
                        }
                    }
                }
            }
        }
    }
    
    // Simplify Debts Dialog
    if (showSimplifyDialog) {
        val balances = remember(expenses) { calculateBalances(group, expenses) }
        val simplified = remember(balances) { simplifyDebts(balances, group.members) }
        
        Dialog(onDismissRequest = { showSimplifyDialog = false }) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Plan de Pagos Simplificado", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("La forma más eficiente de saldar todo.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(16.dp))
                    
                    if (simplified.isEmpty()) {
                        Text("¡Todo está saldado! No hay deudas pendientes.", fontWeight = FontWeight.Medium, color = Color(0xFF4CAF50))
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(simplified) { trans ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(0.3f), RoundedCornerShape(8.dp)).padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(trans.from, fontWeight = FontWeight.Bold)
                                        Text("paga a", style = MaterialTheme.typography.bodySmall)
                                        Text(trans.to, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        "RD$ ${String.format("%.2f", trans.amount)}",
                                        fontWeight = FontWeight.Black,
                                        color = GoldPremium,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { showSimplifyDialog = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("Entendido")
                    }
                }
            }
        }
    }

    if (showAddExpense) {
        AlertDialog(
            onDismissRequest = { showAddExpense = false },
            title = { Text("Registrar Gasto") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PremiumTextField(value = expenseAmt, onValueChange = { expenseAmt = it }, label = "Monto", icon = Icons.Default.AttachMoney, keyboardType = KeyboardType.Decimal)
                    PremiumTextField(value = expenseDesc, onValueChange = { expenseDesc = it }, label = "¿En qué se gastó?", icon = Icons.Default.Description)
                    
                    Text("¿Quiénes participan?", fontWeight = FontWeight.Bold, color = GoldPremium)
                    Column(modifier = Modifier.height(150.dp).verticalScroll(rememberScrollState())) {
                        group.members.forEach { m ->
                            val isSelected = selectedParticipants.contains(m.phone)
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { 
                                    // Toggle logic: If in set, remove. If not, add.
                                    val newSet = if (isSelected) selectedParticipants - m.phone else selectedParticipants + m.phone
                                    selectedParticipants = newSet
                                }.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = isSelected, onCheckedChange = null) // Handled by Row click
                                Spacer(Modifier.width(8.dp))
                                Text(if (m.phone == userPhone) "Tú" else m.name)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val a = expenseAmt.toDoubleOrNull() ?: 0.0
                        if (a > 0 && selectedParticipants.isNotEmpty()) {
                            scope.launch {
                                val user = FirestoreService.getUser(userPhone)
                                val exp = GroupExpense(
                                    groupId = group.id,
                                    description = expenseDesc,
                                    amount = a,
                                    payerPhone = userPhone,
                                    payerName = user?.name ?: "Usuario",
                                    participants = selectedParticipants.toList()
                                )
                                if (FirestoreService.addGroupExpense(group.id, exp)) {
                                    showAddExpense = false
                                    expenseAmt = ""
                                    expenseDesc = ""
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPremium)
                ) {
                    Text("AÑADIR", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// Logic extracted for reuse
fun calculateBalances(group: Group, expenses: List<GroupExpense>): Map<String, Double> {
    val balances = mutableMapOf<String, Double>()
    group.members.forEach { balances[it.phone] = 0.0 }
    
    expenses.forEach { exp ->
        // Payer gets credit (+), Participants get debit (-)
        // Wait, logic check:
        // Payer paid 'Amount'. Payer is effectively owed 'Amount' by the group.
        // Each participant consumes 'Amount / N'.
        // So Payer balance += Amount - (Amount/N if Payer is participant)
        
        // Simpler way:
        // Payer paid X. Payer balance += X.
        // Each participant consumes Y. Participant balance -= Y.
        
        balances[exp.payerPhone] = (balances[exp.payerPhone] ?: 0.0) + exp.amount
        
        val split = exp.amount / exp.participants.size
        exp.participants.forEach { p ->
            balances[p] = (balances[p] ?: 0.0) - split
        }
    }
    return balances
}

data class DebtTransaction(val from: String, val to: String, val amount: Double)

fun simplifyDebts(balances: Map<String, Double>, members: List<GroupMember>): List<DebtTransaction> {
    val debtors = balances.filter { it.value < -0.01 }.toMutableMap()
    val creditors = balances.filter { it.value > 0.01 }.toMutableMap()
    val transactions = mutableListOf<DebtTransaction>()
    val memberMap = members.associateBy { it.phone }

    while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
        // Sort to optimize matching largest with largest
        val (debtorPhone, debtAmount) = debtors.entries.sortedBy { it.value }.first() 
        val (creditorPhone, creditAmount) = creditors.entries.sortedByDescending { it.value }.first()

        val amount = minOf(-debtAmount, creditAmount)
        val debtorName = memberMap[debtorPhone]?.name ?: "Miembro"
        val creditorName = memberMap[creditorPhone]?.name ?: "Miembro"

        transactions.add(DebtTransaction(debtorName, creditorName, amount))

        val newDebt = debtAmount + amount // debtAmount is negative, so adding positive reduces debt towards 0
        val newCredit = creditAmount - amount

        if (newDebt > -0.01) debtors.remove(debtorPhone) else debtors[debtorPhone] = newDebt
        if (newCredit < 0.01) creditors.remove(creditorPhone) else creditors[creditorPhone] = newCredit
    }
    return transactions
}

@Composable
fun GroupBalancesContent(group: Group, expenses: List<GroupExpense>, userPhone: String) {
    val balances = remember(expenses) { calculateBalances(group, expenses) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            group.members.forEach { m ->
                val bal = balances[m.phone] ?: 0.0
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (m.phone == userPhone) "Tú" else m.name, fontWeight = if (m.phone == userPhone) FontWeight.Bold else FontWeight.Normal)
                    Text(
                        text = "${if (bal >= 0) "+" else ""}${String.format("%.2f", bal)}",
                        color = if (bal >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun ExpenseItem(expense: GroupExpense) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(expense.description, fontWeight = FontWeight.Bold)
                Text("Pagado por ${expense.payerName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${expense.currency} ${String.format("%.2f", expense.amount)}", fontWeight = FontWeight.Black, color = GoldPremium)
                Text(SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(expense.date)), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
