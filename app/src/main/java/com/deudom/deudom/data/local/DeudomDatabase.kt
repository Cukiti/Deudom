package com.deudom.deudom.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [LoanEntity::class, FriendEntity::class], version = 13, exportSchema = false)
@TypeConverters(Converters::class)
abstract class DeudomDatabase : RoomDatabase() {
    abstract fun loanDao(): LoanDao
    abstract fun friendDao(): FriendDao
    // abstract fun offlineDao(): OfflineDao -- Disabled temporarily to fix KSP error

    companion object {
        @Volatile
        private var INSTANCE: DeudomDatabase? = null

        fun getDatabase(context: Context): DeudomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DeudomDatabase::class.java,
                    "deudom_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
