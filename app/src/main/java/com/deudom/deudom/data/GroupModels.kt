package com.deudom.deudom.data

import java.util.UUID

data class Group(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val creatorPhone: String = "",
    val members: List<GroupMember> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class GroupMember(
    val phone: String = "",
    val name: String = "",
    val photoUrl: String? = null
)

data class GroupExpense(
    val id: String = UUID.randomUUID().toString(),
    val groupId: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val currency: String = "RD$",
    val payerPhone: String = "",
    val payerName: String = "",
    val date: Long = System.currentTimeMillis(),
    val participants: List<String> = emptyList() // List of member phones
)
