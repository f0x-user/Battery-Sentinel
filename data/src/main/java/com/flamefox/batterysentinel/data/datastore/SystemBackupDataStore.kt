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
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.systemBackupDataStore: DataStore<Preferences> by preferencesDataStore(name = "system_backup")

@Singleton
class SystemBackupDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val BRIGHTNESS = intPreferencesKey("backup_brightness")
        val ADAPTIVE_BRIGHTNESS = booleanPreferencesKey("backup_adaptive_brightness")
        val SCREEN_TIMEOUT = intPreferencesKey("backup_screen_timeout")
        val BATTERY_SAVER = booleanPreferencesKey("backup_battery_saver")
        val SYNC_ENABLED = booleanPreferencesKey("backup_sync_enabled")
        val DOZE_CONSTANTS = stringPreferencesKey("backup_doze_constants")
        val SAVED_AT = longPreferencesKey("backup_saved_at")
    }

    val backup: Flow<SystemBackup?> = context.systemBackupDataStore.data.map { prefs ->
        val savedAt = prefs[Keys.SAVED_AT] ?: 0L
        if (savedAt == 0L) null
        else SystemBackup(
            brightness = prefs[Keys.BRIGHTNESS] ?: 127,
            isAdaptiveBrightness = prefs[Keys.ADAPTIVE_BRIGHTNESS] ?: false,
            screenTimeoutMs = prefs[Keys.SCREEN_TIMEOUT] ?: 30000,
            isBatterySaverEnabled = prefs[Keys.BATTERY_SAVER] ?: false,
            isSyncEnabled = prefs[Keys.SYNC_ENABLED] ?: true,
            dozeConstants = prefs[Keys.DOZE_CONSTANTS] ?: "default",
            savedAt = savedAt
        )
    }

    suspend fun saveBackup(backup: SystemBackup) {
        context.systemBackupDataStore.edit { prefs ->
            prefs[Keys.BRIGHTNESS] = backup.brightness
            prefs[Keys.ADAPTIVE_BRIGHTNESS] = backup.isAdaptiveBrightness
            prefs[Keys.SCREEN_TIMEOUT] = backup.screenTimeoutMs
            prefs[Keys.BATTERY_SAVER] = backup.isBatterySaverEnabled
            prefs[Keys.SYNC_ENABLED] = backup.isSyncEnabled
            prefs[Keys.DOZE_CONSTANTS] = backup.dozeConstants
            prefs[Keys.SAVED_AT] = backup.savedAt
        }
    }
}
