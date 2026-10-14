package com.deudom.deudom.data

import java.util.UUID

data class Payment(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double = 0.0,
    val date: Long = System.currentTimeMillis(),
    val proofUrl: String? = null, // For images/receipts
    val description: String? = null
)
