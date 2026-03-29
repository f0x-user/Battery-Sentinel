package com.flamefox.batterysentinel

import android.content.Context
import android.os.BatteryManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BatteryManagerInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val batteryManager: BatteryManager =
        context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    @Test
    fun batteryManagerIsNotNull() {
        assertNotNull(batteryManager)
    }

    @Test
    fun batteryCapacityPropertyReturnsValidPercentage() {
        val capacity = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        assertTrue("Capacity should be between 0 and 100: $capacity", capacity in 0..100)
    }

    @Test
    fun batteryCurrentPropertyReturnsNonNullValue() {
        val current = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        // On a real device, current should be non-zero (charging or discharging)
        // We just verify the property can be read (Integer.MIN_VALUE = not supported)
        assertTrue("Current should not be Integer.MIN_VALUE", current != Int.MIN_VALUE)
    }

    @Test
    fun batteryChargeCounterPropertyReturnsPositiveValue() {
        val counter = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        assertTrue("Charge counter should be positive on real device: $counter", counter > 0)
    }
}
