package com.flamefox.batterysentinel.domain.model

data class ChargingSession(
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val startPercent: Int,
    val endPercent: Int? = null,
    val startChargeCounter: Int,
    val endChargeCounter: Int? = null,
    val mAhDelivered: Float? = null,
    val estimatedHealthPercent: Float? = null
) {
    val isComplete: Boolean get() = endTime != null
    val durationMs: Long? get() = endTime?.let { it - startTime }
}
