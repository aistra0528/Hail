package com.aistra.hail.utils

internal fun suspendWithOptionalForceStop(
    suspended: Boolean,
    forceStopBeforeSuspend: Boolean,
    forceStop: () -> Boolean,
    suspendAction: () -> Boolean
): Boolean {
    if (suspended && forceStopBeforeSuspend) forceStop()
    return suspendAction()
}
