package com.aistra.hail.utils

object HShell {
    private fun execute(command: String, root: Boolean): Boolean = runCatching {
        Runtime.getRuntime().exec(if (root) "su" else "sh").run {
            outputStream.use {
                it.write(command.toByteArray())
            }
            (waitFor() == 0).also {
                destroy()
            }
        }
    }.getOrDefault(false)

    private fun execSU(command: String) = execute(command, true)

    val checkSU get() = execSU("whoami")

    val lockScreen get() = execSU("input keyevent KEYCODE_POWER")

    fun setAppDisabled(packageName: String, disabled: Boolean): Boolean =
        execSU("pm ${if (disabled) "disable" else "enable"} $packageName")

    fun setAppSuspended(packageName: String, suspended: Boolean): Boolean =
        execSU("pm ${if (suspended) "suspend" else "unsuspend"} $packageName")

    fun uninstallApp(packageName: String) = execSU(
        "pm ${if (HPackages.canUninstallNormally(packageName)) "uninstall" else "uninstall --user current"} $packageName"
    )
}