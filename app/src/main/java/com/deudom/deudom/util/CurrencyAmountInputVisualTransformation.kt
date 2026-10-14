package com.deudom.deudom.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

class CurrencyAmountInputVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        // Use locale-aware symbols
        val symbols = DecimalFormatSymbols.getInstance()
        val groupingSeparator = symbols.groupingSeparator
        val decimalSeparator = symbols.decimalSeparator

        val formattedText = try {
            val parts = originalText.split('.')
            val integerPart = parts[0]
            val decimalPart = if (parts.size > 1) parts[1] else null

            val number = integerPart.toLongOrNull() ?: 0L
            val integerFormatted = DecimalFormat("#,###").format(number)
            
            if (decimalPart != null) {
                "$integerFormatted$decimalSeparator$decimalPart"
            } else if (originalText.endsWith('.')) {
                "$integerFormatted$decimalSeparator"
            } else {
                integerFormatted
            }
        } catch (e: Exception) {
            originalText
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val originalOffset = offset.coerceIn(0, originalText.length)
                var transformedOffset = 0
                var originalCount = 0
                
                for (i in 0 until formattedText.length) {
                    if (transformedOffset >= formattedText.length) break
                    val char = formattedText[i]
                    if (char != groupingSeparator) {
                        originalCount++
                    }
                    transformedOffset++
                    if (originalCount == originalOffset) break
                }
                return transformedOffset.coerceIn(0, formattedText.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val transformedOffset = offset.coerceIn(0, formattedText.length)
                var originalOffset = 0
                for (i in 0 until transformedOffset) {
                    if (formattedText[i] != groupingSeparator) {
                        originalOffset++
                    }
                }
                return originalOffset.coerceIn(0, originalText.length)
            }
        }

        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}
