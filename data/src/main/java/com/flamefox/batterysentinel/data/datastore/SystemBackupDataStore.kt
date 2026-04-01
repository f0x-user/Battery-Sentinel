package com.flamefox.batterysentinel.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.flamefox.batterysentinel.domain.model.SystemBackup
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_BACKUPS = 5

private val Context.systemBackupDataStore: DataStore<Preferences> by preferencesDataStore(name = "system_backup")

@Singleton
class SystemBackupDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val NEXT_SLOT = intPreferencesKey("backup_next_slot")

    private fun slotKey(slot: Int, name: String) = when (name) {
        "brightness" -> intPreferencesKey("backup_${slot}_brightness")
        "adaptive" -> booleanPreferencesKey("backup_${slot}_adaptive")
        "timeout" -> intPreferencesKey("backup_${slot}_timeout")
        "battery_saver" -> booleanPreferencesKey("backup_${slot}_battery_saver")
        "sync" -> booleanPreferencesKey("backup_${slot}_sync")
        "doze" -> stringPreferencesKey("backup_${slot}_doze")
        "saved_at" -> longPreferencesKey("backup_${slot}_saved_at")
        else -> throw IllegalArgumentException("Unknown key: $name")
    }

    /** Neuestes Backup (für Anzeige und Wiederherstellung) */
    val backup: Flow<SystemBackup?> = context.systemBackupDataStore.data.map { prefs ->
        val nextSlot = prefs[NEXT_SLOT] ?: 0
        // Das zuletzt geschriebene Backup ist im Slot (nextSlot - 1 + MAX) % MAX
        val lastSlot = (nextSlot - 1 + MAX_BACKUPS) % MAX_BACKUPS
        readSlot(prefs, lastSlot)
    }

    /** Alle gespeicherten Backups (neueste zuerst) */
    val allBackups: Flow<List<SystemBackup>> = context.systemBackupDataStore.data.map { prefs ->
        val nextSlot = prefs[NEXT_SLOT] ?: 0
        (0 until MAX_BACKUPS).mapNotNull { offset ->
            val slot = (nextSlot - 1 - offset + MAX_BACKUPS * 2) % MAX_BACKUPS
            readSlot(prefs, slot)
        }.sortedByDescending { it.savedAt }
    }

    private fun readSlot(prefs: Preferences, slot: Int): SystemBackup? {
        val savedAt = prefs[longPreferencesKey("backup_${slot}_saved_at")] ?: 0L
        if (savedAt == 0L) return null
        return SystemBackup(
            brightness = prefs[intPreferencesKey("backup_${slot}_brightness")] ?: 127,
            isAdaptiveBrightness = prefs[booleanPreferencesKey("backup_${slot}_adaptive")] ?: false,
            screenTimeoutMs = prefs[intPreferencesKey("backup_${slot}_timeout")] ?: 30000,
            isBatterySaverEnabled = prefs[booleanPreferencesKey("backup_${slot}_battery_saver")] ?: false,
            isSyncEnabled = prefs[booleanPreferencesKey("backup_${slot}_sync")] ?: true,
            dozeConstants = prefs[stringPreferencesKey("backup_${slot}_doze")] ?: "default",
            savedAt = savedAt
        )
    }

    suspend fun saveBackup(backup: SystemBackup) {
        context.systemBackupDataStore.edit { prefs ->
            val slot = prefs[NEXT_SLOT] ?: 0
            prefs[intPreferencesKey("backup_${slot}_brightness")] = backup.brightness
            prefs[booleanPreferencesKey("backup_${slot}_adaptive")] = backup.isAdaptiveBrightness
            prefs[intPreferencesKey("backup_${slot}_timeout")] = backup.screenTimeoutMs
            prefs[booleanPreferencesKey("backup_${slot}_battery_saver")] = backup.isBatterySaverEnabled
            prefs[booleanPreferencesKey("backup_${slot}_sync")] = backup.isSyncEnabled
            prefs[stringPreferencesKey("backup_${slot}_doze")] = backup.dozeConstants
            prefs[longPreferencesKey("backup_${slot}_saved_at")] = backup.savedAt
            prefs[NEXT_SLOT] = (slot + 1) % MAX_BACKUPS
        }
    }

    suspend fun getLatestBackup(): SystemBackup? = backup.first()

    suspend fun clearAll() {
        context.systemBackupDataStore.edit { it.clear() }
    }
}
