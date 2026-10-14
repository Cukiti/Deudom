package com.deudom.deudom.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.deudom.deudom.data.LoanStatus
import com.deudom.deudom.data.Payment
import com.deudom.deudom.data.Installment

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey val id: String,
    val lenderPhone: String,
    val borrowerPhone: String,
    val lenderName: String,
    val lenderPhotoUrl: String?,
    val borrowerName: String,
    val borrowerPhotoUrl: String?,
    val amount: Double,
    val description: String,
    val date: Long,
    val dueDate: Long?,
    val status: String, // Store enum as string
    val currency: String,
    val category: String,
    val paidAmount: Double,
    val payments: List<Payment>,
    val installments: List<Installment>,
    val signatureUrl: String?,
    val isSynced: Boolean = false // New field for sync tracking
)
