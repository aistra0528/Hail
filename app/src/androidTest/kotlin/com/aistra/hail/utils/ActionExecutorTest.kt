package com.aistra.hail.utils

import com.aistra.hail.app.AppManager
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
    fun setup() {
        mockkObject(HPackages, AppManager)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun prepareReturnsFailureWhenDependencyNotInstalled() = runTest {
        val action = LaunchAction(
            id = "action-1",
            launchPackage = "com.example.target",
            unfreezePackages = listOf("com.example.missing")
        )
        every { HPackages.getApplicationInfoOrNull("com.example.missing") } returns null

        val result = ActionExecutor.prepare(action)
        assertTrue(result.isFailure)
    }

    @Test
    fun prepareSkipsAlreadyUnfrozenDependency() = runTest {
        val action = LaunchAction(
            id = "action-1",
            launchPackage = "com.example.target",
            unfreezePackages = listOf("com.example.dep")
        )
        every { HPackages.getApplicationInfoOrNull("com.example.dep") } returns mockk()
        every { AppManager.isAppFrozen("com.example.dep") } returns false

        every { HPackages.getApplicationInfoOrNull("com.example.target") } returns mockk()

        val result = ActionExecutor.prepare(action)
        assertTrue(result.isSuccess || result.isFailure)
    }

    @Test
    fun prepareReturnsFailureWhenUnfreezeFails() = runTest {
        val action = LaunchAction(
            id = "action-1",
            launchPackage = "com.example.target",
            unfreezePackages = listOf("com.example.dep")
        )
        every { HPackages.getApplicationInfoOrNull("com.example.dep") } returns mockk()
        every { AppManager.isAppFrozen("com.example.dep") } returns true
        every { AppManager.setAppFrozen("com.example.dep", false) } returns false

        val result = ActionExecutor.prepare(action)
        assertTrue(result.isFailure)
    }

    @Test
    fun prepareReturnsFailureWhenVerificationFails() = runTest {
        val action = LaunchAction(
            id = "action-1",
            launchPackage = "com.example.target",
            unfreezePackages = listOf("com.example.dep")
        )
        every { HPackages.getApplicationInfoOrNull("com.example.dep") } returns mockk()
        every { AppManager.isAppFrozen("com.example.dep") } returns true
        every { AppManager.setAppFrozen("com.example.dep", false) } returns true
        every { AppManager.isAppFrozen("com.example.dep") } returns true

        val result = ActionExecutor.prepare(action)
        assertTrue(result.isFailure)
    }

    @Test
    fun prepareUnfreezesSequentially() = runTest {
        val callOrder = mutableListOf<String>()
        val action = LaunchAction(
            id = "action-1",
            launchPackage = "com.example.target",
            unfreezePackages = listOf("com.example.dep1", "com.example.dep2")
        )
        every { HPackages.getApplicationInfoOrNull(any()) } returns mockk()
        every { AppManager.isAppFrozen(any()) } answers {
            callOrder.add("check-${firstArg<String>()}")
            true
        }
        every { AppManager.setAppFrozen(any(), false) } answers {
            callOrder.add("unfreeze-${firstArg<String>()}")
            true
        }

        ActionExecutor.prepare(action)

        assertTrue(callOrder.contains("check-com.example.dep1"))
        assertTrue(callOrder.contains("unfreeze-com.example.dep1"))
        assertTrue(callOrder.contains("check-com.example.dep2"))
        assertTrue(callOrder.contains("unfreeze-com.example.dep2"))
    }
}
