package com.deudom.deudom.util

import com.deudom.deudom.data.Loan
import java.text.NumberFormat
import java.util.Locale

enum class ReminderTone {
    FRIENDLY,   // "Hola! Recuerda..."
    FORMAL,     // "Estimado, recordatorio de vencimiento..."
    FUNNY,      // "Mi cartera te extraña..."
    URGENT,     // "Necesito que revises esto..."
    PASSIVE     // "Solo para que estemos en la misma página..."
}

object ReminderUtils {
    
    fun generateMessage(loan: Loan, tone: ReminderTone): String {
        val amountStr = "${loan.currency} ${String.format("%.2f", loan.amount - loan.paidAmount)}"
        val name = loan.borrowerName.split(" ").firstOrNull() ?: "amigo"
        
        return when(tone) {
            ReminderTone.FRIENDLY -> """
                Hola $name! 👋
                Espero que estés súper bien.
                
                Solo pasaba para recordarte el pendiente de $amountStr que tenemos en Deudom.
                Avísame si puedes abonar algo esta semana. ¡Gracias! ✨
            """.trimIndent()
            
            ReminderTone.FORMAL -> """
                Estimado(a) ${loan.borrowerName},
                
                Le enviamos este recordatorio automático sobre el saldo pendiente de $amountStr.
                Fecha de vencimiento original: ${formatDate(loan.dueDate)}
                
                Agradecemos su atención. 
                _Enviado vía Deudom App_
            """.trimIndent()
            
            ReminderTone.FUNNY -> """
                🚨 Alerta de Búsqueda 🚨
                
                Se busca a $amountStr que se perdieron de mi bolsillo y se fueron al tuyo 😆
                
                ¡Haz que regresen a casa! Mi cartera los extraña 😢.
                
                (Es broma, pero sí págame jeje)
            """.trimIndent()
            
            ReminderTone.URGENT -> """
                Hola $name.
                Necesito que por favor revises el pago pendiente de $amountStr lo antes posible.
                
                Me urge cerrar este tema. Quedo atento a tu confirmación.
            """.trimIndent()
            
            ReminderTone.PASSIVE -> """
                Hola! Solo actualizando mis cuentas en la app.
                Me aparece que tenemos un saldo de $amountStr pendiente.
                
                ¿Te cuadra igual? Avísame para marcarlo o si ya me depositaste. 👍
            """.trimIndent()
        }
    }
    
    private fun formatDate(timestamp: Long?): String {
        if (timestamp == null) return "Indefinida"
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}
