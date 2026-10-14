package com.deudom.deudom.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey val phone: String,
    val name: String,
    val photoUrl: String?,
    val isRegistered: Boolean = false,
    val firestoreId: String? = null // To track and delete the relationship in Firestore
)
