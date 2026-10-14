package com.deudom.deudom.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.deudom.deudom.data.Loan
import com.deudom.deudom.data.LoanStatus
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfGenerator {

    fun generateHistoryPdf(context: Context, userPhone: String, loans: List<Loan>) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()
        val titlePaint = Paint()

        // 1. Header
        titlePaint.color = Color.BLACK
        titlePaint.textSize = 24f
        titlePaint.isFakeBoldText = true
        titlePaint.textAlign = Paint.Align.CENTER
        canvas.drawText("DEUDOM - REPORTE FINANCIERO", 297f, 50f, titlePaint)

        titlePaint.textSize = 12f
        titlePaint.isFakeBoldText = false
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        canvas.drawText("Generado el: ${dateFormat.format(Date())}", 297f, 70f, titlePaint)

        // 2. Table Header
        paint.color = Color.DKGRAY
        paint.textSize = 12f
        paint.isFakeBoldText = true
        
        val startY = 120f
        var currentY = startY
        val margin = 40f
        val col1 = margin // Date
        val col2 = 140f   // Person
        val col3 = 300f   // Description
        val col4 = 480f   // Amount

        canvas.drawText("FECHA", col1, currentY, paint)
        canvas.drawText("PERSONA", col2, currentY, paint)
        canvas.drawText("CONCEPTO", col3, currentY, paint)
        canvas.drawText("MONTO", col4, currentY, paint)
        
        canvas.drawLine(margin, currentY + 10f, 555f, currentY + 10f, paint)
        currentY += 30f

        // 3. Rows
        paint.isFakeBoldText = false
        paint.textSize = 10f
        
        val loanDateFormatter = SimpleDateFormat("dd/MM/yy", Locale.getDefault())

        for (loan in loans) {
            if (currentY > 800) { // New Page logic (Simplified: just break for now or single page)
                break // TODO: Handle multi-page
            }

            val isLender = loan.lenderPhone == userPhone
            val person = if (isLender) "Prestaste a: ${loan.borrowerName}" else "Te prestó: ${loan.lenderName}"
            val color = if (isLender) Color.parseColor("#388E3C") else Color.parseColor("#D32F2F") // Green/Red
            
            paint.color = Color.BLACK
            canvas.drawText(loanDateFormatter.format(Date(loan.date)), col1, currentY, paint)
            
            paint.color = color
            canvas.drawText(person.take(20), col2, currentY, paint)
            
            paint.color = Color.BLACK
            canvas.drawText(loan.description.take(25), col3, currentY, paint)
            
            paint.isFakeBoldText = true
            canvas.drawText("${loan.currency} ${loan.amount}", col4, currentY, paint)
            paint.isFakeBoldText = false

            currentY += 20f
        }

        // Footer
        paint.color = Color.LTGRAY
        canvas.drawLine(margin, 810f, 555f, 810f, paint)
        paint.color = Color.GRAY
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Generado por Deudom App", 297f, 830f, paint)

        pdfDocument.finishPage(page)

        // Save file
        val file = File(context.cacheDir, "Deudom_Reporte_${System.currentTimeMillis()}.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        pdfDocument.close()

        sharePdf(context, file)
    }

    private fun sharePdf(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(context, "com.deudom.deudom.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir Reporte PDF"))
    }
}
