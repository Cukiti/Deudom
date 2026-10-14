package com.deudom.deudom.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class ThousandsSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val symbols = DecimalFormatSymbols(Locale.US)
        symbols.groupingSeparator = ','
        symbols.decimalSeparator = '.'
        val decimalFormat = DecimalFormat("#,###.##", symbols)
        
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        
        // Handle parts: integer and decimal
        val parts = originalText.split(".")
        val integerPart = parts[0]
        val decimalPart = if (parts.size > 1) "." + parts[1] else ""
        
        val formattedInteger = if (integerPart.isEmpty()) "" else {
            val doubleValue = integerPart.toDoubleOrNull() ?: 0.0
            decimalFormat.format(doubleValue).split(".")[0]
        }
        
        val formattedText = formattedInteger + decimalPart
        
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                
                var transformedOffset = 0
                var originalCount = 0
                
                for (i in formattedText.indices) {
                    if (formattedText[i] != ',') {
                        originalCount++
                    }
                    transformedOffset++
                    if (originalCount == offset) break
                }
                return transformedOffset
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                
                var originalOffset = 0
                for (i in 0 until offset.coerceAtMost(formattedText.length)) {
                    if (formattedText[i] != ',') {
                        originalOffset++
                    }
                }
                return originalOffset
            }
        }
        
        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}
