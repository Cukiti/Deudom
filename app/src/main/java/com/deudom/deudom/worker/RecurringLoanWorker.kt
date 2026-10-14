package com.deudom.deudom.worker

import android.content.Context
import androidx.work.*
import com.deudom.deudom.data.FirestoreService
import com.deudom.deudom.data.Loan
import com.deudom.deudom.data.LoanStatus
import com.deudom.deudom.data.RecurringLoan
import com.google.firebase.auth.FirebaseAuth
import java.util.*
import java.util.concurrent.TimeUnit

class RecurringLoanWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val auth = FirebaseAuth.getInstance()
        val userPhone = auth.currentUser?.phoneNumber?.replace("+1", "") ?: return Result.failure()

        val recurringLoans = FirestoreService.getRecurringLoans(userPhone)
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        recurringLoans.filter { it.isActive }.forEach { rec ->
            val lastCreated = Calendar.getInstance().apply { timeInMillis = rec.lastCreatedTimestamp }
            val isDifferentMonth = lastCreated.get(Calendar.MONTH) != currentMonth || lastCreated.get(Calendar.YEAR) != currentYear
            
            if (currentDay >= rec.dayOfMonth && isDifferentMonth) {
                // Create the actual loan
                val user = FirestoreService.getUser(userPhone)
                val borrower = FirestoreService.getUser(rec.borrowerPhone)
                
                val newLoan = Loan(
                    lenderPhone = userPhone,
                    lenderName = user?.name ?: "Yo",
                    lenderPhotoUrl = user?.photoUrl,
                    borrowerPhone = rec.borrowerPhone,
                    borrowerName = rec.borrowerName,
                    borrowerPhotoUrl = borrower?.photoUrl,
                    amount = rec.amount,
                    description = rec.description + " (Recurrente)",
                    currency = rec.currency,
                    status = LoanStatus.REQUESTED
                )
                
                if (FirestoreService.createLoan(newLoan)) {
                    FirestoreService.updateRecurringLoan(rec.copy(lastCreatedTimestamp = System.currentTimeMillis()))
                }
            }
        }

        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<RecurringLoanWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "DeudomRecurring",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
