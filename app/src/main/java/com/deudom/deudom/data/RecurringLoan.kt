package com.deudom.deudom.data

import java.util.UUID

data class RecurringLoan(
    val id: String = UUID.randomUUID().toString(),
    val creatorPhone: String = "",
    val borrowerPhone: String = "",
    val borrowerName: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val currency: String = "RD$",
    val dayOfMonth: Int = 1,
    val isActive: Boolean = true,
    val lastCreatedTimestamp: Long = 0
)
