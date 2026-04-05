package com.flamefox.batterysentinel.data.source

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.os.PowerManager
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemSettingsDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val resolver: ContentResolver get() = context.contentResolver
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    fun getBrightness(): Int = try {
        Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS)
    } catch (e: Settings.SettingNotFoundException) {
        127
    }

    fun setBrightness(value: Int): Boolean = try {
        Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, value)
    } catch (e: SecurityException) {
        false
    }

    fun getScreenTimeout(): Int = try {
        Settings.System.getInt(resolver, Settings.System.SCREEN_OFF_TIMEOUT)
    } catch (e: Settings.SettingNotFoundException) {
        30000
    }

    fun setScreenTimeout(value: Int): Boolean = try {
        Settings.System.putInt(resolver, Settings.System.SCREEN_OFF_TIMEOUT, value)
    } catch (e: SecurityException) {
        false
    }

    fun isAdaptiveBrightnessEnabled(): Boolean = try {
        Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE) ==
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
    } catch (e: Settings.SettingNotFoundException) {
        false
    }

    fun setAdaptiveBrightness(enabled: Boolean): Boolean = try {
        val mode = if (enabled) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, mode)
    } catch (e: SecurityException) {
        false
    }

    fun isBatterySaverEnabled(): Boolean = powerManager.isPowerSaveMode

    fun setBatterySaver(enabled: Boolean): Boolean = try {
        Settings.Global.putInt(
            resolver, "low_power", if (enabled) 1 else 0
        )
    } catch (e: SecurityException) {
        false
    }

    fun isSyncEnabled(): Boolean =
        ContentResolver.getMasterSyncAutomatically()

    fun setSync(enabled: Boolean) {
        ContentResolver.setMasterSyncAutomatically(enabled)
    }

    fun getDozeStatus(): String = try {
        Settings.Global.getString(resolver, "device_idle_constants") ?: "default"
    } catch (e: Exception) {
        "unknown"
    }

    fun setDozeConstants(constants: String): Boolean = try {
        Settings.Global.putString(resolver, "device_idle_constants", constants)
    } catch (e: SecurityException) {
        false
    }

    fun isIgnoringBatteryOptimizations(): Boolean =
        powerManager.isIgnoringBatteryOptimizations(context.packageName)

    fun hasWriteSettingsPermission(): Boolean =
        Settings.System.canWrite(context)

    fun hasWriteSecureSettingsPermission(): Boolean =
        context.checkCallingOrSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) ==
                PackageManager.PERMISSION_GRANTED
}
