package com.flamefox.batterysentinel.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.flamefox.batterysentinel.core.database.dao.BatterySampleDao
import com.flamefox.batterysentinel.core.database.dao.ChargingSessionDao
import com.flamefox.batterysentinel.core.database.entity.BatterySampleEntity
import com.flamefox.batterysentinel.core.database.entity.ChargingSessionEntity

@Database(
    entities = [ChargingSessionEntity::class, BatterySampleEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chargingSessionDao(): ChargingSessionDao
    abstract fun batterySampleDao(): BatterySampleDao

    companion object {
        const val DATABASE_NAME = "battery_sentinel.db"
    }
}
