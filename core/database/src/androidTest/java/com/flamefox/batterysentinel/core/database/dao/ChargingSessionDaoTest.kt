package com.flamefox.batterysentinel.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.flamefox.batterysentinel.core.database.AppDatabase
import com.flamefox.batterysentinel.core.database.entity.ChargingSessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChargingSessionDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ChargingSessionDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.chargingSessionDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndRetrieveSession() = runTest {
        val session = ChargingSessionEntity(startTime = 1000L, startPercent = 20, startChargeCounter = 1000)
        val id = dao.insertSession(session)

        val retrieved = dao.getSessionById(id)
        assertNotNull(retrieved)
        assertEquals(20, retrieved!!.startPercent)
    }

    @Test
    fun getActiveSessionReturnsOpenSession() = runTest {
        val open = ChargingSessionEntity(startTime = 1000L, startPercent = 20, startChargeCounter = 1000)
        dao.insertSession(open)

        val active = dao.getActiveSession()
        assertNotNull(active)
        assertNull(active!!.endTime)
    }

    @Test
    fun getActiveSessionReturnsNullWhenAllClosed() = runTest {
        val closed = ChargingSessionEntity(startTime = 1000L, endTime = 2000L, startPercent = 20, endPercent = 80, startChargeCounter = 1000)
        dao.insertSession(closed)

        val active = dao.getActiveSession()
        assertNull(active)
    }

    @Test
    fun getAllSessionsReturnsAllInserted() = runTest {
        dao.insertSession(ChargingSessionEntity(startTime = 1000L, startPercent = 10, startChargeCounter = 500))
        dao.insertSession(ChargingSessionEntity(startTime = 2000L, startPercent = 30, startChargeCounter = 1500))

        val all = dao.getAllSessions().first()
        assertEquals(2, all.size)
    }

    @Test
    fun deleteSessionRemovesIt() = runTest {
        val id = dao.insertSession(ChargingSessionEntity(startTime = 1000L, startPercent = 20, startChargeCounter = 1000))
        dao.deleteSession(id)

        val retrieved = dao.getSessionById(id)
        assertNull(retrieved)
    }

    @Test
    fun averageHealthPercentAggregatesCorrectly() = runTest {
        dao.insertSession(ChargingSessionEntity(startTime = 1000L, startPercent = 0, startChargeCounter = 0, estimatedHealthPercent = 90f))
        dao.insertSession(ChargingSessionEntity(startTime = 2000L, startPercent = 0, startChargeCounter = 0, estimatedHealthPercent = 80f))

        val avg = dao.getAverageHealthPercent().first()
        assertEquals(85f, avg!!, 0.1f)
    }
}
