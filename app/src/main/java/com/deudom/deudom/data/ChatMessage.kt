package com.deudom.deudom.data

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderPhone: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
