package com.flamefox.batterysentinel.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battery_samples")
data class BatterySampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val percentage: Int,
    val currentMa: Int,
    val voltageMv: Int,
    val temperatureCelsius: Float,
    val isScreenOn: Boolean,
    val isCharging: Boolean
)
