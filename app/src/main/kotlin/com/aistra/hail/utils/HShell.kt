package com.aistra.hail.utils

object HShell {
    private fun execute(command: String): Boolean = try {
        Runtime.getRuntime().exec(command).waitFor() == 0
    } catch (t: Throwable) {
        false
    }

    private fun execSU(command: String) = execute("su -c $command")

    val checkSU get() = execSU("clear")

    val lockScreen get() = execSU("input keyevent KEYCODE_POWER")

    fun setAppDisabledAsUser(packageName: String, disabled: Boolean): Boolean =
        execSU("pm ${if (disabled) "disable-user" else "enable"} $packageName")

    fun uninstallApp(packageName: String) = execSU("pm uninstall $packageName")
}