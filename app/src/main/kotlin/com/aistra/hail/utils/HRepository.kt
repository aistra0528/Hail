package com.aistra.hail.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object HRepository {
    suspend fun request(url: String): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            (URL(url).openConnection() as HttpURLConnection).inputStream.bufferedReader()
                .use { it.readText() }
        }
    }
}