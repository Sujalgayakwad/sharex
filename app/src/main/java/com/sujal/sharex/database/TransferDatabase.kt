package com.sujal.sharex.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TransferEntity::class], version = 1)
abstract class TransferDatabase : RoomDatabase() {
    abstract fun transferDao(): TransferDao
}
