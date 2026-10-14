package com.deudom.deudom.repository

import com.deudom.deudom.data.FirestoreService
import com.deudom.deudom.data.Loan
import com.deudom.deudom.data.LoanStatus
import com.deudom.deudom.data.local.LoanDao
import com.deudom.deudom.data.local.LoanEntity
import com.deudom.deudom.data.local.FriendEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DeudomRepository(
    private val loanDao: LoanDao,
    private val friendDao: com.deudom.deudom.data.local.FriendDao
) {

    fun getLoans(phone: String): Flow<List<Loan>> {
        return loanDao.getLoansForUser(phone).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getFriends(): Flow<List<FriendEntity>> {
        return friendDao.getAllFriends()
    }

    suspend fun refreshFriends(phone: String) = withContext(Dispatchers.IO) {
        try {
            val requests = FirestoreService.getFriends(phone)
            val friends = requests.map { req ->
                if (req.senderPhone == phone) {
                    FriendEntity(
                        phone = req.receiverPhone, 
                        name = req.receiverName, 
                        photoUrl = req.receiverPhotoUrl,
                        isRegistered = true,
                        firestoreId = req.id
                    )
                } else {
                    FriendEntity(
                        phone = req.senderPhone, 
                        name = req.senderName, 
                        photoUrl = req.senderPhotoUrl,
                        isRegistered = true,
                        firestoreId = req.id
                    )
                }
            }
            
            val remotePhones = friends.map { it.phone }
            if (remotePhones.isEmpty()) {
                // If there are no remote registered friends, delete all local registered friends
                friendDao.deleteRegisteredFriends()
            } else {
                // Delete local registered friends that are no longer in the remote list
                friendDao.deleteRegisteredFriendsNotIn(remotePhones)
                // Keep local manual friends by only replacing/inserting what came from firestore
                friendDao.insertFriends(friends)
            }
        } catch (e: Exception) {
            // Log error or ignore if offline
        }
    }
    
    suspend fun insertFriend(friend: FriendEntity) = withContext(Dispatchers.IO) {
        friendDao.insertFriends(listOf(friend))
    }
    
    suspend fun deleteFriend(phone: String, firestoreId: String? = null) = withContext(Dispatchers.IO) {
        // 1. Delete in Firestore if registered
        if (firestoreId != null) {
            FirestoreService.deleteFriendship(firestoreId)
        }
        // 2. Delete locally
        friendDao.deleteFriend(phone)
    }
    
    suspend fun updateManualFriend(oldPhone: String, newPhone: String, newName: String) = withContext(Dispatchers.IO) {
        if (oldPhone == newPhone) {
            // Just update name
            friendDao.updateFriend(oldPhone, newName, null)
        } else {
            // ID changed: Delete old, Insert new
            friendDao.deleteFriend(oldPhone)
            friendDao.insertFriends(listOf(FriendEntity(newPhone, newName, null, isRegistered = false)))
        }
    }

    fun getLoanById(loanId: String): Flow<Loan?> {
        return loanDao.getLoanById(loanId).map { it?.toDomain() }
    }

    suspend fun refreshLoans(phone: String) = withContext(Dispatchers.IO) {
        try {
            // Fetch remote loans
            val remoteLender = FirestoreService.getLoansAsLender(phone)
            val remoteBorrower = FirestoreService.getLoansAsBorrower(phone)
            val allRemote = remoteLender + remoteBorrower
            
            // Map remote loans to entities, ensuring status and other fields are updated.
            // Firebase is the ultimate source of truth for synced data.
            val remoteToInsert = allRemote.map { it.toEntity().copy(isSynced = true) }
            
            if (remoteToInsert.isNotEmpty()) {
                // Use insertLoans which uses OnConflictStrategy.REPLACE to update status/data
                loanDao.insertLoans(remoteToInsert)
            }
        } catch (e: Exception) {
            // Handle error or ignore if offline
        }
    }

    suspend fun insertLoan(loan: Loan) = withContext(Dispatchers.IO) {
        val loanEntity = loan.toEntity().copy(isSynced = false) // Force false for local creation
        loanDao.insertLoan(loanEntity)
    }

    suspend fun updateLoan(loan: Loan) = withContext(Dispatchers.IO) {
        val loanEntity = loan.toEntity().copy(isSynced = false)
        if (loanDao.updateLoan(loanEntity) == 0) {
            loanDao.insertLoan(loanEntity)
        }
    }

    suspend fun settleLoan(loan: Loan): Loan = withContext(Dispatchers.IO) {
        val remainingAmount = loan.amount - loan.paidAmount
        val updatedInstallments = loan.installments.map { it.copy(isPaid = true) }
        
        // Use a small epsilon to detect if there's actually any money left to pay
        val settledLoan = if (remainingAmount > 0.001) {
            val finalPayment = com.deudom.deudom.data.Payment(
                amount = remainingAmount,
                date = System.currentTimeMillis(),
                description = "Liquidación total de deuda"
            )
            val updatedPayments = loan.payments + finalPayment
            loan.copy(
                paidAmount = loan.amount, // Force exactly the total amount
                status = LoanStatus.PAID,
                payments = updatedPayments,
                installments = updatedInstallments,
                isSynced = false
            )
        } else {
            loan.copy(
                status = LoanStatus.PAID, 
                paidAmount = loan.amount, // Force exactly the total amount
                installments = updatedInstallments,
                isSynced = false
            )
        }
        
        val entity = settledLoan.toEntity()
        if (loanDao.updateLoan(entity) == 0) loanDao.insertLoan(entity)
        settledLoan
    }

    suspend fun addPayment(loan: Loan, paymentAmount: Double, description: String? = null): Loan = withContext(Dispatchers.IO) {
        val newPaidAmount = loan.paidAmount + paymentAmount
        val isPaid = newPaidAmount >= (loan.amount - 0.01)
        val newStatus = if (isPaid) LoanStatus.PAID else LoanStatus.PENDING

        val newPayment = com.deudom.deudom.data.Payment(
            amount = paymentAmount,
            date = System.currentTimeMillis(),
            description = description
        )
        val updatedPayments = loan.payments + newPayment

        var cumulative = 0.0
        val updatedInstallments = loan.installments.map { inst ->
            cumulative += inst.amount
            // If the total paid amount covers or equals the cumulative needed for this installment, it's paid.
            // Using a small epsilon for floating point comparison issues
            inst.copy(isPaid = newPaidAmount >= (cumulative - 0.01))
        }

        val updatedLoan = loan.copy(
            paidAmount = if (isPaid) loan.amount else newPaidAmount,
            status = newStatus,
            payments = updatedPayments,
            installments = updatedInstallments,
            isSynced = false
        )
        val entity = updatedLoan.toEntity()
        if (loanDao.updateLoan(entity) == 0) loanDao.insertLoan(entity)
        updatedLoan
    }

    suspend fun insertLoans(loans: List<Loan>) = withContext(Dispatchers.IO) {
        if (loans.isNotEmpty()) {
            // Logic: Do not overwrite local loans that are not yet synced.
            // This prevents race conditions where a remote listener event (triggered by another user or a delay)
            // overwrites a local update that is currently pending sync.
            val unsyncedIds = loanDao.getUnsyncedLoans().map { it.id }.toSet()
            val filteredLoans = loans.filter { it.id !in unsyncedIds }
            
            if (filteredLoans.isNotEmpty()) {
                loanDao.insertLoans(filteredLoans.map { it.toEntity().copy(isSynced = true) })
            }
        }
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        loanDao.clearAll()
        friendDao.clearAll()
    }
    
    // New function for Worker
    fun getUnsyncedLoansSync(): List<Loan> {
        return loanDao.getUnsyncedLoans().map { it.toDomain() }
    }
    
    fun markAsSynced(loanId: String) {
        loanDao.markAsSynced(loanId)
    }

    private fun LoanEntity.toDomain() = Loan(
        id = id, lenderPhone = lenderPhone, borrowerPhone = borrowerPhone,
        lenderName = lenderName, lenderPhotoUrl = lenderPhotoUrl,
        borrowerName = borrowerName, borrowerPhotoUrl = borrowerPhotoUrl,
        amount = amount, description = description, date = date,
        dueDate = dueDate, status = LoanStatus.valueOf(status),
        currency = currency, category = category, paidAmount = paidAmount, 
        payments = payments, installments = installments, signatureUrl = signatureUrl,
        isSynced = isSynced
    )

    private fun Loan.toEntity() = LoanEntity(
        id = id, lenderPhone = lenderPhone, borrowerPhone = borrowerPhone,
        lenderName = lenderName, lenderPhotoUrl = lenderPhotoUrl,
        borrowerName = borrowerName, borrowerPhotoUrl = borrowerPhotoUrl,
        amount = amount, description = description, date = date,
        dueDate = dueDate, status = status.name,
        currency = currency, category = category, paidAmount = paidAmount, 
        payments = payments, installments = installments, signatureUrl = signatureUrl,
        isSynced = isSynced
    )
}
