package com.deudom.deudom.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans WHERE lenderPhone = :phone OR borrowerPhone = :phone")
    fun getLoansForUser(phone: String): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE id = :loanId")
    fun getLoanById(loanId: String): Flow<LoanEntity?>

    @Query("SELECT * FROM loans WHERE isSynced = 0")
    fun getUnsyncedLoans(): List<LoanEntity>

    @Query("UPDATE loans SET isSynced = 1 WHERE id = :loanId")
    fun markAsSynced(loanId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLoan(loan: LoanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLoans(loans: List<LoanEntity>)

    @Update
    fun updateLoan(loan: LoanEntity): Int

    @Query("UPDATE loans SET status = :status, paidAmount = :paidAmount, isSynced = 0 WHERE id = :loanId")
    fun updateLoanStatus(loanId: String, status: String, paidAmount: Double)

    @Query("DELETE FROM loans")
    fun clearAll()
}
