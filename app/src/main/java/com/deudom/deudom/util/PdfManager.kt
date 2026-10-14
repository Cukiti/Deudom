package com.deudom.deudom.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.core.content.FileProvider
import com.deudom.deudom.data.Loan
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfManager {
    fun generateLoanReport(context: Context, loan: Loan): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        // Header
        paint.color = Color.parseColor("#FFD700") // Gold
        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText("DEUDOM - Reporte Oficial", 50f, 50f, paint)

        paint.color = Color.BLACK
        paint.textSize = 12f
        paint.isFakeBoldText = false
        canvas.drawText("Generado el: ${sdf.format(Date())}", 50f, 75f, paint)

        // Loan Details
        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText("Detalles del Préstamo", 50f, 120f, paint)

        paint.textSize = 12f
        paint.isFakeBoldText = false
        var y = 150f
        canvas.drawText("Prestador: ${loan.lenderName}", 50f, y, paint); y += 20f
        canvas.drawText("Deudor: ${loan.borrowerName}", 50f, y, paint); y += 20f
        canvas.drawText("Monto Original: ${loan.currency} ${loan.amount}", 50f, y, paint); y += 20f
        canvas.drawText("Monto Pagado: ${loan.currency} ${loan.paidAmount}", 50f, y, paint); y += 20f
        canvas.drawText("Balance Pendiente: ${loan.currency} ${loan.amount - loan.paidAmount}", 50f, y, paint); y += 20f
        canvas.drawText("Concepto: ${loan.description}", 50f, y, paint); y += 20f
        val dueStr = if (loan.dueDate != null) sdf.format(Date(loan.dueDate)) else "N/A"
        canvas.drawText("Fecha de Vencimiento: $dueStr", 50f, y, paint); y += 40f

        // Payments Table
        if (loan.payments.isNotEmpty()) {
            paint.isFakeBoldText = true
            canvas.drawText("Historial de Abonos", 50f, y, paint); y += 30f
            paint.isFakeBoldText = false
            loan.payments.forEach { payment ->
                canvas.drawText("${sdf.format(Date(payment.date))} - ${loan.currency} ${payment.amount}", 70f, y, paint)
                y += 20f
            }
        }

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "Reporte_Deudom_${loan.id.take(8)}.pdf")
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            pdfDocument.close()
            null
        }
    }
}
