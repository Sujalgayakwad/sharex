package com.sujal.sharex.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TransferEntity::class, TrustedDeviceEntity::class], version = 3)
abstract class TransferDatabase : RoomDatabase() {
    abstract fun transferDao(): TransferDao
    abstract fun trustedDeviceDao(): TrustedDeviceDao
}
