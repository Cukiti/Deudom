package com.deudom.deudom

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.deudom.deudom.navigation.Screen
import com.deudom.deudom.ui.screens.*
import com.deudom.deudom.ui.theme.DeudomTheme
import com.deudom.deudom.util.Prefs
import com.deudom.deudom.worker.ReminderWorker
import com.deudom.deudom.util.IconProvider
import com.deudom.deudom.worker.RecurringLoanWorker
import com.deudom.deudom.worker.SyncWorker
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.NetworkType
import androidx.work.Constraints
import java.util.concurrent.TimeUnit
import androidx.work.ExistingPeriodicWorkPolicy

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        
        auth = FirebaseAuth.getInstance()
        
        lifecycleScope.launch(Dispatchers.IO) {
            IconProvider.loadIconPack(this@MainActivity)
        }
        
        val deepLinkLoanId = intent?.data?.let { uri ->
            if (uri.scheme == "deudom" && uri.host == "loan") uri.lastPathSegment else null
        }

        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            var themeMode by remember { mutableStateOf(Prefs.getThemeMode(context)) }
            var themeId by remember { mutableStateOf(Prefs.getThemeId(context)) }
            var iconPackTrigger by remember { mutableStateOf(0) }
            
            val isDark = when (themeMode) {
                Prefs.ThemeMode.LIGHT -> false
                Prefs.ThemeMode.DARK -> true
                Prefs.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            DeudomTheme(themeId = themeId, darkTheme = isDark, iconPackVersion = iconPackTrigger) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    DeudomApp(
                        auth = auth,
                        currentTheme = themeMode,
                        onThemeChange = { newMode ->
                            Prefs.setThemeMode(context, newMode)
                            themeMode = newMode
                        },
                        onThemeIdChange = { newId ->
                            Prefs.setThemeId(context, newId)
                            themeId = newId
                        },
                        onIconPackChange = {
                            IconProvider.loadIconPack(context)
                            iconPackTrigger++
                        },
                        initialLoanId = deepLinkLoanId
                    )
                }
            }
        }
    }
}

@Composable
fun DeudomApp(
    auth: FirebaseAuth,
    currentTheme: Prefs.ThemeMode,
    onThemeChange: (Prefs.ThemeMode) -> Unit,
    onThemeIdChange: (String) -> Unit,
    onIconPackChange: () -> Unit,
    initialLoanId: String? = null
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    val isAppLockedSetting = remember { Prefs.isAppLockEnabled(context) }
    // Force authentication if user is logged in, regardless of setting (as per new requirements for account security)
    var isAuthenticated by remember { mutableStateOf(auth.currentUser == null) }
    var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }
    var userPhone by remember { mutableStateOf(auth.currentUser?.phoneNumber?.replace("+1", "") ?: "") }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    DisposableEffect(auth) {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            val nowLoggedIn = user != null
            if (nowLoggedIn != isLoggedIn) {
                isLoggedIn = nowLoggedIn
                // Reset authentication state on login change
                isAuthenticated = !nowLoggedIn
            }
            userPhone = user?.phoneNumber?.replace("+1", "") ?: ""
        }
        auth.addAuthStateListener(authStateListener)
        onDispose {
            auth.removeAuthStateListener(authStateListener)
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            navController.navigate(Screen.Auth.route) {
                popUpTo(0) { inclusive = true }
            }
        } else {
            val target = if (Prefs.isTutorialDone(context)) Screen.Home.route else Screen.Tutorial.route
            navController.navigate(target) {
                popUpTo(Screen.Auth.route) { inclusive = true }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        ReminderWorker.schedule(context)
        RecurringLoanWorker.schedule(context)
        
        val syncConstraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(syncConstraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("SyncWorker", ExistingPeriodicWorkPolicy.KEEP, syncRequest)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val startDestination = remember(isLoggedIn) {
            if (isLoggedIn) {
                if (Prefs.isTutorialDone(context)) Screen.Home.route else Screen.Tutorial.route
            } else {
                Screen.Auth.route
            }
        }

        val showLock = isLoggedIn && !isAuthenticated && userPhone.isNotBlank()

        NavHost(navController = navController, startDestination = startDestination) {
            composable(Screen.Auth.route) {
                AuthScreen(
                    currentTheme = currentTheme,
                    onThemeChange = onThemeChange,
                    onAuthSuccess = {
                        // Managed by auth state listener
                    }
                )
            }
            composable(Screen.Tutorial.route) {
                TutorialScreen(onTutorialComplete = {
                    Prefs.setTutorialDone(context, true)
                    navController.navigate(Screen.Home.route) { 
                        popUpTo(Screen.Tutorial.route) { inclusive = true } 
                    }
                })
            }
            composable(Screen.Home.route) {
                if (userPhone.isNotBlank() && !showLock) {
                    HomeScreen(
                        userPhone = userPhone,
                        onLogout = {
                            auth.signOut()
                        },
                        currentTheme = currentTheme,
                        onThemeChange = onThemeChange,
                        onThemeIdChange = onThemeIdChange,
                        onIconPackChange = onIconPackChange,
                        initialLoanId = initialLoanId
                    )
                }
            }
        }

        if (showLock) {
            AppLockScreen(
                userPhone = userPhone,
                onAuthenticated = { isAuthenticated = true }
            )
        }
    }
}
