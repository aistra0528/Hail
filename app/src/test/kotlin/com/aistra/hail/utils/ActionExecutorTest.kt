package com.aistra.hail.utils

import android.content.ActivityNotFoundException
import android.content.Intent
import com.aistra.hail.HailApp
import com.aistra.hail.app.HailData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ActionExecutorTest {
    @Before
    fun setUp() {
        val mockApp = mockk<HailApp>(relaxed = true)
        every { mockApp.packageManager } returns mockk(relaxed = true)
        HailApp.setAppForTest(mockApp)

        mockkObject(HailData)
        mockkObject(AppActions)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `prepare returns failure when unfreeze fails`() = runTest {
        val action = LaunchAction(
            id = "test",
            launchPackage = "com.example.launch",
            unfreezePackages = listOf("com.example.dep")
        )
        coEvery { AppActions.ensureUnfrozen("com.example.dep") } returns Result.failure(
            IllegalStateException("Could not unfreeze")
        )

        val result = ActionExecutor.prepare(action)

        assertTrue(result.isFailure)
    }

    @Test
    fun `prepare returns failure when launch intent fails`() = runTest {
        val action = LaunchAction(
            id = "test",
            launchPackage = "com.example.launch",
            unfreezePackages = emptyList()
        )
        coEvery { AppActions.ensureUnfrozen(any()) } returns Result.success(Unit)
        coEvery { AppActions.getLaunchIntent("com.example.launch") } returns Result.failure(
            ActivityNotFoundException("Launch activity not found")
        )

        val result = ActionExecutor.prepare(action)

        assertTrue(result.isFailure)
    }

    @Test
    fun `prepare returns launch intent on success`() = runTest {
        val action = LaunchAction(
            id = "test",
            launchPackage = "com.example.launch",
            unfreezePackages = emptyList()
        )
        val intent = Intent(Intent.ACTION_MAIN)
        coEvery { AppActions.ensureUnfrozen(any()) } returns Result.success(Unit)
        coEvery { AppActions.getLaunchIntent("com.example.launch") } returns Result.success(intent)

        val result = ActionExecutor.prepare(action)

        assertTrue(result.isSuccess)
        assertEquals(intent, result.getOrThrow())
    }
}
