package com.aistra.hail.backup

import com.aistra.hail.app.HailData
import com.aistra.hail.app.AppManager
import org.json.JSONArray
import org.json.JSONObject

/**
 * Validates Hail configuration backup archives before restore.
 *
 * This validator checks the backup format and Hail configuration data only.
 * It does not modify Android package state.
 *
 * validation guarantees:
 *
 * Correct backup format/version.
 * Valid working mode.
 * Valid preferences JSON.
 * Valid application structure.
 * Valid application tag references.
 * Valid tag structure.
 * Unique tag IDs.
 *
 *
 * validation will reject:
 *
 * missing package names
 * blank package names
 * duplicate package names
 * invalid boolean fields
 * invalid tag IDs
 * references to nonexistent tags
 *
 */
object HBackupValidator {

    sealed interface BackupValidationError {

        data class UnsupportedBackupFormat(
            val format: Int
        ) : BackupValidationError

        data class UnsupportedConfigurationVersion(
            val version: String
        ) : BackupValidationError

        data class UnsupportedWorkingMode(
            val workingMode: String
        ) : BackupValidationError

        data object InvalidPreferences : BackupValidationError

        data class PreferenceInvalidEntry(
            val key: String
        ) : BackupValidationError

        data class PreferenceUnsupportedType(
            val key: String,
            val type: String
        ) : BackupValidationError

        data class PreferenceMissingValue(
            val key: String
        ) : BackupValidationError

        data class PreferenceInvalidValue(
            val key: String,
            val type: String
        ) : BackupValidationError

        data class ApplicationCountMismatch(
            val expected: Int,
            val actual: Int
        ) : BackupValidationError

        data class ApplicationMissingPackage(
            val index: Int
        ) : BackupValidationError

        data class DuplicateApplicationPackage(
            val packageName: String
        ) : BackupValidationError

        data class ApplicationInvalidPinned(
            val index: Int
        ) : BackupValidationError

        data class ApplicationInvalidWhitelisted(
            val index: Int
        ) : BackupValidationError

        data class ApplicationInvalidTagId(
            val index: Int,
            val tagIndex: Int
        ) : BackupValidationError

        data class ApplicationUnknownTagId(
            val index: Int,
            val tagId: Int
        ) : BackupValidationError

        data class ApplicationInvalidLegacyTagId(
            val index: Int
        ) : BackupValidationError

        data object InvalidApplicationData : BackupValidationError

        data class TagCountMismatch(
            val expected: Int,
            val actual: Int
        ) : BackupValidationError

        data class TagIncomplete(
            val index: Int
        ) : BackupValidationError

        data class TagInvalidId(
            val index: Int
        ) : BackupValidationError

        data class DuplicateTagId(
            val tagId: Int
        ) : BackupValidationError

        data object InvalidTagData : BackupValidationError
    }
    sealed interface BackupValidationWarning {

        data class WorkingModeChangeWithFrozenApps(
            val currentMode: String,
            val backupMode: String,
            val frozenAppNames: List<String>
        ) : BackupValidationWarning
    }

    class BackupValidationException(
        val errors: List<BackupValidationError>
    ) : IllegalArgumentException()

    data class Result(
        val valid: Boolean,
        val errors: List<BackupValidationError> = emptyList(),
        val warnings: List<BackupValidationWarning> = emptyList()
    )

    fun validate(
        contents: HBackupArchive.Contents
    ): Result {
        val errors = mutableListOf<BackupValidationError>()
        val warnings = mutableListOf<BackupValidationWarning>()

        validateMetadata(contents, errors)
        validatePreferences(contents.preferences, errors)
        validateApps(
            contents.apps,
            contents.tags,
            contents.metadata.applicationCount,
            errors
        )
        validateTags(contents.tags, contents.metadata.tagCount, errors)
        validateWorkingMode(contents, warnings)

        return Result(
            valid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    private fun validateMetadata(
        contents: HBackupArchive.Contents,
        errors: MutableList<BackupValidationError>
    ) {
        val metadata = contents.metadata

        if (metadata.backupFormat != HBackupConstants.BACKUP_FORMAT) {
            errors += BackupValidationError.UnsupportedBackupFormat(
                metadata.backupFormat
            )
        }

        if (metadata.configurationVersion !=
            HBackupConstants.CONFIGURATION_VERSION
        ) {
            errors += BackupValidationError.UnsupportedConfigurationVersion(
                metadata.configurationVersion
            )
        }

        if (metadata.workingMode !in HailData.WORKING_MODE_VALUES) {
            errors += BackupValidationError.UnsupportedWorkingMode(
                metadata.workingMode
            )
        }
    }

    private fun validateWorkingMode(
        contents: HBackupArchive.Contents,
        warnings: MutableList<BackupValidationWarning>
    ) {
        val backupWorkingMode = contents.metadata.workingMode
        val currentWorkingMode = HailData.workingMode

        if (backupWorkingMode == currentWorkingMode) {
            return
        }

        val frozenApps = HailData.checkedList.filter {
            AppManager.isAppFrozen(it.packageName)
        }

        if (frozenApps.isNotEmpty()) {
            warnings += BackupValidationWarning.WorkingModeChangeWithFrozenApps(
                currentMode = currentWorkingMode,
                backupMode = backupWorkingMode,
                frozenAppNames = frozenApps.map {
                    it.name.toString()
                }
            )
        }
    }

    private fun validatePreferences(
        data: ByteArray,
        errors: MutableList<BackupValidationError>
    ) {
        runCatching {
            val preferences = JSONObject(
                data.toString(Charsets.UTF_8)
            )

            preferences.keys().forEach { key ->
                val preference = preferences.optJSONObject(key)

                if (preference == null) {
                    errors += BackupValidationError.PreferenceInvalidEntry(
                        key = key
                    )
                    return@forEach
                }

                val type = preference.optString("type", "")

                if (type.isBlank()) {
                    errors += BackupValidationError.PreferenceInvalidEntry(
                        key = key
                    )
                    return@forEach
                }

                when (type) {
                    "string",
                    "boolean",
                    "int",
                    "long",
                    "float",
                    "string_set" -> {
                        if (!preference.has("value")) {
                            errors += BackupValidationError.PreferenceMissingValue(
                                key = key
                            )
                            return@forEach
                        }

                        val value = preference.get("value")

                        val valid = when (type) {
                            "string" -> value is String
                            "boolean" -> value is Boolean
                            "int" -> value is Int
                            "long" -> value is Number
                            "float" -> value is Number
                            "string_set" -> {
                                value is JSONArray &&
                                        (0 until value.length()).all {
                                            value.get(it) is String
                                        }
                            }

                            else -> false
                        }

                        if (!valid) {
                            errors += BackupValidationError.PreferenceInvalidValue(
                                key = key,
                                type = type
                            )
                        }
                    }

                    "null" -> {
                        // No value is required.
                    }

                    else -> {
                        errors += BackupValidationError.PreferenceUnsupportedType(
                            key = key,
                            type = type
                        )
                    }
                }
            }
        }.onFailure {
            errors += BackupValidationError.InvalidPreferences
        }
    }

    private fun validateApps(
        data: ByteArray,
        tagsData: ByteArray,
        expectedCount: Int,
        errors: MutableList<BackupValidationError>
    ) {
        runCatching {
            val apps = JSONArray(data.toString(Charsets.UTF_8))
            val packageNames = mutableSetOf<String>()

            if (apps.length() != expectedCount) {
                errors += BackupValidationError.ApplicationCountMismatch(
                    expected = expectedCount,
                    actual = apps.length()
                )
            }

            val tags = JSONArray(tagsData.toString(Charsets.UTF_8))
            val validTagIds = buildSet {
                for (i in 0 until tags.length()) {
                    add(tags.getJSONObject(i).getInt("id"))
                }
            }

            for (i in 0 until apps.length()) {
                val app = apps.getJSONObject(i)

                val packageName = app.optString(
                    HailData.KEY_PACKAGE,
                    ""
                )

                if (packageName.isBlank()) {
                    errors += BackupValidationError.ApplicationMissingPackage(
                        index = i
                    )
                } else if (!packageNames.add(packageName)) {
                    errors += BackupValidationError.DuplicateApplicationPackage(
                        packageName = packageName
                    )
                }

                if (app.has("pinned") &&
                    app.get("pinned") !is Boolean
                ) {
                    errors += BackupValidationError.ApplicationInvalidPinned(
                        index = i
                    )
                }

                if (app.has("whitelisted") &&
                    app.get("whitelisted") !is Boolean
                ) {
                    errors += BackupValidationError.ApplicationInvalidWhitelisted(
                        index = i
                    )
                }

                if (app.has("tags")) {
                    val tags = app.getJSONArray("tags")

                    for (tagIndex in 0 until tags.length()) {
                        val tagId = tags.get(tagIndex)

                        if (tagId !is Int) {
                            errors += BackupValidationError.ApplicationInvalidTagId(
                                index = i,
                                tagIndex = tagIndex
                            )
                        } else if (tagId !in validTagIds) {
                            errors += BackupValidationError.ApplicationUnknownTagId(
                                index = i,
                                tagId = tagId
                            )
                        }
                    }
                } else if (app.has("tag")) {
                    val tagId = app.get("tag")

                    if (tagId !is Int) {
                        errors += BackupValidationError.ApplicationInvalidLegacyTagId(
                            index = i
                        )
                    } else if (tagId !in validTagIds) {
                        errors += BackupValidationError.ApplicationUnknownTagId(
                            index = i,
                            tagId = tagId
                        )
                    }
                }
            }
        }.onFailure {
            errors += BackupValidationError.InvalidApplicationData
        }
    }

    private fun validateTags(
        data: ByteArray,
        expectedCount: Int,
        errors: MutableList<BackupValidationError>
    ) {
        runCatching {
            val tags = JSONArray(data.toString(Charsets.UTF_8))
            val tagIds = mutableSetOf<Int>()

            if (tags.length() != expectedCount) {
                errors += BackupValidationError.TagCountMismatch(
                    expected = expectedCount,
                    actual = tags.length()
                )
            }

            for (i in 0 until tags.length()) {
                val tag = tags.getJSONObject(i)

                if (!tag.has(HailData.KEY_TAG) ||
                    !tag.has("id")
                ) {
                    errors += BackupValidationError.TagIncomplete(
                        index = i
                    )
                    continue
                }

                val tagId = tag.get("id")

                if (tagId !is Int) {
                    errors += BackupValidationError.TagInvalidId(
                        index = i
                    )
                } else if (!tagIds.add(tagId)) {
                    errors += BackupValidationError.DuplicateTagId(
                        tagId = tagId
                    )
                }
            }
        }.onFailure {
            errors += BackupValidationError.InvalidTagData
        }
    }
}