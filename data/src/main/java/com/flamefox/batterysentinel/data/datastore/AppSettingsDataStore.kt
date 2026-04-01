package com.flamefox.batterysentinel.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.flamefox.batterysentinel.domain.model.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class AppSettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val CHARGE_ALARM_THRESHOLD = intPreferencesKey("charge_alarm_threshold")
        val TEMPERATURE_ALARM_CELSIUS = floatPreferencesKey("temperature_alarm_celsius")
        val ANOMALY_MULTIPLIER = floatPreferencesKey("anomaly_multiplier")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            chargeAlarmThreshold = prefs[Keys.CHARGE_ALARM_THRESHOLD] ?: 80,
            temperatureAlarmThresholdCelsius = prefs[Keys.TEMPERATURE_ALARM_CELSIUS] ?: 40f,
            anomalyMultiplier = prefs[Keys.ANOMALY_MULTIPLIER] ?: 2.0f,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false
        )
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CHARGE_ALARM_THRESHOLD] = settings.chargeAlarmThreshold
            prefs[Keys.TEMPERATURE_ALARM_CELSIUS] = settings.temperatureAlarmThresholdCelsius
            prefs[Keys.ANOMALY_MULTIPLIER] = settings.anomalyMultiplier
            prefs[Keys.NOTIFICATIONS_ENABLED] = settings.notificationsEnabled
            prefs[Keys.ONBOARDING_COMPLETED] = settings.onboardingCompleted
        }
    }
}
