package com.deudom.deudom.util

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder

fun generateQRCode(content: String): Bitmap? {
    return try {
        val writer = MultiFormatWriter()
        val matrix = writer.encode(content, BarcodeFormat.QR_CODE, 500, 500)
        BarcodeEncoder().createBitmap(matrix)
    } catch (e: Exception) {
        null
    }
}
