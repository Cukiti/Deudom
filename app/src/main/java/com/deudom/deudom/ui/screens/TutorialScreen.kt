package com.deudom.deudom.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import com.deudom.deudom.R
import com.deudom.deudom.ui.theme.GoldPremium
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource

import com.deudom.deudom.data.FirestoreService
import com.deudom.deudom.data.User
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun TutorialScreen(onTutorialComplete: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()
    
    // Financial Setup State
    var income by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var reserve by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    
    val auth = FirebaseAuth.getInstance()
    val userPhone = auth.currentUser?.phoneNumber?.replace("+1", "") ?: ""

    val pages = listOf(
        TutorialPageData(
            "Bienvenido a Deudom", 
            "La forma inteligente y social de gestionar tus préstamos y deudas con amigos.", 
            R.raw.wallet, 
            Icons.Default.AccountBalanceWallet
        ),
        TutorialPageData(
            "Tu Reputación Vale Oro", 
            "Mantén un alto Score de Fiabilidad pagando a tiempo. ¡Demuestra que eres de confianza!", 
            R.raw.reputacion, 
            Icons.Default.VerifiedUser
        ),
        TutorialPageData(
            "Gana Cukitis 💎", 
            "Cada pago puntual te da Cukitis. Úsalos para desbloquear temas y personalizar la app.", 
            R.raw.cukiti, 
            Icons.Default.Diamond
        ),
        TutorialPageData(
            "Configura tu Cartera", 
            "Define tus ingresos y ahorros para que Deudom cuide tu salud financiera.", 
            null, 
            Icons.Default.Settings
        )
    )

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f), userScrollEnabled = pagerState.currentPage < 3) { page -> 
            if (page == 3) {
                WalletSetupPage(
                    income = income, onIncomeChange = { income = it },
                    balance = balance, onBalanceChange = { balance = it },
                    reserve = reserve, onReserveChange = { reserve = it }
                )
            } else {
                TutorialPage(pages[page]) 
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(4) { i -> Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(if (pagerState.currentPage == i) GoldPremium else MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))) }
            }
            
            Button(
                onClick = { 
                    if (pagerState.currentPage < 3) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } 
                    } else {
                        scope.launch {
                            isSaving = true
                            val inc = income.toDoubleOrNull() ?: 0.0
                            val bal = balance.toDoubleOrNull() ?: 0.0
                            val res = reserve.toDoubleOrNull() ?: 0.0
                            FirestoreService.updateFinancialInfo(
                                phone = userPhone,
                                walletCash = inc,
                                balance = bal,
                                reserve = res,
                                lastWalletUpdateTimestamp = System.currentTimeMillis()
                            )
                            isSaving = false
                            onTutorialComplete()
                        }
                    }
                }, 
                colors = ButtonDefaults.buttonColors(containerColor = GoldPremium), 
                shape = MaterialTheme.shapes.medium,
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                } else {
                    Text(text = if (pagerState.currentPage == 3) stringResource(R.string.begin) else stringResource(R.string.next), fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
    }
}

@Composable
fun WalletSetupPage(
    income: String, onIncomeChange: (String) -> Unit,
    balance: String, onBalanceChange: (String) -> Unit,
    reserve: String, onReserveChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.AccountBalanceWallet, null, modifier = Modifier.size(80.dp), tint = GoldPremium)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Mi Cartera Personal", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text("Configura tus números básicos", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = income,
            onValueChange = onIncomeChange,
            label = { Text("¿Cuánto cobras al mes?") },
            prefix = { Text("RD$ ", color = GoldPremium) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = balance,
            onValueChange = onBalanceChange,
            label = { Text("¿Cuánto dinero tienes ahora?") },
            prefix = { Text("RD$ ", color = GoldPremium) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = reserve,
            onValueChange = onReserveChange,
            label = { Text("Reserva de Emergencia (Intocable)") },
            prefix = { Text("RD$ ", color = Color.Red) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Red,
                focusedLabelColor = Color.Red
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Este dinero no se usará para préstamos.",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Red.copy(alpha = 0.7f)
        )
    }
}

data class TutorialPageData(val title: String, val description: String, val lottieRawId: Int?, val icon: ImageVector)

@Composable
fun TutorialPage(data: TutorialPageData) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(modifier = Modifier.size(300.dp).padding(bottom = 32.dp), contentAlignment = Alignment.Center) {
            if (data.lottieRawId != null) {
                val comp by rememberLottieComposition(LottieCompositionSpec.RawRes(data.lottieRawId))
                LottieAnimation(comp, iterations = LottieConstants.IterateForever, modifier = Modifier.fillMaxSize())
            } else {
                val itran = rememberInfiniteTransition(label = "pulse")
                val sc by itran.animateFloat(0.9f, 1.1f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "scale")
                Icon(data.icon, null, modifier = Modifier.size(150.dp * sc), tint = GoldPremium)
            }
        }
        Text(data.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text(data.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f), textAlign = TextAlign.Center)
    }
}
