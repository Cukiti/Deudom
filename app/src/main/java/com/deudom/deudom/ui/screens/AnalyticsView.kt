package com.deudom.deudom.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deudom.deudom.data.Loan
import com.deudom.deudom.data.LoanStatus
import com.deudom.deudom.data.User
import com.deudom.deudom.data.SavingGoal
import com.deudom.deudom.data.FirestoreService
import com.deudom.deudom.ui.theme.GoldPremium
import com.deudom.deudom.util.CategoryProvider
import com.deudom.deudom.util.CurrencyManager
import kotlinx.coroutines.launch
import java.util.Calendar
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import com.deudom.deudom.R
import com.deudom.deudom.ui.components.AutoResizedText
import com.deudom.deudom.ui.components.PremiumPullToRefresh
import kotlinx.coroutines.delay

@Composable
fun AnalyticsView(
    user: User?, 
    lender: List<Loan>, 
    borrower: List<Loan>, 
    goals: List<SavingGoal> = emptyList(),
    onRef: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    val baseCurrency = user?.baseCurrency ?: "RD$"
    
    val totalLent = lender.filter { it.status != LoanStatus.PAID }.sumOf { 
        CurrencyManager.convert(it.amount - it.paidAmount, it.currency, baseCurrency)
    }
    val totalBorrowed = borrower.filter { it.status != LoanStatus.PAID }.sumOf { 
        CurrencyManager.convert(it.amount - it.paidAmount, it.currency, baseCurrency)
    }
    
    val netWorth = totalLent - totalBorrowed
    val cashInHand = CurrencyManager.convert(user?.currentBalance ?: 0.0, "RD$", baseCurrency)
    val totalWealth = cashInHand + totalLent - totalBorrowed
    
    val mutualDebts = remember(lender, borrower) {
        val activeLender = lender.filter { it.status == LoanStatus.PENDING || it.status == LoanStatus.OVERDUE }
        val activeBorrower = borrower.filter { it.status == LoanStatus.PENDING || it.status == LoanStatus.OVERDUE }
        val list = mutableListOf<Pair<Loan, Loan>>()
        activeLender.forEach { l ->
            val matching = activeBorrower.find { b -> b.lenderPhone == l.borrowerPhone }
            if (matching != null) list.add(l to matching)
        }
        list
    }

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
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Mi Resumen Financiero", 
                style = MaterialTheme.typography.headlineMedium, 
                fontWeight = FontWeight.ExtraBold, 
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Entiende cómo se mueve tu dinero de un vistazo", 
                style = MaterialTheme.typography.bodyMedium, 
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            // MAIN PREMIUM CARD (Patrimonio Neto)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF2A2A2A), Color(0xFF121212))
                        )
                    )
            ) {
                // Decorative Circle
                Box(modifier = Modifier.align(Alignment.TopEnd).offset(x = 40.dp, y = (-40).dp).size(150.dp).background(Brush.radialGradient(listOf(GoldPremium.copy(alpha=0.15f), Color.Transparent)), shape = CircleShape))

                Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("BALANCE DE PRÉSTAMOS", style = MaterialTheme.typography.labelSmall, color = Color.LightGray.copy(alpha=0.8f), letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    AutoResizedText(
                        text = CurrencyManager.format(netWorth, baseCurrency),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = GoldPremium
                    )
                    
                    Spacer(Modifier.height(32.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("A mi favor", color = Color.LightGray, style = MaterialTheme.typography.labelMedium)
                            }
                            Text(CurrencyManager.format(totalLent, baseCurrency), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        
                        Box(Modifier.width(1.dp).height(32.dp).background(Color.White.copy(alpha = 0.2f)))
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Lo que debo", color = Color.LightGray, style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFFF44336), modifier = Modifier.size(16.dp))
                            }
                            Text(CurrencyManager.format(totalBorrowed, baseCurrency), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
    
            if (mutualDebts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SyncAlt, null, tint = GoldPremium)
                    Spacer(Modifier.width(8.dp))
                    Text("Oportunidad de ahorro", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Text("Detectamos deudas mutuas. ¡Simplifícalas para cruzar cuentas y pagar menos!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary); Spacer(modifier = Modifier.height(16.dp))
                
                mutualDebts.forEach { (loanLent, loanBorrowed) ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), 
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), 
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) { 
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(GoldPremium.copy(0.2f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, null, tint = GoldPremium)
                                }
                                Spacer(Modifier.width(12.dp))
                                Text("Con ${loanLent.borrowerName}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Le debes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                    Text(CurrencyManager.format(loanBorrowed.amount - loanBorrowed.paidAmount, loanBorrowed.currency), fontWeight = FontWeight.Bold, color = Color(0xFFF44336))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Te debe", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                    Text(CurrencyManager.format(loanLent.amount - loanLent.paidAmount, loanLent.currency), fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { scope.launch { if (FirestoreService.simplifyDebts(loanLent, loanBorrowed)) onRef() } }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = GoldPremium), shape = RoundedCornerShape(12.dp)) { 
                                Text("SIMPLIFICAR DEUDA", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp) 
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Text("Proyección del Mes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(16.dp))
            
            // Forecast logic
            val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val dueThisMonth = lender.filter { 
                it.status == LoanStatus.PENDING || it.status == LoanStatus.OVERDUE
            }.filter { 
                val cal = Calendar.getInstance().apply { timeInMillis = it.dueDate ?: 0 }
                cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
            }.sumOf { CurrencyManager.convert(it.amount - it.paidAmount, it.currency, baseCurrency) }
            
            val toPayThisMonth = borrower.filter { 
                it.status == LoanStatus.PENDING || it.status == LoanStatus.OVERDUE
            }.filter { 
                val cal = Calendar.getInstance().apply { timeInMillis = it.dueDate ?: 0 }
                cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
            }.sumOf { CurrencyManager.convert(it.amount - it.paidAmount, it.currency, baseCurrency) }
            
            Card(
                modifier = Modifier.fillMaxWidth(), 
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF64B5F6).copy(0.15f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.DateRange, null, tint = Color(0xFF64B5F6))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Estimado fin de mes", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Si se pagan todas las deudas del mes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    val projected = netWorth + dueThisMonth - toPayThisMonth
                    AutoResizedText(
                        text = CurrencyManager.format(projected, baseCurrency),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = if (projected >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                    
                    Spacer(Modifier.height(20.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Entradas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                Text(CurrencyManager.format(dueThisMonth, baseCurrency), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Salidas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                Text(CurrencyManager.format(toPayThisMonth, baseCurrency), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.TrendingDown, null, tint = Color(0xFFF44336), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("Salud Financiera", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(16.dp))
            
            val financialScore = remember(user, lender, borrower) {
                var score = 70 // Base
                if ((user?.cukitis ?: 0) > 100) score += 10
                if (borrower.any { it.status == LoanStatus.OVERDUE }) score -= 20
                if (lender.any { it.status == LoanStatus.OVERDUE }) score -= 5
                
                val debtRatio = if (totalWealth > 0) totalBorrowed / totalWealth else 1.0
                if (debtRatio > 0.5) score -= 15
                else if (debtRatio < 0.2) score += 5
                
                score.coerceIn(0, 100)
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                        CircularProgressIndicator(
                            progress = { financialScore / 100f },
                            modifier = Modifier.fillMaxSize(),
                            color = when {
                                financialScore >= 80 -> Color(0xFF4CAF50)
                                financialScore >= 50 -> GoldPremium
                                else -> Color(0xFFF44336)
                            },
                            strokeWidth = 12.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$financialScore", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                            Text("Puntos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    Text(
                        when {
                            financialScore >= 80 -> "¡Excelente control! Tu salud financiera es sólida."
                            financialScore >= 50 -> "Vas por buen camino, pero hay margen de mejora."
                            else -> "¡Alerta! Tu nivel de deuda es riesgoso."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    Spacer(Modifier.height(16.dp))
                    
                    Text("CONSEJOS INTELIGENTES", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = GoldPremium, letterSpacing = 1.sp)
                    Spacer(Modifier.height(12.dp))
                    
                    val tips = mutableListOf<String>()
                    if (borrower.any { it.status == LoanStatus.OVERDUE }) tips.add("Paga tus deudas vencidas lo antes posible para evitar recargos.")
                    if (totalBorrowed > totalLent * 2) tips.add("Estás tomando más prestado de lo que prestas. Intenta reducir gastos.")
                    if (user?.emergencyReserve ?: 0.0 == 0.0) tips.add("Empieza a construir tu reserva de emergencia para mayor seguridad.")
                    if (tips.isEmpty()) tips.add("¡Sigue así! Mantén tus pagos al día para seguir subiendo de nivel.")
                    
                    tips.forEach { tip ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Lightbulb, null, tint = GoldPremium, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(tip, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("Distribución de mis Fondos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    FinancialPulseBar(icon = Icons.Default.AccountBalanceWallet, label = "Dinero en mano", amount = cashInHand, total = if (totalWealth > 0) totalWealth else 1.0, color = Color(0xFF64B5F6), currency = baseCurrency)
                    Spacer(Modifier.height(24.dp))
                    FinancialPulseBar(icon = Icons.Default.FlightTakeoff, label = "Dinero prestado (en la calle)", amount = totalLent, total = if (totalWealth > 0) totalWealth else 1.0, color = GoldPremium, currency = baseCurrency)
                    Spacer(Modifier.height(24.dp))
                    FinancialPulseBar(icon = Icons.Default.Savings, label = "Mi capital real", amount = totalWealth, total = if (totalWealth > 0) totalWealth else 1.0, color = Color(0xFF4CAF50), isTotal = true, currency = baseCurrency)
                }
            }
    
            Spacer(modifier = Modifier.height(32.dp))
            Text("Categorías de Préstamos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val categoryTotals = (lender + borrower).groupBy { it.category }.mapValues { entry -> entry.value.sumOf { l -> CurrencyManager.convert(l.amount, l.currency, baseCurrency) } }.toList().sortedByDescending { it.second }
                    if (categoryTotals.isNotEmpty()) {
                        val totalAll = categoryTotals.sumOf { it.second }
                        categoryTotals.forEach { (catName, total) ->
                            val catEmoji = CategoryProvider.getEmoji(catName); val percentage = if (totalAll > 0) (total / totalAll * 100).toInt() else 0
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                    Text(catEmoji, fontSize = 24.sp)
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) { 
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { 
                                        Text(catName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                        val budget = user?.categoryBudgets?.get(catName) ?: 0.0
                                        if (budget > 0) {
                                            Text("${CurrencyManager.format(total, baseCurrency)} / ${CurrencyManager.format(budget, baseCurrency)}", style = MaterialTheme.typography.labelMedium, color = if (total > budget) Color.Red else GoldPremium)
                                        } else {
                                            Text("$percentage%", style = MaterialTheme.typography.labelMedium, color = GoldPremium, fontWeight = FontWeight.Bold) 
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    val budget = user?.categoryBudgets?.get(catName) ?: 0.0
                                    val progVal = if (budget > 0) (total / budget).toFloat() else if (totalAll > 0) (total / totalAll).toFloat() else 0f
                                    
                                    LinearProgressIndicator(
                                        progress = { progVal.coerceIn(0f, 1f) }, 
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), 
                                        color = if (budget > 0 && total > budget) Color.Red else GoldPremium, 
                                        trackColor = (if (budget > 0 && total > budget) Color.Red else GoldPremium).copy(alpha = 0.15f)
                                    ) 
                                }
                            }
                        }
                    } else { Text("Aún no tienes préstamos registrados para mostrar categorías.", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 16.dp)) }
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun FinancialPulseBar(icon: ImageVector, label: String, amount: Double, total: Double, color: Color, isTotal: Boolean = false, currency: String = "RD$") {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = CurrencyManager.format(amount, currency), 
                    style = MaterialTheme.typography.titleSmall, 
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { (amount / total).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(if (isTotal) 8.dp else 6.dp).clip(CircleShape), color = color, trackColor = color.copy(alpha = 0.15f))
        }
    }
}
