package com.flamefox.batterysentinel.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.flamefox.batterysentinel.core.common.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val MAIN_ACTIVITY_CLASS = "com.flamefox.batterysentinel.MainActivity"

@Singleton
class BatteryNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannels()
    }

    private fun createChannels() {
        val monitorChannel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_MONITOR,
            "Battery Monitor",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Persistent battery monitoring service" }

        val alertChannel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ALERTS,
            "Battery Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Charge alarms and temperature warnings"
            enableVibration(true)
        }

        notificationManager.createNotificationChannel(monitorChannel)
        notificationManager.createNotificationChannel(alertChannel)
    }

    private fun buildMainActivityIntent(): PendingIntent {
        val intent = try {
            val clazz = Class.forName(MAIN_ACTIVITY_CLASS)
            Intent(context, clazz).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        } catch (e: ClassNotFoundException) {
            Intent()
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun buildServiceNotification(percentage: Int, isCharging: Boolean): Notification {
        val status = if (isCharging) "Wird geladen" else "Entlädt"
        return NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_MONITOR)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("BatterySentinel")
            .setContentText("$percentage% — $status")
            .setContentIntent(buildMainActivityIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()
    }

    fun showChargeAlarmNotification(percentage: Int, threshold: Int) {
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("Ladestand erreicht")
            .setContentText("Batterie bei $percentage% (Schwelle: $threshold%) — Ladekabel trennen für bessere Akku-Gesundheit")
            .setContentIntent(buildMainActivityIntent())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(Constants.NOTIFICATION_ID_CHARGE_ALARM, notification)
    }

    fun showTemperatureAlarmNotification(tempCelsius: Float) {
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("Hohe Akkutemperatur")
            .setContentText("Akkutemperatur %.1f°C — Gerät abkühlen lassen".format(tempCelsius))
            .setContentIntent(buildMainActivityIntent())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(Constants.NOTIFICATION_ID_TEMP_ALARM, notification)
    }

    fun showAnomalyNotification(currentDrain: Float, baseline: Float) {
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("Ungewöhnlicher Akkuverbrauch")
            .setContentText("Verbrauch %.1f%%/h vs. Basislinie %.1f%%/h — Hintergrund-Apps prüfen".format(currentDrain, baseline))
            .setContentIntent(buildMainActivityIntent())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(Constants.NOTIFICATION_ID_ANOMALY, notification)
    }

    fun cancelChargeAlarm() {
        notificationManager.cancel(Constants.NOTIFICATION_ID_CHARGE_ALARM)
    }
}
