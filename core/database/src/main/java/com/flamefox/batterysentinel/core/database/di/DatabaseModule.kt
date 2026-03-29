package com.flamefox.batterysentinel.core.database.di

import android.content.Context
import androidx.room.Room
import com.flamefox.batterysentinel.core.database.AppDatabase
import com.flamefox.batterysentinel.core.database.dao.BatterySampleDao
import com.flamefox.batterysentinel.core.database.dao.ChargingSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideChargingSessionDao(db: AppDatabase): ChargingSessionDao = db.chargingSessionDao()

    @Provides
    fun provideBatterySampleDao(db: AppDatabase): BatterySampleDao = db.batterySampleDao()
}
