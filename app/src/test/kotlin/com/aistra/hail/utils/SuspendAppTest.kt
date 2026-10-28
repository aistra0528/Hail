package com.aistra.hail.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuspendAppTest {
    @Test
    fun forceStopsBeforeSuspendingWhenEnabled() {
        val operations = mutableListOf<String>()
        val result = suspendWithOptionalForceStop(
            suspended = true,
            forceStopBeforeSuspend = true,
            forceStop = {
                operations += "force-stop"
                false
            },
            suspendAction = {
                operations += "suspend"
                true
            }
        )

        assertEquals(listOf("force-stop", "suspend"), operations)
        assertTrue(result)
    }

    @Test
    fun skipsForceStopWhenDisabled() {
        val operations = mutableListOf<String>()
        suspendWithOptionalForceStop(
            suspended = true,
            forceStopBeforeSuspend = false,
            forceStop = {
                operations += "force-stop"
                true
            },
            suspendAction = {
                operations += "suspend"
                true
            }
        )

        assertEquals(listOf("suspend"), operations)
    }

    @Test
    fun skipsForceStopWhenUnsuspending() {
        val operations = mutableListOf<String>()
        suspendWithOptionalForceStop(
            suspended = false,
            forceStopBeforeSuspend = true,
            forceStop = {
                operations += "force-stop"
                true
            },
            suspendAction = {
                operations += "unsuspend"
                true
            }
        )

        assertEquals(listOf("unsuspend"), operations)
    }
}
