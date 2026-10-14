package com.deudom.deudom.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.deudom.deudom.navigation.AuthState
import com.deudom.deudom.data.FirestoreService
import com.deudom.deudom.data.User
import com.deudom.deudom.ui.theme.GoldPremium
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import com.deudom.deudom.ui.components.PremiumTextField
import com.deudom.deudom.util.PhoneVisualTransformation
import com.deudom.deudom.util.formatPhone

import androidx.compose.ui.res.stringResource
import com.deudom.deudom.R

import com.deudom.deudom.util.Prefs
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun AuthScreen(
    currentTheme: Prefs.ThemeMode,
    onThemeChange: (Prefs.ThemeMode) -> Unit,
    onAuthSuccess: () -> Unit
) {
    var authState by remember { mutableStateOf(AuthState.LOGIN) }
    var phone by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()

    val isSystemDark = isSystemInDarkTheme()
    val effectiveIsDark = when (currentTheme) {
        Prefs.ThemeMode.LIGHT -> false
        Prefs.ThemeMode.DARK -> true
        Prefs.ThemeMode.SYSTEM -> isSystemDark
    }

    val validatePhone: (String) -> Boolean = { input ->
        val digits = input.filter { it.isDigit() }
        if (digits.length != 10) { phoneError = "El número debe tener 10 dígitos"; false }
        else { val prefix = digits.take(3); if (prefix !in listOf("809", "829", "849")) { phoneError = "Prefijo inválido"; false } else { phoneError = null; true } }
    }

    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) { 
            isLoading = true
            scope.launch { 
                auth.signInWithCredential(credential).addOnCompleteListener { task -> 
                    if (task.isSuccessful) onAuthSuccess() 
                    else { phoneError = "Error al autenticar: ${task.exception?.message}"; isLoading = false } 
                } 
            } 
        }
        override fun onVerificationFailed(e: FirebaseException) { 
            phoneError = "Error: ${e.message}"
            isLoading = false 
        }
        override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) { 
            verificationId = id
            resendToken = token
            authState = AuthState.OTP
            isLoading = false 
        }
    }

    val startPhoneNumberVerification = { phoneNumber: String ->
        activity?.let {
            isLoading = true
            val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber("+1$phoneNumber")
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(it)
                .setCallbacks(callbacks)
            
            if (resendToken != null) {
                optionsBuilder.setForceResendingToken(resendToken!!)
            }
            
            PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Theme Switcher at top right
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { 
                val newMode = if (effectiveIsDark) Prefs.ThemeMode.LIGHT else Prefs.ThemeMode.DARK
                onThemeChange(newMode)
            }) {
                Icon(
                    imageVector = if (effectiveIsDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Cambiar tema",
                    tint = GoldPremium
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .animateContentSize(), 
            horizontalAlignment = Alignment.CenterHorizontally, 
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = stringResource(R.string.welcome_title), style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 4.sp, color = GoldPremium), modifier = Modifier.padding(bottom = 48.dp))
            
            AnimatedContent(
                targetState = authState, 
                transitionSpec = { 
                    if (targetState == AuthState.OTP) {
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    }
                }, 
                label = "authTransition"
            ) { target ->
                when (target) {
                    AuthState.LOGIN, AuthState.SIGNUP -> AuthForm(
                        isSignup = target == AuthState.SIGNUP, 
                        phone = phone, 
                        name = name, 
                        phoneError = phoneError, 
                        isLoading = isLoading, 
                        onPhoneChange = { if (it.length <= 10) { phone = it; if (it.length == 10) validatePhone(it) else phoneError = null } }, 
                        onNameChange = { name = it }, 
                        onSubmit = {
                            if (validatePhone(phone)) { 
                                scope.launch { 
                                    isLoading = true
                                    val existingUser = FirestoreService.getUser(phone)
                                    if (authState == AuthState.SIGNUP) { 
                                        if (existingUser != null) { phoneError = "El usuario ya existe"; isLoading = false } 
                                        else {
                                            // Clear previous token for new signup attempt
                                            resendToken = null
                                            startPhoneNumberVerification(phone)
                                        }
                                    } else { 
                                        if (existingUser == null) { phoneError = "El usuario no existe"; isLoading = false } 
                                        else {
                                            // Clear previous token for new login attempt
                                            resendToken = null
                                            startPhoneNumberVerification(phone)
                                        }
                                    } 
                                } 
                            }
                        }, 
                        onSwitchMode = { 
                            authState = if (authState == AuthState.LOGIN) AuthState.SIGNUP else AuthState.LOGIN
                            phoneError = null 
                        }
                    )
                    AuthState.OTP -> OtpForm(
                        phone = phone, 
                        otpCode = otpCode, 
                        onOtpChange = { 
                            if (it.length <= 6) { 
                                otpCode = it
                                if (it.length == 6) { 
                                    scope.launch { 
                                        isLoading = true
                                        val cred = PhoneAuthProvider.getCredential(verificationId!!, it)
                                        auth.signInWithCredential(cred).addOnCompleteListener { task -> 
                                            if (task.isSuccessful) { 
                                                if (name.isNotEmpty()) {
                                                    scope.launch { 
                                                        FirestoreService.saveUser(User(name = name, phone = phone))
                                                        onAuthSuccess() 
                                                    } 
                                                } else {
                                                    onAuthSuccess() 
                                                }
                                            } else { 
                                                Toast.makeText(context, "Código inválido", Toast.LENGTH_SHORT).show()
                                                isLoading = false 
                                            } 
                                        } 
                                    } 
                                } 
                            } 
                        }, 
                        onResend = { 
                            startPhoneNumberVerification(phone)
                        }, 
                        onBack = { authState = AuthState.LOGIN }
                    )
                }
            }
        }
    }
}

@Composable
fun AuthForm(isSignup: Boolean, phone: String, name: String, phoneError: String?, isLoading: Boolean, onPhoneChange: (String) -> Unit, onNameChange: (String) -> Unit, onSubmit: () -> Unit, onSwitchMode: () -> Unit) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally, 
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = if (isSignup) stringResource(R.string.signup) else stringResource(R.string.login), style = MaterialTheme.typography.headlineSmall)
        
        AnimatedVisibility(
            visible = isSignup,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            PremiumTextField(value = name, onValueChange = onNameChange, label = stringResource(R.string.full_name), icon = Icons.Default.Person)
        }

        PremiumTextField(value = phone, onValueChange = onPhoneChange, label = stringResource(R.string.phone_number), icon = Icons.Default.Phone, keyboardType = KeyboardType.Phone, visualTransformation = PhoneVisualTransformation(), isError = phoneError != null, supportingText = phoneError, prefix = "+1 ")
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = onSubmit, 
            enabled = !isLoading, 
            modifier = Modifier.fillMaxWidth().height(56.dp), 
            colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = Color.Black), 
            shape = MaterialTheme.shapes.medium
        ) { 
            if (isLoading) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp)) 
            else Text(if (isSignup) stringResource(R.string.register_btn) else stringResource(R.string.enter_btn), fontWeight = FontWeight.Bold) 
        }
        
        TextButton(onClick = onSwitchMode, enabled = !isLoading) { 
            Text(text = if (isSignup) stringResource(R.string.already_have_account) else stringResource(R.string.dont_have_account), color = MaterialTheme.colorScheme.secondary) 
        }
    }
}

@Composable
fun OtpForm(phone: String, otpCode: String, onOtpChange: (String) -> Unit, onResend: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally, 
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Verificación SMS", style = MaterialTheme.typography.headlineSmall)
        Text(text = "Enviamos un código a \n${formatPhone(phone)}", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.secondary)
        PremiumTextField(value = otpCode, onValueChange = onOtpChange, label = "Código de 6 dígitos", icon = Icons.Default.Lock, keyboardType = KeyboardType.NumberPassword)
        Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = GoldPremium, contentColor = Color.Black)) { Text("VERIFICAR", fontWeight = FontWeight.Bold) }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("Atrás", color = MaterialTheme.colorScheme.secondary) }
            TextButton(onClick = onResend) { Text("Reenviar código", color = GoldPremium) }
        }
    }
}
