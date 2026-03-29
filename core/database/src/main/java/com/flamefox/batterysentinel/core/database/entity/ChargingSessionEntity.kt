package com.flamefox.batterysentinel.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "charging_sessions")
data class ChargingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val startPercent: Int,
    val endPercent: Int? = null,
    val startChargeCounter: Int,
    val endChargeCounter: Int? = null,
    val mAhDelivered: Float? = null,
    val estimatedHealthPercent: Float? = null
)
