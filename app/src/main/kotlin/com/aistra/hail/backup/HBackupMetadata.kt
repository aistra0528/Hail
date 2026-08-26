package com.aistra.hail.backup

import android.os.Build
import org.json.JSONObject

/**
 * Metadata describing a Hail configuration backup.
 *
 * The metadata is informational and is also used during restore validation.
 * The configuration itself remains in Hail's existing persistence format.
 */
data class HBackupMetadata(
    val backupFormat: Int,
    val hailVersion: String,
    val configurationVersion: String,
    val createdUtc: String,
    val deviceName: String,
    val androidSdk: Int,
    val workingMode: String,
    val applicationCount: Int,
    val tagCount: Int
) {
    /**
     * Converts this metadata to the JSON representation stored in a backup.
     */
    fun toJson(): JSONObject = JSONObject().apply {
        put(KEY_BACKUP_FORMAT, backupFormat)
        put(KEY_HAIL_VERSION, hailVersion)
        put(KEY_CONFIGURATION_VERSION, configurationVersion)
        put(KEY_CREATED_UTC, createdUtc)
        put(KEY_DEVICE_NAME, deviceName)
        put(KEY_ANDROID_SDK, androidSdk)
        put(KEY_WORKING_MODE, workingMode)
        put(KEY_APPLICATION_COUNT, applicationCount)
        put(KEY_TAG_COUNT, tagCount)
    }

    companion object {
        private const val KEY_BACKUP_FORMAT = "backupFormat"
        private const val KEY_HAIL_VERSION = "hailVersion"
        private const val KEY_CONFIGURATION_VERSION = "configurationVersion"
        private const val KEY_CREATED_UTC = "createdUtc"
        private const val KEY_DEVICE_NAME = "deviceName"
        private const val KEY_ANDROID_SDK = "androidSdk"
        private const val KEY_WORKING_MODE = "workingMode"
        private const val KEY_APPLICATION_COUNT = "applicationCount"
        private const val KEY_TAG_COUNT = "tagCount"

        /**
         * Creates metadata from the JSON representation stored in a backup.
         */
        fun fromJson(json: JSONObject): HBackupMetadata = HBackupMetadata(
            backupFormat = json.getInt(KEY_BACKUP_FORMAT),
            hailVersion = json.getString(KEY_HAIL_VERSION),
            configurationVersion = json.getString(KEY_CONFIGURATION_VERSION),
            createdUtc = json.getString(KEY_CREATED_UTC),
            deviceName = json.getString(KEY_DEVICE_NAME),
            androidSdk = json.getInt(KEY_ANDROID_SDK),
            workingMode = json.getString(KEY_WORKING_MODE),
            applicationCount = json.getInt(KEY_APPLICATION_COUNT),
            tagCount = json.getInt(KEY_TAG_COUNT)
        )

        /**
         * Creates metadata using the current Hail and Android versions.
         */
        fun create(
            hailVersion: String,
            workingMode: String,
            applicationCount: Int,
            tagCount: Int,
            deviceName: String,
            createdUtc: String
        ): HBackupMetadata = HBackupMetadata(
            backupFormat = HBackupConstants.BACKUP_FORMAT,
            hailVersion = hailVersion,
            configurationVersion = HBackupConstants.CONFIGURATION_VERSION,
            createdUtc = createdUtc,
            deviceName = deviceName,
            androidSdk = Build.VERSION.SDK_INT,
            workingMode = workingMode,
            applicationCount = applicationCount,
            tagCount = tagCount
        )
    }
}