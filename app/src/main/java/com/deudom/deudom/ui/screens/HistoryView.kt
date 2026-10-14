package com.deudom.deudom.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.deudom.deudom.R
import com.deudom.deudom.data.Loan
import com.deudom.deudom.data.LoanStatus
import com.deudom.deudom.ui.components.LoanItem
import com.deudom.deudom.ui.theme.GoldPremium
import com.deudom.deudom.util.PdfGenerator
import java.text.SimpleDateFormat
import java.util.*

import com.deudom.deudom.ui.components.PremiumPullToRefresh
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.material.icons.filled.TableChart
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryView(
    userPhone: String, 
    lender: List<Loan>, 
    borrower: List<Loan>, 
    privacy: Boolean,
    onLoan: (Loan) -> Unit,
    onPayment: (Loan) -> Unit,
    onRef: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableIntStateOf(0) } // 0: Todos, 1: Prestados, 2: Tomados
    var showPaid by remember { mutableStateOf(true) }
    var showExportMenu by remember { mutableStateOf(false) }
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Historial de Deudas", "Solicitudes")
    
    val allLoans = remember(lender, borrower, searchQuery, selectedFilter, showPaid) {
        (lender + borrower)
            .filter { 
                (searchQuery.isEmpty() || it.lenderName.contains(searchQuery, ignoreCase = true) || it.borrowerName.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true)) &&
                (selectedFilter == 0 || (selectedFilter == 1 && it.lenderPhone == userPhone) || (selectedFilter == 2 && it.borrowerPhone == userPhone)) &&
                (showPaid || it.status != LoanStatus.PAID) &&
                (it.status != LoanStatus.REQUESTED || it.lenderPhone == userPhone) // For history tab, hide incoming requests (where user is borrower)
            }
            .sortedByDescending { it.date }
    }
    
    val groupedLoans = remember(allLoans) {
        allLoans.groupBy { 
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(it.date)).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
    }

    val incomingRequests = remember(borrower) {
        borrower.filter { it.status == LoanStatus.REQUESTED }.sortedByDescending { it.date }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
        ) {
            Column(modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 0.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.history),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )                
                    Box {
                        IconButton(onClick = { showExportMenu = true }, modifier = Modifier.clip(CircleShape).background(GoldPremium.copy(alpha = 0.15f))) {
                            Icon(Icons.Default.Share, "Exportar Reporte", tint = GoldPremium)
                        }
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Reporte de Texto") },
                                onClick = {
                                    showExportMenu = false
                                    val reportText = generateTextReport(allLoans, userPhone)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Mi Historial Financiero - Deudom")
                                        putExtra(Intent.EXTRA_TEXT, reportText)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Enviar Reporte"))
                                },
                                leadingIcon = { Icon(Icons.Default.Description, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Reporte PDF Profesional") },
                                onClick = {
                                    showExportMenu = false
                                    PdfGenerator.generateHistoryPdf(context, userPhone, allLoans)
                                },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, null, tint = Color.Red) }
                            )
                            DropdownMenuItem(
                                text = { Text("Reporte CSV (Excel)") },
                                onClick = {
                                    showExportMenu = false
                                    exportToCsv(context, allLoans, userPhone)
                                },
                                leadingIcon = { Icon(Icons.Default.TableChart, null, tint = Color(0xFF2E7D32)) }
                            )
                        }
                    }
                }
                
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
                            text = { 
                                if (index == 1 && incomingRequests.isNotEmpty()) {
                                    BadgedBox(
                                        badge = {
                                            Badge(
                                                containerColor = GoldPremium,
                                                contentColor = Color.Black,
                                                modifier = Modifier.offset(x = 12.dp, y = (-4).dp)
                                            ) {
                                                Text(incomingRequests.size.toString(), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    ) {
                                        Text(title, fontWeight = if(selectedTab == index) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(end = 16.dp))
                                    }
                                } else {
                                    Text(title, fontWeight = if(selectedTab == index) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        )
                    }
                }
            }
        }
        
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                }
            },
            label = "tabAnim"
        ) { currentTab ->
            if (currentTab == 0) {
                // Historial Tab
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Buscar por nombre, concepto...") },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = GoldPremium) },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPremium),
                            singleLine = true
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedFilter == 0,
                                onClick = { selectedFilter = 0 },
                                label = { Text("Todos", fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldPremium.copy(0.2f), selectedLabelColor = GoldPremium),
                                shape = RoundedCornerShape(12.dp)
                            )
                            FilterChip(
                                selected = selectedFilter == 1,
                                onClick = { selectedFilter = 1 },
                                label = { Text("Prestados", fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF4CAF50).copy(0.2f), selectedLabelColor = Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(12.dp)
                            )
                            FilterChip(
                                selected = selectedFilter == 2,
                                onClick = { selectedFilter = 2 },
                                label = { Text("Tomados", fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFF44336).copy(0.2f), selectedLabelColor = Color(0xFFF44336)),
                                shape = RoundedCornerShape(12.dp)
                            )
                            FilterChip(
                                selected = !showPaid,
                                onClick = { showPaid = !showPaid },
                                label = { Text("Ocultar Pagados") },
                                leadingIcon = { if (!showPaid) Icon(Icons.Default.Check, null) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
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
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        if (allLoans.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.AutoMirrored.Filled.Sort, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(0.3f))
                                    Spacer(Modifier.height(16.dp))
                                    Text("No se encontraron movimientos.", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                groupedLoans.forEach { (month, loans) ->
                                    item {
                                        Text(
                                            text = month,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = GoldPremium,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp, top = 8.dp),
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    items(loans) { loan ->
                                        LoanItem(
                                            loan = loan, 
                                            isLender = loan.lenderPhone == userPhone, 
                                            privacy = privacy, 
                                            onClick = { onLoan(loan) },
                                            onAction = { onPayment(loan) }
                                        )
                                    }
                                }
                                item { Spacer(Modifier.height(80.dp)) }
                            }
                        }
                    }
                }
            } else {
                // Solicitudes Tab
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
                    if (incomingRequests.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Event, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(0.3f))
                                Spacer(Modifier.height(16.dp))
                                Text("No hay solicitudes pendientes.", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(incomingRequests) { loan ->
                                LoanItem(
                                    loan = loan,
                                    isLender = false,
                                    privacy = privacy,
                                    onClick = { onLoan(loan) }
                                )
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }
}

fun generateTextReport(loans: List<Loan>, userPhone: String): String {
    val reportText = StringBuilder()
    reportText.append("--- REPORTE DE MOVIMIENTOS DEUDOM ---\n")
    reportText.append("Fecha de generación: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}\n")
    reportText.append("------------------------------------\n\n")
    
    loans.forEach { loan ->
        val isLender = loan.lenderPhone == userPhone
        val role = if (isLender) "PRESTASTE A" else "LE DEBES A"
        val person = if (isLender) loan.borrowerName else loan.lenderName
        val statusStr = when(loan.status) {
            LoanStatus.PAID -> "[PAGADO]"
            LoanStatus.OVERDUE -> "[VENCIDO]"
            LoanStatus.REQUESTED -> "[PENDIENTE ACEPTAR]"
            else -> "[ACTIVO]"
        }
        
        reportText.append("${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(loan.date))} $statusStr\n")
        reportText.append("$role: $person\n")
        reportText.append("MONTO: ${loan.currency} ${loan.amount}\n")
        reportText.append("CONCEPTO: ${loan.description}\n")
        reportText.append("------------------------------------\n")
    }
    return reportText.toString()
}

fun exportToCsv(context: android.content.Context, loans: List<Loan>, userPhone: String) {
    val csvHeader = "Fecha,Estado,Tipo,Persona,Monto,Pagado,Moneda,Categoria,Concepto\n"
    val csvContent = StringBuilder(csvHeader)
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    loans.forEach { loan ->
        val isLender = loan.lenderPhone == userPhone
        val type = if (isLender) "Prestamo Enviado" else "Prestamo Recibido"
        val person = if (isLender) loan.borrowerName else loan.lenderName
        val date = sdf.format(Date(loan.date))
        
        csvContent.append("\"$date\",")
        csvContent.append("\"${loan.status}\",")
        csvContent.append("\"$type\",")
        csvContent.append("\"$person\",")
        csvContent.append("${loan.amount},")
        csvContent.append("${loan.paidAmount},")
        csvContent.append("\"${loan.currency}\",")
        csvContent.append("\"${loan.category}\",")
        csvContent.append("\"${loan.description}\"\n")
    }

    try {
        val fileName = "deudom_historial_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { it.write(csvContent.toString().toByteArray()) }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Historial Deudom (CSV)")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir CSV"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Error al generar CSV", android.widget.Toast.LENGTH_SHORT).show()
    }
}
