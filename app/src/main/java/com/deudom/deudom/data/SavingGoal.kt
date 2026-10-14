package com.deudom.deudom.data

import java.util.UUID

data class SavingGoal(
    val id: String = UUID.randomUUID().toString(),
    val userPhone: String = "",
    val name: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0, // This could be manually assigned or linked to balance
    val deadline: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
