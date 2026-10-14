package com.deudom.deudom.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.deudom.deudom.data.FirestoreService
import com.deudom.deudom.data.Loan
import com.deudom.deudom.data.LoanStatus
import com.deudom.deudom.data.local.DeudomDatabase
import com.deudom.deudom.data.local.LoanEntity

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = DeudomDatabase.getDatabase(applicationContext)
        val loanDao = db.loanDao()
        
        // Fetch unsynced loans
        val unsyncedEntities = loanDao.getUnsyncedLoans()
        if (unsyncedEntities.isEmpty()) {
            return Result.success()
        }

        var successCount = 0
        var failureCount = 0

        unsyncedEntities.forEach { entity ->
            val loan = entity.toDomain()
            // Upload to Firestore (createLoan acts as upsert)
            val success = FirestoreService.createLoan(loan)
            
            if (success) {
                loanDao.markAsSynced(entity.id)
                successCount++
            } else {
                failureCount++
            }
        }

        // If at least one failed, retry later
        return if (failureCount == 0) Result.success() else Result.retry()
    }

    // Helper mapper since we are in a worker and might not have access to Repository private extensions easily
    // Duplicate logic for safety and isolation
    private fun LoanEntity.toDomain() = Loan(
        id = id, lenderPhone = lenderPhone, borrowerPhone = borrowerPhone,
        lenderName = lenderName, lenderPhotoUrl = lenderPhotoUrl,
        borrowerName = borrowerName, borrowerPhotoUrl = borrowerPhotoUrl,
        amount = amount, description = description, date = date,
        dueDate = dueDate, status = LoanStatus.valueOf(status),
        currency = currency, category = category, paidAmount = paidAmount, 
        payments = payments, installments = installments, signatureUrl = signatureUrl,
        isSynced = true
    )

    companion object {
        fun scheduleNow(context: Context) {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()
            
            val request = androidx.work.OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.LINEAR,
                    10,
                    java.util.concurrent.TimeUnit.SECONDS
                )
                .build()
            
            androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                "SyncLoansImmediate",
                androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE,
                request
            )
        }
    }
}
