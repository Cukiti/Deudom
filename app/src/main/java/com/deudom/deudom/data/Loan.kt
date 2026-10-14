package com.deudom.deudom.data

import java.util.UUID

data class Loan(
    val id: String = UUID.randomUUID().toString(),
    val lenderPhone: String = "",
    val borrowerPhone: String = "",
    val lenderName: String = "",
    val lenderPhotoUrl: String? = null, // New
    val borrowerName: String = "",
    val borrowerPhotoUrl: String? = null, // New
    val amount: Double = 0.0,
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val status: LoanStatus = LoanStatus.PENDING,
    val currency: String = "RD$",
    val category: String = "Otros",
    val paidAmount: Double = 0.0,
    val payments: List<Payment> = emptyList(),
    val installments: List<Installment> = emptyList(),
    val signatureUrl: String? = null,
    val isSynced: Boolean = true // Default true assuming fetched from remote unless created locally
)

enum class LoanStatus {
    REQUESTED, PENDING, PAID, OVERDUE, REJECTED
}
