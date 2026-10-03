package com.sujal.sharex.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfers ORDER BY timestamp DESC")
    fun getAll(): List<TransferEntity>

    @Insert
    fun insert(transfer: TransferEntity)
}
