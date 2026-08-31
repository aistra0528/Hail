package com.aistra.hail.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.copyTo as ktCopyTo

object HFiles {
    fun exists(path: String): Boolean = when {
        HTarget.O -> Files.exists(Paths.get(path))
        else -> File(path).exists()
    }

    fun createDirectories(dir: String): Boolean = when {
        HTarget.O -> Files.createDirectories(Paths.get(dir)).exists()
        else -> File(dir).mkdirs()
    }

    suspend fun copy(source: InputStream, target: OutputStream) = withContext(Dispatchers.IO) {
        source.ktCopyTo(target)
    }

    /**
     * This method is not recommended on huge files. It has an internal limitation of 2 GB file size.
     */
    fun read(source: String): String? = runCatching {
        File(source).readText()
    }.getOrNull()

    fun write(target: String, text: String): Boolean = runCatching {
        File(target).writeText(text)
        true
    }.getOrDefault(false)
}