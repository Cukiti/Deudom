package com.deudom.deudom.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deudom.deudom.data.PersonalExpense
import com.deudom.deudom.ui.theme.GoldPremium
import com.deudom.deudom.util.CategoryProvider
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalHistoryScreen(
    expenses: List<PersonalExpense>,
    privacy: Boolean,
    onClose: () -> Unit
) {
    var selectedDay by remember { mutableStateOf("") }
    var selectedMonth by remember { mutableStateOf("") }
    var selectedYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR).toString()) }
    
    val months = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
    val years = (2024..Calendar.getInstance().get(Calendar.YEAR)).map { it.toString() }.reversed()

    val filteredExpenses = remember(expenses, selectedDay, selectedMonth, selectedYear) {
        expenses.filter { expense ->
            val cal = Calendar.getInstance().apply { timeInMillis = expense.date }
            val dayMatch = selectedDay.isEmpty() || cal.get(Calendar.DAY_OF_MONTH).toString() == selectedDay
            val monthMatch = selectedMonth.isEmpty() || SimpleDateFormat("MMM", Locale("es", "ES")).format(cal.time).replaceFirstChar { it.uppercase() }.contains(selectedMonth, ignoreCase = true)
            val yearMatch = selectedYear.isEmpty() || cal.get(Calendar.YEAR).toString() == selectedYear
            dayMatch && monthMatch && yearMatch
        }.sortedByDescending { it.date }
    }

    val totalSpent = filteredExpenses.sumOf { it.amount }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Historial de Gastos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text("${filteredExpenses.size} registros", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cerrar", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            // --- PREMIUM TOTAL PANEL WITH SAFE AREA ---
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(), // CRITICAL: Avoids overlap with phone buttons
                tonalElevation = 12.dp,
                shadowElevation = 24.dp,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("TOTAL ACUMULADO", style = MaterialTheme.typography.labelSmall, color = GoldPremium, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        Text(
                            text = if (privacy) "****" else "RD$ ${String.format("%,.2f", totalSpent)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(GoldPremium, GoldPremium.copy(alpha = 0.7f)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Payments, null, tint = Color.Black, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // --- MODERN COMPACT FILTERS ---
            Card(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Day Selector
                    OutlinedTextField(
                        value = selectedDay,
                        onValueChange = { if (it.length <= 2) selectedDay = it.filter { c -> c.isDigit() } },
                        placeholder = { Text("Día", fontSize = 12.sp) },
                        modifier = Modifier.weight(0.7f).height(52.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPremium)
                    )

                    // Month Selector
                    var monthExp by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { monthExp = true },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Text(if (selectedMonth.isEmpty()) "Mes" else selectedMonth, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(expanded = monthExp, onDismissRequest = { monthExp = false }) {
                            DropdownMenuItem(text = { Text("Todos") }, onClick = { selectedMonth = ""; monthExp = false })
                            months.forEach { m -> DropdownMenuItem(text = { Text(m) }, onClick = { selectedMonth = m; monthExp = false }) }
                        }
                    }

                    // Year Selector
                    var yearExp by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { yearExp = true },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Text(selectedYear, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(expanded = yearExp, onDismissRequest = { yearExp = false }) {
                            years.forEach { y -> DropdownMenuItem(text = { Text(y) }, onClick = { selectedYear = y; yearExp = false }) }
                        }
                    }
                }
            }

            if (filteredExpenses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ReceiptLong, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(Modifier.height(16.dp))
                        Text("Sin gastos en este periodo", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredExpenses) { expense ->
                        PremiumExpenseItem(expense, privacy)
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumExpenseItem(expense: PersonalExpense, privacy: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(CategoryProvider.getEmoji(expense.category), fontSize = 24.sp)
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(Modifier.weight(1f)) {
                Text(expense.description, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                Text(
                    text = "${SimpleDateFormat("dd MMM yyyy", Locale("es", "ES")).format(Date(expense.date))} • ${expense.category}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            Text(
                text = if (privacy) "****" else "- RD$ ${String.format("%.2f", expense.amount)}",
                color = Color(0xFFF44336),
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}