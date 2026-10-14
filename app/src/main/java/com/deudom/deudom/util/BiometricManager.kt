package com.deudom.deudom.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricManager {
    
    enum class SecurityStatus {
        READY,
        NOT_AVAILABLE,
        NO_HARDWARE,
        NONE_ENROLLED
    }

    fun checkSecurityStatus(context: Context): SecurityStatus {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> SecurityStatus.READY
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> SecurityStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> SecurityStatus.NOT_AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> SecurityStatus.NONE_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> SecurityStatus.NOT_AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> SecurityStatus.NOT_AVAILABLE
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> SecurityStatus.NOT_AVAILABLE
            else -> SecurityStatus.NOT_AVAILABLE // Includes NO_DEVICE_CREDENTIAL
        }
    }

    fun showBiometricPrompt(
        activity: FragmentActivity?, 
        onError: (String) -> Unit = {},
        onAuthenticated: () -> Unit
    ) {
        activity?.let {
            val executor = ContextCompat.getMainExecutor(it)
            val biometricPrompt = BiometricPrompt(it, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onAuthenticated()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Always call onError to allow the caller to handle state (like hiding a loading spinner)
                    onError(errString.toString())
                }
            })

            val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Acceso Seguro")
                .setSubtitle("Usa tu Huella, Rostro o PIN")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)

            // Note: setNegativeButtonText is NOT allowed when DEVICE_CREDENTIAL is used on some API levels.
            // BiometricPrompt handles the fallback UI automatically.

            try {
                biometricPrompt.authenticate(promptInfoBuilder.build())
            } catch (e: Exception) {
                onError("No se pudo iniciar la autenticación.")
            }
        }
    }

    fun openSecuritySettings(context: Context) {
        val intent = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Intent(Settings.ACTION_SECURITY_SETTINGS)
            } else {
                Intent(Settings.ACTION_SECURITY_SETTINGS)
            }
        } catch (e: Exception) {
            Intent(Settings.ACTION_SETTINGS)
        }
        context.startActivity(intent)
    }
}

