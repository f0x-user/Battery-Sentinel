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

    fun buildServiceNotification(percentage: Int, isCharging: Boolean): Notification {
        val status = if (isCharging) "Charging" else "Discharging"
        return NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_MONITOR)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("BatterySentinel Active")
            .setContentText("$percentage% — $status")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()
    }

    fun showChargeAlarmNotification(percentage: Int, threshold: Int) {
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("Charge Threshold Reached")
            .setContentText("Battery at $percentage% (threshold: $threshold%) — unplug to protect battery health")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(Constants.NOTIFICATION_ID_CHARGE_ALARM, notification)
    }

    fun showTemperatureAlarmNotification(tempCelsius: Float) {
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("High Battery Temperature")
            .setContentText("Battery temperature is %.1f°C — let device cool down".format(tempCelsius))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(Constants.NOTIFICATION_ID_TEMP_ALARM, notification)
    }

    fun showAnomalyNotification(currentDrain: Float, baseline: Float) {
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("Abnormal Battery Drain")
            .setContentText("Screen-off drain %.1f%%/h vs baseline %.1f%%/h — check background apps".format(currentDrain, baseline))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(Constants.NOTIFICATION_ID_ANOMALY, notification)
    }

    fun cancelChargeAlarm() {
        notificationManager.cancel(Constants.NOTIFICATION_ID_CHARGE_ALARM)
    }
}
