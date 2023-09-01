package com.aistra.hail.utils

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
    }.getOrElse { 0 to it.stackTraceToString() }

    private fun execSU(command: String) = execute(command, true)

    val checkSU get() = execSU("whoami").first == 0

    val lockScreen get() = execSU("input keyevent KEYCODE_POWER").first == 0

    fun setAppDisabled(packageName: String, disabled: Boolean): Boolean =
        execSU("pm ${if (disabled) "disable" else "enable"} $packageName").first == 0

    fun setAppHidden(packageName: String, hidden: Boolean): Boolean =
        execSU("pm ${if (hidden) "hide" else "unhide"} $packageName").first == 0

    fun setAppSuspended(packageName: String, suspended: Boolean): Boolean =
        execSU("pm ${if (suspended) "suspend" else "unsuspend"} $packageName").first == 0

    fun uninstallApp(packageName: String) = execSU(
        "pm ${if (HPackages.canUninstallNormally(packageName)) "uninstall" else "uninstall --user current"} $packageName"
    ).first == 0
}