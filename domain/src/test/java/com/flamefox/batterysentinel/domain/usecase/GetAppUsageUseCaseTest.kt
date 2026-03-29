package com.flamefox.batterysentinel.domain.usecase

import com.flamefox.batterysentinel.domain.model.AppUsage
import com.flamefox.batterysentinel.domain.repository.AppUsageRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetAppUsageUseCaseTest {

    private val repository: AppUsageRepository = mockk()
    private val useCase = GetAppUsageUseCase(repository)

    private val apps = listOf(
        AppUsage("com.a", "App A", 3600000L),
        AppUsage("com.b", "App B", 7200000L),
        AppUsage("com.c", "App C", 1800000L)
    )

    @Test
    fun `returns sorted list when permission granted`() = runTest {
        every { repository.hasUsageStatsPermission() } returns true
        coEvery { repository.getAppUsageStats(any()) } returns apps

        val result = useCase()
        assertEquals("com.b", result[0].packageName)
        assertEquals("com.a", result[1].packageName)
        assertEquals("com.c", result[2].packageName)
    }

    @Test
    fun `returns empty list when permission not granted`() = runTest {
        every { repository.hasUsageStatsPermission() } returns false

        val result = useCase()
        assertTrue(result.isEmpty())
    }
}
