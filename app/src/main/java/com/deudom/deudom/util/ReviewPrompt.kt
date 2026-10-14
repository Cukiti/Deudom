package com.deudom.deudom.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.deudom.deudom.ui.theme.GoldPremium

object ReviewPrompt {
    fun shouldShow(context: Context): Boolean {
        val prefs = context.getSharedPreferences("deudom_prefs", Context.MODE_PRIVATE)
        val interactions = prefs.getInt("positive_interactions", 0)
        // Show every 5 positive interactions
        return interactions > 0 && interactions % 5 == 0
    }

    fun incrementInteraction(context: Context) {
        val prefs = context.getSharedPreferences("deudom_prefs", Context.MODE_PRIVATE)
        val current = prefs.getInt("positive_interactions", 0)
        prefs.edit().putInt("positive_interactions", current + 1).apply()
    }

    fun openPlayStore(context: Context) {
        val appId = context.packageName
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appId")))
        } catch (e: Exception) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appId")))
        }
    }
}

@Composable
fun ReviewDialog(onDismiss: () -> Unit, onRate: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Te gusta Deudom?") },
        text = { Text("Tu opinión nos ayuda a mejorar y seguir siendo gratis.") },
        confirmButton = {
            Button(
                onClick = onRate,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = GoldPremium)
            ) {
                Text("⭐ VALORAR", color = androidx.compose.ui.graphics.Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Ahora no")
            }
        }
    )
}
