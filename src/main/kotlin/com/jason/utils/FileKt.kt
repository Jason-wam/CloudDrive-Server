package com.jason.utils

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.math.BigInteger
import java.nio.file.Files
import java.security.MessageDigest


inline val File.children: List<File>
    get() {
        return listFiles()?.toList() ?: emptyList()
    }

fun File.allChildren(): List<File> {
    return ArrayList<File>().apply {
        listFiles()?.forEach {
            if (it.isDirectory) {
                addAll(it.allChildren())
                add(it)
            } else {
                add(it)
            }
        }
    }
}

fun File.createMD5String(block: ((bytesRead: Long) -> Unit)? = null): String {
    return inputStream().use { stream ->
        stream.createMD5String(block)
    }
}

fun File.createSketchedMD5String(): String {
    return inputStream().use { stream ->
        stream.createSketchedMD5String()
    }
}

fun InputStream.createMD5String(block: ((bytesRead: Long) -> Unit)? = null): String {
    return use { stream ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var bytesRead: Int
        var totalBytesRead: Long = 0
        val messageDigest = MessageDigest.getInstance("MD5")

        while (stream.read(buffer).also { bytesRead = it } != -1) {
            messageDigest.update(buffer, 0, bytesRead)
            totalBytesRead += bytesRead
            block?.invoke(totalBytesRead)
        }

        val digest = messageDigest.digest()
        val checksum = BigInteger(1, digest).toString(16)
        checksum.padStart(32, '0')
    }
}

fun InputStream.createSketchedMD5String(): String {
    return use { stream ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var bytesRead: Int
        var totalBytesRead: Long = 0
        val messageDigest = MessageDigest.getInstance("MD5")

        while (stream.read(buffer).also { bytesRead = it } != -1) {
            messageDigest.update(buffer, 0, bytesRead)
            totalBytesRead += bytesRead
            if (totalBytesRead > 2.MB) {//2MB
                break
            }
        }

        val digest = messageDigest.digest()
        val checksum = BigInteger(1, digest).toString(16)
        checksum.padStart(32, '0')
    }
}


fun String.formatPath(): String {
    return replace("\\", "\\\\")
}

fun File.symbolicPath(): String {
    if (isSymlink()) {
        return absolutePath
    }
    val symbolicDir = File(Configure.cacheDir, "symbolic").also { it.mkdirs() }
    return createSymbolicLink(symbolicDir, absolutePath.toMd5String() + ".$extension").absolutePath.formatPath()
}

fun File.createSymbolicLink(toDir: File, fileName: String, overwrite: Boolean = false): File {
    if (toDir.exists().not()) {
        toDir.mkdirs()
    }

    val link = File(toDir, fileName)
    if (Files.exists(link.toPath()) && !overwrite) {
        return link
    }

    Files.deleteIfExists(link.toPath())
    Files.createSymbolicLink(link.toPath(), toPath())
    return link
}

fun File.createSymbolicLink(toFilePath: File, overwrite: Boolean = false): File {
    if (Files.exists(toFilePath.toPath()) && !overwrite) {
        return toFilePath
    }

    Files.deleteIfExists(toFilePath.toPath())
    Files.createSymbolicLink(toFilePath.toPath(), toPath())
    return toFilePath
}

fun File.isSymlink(): Boolean {
    return Files.isSymbolicLink(toPath())
}