package com.deudom.deudom.data

import java.util.UUID

data class Installment(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double = 0.0,
    val dueDate: Long = 0,
    val isPaid: Boolean = false
)
