package com.aistra.hail.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object HRepository {
    suspend fun request(url: String): Any = withContext(Dispatchers.IO) {
        try {
            (URL(url).openConnection() as HttpURLConnection).inputStream.bufferedReader()
                .use { it.readText() }
        } catch (exception: Exception) {
            exception
        }
    }
}