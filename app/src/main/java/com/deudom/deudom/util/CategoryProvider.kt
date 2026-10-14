package com.deudom.deudom.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class LoanCategory(
    val name: String,
    val icon: ImageVector,
    val emoji: String
)

object CategoryProvider {
    val categories = listOf(
        LoanCategory("Personal", Icons.Default.Payments, "💸"),
        LoanCategory("Comida", Icons.Default.Restaurant, "🍔"),
        LoanCategory("Salida", Icons.Default.Celebration, "🍻"),
        LoanCategory("Servicios", Icons.Default.FlashOn, "⚡"),
        LoanCategory("Transporte", Icons.Default.DirectionsCar, "🚗"),
        LoanCategory("Compras", Icons.Default.ShoppingBag, "🛍️"),
        LoanCategory("Otros", Icons.Default.Inventory2, "📦")
    )

    fun getIcon(name: String): ImageVector {
        return categories.find { it.name == name }?.icon ?: Icons.Default.Inventory2
    }

    fun getEmoji(name: String): String {
        return categories.find { it.name == name }?.emoji ?: "📦"
    }
}
