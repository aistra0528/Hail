package com.aistra.hail.backup

/**
 * Constants used by the Hail configuration backup format.
 */
object HBackupConstants {
    const val BACKUP_FORMAT = 1
    const val CONFIGURATION_VERSION = "v1"

    const val FILE_EXTENSION = ".hail"

    const val MANIFEST_FILE = "manifest.json"

    const val PREFERENCES_FILE = "preferences.json"

    const val CONFIGURATION_DIRECTORY = "v1"
    const val APPS_FILE = "$CONFIGURATION_DIRECTORY/apps.json"
    const val TAGS_FILE = "$CONFIGURATION_DIRECTORY/tags.json"

    const val MAX_PREFERENCES_SIZE = 1 * 1024 * 1024
    const val MAX_APPS_SIZE = 10 * 1024 * 1024
    const val MAX_TAGS_SIZE = 1 * 1024 * 1024
    const val MAX_MANIFEST_SIZE = 64 * 1024
}