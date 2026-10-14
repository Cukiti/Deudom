package com.deudom.deudom.data

import java.util.UUID

data class PersonalExpense(
    val id: String = UUID.randomUUID().toString(),
    val userPhone: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val category: String = "Otros",
    val date: Long = System.currentTimeMillis()
)
