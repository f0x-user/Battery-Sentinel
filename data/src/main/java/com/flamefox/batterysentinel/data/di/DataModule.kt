package com.flamefox.batterysentinel.data.di

import com.flamefox.batterysentinel.data.repository.AppUsageRepositoryImpl
import com.flamefox.batterysentinel.data.repository.BatteryRepositoryImpl
import com.flamefox.batterysentinel.data.repository.ChargingSessionRepositoryImpl
import com.flamefox.batterysentinel.data.repository.SystemSettingsRepositoryImpl
import com.flamefox.batterysentinel.domain.repository.AppUsageRepository
import com.flamefox.batterysentinel.domain.repository.BatteryRepository
import com.flamefox.batterysentinel.domain.repository.ChargingSessionRepository
import com.flamefox.batterysentinel.domain.repository.SystemSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindBatteryRepository(impl: BatteryRepositoryImpl): BatteryRepository

    @Binds
    @Singleton
    abstract fun bindChargingSessionRepository(impl: ChargingSessionRepositoryImpl): ChargingSessionRepository

    @Binds
    @Singleton
    abstract fun bindAppUsageRepository(impl: AppUsageRepositoryImpl): AppUsageRepository

    @Binds
    @Singleton
    abstract fun bindSystemSettingsRepository(impl: SystemSettingsRepositoryImpl): SystemSettingsRepository
}
