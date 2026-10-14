package com.deudom.deudom.data

import java.util.UUID

data class FriendRequest(
    val id: String = UUID.randomUUID().toString(),
    val senderPhone: String = "",
    val senderName: String = "",
    val senderPhotoUrl: String? = null, // New
    val receiverPhone: String = "",
    val receiverName: String = "",
    val receiverPhotoUrl: String? = null, // New
    val status: FriendshipStatus = FriendshipStatus.PENDING,
    val timestamp: Long = System.currentTimeMillis()
)

enum class FriendshipStatus {
    PENDING, ACCEPTED, REJECTED
}
