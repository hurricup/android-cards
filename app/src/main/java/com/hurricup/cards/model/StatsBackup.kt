package com.hurricup.cards.model

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object StatsBackup {
    /** Zips the given subdirectories of [filesDir], preserving their relative paths in the archive. */
    fun zip(filesDir: File, dirs: List<String>, output: OutputStream) {
        ZipOutputStream(output).use { zos ->
            for (dir in dirs) {
                File(filesDir, dir).listFiles()?.forEach { file ->
                    zos.putNextEntry(ZipEntry("$dir/${file.name}"))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
    }

    /** Restores an archive (entries carry their relative paths) under [filesDir]. */
    fun unzip(input: InputStream, filesDir: File) {
        ZipInputStream(input).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val file = File(filesDir, entry.name)
                    file.parentFile?.mkdirs()
                    file.outputStream().use { zis.copyTo(it) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
