package com.aistra.hail.utils

import android.os.Build
import androidx.annotation.RequiresApi

object HShell {
    fun execute(command: String, root: Boolean): Pair<Int, String?> = runCatching {
        Runtime.getRuntime().exec(if (root) "su" else "sh").run {
            outputStream.use {
                it.write(command.toByteArray())
            }
            waitFor() to (if (inputStream.available() > 0) inputStream else errorStream).use {
                it.bufferedReader().readText()
            }.also { destroy() }
        }
    }.getOrElse { 1 to it.stackTraceToString() }

    private fun execSU(command: String) = execute(command, true)

    val checkSU get() = execSU("whoami").first == 0

    val lockScreen get() = execSU("input keyevent KEYCODE_POWER").first == 0

    fun forceStopApp(packageName: String, userId: Int = HPackages.myUserId): Boolean =
        execSU("am force-stop --user $userId $packageName").first == 0

    fun setAppDisabled(packageName: String, disabled: Boolean, userId: Int = HPackages.myUserId): Boolean =
        execSU("pm ${if (disabled) "disable" else "enable"} --user $userId $packageName").first == 0

    fun setAppHidden(packageName: String, hidden: Boolean, userId: Int = HPackages.myUserId): Boolean =
        execSU("pm ${if (hidden) "hide" else "unhide"} --user $userId $packageName").first == 0

    fun setAppSuspended(packageName: String, suspended: Boolean, userId: Int = HPackages.myUserId): Boolean =
        execSU("pm ${if (suspended) "suspend" else "unsuspend"} --user $userId $packageName").first == 0

    fun uninstallApp(packageName: String, userId: Int = HPackages.myUserId) = execSU(
        "pm ${if (HPackages.canUninstallNormally(packageName, userId)) "uninstall" else "uninstall --user $userId"} $packageName"
    ).first == 0

    fun reinstallApp(packageName: String, userId: Int = HPackages.myUserId) =
        execSU("pm install-existing --user $userId $packageName").first == 0

    @RequiresApi(Build.VERSION_CODES.P)
    fun setAppRestricted(packageName: String, restricted: Boolean, userId: Int = HPackages.myUserId) = execSU(
        "appops set --user $userId $packageName RUN_ANY_IN_BACKGROUND ${if (restricted) "ignore" else "allow"}"
    ).first == 0
}
