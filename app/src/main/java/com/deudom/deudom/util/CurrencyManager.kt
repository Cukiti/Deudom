package com.deudom.deudom.util

object CurrencyManager {
    // Basic exchange rates relative to RD$ (can be expanded to fetch from API)
    private val rates = mapOf(
        "RD$" to 1.0,
        "USD" to 60.5, // 1 USD = 60.5 RD$
        "EUR" to 65.2  // 1 EUR = 65.2 RD$
    )

    fun convert(amount: Double, from: String, to: String): Double {
        if (from == to) return amount
        
        val rateFrom = rates[from] ?: 1.0
        val rateTo = rates[to] ?: 1.0
        
        // Convert to base (RD$) then to target
        val amountInBase = amount * rateFrom
        return amountInBase / rateTo
    }

    fun format(amount: Double, currency: String): String {
        if (amount.isNaN() || amount.isInfinite()) return "$currency 0.00"
        val symbols = java.text.DecimalFormatSymbols.getInstance()
        val formatter = java.text.DecimalFormat("#,##0.00", symbols)
        return "$currency ${formatter.format(amount)}"
    }

    fun formatAbbreviated(amount: Double, currency: String): String {
        return if (amount >= 1_000_000) {
            String.format(java.util.Locale.US, "$currency %.2fM", amount / 1_000_000)
        } else if (amount >= 10_000) {
            String.format(java.util.Locale.US, "$currency %.1fK", amount / 1_000)
        } else {
            format(amount, currency)
        }
    }

    fun parseAmount(text: String): Double? {
        if (text.isBlank()) return null
        return try {
            // Remove everything except digits, dots and commas
            val sanitized = text.filter { it.isDigit() || it == '.' || it == ',' }
            if (sanitized.isEmpty()) return null
            
            // Heuristic: if there are both dots and commas, the last one is the decimal separator
            val lastDot = sanitized.lastIndexOf('.')
            val lastComma = sanitized.lastIndexOf(',')
            
            val cleanText = if (lastDot > lastComma) {
                // Dot is decimal, remove commas
                sanitized.replace(",", "")
            } else if (lastComma > lastDot) {
                // Comma is decimal, remove dots and replace comma with dot for Double parsing
                sanitized.replace(".", "").replace(",", ".")
            } else {
                // Only one type of separator or none
                sanitized.replace(",", ".")
            }
            
            cleanText.toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }
}
