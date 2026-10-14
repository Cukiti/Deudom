package com.deudom.deudom.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class CurrencyVisualTransformation(
    private val currencySymbol: String = ""
) : VisualTransformation {

    private val symbols = DecimalFormatSymbols(Locale.US).apply { groupingSeparator = ',' }
    private val formatter = DecimalFormat("#,###", symbols)

    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        // Clean non-digits just in case (though input type usually handles this)
        val cleanText = originalText.filter { it.isDigit() }
        if (cleanText.isEmpty()) return TransformedText(AnnotatedString(""), OffsetMapping.Identity)

        // Format
        val formattedNumber = try {
            formatter.format(cleanText.toLong())
        } catch (e: Exception) {
            cleanText
        }

        val finalText = if (currencySymbol.isNotEmpty()) "$currencySymbol $formattedNumber" else formattedNumber

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                // Approximate mapping (simplified for forward typing)
                // A robust mapping calculates exact index of each digit, but for "add to end" simplified logic works often
                // Re-calculating properly:
                
                if (offset == 0) return if (currencySymbol.isNotEmpty()) currencySymbol.length + 1 else 0
                
                // Count digits before this offset in original
                val digitsBefore = originalText.take(offset).count { it.isDigit() }
                
                // Find where these digits end in transformed
                var transformedOffset = if (currencySymbol.isNotEmpty()) currencySymbol.length + 1 else 0
                var digitsFound = 0
                
                while (digitsFound < digitsBefore && transformedOffset < finalText.length) {
                    if (finalText[transformedOffset].isDigit()) {
                        digitsFound++
                    }
                    transformedOffset++
                }
                return transformedOffset
            }

            override fun transformedToOriginal(offset: Int): Int {
                val prefixLen = if (currencySymbol.isNotEmpty()) currencySymbol.length + 1 else 0
                if (offset <= prefixLen) return 0
                
                val cleanOffset = offset.coerceAtMost(finalText.length)
                // Count digits in transformed up to offset
                val digitsCount = finalText.substring(prefixLen, cleanOffset).count { it.isDigit() }
                return digitsCount.coerceAtMost(originalText.length)
            }
        }

        return TransformedText(AnnotatedString(finalText), offsetMapping)
    }
}
