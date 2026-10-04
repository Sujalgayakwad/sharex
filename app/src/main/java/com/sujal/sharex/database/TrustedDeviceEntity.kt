package com.sujal.sharex.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trusted_devices")
data class TrustedDeviceEntity(
    @PrimaryKey
    val endpointId: String,
    val deviceName: String,
    val autoAccept: Boolean = false,
    val isBlocked: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis()
)
