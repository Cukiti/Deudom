package com.deudom.deudom.ui.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deudom.deudom.data.PersonalExpense
import com.deudom.deudom.data.User
import com.deudom.deudom.ui.theme.GoldPremium
import com.deudom.deudom.util.CategoryProvider
import com.deudom.deudom.util.CurrencyManager
import com.deudom.deudom.util.CurrencyAmountInputVisualTransformation
import java.util.Calendar

import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseBottomSheet(
    user: User?,
    personalExpenses: List<PersonalExpense>,
    onDismiss: () -> Unit,
    onSave: (Double, String, String) -> Unit,
    onScanReceipt: () -> Unit,
    isScanning: Boolean
) {
    var amountTextFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(CategoryProvider.categories.first().name) }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = GoldPremium.copy(alpha = 0.5f)) },
        properties = ModalBottomSheetDefaults.properties(
            shouldDismissOnBackPress = false 
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding() 
                .imePadding() // CRITICAL: Adds space for the keyboard
                .verticalScroll(rememberScrollState()) // Allow scrolling when keyboard is open
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Nuevo Gasto",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = amountTextFieldValue,
                onValueChange = { amountTextFieldValue = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("0.00", fontSize = 24.sp) },
                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, color = GoldPremium),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                visualTransformation = CurrencyAmountInputVisualTransformation(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPremium,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                leadingIcon = { Icon(Icons.Default.AddCard, null, tint = GoldPremium) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // --- QUICK AMOUNT BUTTONS FIXED ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(50, 100, 500, 1000).forEach { amount ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { 
                                // Parse existing amount safely using official manager
                                val currentVal = CurrencyManager.parseAmount(amountTextFieldValue.text) ?: 0.0
                                val newVal = currentVal + amount
                                
                                // Format back to string (avoiding trailing .0 if whole number for cleaner look)
                                val newText = if (newVal % 1 == 0.0) newVal.toLong().toString() else newVal.toString()
                                
                                amountTextFieldValue = TextFieldValue(
                                    text = newText,
                                    selection = TextRange(newText.length)
                                )
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = GoldPremium.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldPremium.copy(alpha = 0.2f))
                    ) {
                        Text(
                            "+$amount",
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldPremium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Categoría", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.height(180.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(CategoryProvider.categories) { cat ->
                    val isSelected = selectedCategory == cat.name
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) GoldPremium.copy(alpha = 0.2f) else Color.Transparent)
                            .border(
                                1.dp, 
                                if (isSelected) GoldPremium else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedCategory = cat.name }
                            .padding(8.dp)
                    ) {
                        Text(cat.emoji, fontSize = 24.sp)
                        Text(
                            cat.name, 
                            fontSize = 10.sp, 
                            maxLines = 1, 
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) GoldPremium else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("¿En qué gastaste?") },
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.EditNote, null) }
            )

            val budget = user?.categoryBudgets?.get(selectedCategory) ?: 0.0
            if (budget > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                val currentSpent = personalExpenses.filter { 
                    val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                    val now = Calendar.getInstance()
                    cal.get(Calendar.MONTH) == now.get(Calendar.MONTH) && it.category == selectedCategory 
                }.sumOf { it.amount }
                val newAmount = CurrencyManager.parseAmount(amountTextFieldValue.text) ?: 0.0
                val progress = ((currentSpent + newAmount) / budget).toFloat().coerceIn(0f, 1.1f)
                
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Estado del Presupuesto", style = MaterialTheme.typography.labelSmall)
                        Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (progress > 1f) Color.Red else GoldPremium)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = if (progress > 1f) Color.Red else GoldPremium,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onScanReceipt,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isScanning) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    else Icon(Icons.Default.CameraAlt, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Recibo")
                }

                Button(
                    onClick = { 
                        val amount = CurrencyManager.parseAmount(amountTextFieldValue.text) ?: 0.0
                        if (amount > 0) onSave(amount, description, selectedCategory)
                    },
                    modifier = Modifier.weight(1.5f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = Color.Black)
                ) {
                    Text("GUARDAR GASTO", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}