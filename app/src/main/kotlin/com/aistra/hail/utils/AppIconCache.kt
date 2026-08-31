package com.aistra.hail.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.widget.ImageView
import androidx.collection.LruCache
import com.aistra.hail.R
import com.aistra.hail.app.HailData
import kotlinx.coroutines.*
import me.zhanghai.android.appiconloader.AppIconLoader
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

/**
 * @author Rikka
 * Source
 * https://raw.githubusercontent.com/RikkaApps/Shizuku/master/manager/src/main/java/moe/shizuku/manager/utils/AppIconCache.kt
 */
object AppIconCache : CoroutineScope {

    private class AppIconLruCache constructor(maxSize: Int) :
        LruCache<Triple<String, Int, Int>, Bitmap>(maxSize) {

        override fun sizeOf(key: Triple<String, Int, Int>, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    override val coroutineContext: CoroutineContext get() = Dispatchers.Main

    private val lruCache: LruCache<Triple<String, Int, Int>, Bitmap>

    private val dispatcher: CoroutineDispatcher

    private val diskCacheDir: File by lazy {
        File(com.aistra.hail.HailApp.app.filesDir, "v1/icons").apply { mkdirs() }
    }

    private var appIconLoaders = mutableMapOf<Int, AppIconLoader>()

    private var shrinkNonAdaptiveIcons: Boolean

    private val cf by lazy { ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) }) }

    init {
        // Initialize app icon lru cache
        val maxMemory = Runtime.getRuntime().maxMemory() / 1024
        val availableCacheSize = (maxMemory / 4).toInt()
        lruCache = AppIconLruCache(availableCacheSize)

        // Initialize load icon scheduler
        val availableProcessorsCount = try {
            Runtime.getRuntime().availableProcessors()
        } catch (ignored: Exception) {
            1
        }
        val threadCount = 1.coerceAtLeast(availableProcessorsCount / 2)
        val loadIconExecutor: Executor = Executors.newFixedThreadPool(threadCount)
        dispatcher = loadIconExecutor.asCoroutineDispatcher()
        shrinkNonAdaptiveIcons = HailData.synthesizeAdaptiveIcons
    }

    private fun get(packageName: String, userId: Int, size: Int): Bitmap? {
        return lruCache[Triple(packageName, userId, size)]
    }

    private fun put(packageName: String, userId: Int, size: Int, bitmap: Bitmap) {
        if (get(packageName, userId, size) == null) {
            lruCache.put(Triple(packageName, userId, size), bitmap)
        }
    }

    private fun diskKey(info: ApplicationInfo, userId: Int, size: Int): String {
        val sourceSignature = runCatching {
            "${info.sourceDir}:${File(info.sourceDir).lastModified()}"
        }.getOrDefault(info.sourceDir ?: "")
        val value = listOf(
            info.packageName,
            userId,
            size,
            sourceSignature,
            HailData.iconPack,
            HailData.synthesizeAdaptiveIcons
        ).joinToString("|")
        return MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun diskFile(info: ApplicationInfo, userId: Int, size: Int) =
        File(diskCacheDir, "${diskKey(info, userId, size)}.png")

    private fun readDisk(info: ApplicationInfo, userId: Int, size: Int): Bitmap? = runCatching {
        diskFile(info, userId, size).takeIf { it.isFile }?.let { file ->
            BitmapFactory.decodeFile(file.path)?.takeIf { it.width == size && it.height == size }
        }
    }.getOrNull()

    private fun writeDisk(info: ApplicationInfo, userId: Int, size: Int, bitmap: Bitmap) {
        runCatching {
            if (!diskCacheDir.exists()) diskCacheDir.mkdirs()
            val target = diskFile(info, userId, size)
            val temporary = File(target.path + ".tmp")
            temporary.outputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
            check(temporary.renameTo(target))
        }
    }

    fun clear() {
        lruCache.evictAll()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { diskCacheDir.deleteRecursively() }
        }
    }

    fun prefetch(context: Context, applications: Collection<ApplicationInfo>, userId: Int = 0): Job = launch {
        withContext(dispatcher) {
            val size = context.resources.getDimensionPixelSize(R.dimen.app_icon_size)
            applications.map { info ->
                async { getOrLoadBitmap(context, info, userId, size) }
            }.awaitAll()
        }
    }

    @SuppressLint("NewApi")
    suspend fun getOrLoadBitmap(context: Context, info: ApplicationInfo, userId: Int, size: Int): Bitmap {
        val cachedBitmap = get(info.packageName, userId, size)
        if (cachedBitmap != null) {
            return cachedBitmap
        }
        readDisk(info, userId, size)?.also {
            put(info.packageName, userId, size, it)
            return it
        }
        var loader = appIconLoaders[size]
        if (loader == null || shrinkNonAdaptiveIcons != HailData.synthesizeAdaptiveIcons) {
            shrinkNonAdaptiveIcons = HailData.synthesizeAdaptiveIcons
            loader = AppIconLoader(size, shrinkNonAdaptiveIcons, context)
            appIconLoaders[size] = loader
        }
        val bitmap = IconPack.loadIcon(info.packageName) ?: loader.loadIcon(info, false)
        put(info.packageName, userId, size, bitmap)
        writeDisk(info, userId, size, bitmap)
        return bitmap
    }

    @JvmStatic
    fun loadIconBitmapAsync(
        context: Context,
        info: ApplicationInfo,
        userId: Int,
        view: ImageView,
        setColorFilter: Boolean = false
    ): Job {
        return launch {
            view.setTag(info.packageName)
            val size = view.measuredWidth.let {
                if (it > 0) it else context.resources.getDimensionPixelSize(R.dimen.app_icon_size)
            }
            if (shrinkNonAdaptiveIcons != HailData.synthesizeAdaptiveIcons) {
                lruCache.evictAll()
            } else {
                val cachedBitmap = get(info.packageName, userId, size)
                if (cachedBitmap != null) {
                    if (view.tag == info.packageName) {
                        view.setImageBitmap(cachedBitmap)
                        view.colorFilter = if (setColorFilter) cf else null
                    }
                    return@launch
                }
            }

            val bitmap = try {
                withContext(dispatcher) {
                    getOrLoadBitmap(context, info, userId, size)
                }
            } catch (e: CancellationException) {
                // do nothing if canceled
                return@launch
            } catch (e: Throwable) {
                null
            }

            if (view.tag == info.packageName) {
                if (bitmap != null) {
                    view.setImageBitmap(bitmap)
                } else {
                    view.setImageDrawable(if (HTarget.O) context.packageManager.defaultActivityIcon else null)
                }
                view.colorFilter = if (setColorFilter) cf else null
            }
        }
    }
}