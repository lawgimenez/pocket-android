package com.pocket.util.java

import android.util.Log
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object PktFileUtils {
    @JvmStatic
    @Suppress("MagicNumber")
    fun unzip(path: String): Boolean {
        val zipInputStream: ZipInputStream
        return try {
            zipInputStream = ZipInputStream(BufferedInputStream(FileInputStream(path)))
            lateinit var zipEntry: ZipEntry
            val buffer = ByteArray(1024)
            var count: Int
            // directory path will probably be something like /data/user/0/com.ideashower.readitlater.pro/
            val directoryPath = path.substring(
                startIndex = 0,
                endIndex = path.lastIndexOf("/") + 1
            )
            // canonical directory path will probably be something like /data/data/com.ideashower.readitlater.pro/
            val canonicalDirectoryPath = File(directoryPath).canonicalPath
            // Look at each file entry in the zip
            while (zipInputStream.nextEntry?.also { zipEntry = it } != null) {
                val entryFullPath = directoryPath + zipEntry.name

                // check for zip path traversal attack
                // https://support.google.com/faqs/answer/9294009
                val canonicalPath = File(directoryPath, zipEntry.name).canonicalPath
                // converting to a canonical path will likely change /data/user/0/ to /data/data/
                // but either one should be fine
                if (!canonicalPath.startsWith(directoryPath) && !canonicalPath.startsWith(canonicalDirectoryPath)) {
                    // we're under attack!  don't unzip this!
                    throw SecurityException("zip path traversal attack")
                }

                // if the entry is a directory, create the directory
                if (zipEntry.isDirectory) {
                    File(entryFullPath).mkdirs()
                    continue
                }

                // write the entry to a file
                val fileOutputStream = FileOutputStream(entryFullPath)
                while (zipInputStream.read(buffer).also { count = it } != -1) {
                    fileOutputStream.write(buffer, 0, count)
                }
                fileOutputStream.close()
                zipInputStream.closeEntry()
            }
            zipInputStream.close()
            true
        } catch (e: Exception) {
            Log.e(PktFileUtils::class.simpleName, e.message ?: "unzip failed")
            false
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun createFile(path: String): File {
        return createFile(File(path), true)
    }

    /**
     * Creates the parent directory of this file if needed.
     *
     *
     * Note: Why not just use commons io? Well, this was copied over from an original
     * offline downloading class called RilFileWriter
     * which had this weird retrying attempt at creating the structure. The reason why wasn't documented,
     * but likely was a workaround to a strange issue seen on Android where [File.mkdirs] fails for short
     * periods of time, possibly related to fsync issues or something. During refactoring of offline downloading
     * this method was moved here. The offline downloading will still use it until we can prove we don't need
     * this hacky workaround.
     *
     * @param file The file to ready
     * @param createEmptyFile true to also create an empty file, false to only create the parent directory structure.
     * @return A file representing the path.
     * @throws IOException
     */
    @JvmStatic
    @Throws(IOException::class)
    @Suppress("MagicNumber", "NestedBlockDepth")
    fun createFile(file: File, createEmptyFile: Boolean): File {
        val directory = file.parentFile
        var directoryReady = false
        var mkdirAttempts = 4
        if (directory?.exists() == false) {
            while (!directoryReady && mkdirAttempts > 0) {
                directoryReady = directory.mkdirs()
                if (!directoryReady) {
                    mkdirAttempts--
                    try {
                        Thread.sleep(250)
                    } catch (ignore: InterruptedException) {
                        // TODO add some reporting to see if this workaround is still needed
                        Logs.printStackTrace(ignore)
                    }
                }
            }
        }
        if (createEmptyFile) file.createNewFile()
        return file
    }
}