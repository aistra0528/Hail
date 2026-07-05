package com.aistra.hail.utils

import android.os.Build
import androidx.annotation.RequiresApi

object HShell {
    fun execute(command: String, root: Boolean): Pair<Int, String?> =
        executeShell(if (root) arrayOf("su") else arrayOf("sh"), command)

    private fun executeShell(shell: Array<String>, command: String): Pair<Int, String?> = runCatching {
        Runtime.getRuntime().exec(shell).run {
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

    fun forceStopApp(packageName: String): Boolean = execSU("am force-stop --user current $packageName").first == 0

    fun setAppDisabled(packageName: String, disabled: Boolean): Boolean =
        execSU("pm ${if (disabled) "disable" else "enable"} --user current $packageName").first == 0

    fun setAppHidden(packageName: String, hidden: Boolean): Boolean =
        execSU("pm ${if (hidden) "hide" else "unhide"} --user current $packageName").first == 0

    fun setAppSuspended(packageName: String, suspended: Boolean): Boolean =
        execSU("pm ${if (suspended) "suspend" else "unsuspend"} --user current $packageName").first == 0

    // Hail runs in an isolated mount namespace where other apps' /data/data is
    // masked; Magisk su inherits it, so a plain-su rm no-ops. --mount-master
    // enters the global namespace where the cache dirs are visible.
    fun clearAppCache(packageName: String): Boolean = executeShell(
        arrayOf("su", "--mount-master"),
        "rm -rf /data/data/$packageName/cache/* /data/data/$packageName/code_cache/* " +
            "/data/media/0/Android/data/$packageName/cache/*"
    ).first == 0

    fun uninstallApp(packageName: String) = execSU(
        "pm ${if (HPackages.canUninstallNormally(packageName)) "uninstall" else "uninstall --user current"} $packageName"
    ).first == 0

    fun reinstallApp(packageName: String) = execSU("pm install-existing --user current $packageName").first == 0

    @RequiresApi(Build.VERSION_CODES.P)
    fun setAppRestricted(packageName: String, restricted: Boolean) = execSU(
        "appops set --user current $packageName RUN_ANY_IN_BACKGROUND ${if (restricted) "ignore" else "allow"}"
    ).first == 0
}