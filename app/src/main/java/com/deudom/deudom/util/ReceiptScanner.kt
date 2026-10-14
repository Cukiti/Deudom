package com.deudom.deudom.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.util.regex.Pattern
import java.util.Calendar

object ReceiptScanner {
    
    data class ScanResult(val amount: Double?, val merchant: String?, val date: Long? = null)

    suspend fun scanReceipt(context: Context, bitmap: Bitmap): ScanResult {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            
            val visionText = recognizer.process(image).await()
            val text = visionText.text
            val lines = text.split("\n").filter { it.isNotBlank() }

            if (lines.isEmpty()) return ScanResult(null, null, null)

            // Merchant Heuristic
            var merchant: String? = null
            for (line in lines.take(5)) {
                // Ignore lines with numbers or dates
                if (line.length > 3 && !line.any { it.isDigit() } && !line.contains(Regex("[0-9]{2}/[0-9]{2}"))) {
                    merchant = line.trim()
                    break
                }
            }

            // Date Extraction
            var date: Long? = null
            val datePatterns = listOf(
                Pattern.compile("(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2,4})"),
                Pattern.compile("(\\d{4})[/-](\\d{1,2})[/-](\\d{1,2})")
            )
            
            for (line in lines) {
                for (pattern in datePatterns) {
                    val matcher = pattern.matcher(line)
                    if (matcher.find()) {
                        try {
                            val cal = Calendar.getInstance()
                            // Simple heuristic: if it has 4 digits at the end or start, it's the year
                            val g1 = matcher.group(1).toInt()
                            val g2 = matcher.group(2).toInt()
                            val g3 = matcher.group(3).toInt()
                            
                            if (g1 > 1000) { // YYYY-MM-DD
                                cal.set(g1, g2 - 1, g3)
                            } else if (g3 > 1000) { // DD-MM-YYYY
                                cal.set(g3, g2 - 1, g1)
                            } else { // DD-MM-YY (assume 2000+)
                                cal.set(2000 + g3, g2 - 1, g1)
                            }
                            date = cal.timeInMillis
                            break
                        } catch (e: Exception) {}
                    }
                }
                if (date != null) break
            }

            var amount: Double? = null
            // Improved regex for amount: supports digits with , or . separators
            val amountPattern = Pattern.compile("(\\d{1,10}(?:[.,]\\d{2,3})*(?:[.,]\\d{2})?)") 
            
            // Search for "TOTAL", "PAGAR", "IMPORTE", "NETO", "SALDO"
            val keywords = listOf("TOTAL", "PAGAR", "IMPORTE", "NETO", "SALDO", "PAYABLE", "BALANCE")
            val totalLineIndex = lines.indexOfFirst { line ->
                keywords.any { line.contains(it, ignoreCase = true) }
            }
            
            if (totalLineIndex != -1) {
                // Search in the same line
                val matcher = amountPattern.matcher(lines[totalLineIndex])
                if (matcher.find()) {
                    amount = parseAmount(matcher.group(1))
                } 
                
                // If not found in same line, search in the next 3 lines
                if (amount == null) {
                    for (j in 1..3) {
                        if (totalLineIndex + j < lines.size) {
                            val nextLine = lines[totalLineIndex + j]
                            val nextMatcher = amountPattern.matcher(nextLine)
                            if (nextMatcher.find()) {
                                amount = parseAmount(nextMatcher.group(1))
                                if (amount != null) break
                            }
                        }
                    }
                }
            }
            
            // Fallback: search for the largest amount in the bottom half of the receipt
            if (amount == null || amount < 0.01) {
                var maxAmount = 0.0
                val bottomLines = if (lines.size > 10) lines.takeLast(10) else lines
                for (line in bottomLines) {
                    val matcher = amountPattern.matcher(line)
                    while (matcher.find()) {
                        val value = parseAmount(matcher.group(1))
                        // Heuristic: common expenses are between 1 and 500,000
                        if (value != null && value > maxAmount && value < 500000) { 
                            maxAmount = value
                        }
                    }
                }
                if (maxAmount > 0.0) amount = maxAmount
            }

            return ScanResult(amount, merchant, date)

        } catch (e: Exception) {
            Log.e("ReceiptScanner", "Error scanning receipt", e)
            return ScanResult(null, null, null)
        }
    }

    private fun parseAmount(raw: String?): Double? {
        if (raw == null) return null
        return try {
            var clean = raw.replace(Regex("[^0-9.,]"), "")
            
            if (clean.contains(",") && clean.contains(".")) {
                 if (clean.lastIndexOf(",") > clean.lastIndexOf(".")) {
                     clean = clean.replace(".", "").replace(",", ".")
                 } else {
                     clean = clean.replace(",", "")
                 }
            } else if (clean.contains(",")) {
                if (clean.length - clean.lastIndexOf(",") == 3) {
                    clean = clean.replace(",", ".")
                } else {
                    clean = clean.replace(",", "")
                }
            }
            
            clean.toDouble()
        } catch (e: Exception) {
            null
        }
    }
}
