package com.hurricup.cards.model

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object StatsBackup {
    fun zip(statsDir: File, output: OutputStream) {
        ZipOutputStream(output).use { zos ->
            statsDir.listFiles()?.forEach { file ->
                zos.putNextEntry(ZipEntry(file.name))
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
    }

    fun unzip(input: InputStream, statsDir: File) {
        statsDir.mkdirs()
        ZipInputStream(input).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val file = File(statsDir, entry.name)
                file.outputStream().use { zis.copyTo(it) }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
