package com.sujal.sharex.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TransferEntity::class], version = 2)
abstract class TransferDatabase : RoomDatabase() {
    abstract fun transferDao(): TransferDao
}
