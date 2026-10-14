package com.deudom.deudom.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deudom.deudom.data.PersonalExpense
import com.deudom.deudom.ui.theme.GoldPremium
import com.deudom.deudom.util.CurrencyManager
import java.text.SimpleDateFormat
import java.util.*
import com.deudom.deudom.ui.components.AutoResizedText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalExpensesHistoryScreen(
    expenses: List<PersonalExpense>,
    onDismiss: () -> Unit,
    selectedDay: String,
    selectedMonth: String,
    selectedYear: String,
    onDayFilterClick: () -> Unit,
    onMonthFilterClick: () -> Unit,
    onYearFilterClick: () -> Unit,
    onClearFilters: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp).statusBarsPadding().navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = GoldPremium)
                }
                Text("Mis Gastos Personales", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedDay.isNotEmpty(),
                    onClick = onDayFilterClick,
                    label = { Text(selectedDay.ifEmpty { "Día" }) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldPremium.copy(alpha = 0.9f),
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                FilterChip(
                    selected = selectedMonth.isNotEmpty(),
                    onClick = onMonthFilterClick,
                    label = { Text(selectedMonth.ifEmpty { "Mes" }) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldPremium.copy(alpha = 0.9f),
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                FilterChip(
                    selected = selectedYear.isNotEmpty(),
                    onClick = onYearFilterClick,
                    label = { Text(selectedYear.ifEmpty { "Año" }) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldPremium.copy(alpha = 0.9f),
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                if (selectedDay.isNotEmpty() || selectedMonth.isNotEmpty() || selectedYear.isNotEmpty()) {
                    Button(
                        onClick = onClearFilters,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Limpiar", fontSize = 12.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            if (expenses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.History, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Text("No hay gastos para los filtros seleccionados.", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(expenses.sortedByDescending { it.date }) { exp ->
                        PersonalExpenseItem(exp)
                    }
                }
            }
        }
    }
}

@Composable
fun PersonalExpenseItem(expense: PersonalExpense) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.description.ifEmpty { "Gasto sin concepto" }, fontWeight = FontWeight.Bold)
                Text(
                    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(expense.date)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(Modifier.width(8.dp))
            AutoResizedText(
                text = "- RD$ ${String.format("%.2f", expense.amount)}",
                color = Color(0xFFF44336),
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                modifier = Modifier.weight(0.4f)
            )
        }
    }
}
