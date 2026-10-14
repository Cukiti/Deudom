package com.deudom.deudom.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.airbnb.lottie.compose.*
import com.deudom.deudom.R
import com.deudom.deudom.data.FirestoreService
import com.deudom.deudom.data.User
import com.deudom.deudom.ui.components.PinInputView
import com.deudom.deudom.ui.theme.GoldPremium
import com.deudom.deudom.util.BiometricManager
import com.deudom.deudom.util.HapticManager
import com.deudom.deudom.util.Prefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AppLockScreen(
    userPhone: String,
    onAuthenticated: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf<User?>(null) }
    var isCreatingPin by remember { mutableStateOf(false) }
    var tempPin by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isVerified by remember { mutableStateOf(false) }

    val lottieComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.deuverification))
    val lottieProgress by animateLottieCompositionAsState(
        composition = lottieComposition,
        isPlaying = isVerified,
        iterations = 1
    )

    LaunchedEffect(lottieProgress) {
        if (isVerified && lottieProgress == 1f) {
            delay(300)
            onAuthenticated()
        }
    }
    
    // Fetch user security data from account (Firestore)
    LaunchedEffect(userPhone) {
        isLoading = true
        user = FirestoreService.getUser(userPhone)
        
        val status = BiometricManager.checkSecurityStatus(context)
        
        // 1. Logic: If device has system security, prioritize it
        if (status == BiometricManager.SecurityStatus.READY) {
            BiometricManager.showBiometricPrompt(
                activity = context as? FragmentActivity,
                onError = { 
                    isLoading = false // Show PIN pad as fallback if biometric fails or canceled
                },
                onAuthenticated = {
                    isVerified = true
                }
            )
        } else {
            // 2. Logic: No device security. Check if user has an account PIN.
            if (user?.pin == null) {
                isCreatingPin = true
            }
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        if (isVerified) {
            LottieAnimation(
                composition = lottieComposition,
                progress = { lottieProgress },
                modifier = Modifier.size(200.dp)
            )
        } else if (isLoading) {
            val infiniteTransition = rememberInfiniteTransition(label = "loading")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(120.dp)
                ) {
                    // Outer glow
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha * 0.5f
                            }
                            .background(GoldPremium.copy(alpha = 0.2f), CircleShape)
                    )
                    
                    // Inner circle
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(GoldPremium.copy(alpha = 0.1f), CircleShape)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security",
                            tint = GoldPremium.copy(alpha = alpha),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    
                    CircularProgressIndicator(
                        modifier = Modifier.size(120.dp),
                        color = GoldPremium,
                        strokeWidth = 2.dp,
                        trackColor = GoldPremium.copy(alpha = 0.1f)
                    )
                }
                
                Spacer(Modifier.height(32.dp))
                
                Text(
                    text = "Protegiendo tus datos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Verificando seguridad...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = alpha)
                )
            }
        } else if (isCreatingPin) {
            // Flow: Force PIN Creation for account security
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Icon(Icons.Default.Security, null, modifier = Modifier.size(64.dp), tint = GoldPremium)
                
                Text(
                    text = if (tempPin.isEmpty()) "Crea tu PIN de seguridad" else "Confirma tu PIN",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "Tu dispositivo no tiene seguridad activa. Crea un PIN vinculado a tu cuenta Deudom para proteger tus datos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )

                PinInputView(
                    onPinComplete = { pin ->
                        if (tempPin.isEmpty()) {
                            tempPin = pin
                            HapticManager.vibrateSuccess(context)
                        } else {
                            if (tempPin == pin) {
                                // Save to Firestore account
                                scope.launch {
                                    isLoading = true
                                    if (FirestoreService.updatePin(userPhone, pin)) {
                                        HapticManager.vibrateSuccess(context)
                                        isVerified = true
                                    } else {
                                        Toast.makeText(context, "Error al guardar PIN", Toast.LENGTH_SHORT).show()
                                        isLoading = false
                                    }
                                }
                            } else {
                                tempPin = ""
                                HapticManager.vibrateError(context)
                                Toast.makeText(context, "Los PIN no coinciden. Intenta de nuevo.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        } else {
            // Flow: Verify existing Account PIN or System Security
            val hasSystemSecurity = BiometricManager.checkSecurityStatus(context) == BiometricManager.SecurityStatus.READY
            
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Deudom está bloqueado",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                
                if (hasSystemSecurity) {
                    Text(
                        text = "Usa el sistema de seguridad de tu teléfono para acceder.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(Modifier.height(32.dp))
                    
                    Button(
                        onClick = {
                            BiometricManager.showBiometricPrompt(
                                activity = context as? FragmentActivity,
                                onAuthenticated = { isVerified = true }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPremium),
                        shape = CircleShape,
                        modifier = Modifier.size(80.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = "Desbloquear", tint = Color.Black, modifier = Modifier.size(40.dp))
                    }
                } else {
                    Text(
                        text = "Ingresa tu PIN de cuenta",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    PinInputView(
                        onPinComplete = { pin ->
                            if (user?.pin == pin) {
                                HapticManager.vibrateSuccess(context)
                                isVerified = true
                            } else {
                                HapticManager.vibrateError(context)
                                Toast.makeText(context, "PIN Incorrecto", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}
