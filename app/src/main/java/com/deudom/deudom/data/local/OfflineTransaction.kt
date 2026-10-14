package com.deudom.deudom.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_transactions")
data class OfflineTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "PERSONAL_EXPENSE", "LOAN", "PAYMENT"
    val payloadJson: String,
    val timestamp: Long = System.currentTimeMillis()
)
