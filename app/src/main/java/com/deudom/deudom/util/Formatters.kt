package com.deudom.deudom.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

fun formatCurrency(amount: Double, currency: String = "RD$"): String {
    val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
    format.currency = Currency.getInstance("USD") // Fallback, normally check currency code
    return "$currency ${String.format("%,.2f", amount)}"
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "Hace un momento"
        minutes < 60 -> "Hace $minutes min"
        hours < 24 -> "Hace $hours h"
        days < 2 -> "Ayer"
        else -> SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(timestamp))
    }
}

fun formatPhone(phone: String): String {
    if (phone.length == 10) {
        return "(${phone.substring(0, 3)}) ${phone.substring(3, 6)}-${phone.substring(6)}"
    }
    return phone
}

class PhoneVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 10) text.text.substring(0, 10) else text.text
        var out = ""
        for (i in trimmed.indices) {
            if (i == 0) out += "("
            out += trimmed[i]
            if (i == 2) out += ") "
            if (i == 5) out += "-"
        }

        val numberOffsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset <= 3) return offset + 1
                if (offset <= 6) return offset + 3
                if (offset <= 10) return offset + 4
                return 14
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset <= 4) return offset - 1
                if (offset <= 9) return offset - 3
                if (offset <= 14) return offset - 4
                return 10
            }
        }

        return TransformedText(AnnotatedString(out), numberOffsetTranslator)
    }
}
