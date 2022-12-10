package com.aistra.hail.utils

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists

object HFiles {
    const val DIR_OUTPUT = "/storage/emulated/0/Download"

    fun exists(path: String): Boolean = when {
        HTarget.O -> Files.exists(Paths.get(path))
        else -> File(path).exists()
    }

    fun createDirectories(dir: String): Boolean = when {
        HTarget.O -> Files.createDirectories(Paths.get(dir)).exists()
        else -> File(dir).mkdirs()
    }

    fun copy(source: String, target: String): Boolean = try {
        when {
            HTarget.O -> Files.copy(
                Paths.get(source), Paths.get(target), StandardCopyOption.REPLACE_EXISTING
            )
            else -> FileInputStream(source).channel.use {
                FileOutputStream(target).channel.use { out ->
                    out.transferFrom(it, 0, it.size())
                }
            }
        }
        true
    } catch (t: Throwable) {
        false
    }

    /**
     * This method is not recommended on huge files. It has an internal limitation of 2 GB file size.
     */
    fun read(source: String): String? = try {
        File(source).readText()
    } catch (t: Throwable) {
        null
    }

    fun write(target: String, text: String): Boolean = try {
        File(target).writeText(text)
        true
    } catch (t: Throwable) {
        false
    }
}