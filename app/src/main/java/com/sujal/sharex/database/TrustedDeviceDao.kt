package com.sujal.sharex.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface TrustedDeviceDao {
    @Query("SELECT * FROM trusted_devices")
    fun getAll(): List<TrustedDeviceEntity>

    @Query("SELECT * FROM trusted_devices WHERE endpointId = :endpointId")
    fun getById(endpointId: String): TrustedDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(device: TrustedDeviceEntity)

    @Update
    fun update(device: TrustedDeviceEntity)

    @Query("DELETE FROM trusted_devices WHERE endpointId = :endpointId")
    fun deleteById(endpointId: String)
}
