package com.flamefox.batterysentinel.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.flamefox.batterysentinel.core.database.entity.ChargingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChargingSessionDao {

    @Query("SELECT * FROM charging_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<ChargingSessionEntity>>

    @Query("SELECT * FROM charging_sessions ORDER BY startTime DESC LIMIT :limit")
    fun getRecentSessions(limit: Int): Flow<List<ChargingSessionEntity>>

    @Query("SELECT * FROM charging_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): ChargingSessionEntity?

    @Query("SELECT * FROM charging_sessions WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveSession(): ChargingSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChargingSessionEntity): Long

    @Update
    suspend fun updateSession(session: ChargingSessionEntity)

    @Query("DELETE FROM charging_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("SELECT AVG(estimatedHealthPercent) FROM charging_sessions WHERE estimatedHealthPercent IS NOT NULL")
    fun getAverageHealthPercent(): Flow<Float?>

    @Query("DELETE FROM charging_sessions WHERE startTime < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
