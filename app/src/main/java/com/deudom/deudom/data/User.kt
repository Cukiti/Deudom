package com.deudom.deudom.data

data class User(
    val uid: String = "",
    val name: String = "",
    val phone: String = "",
    val goodPayCount: Int = 0,
    val badPayCount: Int = 0,
    val rank: String = "Bronce",
    val fcmToken: String = "",
    val photoUrl: String? = null,
    val walletCash: Double = 0.0,
    val currentBalance: Double = 0.0,
    val emergencyReserve: Double = 0.0,
    val baseCurrency: String = "RD$",
    val lastWalletUpdateTimestamp: Long = 0,
    val categoryBudgets: Map<String, Double> = emptyMap(),
    val score: Int = 50,
    val cukitis: Int = 0, // Virtual currency for rewards
    val unlockedThemes: List<String> = emptyList(),
    val pin: String? = null, // 4-digit security PIN for accounts without system security
    val createdAt: Long = System.currentTimeMillis()
)
