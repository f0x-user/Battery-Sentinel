package com.flamefox.batterysentinel.di

import com.flamefox.batterysentinel.domain.repository.BatteryRepository
import com.flamefox.batterysentinel.domain.repository.ChargingSessionRepository
import com.flamefox.batterysentinel.domain.repository.AppUsageRepository
import com.flamefox.batterysentinel.domain.repository.SystemSettingsRepository
import com.flamefox.batterysentinel.domain.usecase.ControlBrightnessUseCase
import com.flamefox.batterysentinel.domain.usecase.DetectAnomalyUseCase
import com.flamefox.batterysentinel.domain.usecase.GetAppUsageUseCase
import com.flamefox.batterysentinel.domain.usecase.GetBatteryHealthUseCase
import com.flamefox.batterysentinel.domain.usecase.GetBatteryStateUseCase
import com.flamefox.batterysentinel.domain.usecase.GetChargingSessionsUseCase
import com.flamefox.batterysentinel.domain.usecase.GetDrainRateUseCase
import com.flamefox.batterysentinel.domain.usecase.GetPerAppBatteryUseCase
import com.flamefox.batterysentinel.domain.usecase.ToggleBatterySaverUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideGetBatteryStateUseCase(repository: BatteryRepository) =
        GetBatteryStateUseCase(repository)

    @Provides
    fun provideGetChargingSessionsUseCase(repository: ChargingSessionRepository) =
        GetChargingSessionsUseCase(repository)

    @Provides
    fun provideGetBatteryHealthUseCase(repository: ChargingSessionRepository) =
        GetBatteryHealthUseCase(repository)

    @Provides
    fun provideGetDrainRateUseCase(repository: BatteryRepository) =
        GetDrainRateUseCase(repository)

    @Provides
    fun provideGetAppUsageUseCase(repository: AppUsageRepository) =
        GetAppUsageUseCase(repository)

    @Provides
    fun provideGetPerAppBatteryUseCase(repository: AppUsageRepository) =
        GetPerAppBatteryUseCase(repository)

    @Provides
    fun provideControlBrightnessUseCase(repository: SystemSettingsRepository) =
        ControlBrightnessUseCase(repository)

    @Provides
    fun provideToggleBatterySaverUseCase(repository: SystemSettingsRepository) =
        ToggleBatterySaverUseCase(repository)

    @Provides
    fun provideDetectAnomalyUseCase(
        batteryRepository: BatteryRepository,
        settingsRepository: SystemSettingsRepository
    ) = DetectAnomalyUseCase(batteryRepository, settingsRepository)
}
