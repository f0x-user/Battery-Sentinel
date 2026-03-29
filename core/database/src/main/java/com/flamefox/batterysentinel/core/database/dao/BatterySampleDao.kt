package com.flamefox.batterysentinel.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.flamefox.batterysentinel.core.database.entity.BatterySampleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BatterySampleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSample(sample: BatterySampleEntity)

    @Query("SELECT * FROM battery_samples WHERE timestamp > :since ORDER BY timestamp DESC")
    fun getSamplesSince(since: Long): Flow<List<BatterySampleEntity>>

    @Query("SELECT * FROM battery_samples WHERE isScreenOn = 0 AND isCharging = 0 AND timestamp > :since ORDER BY timestamp DESC")
    suspend fun getScreenOffSamplesSince(since: Long): List<BatterySampleEntity>

    @Query("SELECT * FROM battery_samples WHERE isScreenOn = 1 AND isCharging = 0 AND timestamp > :since ORDER BY timestamp DESC")
    suspend fun getScreenOnSamplesSince(since: Long): List<BatterySampleEntity>

    @Query("DELETE FROM battery_samples WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("SELECT COUNT(*) FROM battery_samples")
    suspend fun count(): Int
}
