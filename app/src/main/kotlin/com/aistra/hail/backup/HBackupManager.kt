package com.aistra.hail.backup

import com.aistra.hail.app.HailData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Coordinates Hail configuration backup and restore operations.
 *
 * This class provides the boundary between the backup archive format and
 * Hail's existing configuration persistence. It does not modify Android
 * package state.
 */
object HBackupManager {

    class HBackupException(
        val error: BackupManagerError
    ) : Exception()

    sealed interface BackupManagerError {

        data class UnsupportedPreferenceValue(
            val key: String
        ) : BackupManagerError

        data class UnsupportedPreferenceType(
            val key: String,
            val type: String
        ) : BackupManagerError
    }

    /**
     * Creates a backup of the current Hail configuration.
     *
     * The existing Hail persistence data is copied into the archive without
     * modifying the current configuration or Android package state.
     */
    suspend fun save(
        output: OutputStream,
        deviceName: String
    ) = withContext(Dispatchers.IO) {
        val apps = HailData.getAppsJson()
        val tags = HailData.getTagsJson()
        val preferences = preferencesToJson(HailData.getPreferences())
            .toByteArray(Charsets.UTF_8)

        val metadata = HBackupMetadata.create(
            hailVersion = HailData.VERSION,
            workingMode = HailData.workingMode,
            applicationCount = HailData.checkedList.size,
            tagCount = HailData.tags.size,
            deviceName = deviceName,
            createdUtc = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                Locale.US
            ).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())
        )

        HBackupArchive.create(
            output = output,
            metadata = metadata,
            preferences = ByteArrayInputStream(preferences),
            apps = ByteArrayInputStream(apps),
            tags = ByteArrayInputStream(tags)
        )
    }

    /**
     * Reads a backup archive.
     *
     * The archive is structurally read by HBackupArchive. No Hail state is
     * changed by this operation.
     */
    suspend fun read(
        input: InputStream
    ): HBackupArchive.Contents =
        HBackupArchive.read(input)

    /**
     * Restores configuration from an already-read backup archive.
     *
     * The caller is responsible for validation, confirmation, and reset
     * sequencing before invoking this method.
     *
     * Android package state is never modified.
     */
    fun restore(contents: HBackupArchive.Contents) {
        HailData.restoreConfiguration(
            preferences = JSONObject(
                contents.preferences.toString(Charsets.UTF_8)
            ),
            apps = contents.apps,
            tags = contents.tags
        )
    }

    /**
     * Converts SharedPreferences data to the JSON representation used by
     * HailData.restoreConfiguration().
     */
    private fun preferencesToJson(
        preferences: Map<String, *>
    ): String {
        val result = JSONObject()

        preferences.forEach { (key, value) ->
            val preference = JSONObject()

            when (value) {
                null -> {
                    preference.put("type", "null")
                }

                is String -> {
                    preference.put("type", "string")
                    preference.put("value", value)
                }

                is Boolean -> {
                    preference.put("type", "boolean")
                    preference.put("value", value)
                }

                is Int -> {
                    preference.put("type", "int")
                    preference.put("value", value)
                }

                is Long -> {
                    preference.put("type", "long")
                    preference.put("value", value)
                }

                is Float -> {
                    preference.put("type", "float")
                    preference.put("value", value.toDouble())
                }

                is Double -> {
                    preference.put("type", "float")
                    preference.put("value", value)
                }

                is Set<*> -> {
                    val values = JSONArray()

                    value.forEach {
                        if (it !is String) {
                            throw HBackupException(
                                BackupManagerError.UnsupportedPreferenceValue(key)
                            )
                        }

                        values.put(it)
                    }

                    preference.put("type", "string_set")
                    preference.put("value", values)
                }

                else -> {
                    throw HBackupException(
                        BackupManagerError.UnsupportedPreferenceType(
                            key = key,
                            type = value::class.java.name
                        )
                    )
                }
            }

            result.put(key, preference)
        }

        return result.toString()
    }
}
