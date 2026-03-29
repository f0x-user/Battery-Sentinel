package com.flamefox.batterysentinel.domain.usecase

import com.flamefox.batterysentinel.domain.repository.ChargingSessionRepository
import kotlinx.coroutines.flow.Flow

private const val PIXEL_8_PRO_DESIGN_CAPACITY_MAH = 5050f

class GetBatteryHealthUseCase(private val repository: ChargingSessionRepository) {
    operator fun invoke(): Flow<Float?> = repository.getAverageHealthPercent()

    fun estimateHealthFromSession(mAhDelivered: Float, startPercent: Int, endPercent: Int): Float {
        val percentCharged = (endPercent - startPercent).coerceAtLeast(1)
        val estimatedFullCapacity = mAhDelivered * (100f / percentCharged)
        return (estimatedFullCapacity / PIXEL_8_PRO_DESIGN_CAPACITY_MAH * 100f).coerceIn(0f, 100f)
    }
}
