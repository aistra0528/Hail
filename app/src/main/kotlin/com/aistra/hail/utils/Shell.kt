package com.aistra.hail.utils

object Shell {
    private fun execute(command: String): Boolean {
        try {
            return Runtime.getRuntime().exec(command).waitFor() == 0
        } catch (e: Exception) {
        }
        return false
    }

    fun execSU(command: String) = execute("su -c $command")

    val checkSU: Boolean get() = execSU("clear")
}