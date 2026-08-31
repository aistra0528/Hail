package com.aistra.hail.utils

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ApplicationInfo
import com.aistra.hail.HailApp
import com.aistra.hail.app.AppManager
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

class AppActionsTest {

    @Before
    fun setUp() {
        val mockApp = mockk<HailApp>(relaxed = true)
        every { mockApp.packageManager } returns mockk(relaxed = true)
        HailApp.setAppForTest(mockApp)

        mockkObject(HailData)
        mockkObject(AppManager)
        mockkObject(HPackages)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `ensureUnfrozen returns failure when package not found`() = runTest {
        every { AppManager.isAppFrozen("missing") } returns false
        every { HPackages.getApplicationInfoOrNull("missing") } returns null

        val result = AppActions.ensureUnfrozen("missing")

        assertTrue(result.isFailure)
    }

    @Test
    fun `ensureUnfrozen returns failure when unfreeze fails`() = runTest {
        val pkg = "com.example.test"
        every { HPackages.getApplicationInfoOrNull(pkg) } returns mockk<ApplicationInfo>()
        every { AppManager.isAppFrozen(pkg) } returns true
        coEvery { AppManager.setAppFrozen(pkg, false) } returns false

        val result = AppActions.ensureUnfrozen(pkg)

        assertTrue(result.isFailure)
    }

    @Test
    fun `ensureUnfrozen returns success when already unfrozen`() = runTest {
        val pkg = "com.example.test"
        every { HPackages.getApplicationInfoOrNull(pkg) } returns mockk<ApplicationInfo>()
        every { AppManager.isAppFrozen(pkg) } returns false

        val result = AppActions.ensureUnfrozen(pkg)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `freezePackages returns failure when app unavailable`() = runTest {
        every { HPackages.getApplicationInfoOrNull("missing") } returns null

        val result = AppActions.freezePackages(true, listOf("missing"))

        assertTrue(result.isFailure)
    }

    @Test
    fun `freezePackages returns failure when freeze fails`() = runTest {
        val pkg = "com.example.test"
        every { HPackages.getApplicationInfoOrNull(pkg) } returns mockk<ApplicationInfo>()
        coEvery { AppManager.setAppFrozen(pkg, true) } returns false

        val result = AppActions.freezePackages(true, listOf(pkg))

        assertTrue(result.isFailure)
    }

    @Test
    fun `freezePackages returns success when all frozen`() = runTest {
        val pkg = "com.example.test"
        every { HPackages.getApplicationInfoOrNull(pkg) } returns mockk<ApplicationInfo>()
        coEvery { AppManager.setAppFrozen(pkg, true) } returns true

        val result = AppActions.freezePackages(true, listOf(pkg))

        assertTrue(result.isSuccess)
    }

    @Test
    fun `getLaunchIntent returns failure when no launch intent found`() = runTest {
        every { HailData.workingMode } returns HailData.MODE_DEFAULT
        every { HailApp.app.packageManager.getLaunchIntentForPackage("missing") } returns null

        val result = AppActions.getLaunchIntent("missing")

        assertTrue(result.isFailure)
    }

    @Test
    fun `getLaunchIntent returns intent when available`() = runTest {
        every { HailData.workingMode } returns HailData.MODE_DEFAULT
        val intent = mockk<Intent>(relaxed = true)
        every { HailApp.app.packageManager.getLaunchIntentForPackage("com.example.test") } returns intent
        every { HailApp.app.setAutoFreezeService() } returns Unit
        every { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) } returns intent

        val result = AppActions.getLaunchIntent("com.example.test")

        assertTrue(result.isSuccess)
        assertEquals(intent, result.getOrThrow())
    }
}
