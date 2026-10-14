package com.deudom.deudom.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {
    @Query("SELECT * FROM friends ORDER BY name ASC")
    fun getAllFriends(): Flow<List<FriendEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFriends(friends: List<FriendEntity>)

    @Query("DELETE FROM friends WHERE phone = :phone")
    fun deleteFriend(phone: String)

    @Query("UPDATE friends SET name = :newName, photoUrl = :newPhoto WHERE phone = :phone")
    fun updateFriend(phone: String, newName: String, newPhoto: String?)
    
    @Query("DELETE FROM friends WHERE isRegistered = 1")
    fun deleteRegisteredFriends()
    
    @Query("DELETE FROM friends WHERE isRegistered = 1 AND phone NOT IN (:phones)")
    fun deleteRegisteredFriendsNotIn(phones: List<String>)

    @Query("DELETE FROM friends")
    fun clearAll()
}
