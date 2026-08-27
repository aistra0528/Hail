package com.aistra.hail.utils

import android.os.Build
import androidx.annotation.RequiresApi
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object HShell {
    init {
        Shell.setDefaultBuilder(Shell.Builder.create().setFlags(Shell.FLAG_REDIRECT_STDERR))
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var warmupJob: Job? = null

    private fun getCurrentUserId(): Int = execSU("am get-current-user").second?.trim()?.toIntOrNull() ?: 0

    private val userArg: String
        get() = "--user ${getCurrentUserId()}"

    fun start() = synchronized(this) {
        if (warmupJob?.isActive == true) return@synchronized
        Shell.getCachedShell()?.let { shell ->
            if (shell.isAlive) return@synchronized
            shell.close()
        }
        warmupJob = scope.launch {
            executeRoot(":")
            synchronized(this@HShell) {
                warmupJob = null
                if (!isActive) Shell.getCachedShell()?.close()
            }
        }
    }

    fun stop() = synchronized(this) {
        warmupJob?.cancel()
        warmupJob = null
        Shell.getCachedShell()?.close()
    }

    fun execute(command: String, root: Boolean): Pair<Int, String?> = if (root) {
        executeRoot(command)
    } else runCatching {
        ProcessBuilder("sh").redirectErrorStream(true).start().run {
            outputStream.use { it.write(command.toByteArray()) }
            waitFor() to inputStream.bufferedReader().use { it.readText() }.also { destroy() }
        }
    }.getOrElse { 1 to it.stackTraceToString() }

    private fun executeRoot(command: String): Pair<Int, String?> = synchronized(this) {
        runCatching {
            Shell.getCachedShell()?.let { shell ->
                if (!shell.isAlive) shell.close()
            }
            Shell.cmd(command).exec().let { result ->
                if (Shell.getCachedShell()?.isAlive == false) Shell.getCachedShell()?.close()
                result.code to result.out.joinToString("\n").ifBlank { null }
            }
        }.getOrElse { 1 to it.stackTraceToString() }
    }

    private fun execSU(command: String) = execute(command, true)

    val checkSU get() = execSU("whoami").first == 0

    val lockScreen get() = execSU("input keyevent KEYCODE_POWER").first == 0

    fun forceStopApp(packageName: String): Boolean = execSU("am force-stop $userArg $packageName").first == 0

    fun setAppDisabled(packageName: String, disabled: Boolean): Boolean =
        execSU("pm ${if (disabled) "disable" else "enable"} $userArg $packageName").first == 0

    fun setAppHidden(packageName: String, hidden: Boolean): Boolean =
        execSU("pm ${if (hidden) "hide" else "unhide"} $userArg $packageName").first == 0

    fun setAppSuspended(packageName: String, suspended: Boolean): Boolean =
        execSU("pm ${if (suspended) "suspend" else "unsuspend"} $userArg $packageName").first == 0

    fun uninstallApp(packageName: String) = execSU(
        "pm ${if (HPackages.canUninstallNormally(packageName)) "uninstall" else "uninstall $userArg"} $packageName"
    ).first == 0

    fun reinstallApp(packageName: String) = execSU("pm install-existing $userArg $packageName").first == 0

    @RequiresApi(Build.VERSION_CODES.P)
    fun setAppRestricted(packageName: String, restricted: Boolean) = execSU(
        "appops set $userArg $packageName RUN_ANY_IN_BACKGROUND ${if (restricted) "ignore" else "allow"}"
    ).first == 0
}