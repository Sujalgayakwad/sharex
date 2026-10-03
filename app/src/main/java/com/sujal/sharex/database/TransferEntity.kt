package com.sujal.sharex.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transfers")
data class TransferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val isSent: Boolean,
    val timestamp: Long,
    val status: String
)
