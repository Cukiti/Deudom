package com.deudom.deudom.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent // Added
import android.content.Context
import android.content.Intent // Added
import android.net.Uri // Added
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.deudom.deudom.R
import com.deudom.deudom.data.FirestoreService
import com.deudom.deudom.data.LoanStatus
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val auth = FirebaseAuth.getInstance()
        val userPhone = auth.currentUser?.phoneNumber?.replace("+1", "") ?: return Result.failure()

        val loansAsLender = FirestoreService.getLoansAsLender(userPhone)
        val loansAsBorrower = FirestoreService.getLoansAsBorrower(userPhone)
        val allLoans = (loansAsLender + loansAsBorrower).filter { it.status != LoanStatus.PAID }

        val now = System.currentTimeMillis()
        val oneDayMillis = TimeUnit.DAYS.toMillis(1)

        allLoans.forEach { loan ->
            loan.dueDate?.let { dueDate ->
                val diff = dueDate - now
                if (diff in 0..oneDayMillis) {
                    showNotification(
                        loan.id.hashCode(),
                        "Vence Mañana",
                        "El préstamo '${loan.description}' vence en menos de 24h. Monto: ${loan.currency} ${loan.amount - loan.paidAmount}",
                        loan.id
                    )
                } else if (diff < 0) {
                    showNotification(
                        loan.id.hashCode(),
                        "¡PAGO VENCIDO!",
                        "El préstamo '${loan.description}' ya venció. Por favor, regulariza la situación.",
                        loan.id
                    )
                }
            }
        }

        // Smart Insight Logic (Friday Reminder)
        val calendar = java.util.Calendar.getInstance()
        val isFriday = calendar.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.FRIDAY
        
        if (isFriday) {
            val pendingLends = loansAsLender.filter { it.status == LoanStatus.PENDING }
            if (pendingLends.isNotEmpty()) {
                val topDebtor = pendingLends.maxByOrNull { it.amount }
                topDebtor?.let {
                    if (it.dueDate == null || it.dueDate > now) {
                        showNotification(
                            9999,
                            "💡 Tip Inteligente",
                            "Hoy es viernes. Buen día para recordar a ${it.borrowerName} sobre su deuda de ${it.currency} ${it.amount}.",
                            it.id
                        )
                    }
                }
            }
        }

        return Result.success()
    }

    private fun showNotification(id: Int, title: String, message: String, loanId: String?) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "deudom_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Recordatorios de Deudom", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("deudom://loan/$loanId"))
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 
            id, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent) // Set Intent
            .build()

        notificationManager.notify(id, notification)
    }

    companion object {
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<ReminderWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "DeudomReminder",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
