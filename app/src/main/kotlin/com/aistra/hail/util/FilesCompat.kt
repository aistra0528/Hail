package com.aistra.hail.util

import android.os.Build
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths

object FilesCompat {
    private const val CAPACITY = 1024
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
    fun readK(source: String): String? {
        try {
            return File(source).readText()
        } catch (e: Exception) {
        }
        return null
    }

    fun read(source: String): String? {
        var string: String?
        var inputChannel: FileChannel? = null
        try {
            inputChannel = FileInputStream(source).channel
            val byteBuffer = ByteBuffer.allocate(CAPACITY)
            val charBuffer = CharBuffer.allocate(CAPACITY)
            val decoder = Charset.forName(Charsets.UTF_8.name()).newDecoder()
            val builder = StringBuilder()
            while (inputChannel.read(byteBuffer) >= 0) {
                byteBuffer.flip()
                decoder.decode(byteBuffer, charBuffer, false)
                charBuffer.flip()
                while (charBuffer.hasRemaining()) {
                    builder.append(charBuffer.get())
                }
                byteBuffer.clear()
                charBuffer.clear()
            }
            string = builder.toString()
        } catch (e: Exception) {
            string = null
        }
        inputChannel?.close()
        return string
    }

    fun writeK(text: String, target: String): Boolean {
        try {
            File(target).writeText(text)
            return true
        } catch (e: Exception) {
        }
        return false
    }

    fun write(text: String, target: String): Boolean {
        var ok = false
        var outputChannel: FileChannel? = null
        try {
            outputChannel = FileOutputStream(target).channel
            text.toByteArray(Charsets.UTF_8).let {
                outputChannel.write(ByteBuffer.wrap(it).apply {
                    put(it)
                    flip()
                })
            }
            ok = true
        } catch (e: Exception) {
        }
        outputChannel?.close()
        return ok
    }
}