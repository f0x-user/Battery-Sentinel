package com.flamefox.batterysentinel.data.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import com.flamefox.batterysentinel.core.common.Constants
import com.flamefox.batterysentinel.core.database.dao.BatterySampleDao
import com.flamefox.batterysentinel.core.database.dao.ChargingSessionDao
import com.flamefox.batterysentinel.core.database.entity.BatterySampleEntity
import com.flamefox.batterysentinel.core.database.entity.ChargingSessionEntity
import com.flamefox.batterysentinel.data.datastore.AppSettingsDataStore
import com.flamefox.batterysentinel.data.source.BatteryManagerDataSource
import com.flamefox.batterysentinel.domain.model.BatteryState
import com.flamefox.batterysentinel.domain.model.ChargeStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BatteryMonitorService : Service() {

    @Inject lateinit var batteryDataSource: BatteryManagerDataSource
    @Inject lateinit var notificationManager: BatteryNotificationManager
    @Inject lateinit var batterySampleDao: BatterySampleDao
    @Inject lateinit var chargingSessionDao: ChargingSessionDao
    @Inject lateinit var settingsDataStore: AppSettingsDataStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var chargeAlarmFired = false
    private var tempAlarmFired = false
    private var currentSessionId: Long? = null

    companion object {
        private val _batteryState = MutableStateFlow<BatteryState?>(null)
        val batteryState: StateFlow<BatteryState?> = _batteryState.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(
            Constants.NOTIFICATION_ID_SERVICE,
            notificationManager.buildServiceNotification(0, false)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startMonitoring()
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startMonitoring() {
        batteryDataSource.observeBatteryState()
            .onEach { state ->
                _batteryState.value = state
                updateServiceNotification(state)
                persistSample(state)
                handleChargeSession(state)
                checkAlarms(state)
            }
            .launchIn(serviceScope)

        serviceScope.launch(Dispatchers.IO) {
            // Batterie-Samples: 14 Tage aufbewahren
            batterySampleDao.deleteOlderThan(System.currentTimeMillis() - Constants.SEVEN_DAY_MS * 2)
            // Ladesitzungen: 90 Tage aufbewahren (DSGVO-Aufbewahrungsfrist)
            chargingSessionDao.deleteOlderThan(System.currentTimeMillis() - Constants.NINETY_DAY_MS)
        }
    }

    private fun updateServiceNotification(state: BatteryState) {
        val notification = notificationManager.buildServiceNotification(state.percentage, state.isCharging)
        notificationManager.let {
            val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.notify(Constants.NOTIFICATION_ID_SERVICE, notification)
        }
    }

    private fun persistSample(state: BatteryState) {
        serviceScope.launch(Dispatchers.IO) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            val isScreenOn = powerManager.isInteractive
            batterySampleDao.insertSample(
                BatterySampleEntity(
                    timestamp = state.timestamp,
                    percentage = state.percentage,
                    currentMa = state.currentMa,
                    voltageMv = state.voltageMv,
                    temperatureCelsius = state.temperatureCelsius,
                    isScreenOn = isScreenOn,
                    isCharging = state.isCharging
                )
            )
        }
    }

    private fun handleChargeSession(state: BatteryState) {
        serviceScope.launch(Dispatchers.IO) {
            if (state.isCharging) {
                if (currentSessionId == null) {
                    val activeSession = chargingSessionDao.getActiveSession()
                    if (activeSession == null) {
                        val newSession = ChargingSessionEntity(
                            startTime = state.timestamp,
                            startPercent = state.percentage,
                            startChargeCounter = state.chargeCounter
                        )
                        currentSessionId = chargingSessionDao.insertSession(newSession)
                    } else {
                        currentSessionId = activeSession.id
                    }
                }
            } else {
                currentSessionId?.let { sessionId ->
                    val session = chargingSessionDao.getSessionById(sessionId)
                    if (session != null && session.endTime == null) {
                        val mAhDelivered = calculateMahDelivered(
                            session.startChargeCounter,
                            state.chargeCounter
                        )
                        val health = session.endPercent?.let { endPct ->
                            val percentCharged = (endPct - session.startPercent).coerceAtLeast(1)
                            (mAhDelivered * (100f / percentCharged) / Constants.PIXEL_8_PRO_DESIGN_CAPACITY_MAH * 100f).coerceIn(0f, 100f)
                        }

                        chargingSessionDao.updateSession(
                            session.copy(
                                endTime = state.timestamp,
                                endPercent = state.percentage,
                                endChargeCounter = state.chargeCounter,
                                mAhDelivered = mAhDelivered,
                                estimatedHealthPercent = health
                            )
                        )
                    }
                    currentSessionId = null
                    chargeAlarmFired = false
                }
            }
        }
    }

    private fun calculateMahDelivered(startCounter: Int, endCounter: Int): Float {
        // BATTERY_PROPERTY_CHARGE_COUNTER in microampere-hours → convert to mAh
        return ((endCounter - startCounter) / 1000f).coerceAtLeast(0f)
    }

    private fun checkAlarms(state: BatteryState) {
        serviceScope.launch {
            val settings = settingsDataStore.settings.first()
            if (!settings.notificationsEnabled) return@launch

            if (state.isCharging && !chargeAlarmFired &&
                state.percentage >= settings.chargeAlarmThreshold) {
                chargeAlarmFired = true
                notificationManager.showChargeAlarmNotification(
                    state.percentage, settings.chargeAlarmThreshold
                )
            }

            if (state.temperatureCelsius >= settings.temperatureAlarmThresholdCelsius && !tempAlarmFired) {
                tempAlarmFired = true
                notificationManager.showTemperatureAlarmNotification(state.temperatureCelsius)
            } else if (state.temperatureCelsius < settings.temperatureAlarmThresholdCelsius - 2f) {
                tempAlarmFired = false
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
