package com.aistra.hail.utils

import android.os.Build
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Paths

object Files {
    fun copy(source: String, target: String): Boolean {
        var ok = false
        var inputChannel: FileChannel? = null
        var outputChannel: FileChannel? = null
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                inputChannel = FileInputStream(source).channel
                outputChannel = FileOutputStream(target).channel
                outputChannel.transferFrom(inputChannel, 0, inputChannel.size())
            } else Files.copy(Paths.get(source), Paths.get(target))
            ok = true
        } catch (e: Exception) {
        }
        inputChannel?.close()
        outputChannel?.close()
        return ok
    }

    /**
     * This method is not recommended on huge files. It has an internal limitation of 2 GB file size.
     */
    fun read(source: String): String? {
        try {
            return File(source).readText()
        } catch (e: Exception) {
        }
        return null
    }

    fun write(target: String, text: String): Boolean {
        try {
            File(target).writeText(text)
            return true
        } catch (e: Exception) {
        }
        return false
    }
}