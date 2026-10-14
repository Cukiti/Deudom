package com.deudom.deudom.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface OfflineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: OfflineTransaction)

    @Query("SELECT * FROM offline_transactions ORDER BY timestamp ASC")
    suspend fun getAllTransactions(): List<OfflineTransaction>

    @Delete
    suspend fun deleteTransaction(transaction: OfflineTransaction)
}
