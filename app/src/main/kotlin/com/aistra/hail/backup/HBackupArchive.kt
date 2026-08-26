package com.aistra.hail.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.io.ByteArrayOutputStream

/**
 * Reads and writes Hail configuration backup archives.
 *
 * This class is responsible only for the .hail archive container. It does not
 * interpret Hail configuration data or modify Hail state.
 */
object HBackupArchive {

    /**
     * Represents the configuration data contained in a backup archive.
     *
     * The streams are positioned at the beginning of their respective data.
     * The caller is responsible for closing them.
     */
    class Contents(
        val metadata: HBackupMetadata,
        val preferences: ByteArray,
        val apps: ByteArray,
        val tags: ByteArray
    )

    class BackupArchiveException(
        val error: BackupArchiveError
    ) : IllegalArgumentException()

    sealed interface BackupArchiveError {
        data class EntryTooLarge(
            val entryName: String,
            val maxSize: Int
        ) : BackupArchiveError

        data class DuplicateEntry(
            val entryName: String
        ) : BackupArchiveError

        data class MissingEntry(
            val entryName: String
        ) : BackupArchiveError

        data object InvalidMetadata : BackupArchiveError
    }

    private fun readEntry(
        zip: ZipInputStream,
        entryName: String,
        maxSize: Int
    ): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0

        while (true) {
            val count = zip.read(buffer)

            if (count == -1) {
                break
            }

            total += count

            if (total > maxSize) {
                throw BackupArchiveException(
                    BackupArchiveError.EntryTooLarge(
                        entryName = entryName,
                        maxSize = maxSize
                    )
                )
            }

            output.write(buffer, 0, count)
        }

        return output.toByteArray()
    }


    /**
     * Creates a Hail backup archive.
     *
     * The supplied configuration data is copied into the archive without
     * interpreting its contents.
     */
    suspend fun create(
        output: OutputStream,
        metadata: HBackupMetadata,
        preferences: InputStream,
        apps: InputStream,
        tags: InputStream
    ) = withContext(Dispatchers.IO) {
        ZipOutputStream(output).use { zip ->
            writeManifest(
                zip,
                metadata.toJson().toString().toByteArray(Charsets.UTF_8)
            )

            writeEntry(
                zip,
                HBackupConstants.PREFERENCES_FILE,
                preferences
            )

            writeEntry(
                zip,
                HBackupConstants.APPS_FILE,
                apps
            )

            writeEntry(
                zip,
                HBackupConstants.TAGS_FILE,
                tags
            )
        }
    }

    /**
     * Reads a complete Hail backup archive.
     *
     * The archive is validated structurally by requiring all expected entries.
     * Semantic validation of metadata belongs to the backup validator.
     */
    suspend fun read(
        input: InputStream
    ): Contents = withContext(Dispatchers.IO) {
        var metadata: HBackupMetadata? = null
        var preferences: ByteArray? = null
        var apps: ByteArray? = null
        var tags: ByteArray? = null

        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry

            while (entry != null) {
                if (!entry.isDirectory) {
                    when (entry.name) {
                        HBackupConstants.MANIFEST_FILE -> {
                            metadata = runCatching {
                                HBackupMetadata.fromJson(
                                    JSONObject(
                                        readEntry(
                                            zip,
                                            HBackupConstants.MANIFEST_FILE,
                                            HBackupConstants.MAX_MANIFEST_SIZE
                                        ).toString(Charsets.UTF_8)
                                    )
                                )
                            }.getOrElse {
                                throw BackupArchiveException(
                                    BackupArchiveError.InvalidMetadata
                                )
                            }
                        }

                        HBackupConstants.PREFERENCES_FILE -> {
                            if (preferences != null) {
                                throw BackupArchiveException(
                                    BackupArchiveError.DuplicateEntry(
                                        HBackupConstants.PREFERENCES_FILE
                                    )
                                )
                            }
                            preferences = readEntry(
                                zip,
                                HBackupConstants.PREFERENCES_FILE,
                                HBackupConstants.MAX_PREFERENCES_SIZE
                            )
                        }

                        HBackupConstants.APPS_FILE -> {
                            if (apps != null) {
                                throw BackupArchiveException(
                                    BackupArchiveError.DuplicateEntry(
                                        HBackupConstants.APPS_FILE
                                    )
                                )
                            }
                            apps = readEntry(
                                zip,
                                HBackupConstants.APPS_FILE,
                                HBackupConstants.MAX_APPS_SIZE
                            )
                        }

                        HBackupConstants.TAGS_FILE -> {
                            if (tags != null) {
                                throw BackupArchiveException(
                                    BackupArchiveError.DuplicateEntry(
                                        HBackupConstants.TAGS_FILE
                                    )
                                )
                            }
                            tags = readEntry(
                                zip,
                                HBackupConstants.TAGS_FILE,
                                HBackupConstants.MAX_TAGS_SIZE
                            )
                        }
                    }
                }

                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        Contents(
            metadata = metadata
                ?: throw BackupArchiveException(
                    BackupArchiveError.MissingEntry(
                        HBackupConstants.MANIFEST_FILE
                    )
                ),
            preferences = preferences
                ?: throw BackupArchiveException(
                    BackupArchiveError.MissingEntry(
                        HBackupConstants.PREFERENCES_FILE
                    )
                ),
            apps = apps
                ?: throw BackupArchiveException(
                    BackupArchiveError.MissingEntry(
                        HBackupConstants.APPS_FILE
                    )
                ),
            tags = tags
                ?: throw BackupArchiveException(
                    BackupArchiveError.MissingEntry(
                        HBackupConstants.TAGS_FILE
                    )
                )
        )
    }

    private fun writeManifest(
        zip: ZipOutputStream,
        data: ByteArray
    ) {
        zip.putNextEntry(
            ZipEntry(HBackupConstants.MANIFEST_FILE)
        )
        zip.write(data)
        zip.closeEntry()
    }


    private fun writeEntry(
        zip: ZipOutputStream,
        name: String,
        input: InputStream
    ) {
        zip.putNextEntry(ZipEntry(name))
        input.copyTo(zip)
        zip.closeEntry()
    }
}