package com.sujal.sharex

import android.app.Application
import androidx.room.Room
import com.sujal.sharex.database.TransferDatabase

class ShareXApplication : Application() {
    lateinit var database: TransferDatabase

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            TransferDatabase::class.java, "sharex-db"
        ).build()
    }
}
