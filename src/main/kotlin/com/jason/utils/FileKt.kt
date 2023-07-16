package com.jason.utils

import com.jason.model.findFirstMedia
import com.jason.utils.ffmpeg.Encoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File
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
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var bytesRead: Int
    var totalBytesRead: Long = 0
    val messageDigest = MessageDigest.getInstance("MD5")

    while (read(buffer).also { bytesRead = it } != -1) {
        messageDigest.update(buffer, 0, bytesRead)
        totalBytesRead += bytesRead
        block?.invoke(totalBytesRead)
    }

    val digest = messageDigest.digest()
    val checksum = BigInteger(1, digest).toString(16)
    return checksum.padStart(32, '0')
}

fun InputStream.createSketchedMD5String(): String {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var bytesRead: Int
    var totalBytesRead: Long = 0
    val messageDigest = MessageDigest.getInstance("MD5")

    while (read(buffer).also { bytesRead = it } != -1) {
        messageDigest.update(buffer, 0, bytesRead)
        totalBytesRead += bytesRead
        if (totalBytesRead > 0.5.MB) {//2MB
            break
        }
    }

    val digest = messageDigest.digest()
    val checksum = BigInteger(1, digest).toString(16)
    return checksum.padStart(32, '0')
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


suspend fun File.createGif(): File? = withContext(Dispatchers.IO) {
    if (isFile.not()) {
        LoggerFactory.getLogger("Thumbnail").error(
            "create Gif failed, not a valid file >> $absolutePath"
        )
        return@withContext null
    }
    if (exists().not()) {
        LoggerFactory.getLogger("Thumbnail").error(
            "createThumbnail failed, file not exist >> $absolutePath"
        )
        return@withContext null
    } else {
        if (name.endsWith(".gif", true)) {
            LoggerFactory.getLogger("Thumbnail").info("return original gif >> $absolutePath")
            return@withContext this@createGif
        } else {
            if (MediaType.isVideo(this@createGif).not()) {
                return@withContext null
            } else {
                LoggerFactory.getLogger("Thumbnail").info("create gif from video >> $absolutePath")
                val image = File(Configure.thumbDir, "${path.toMd5String()}.gif")
                val succeed = Encoder(Configure.ffmpeg).input(this@createGif).fps(10).t(10f).resize(720)
                    .threads(3)
                    .startAtHalfDuration(true).execute(image)
                return@withContext if (succeed) image else null
            }
        }
    }
}

suspend fun File.createThumbnail(): File? = withContext(Dispatchers.IO) {


    if (isDirectory) {
        LoggerFactory.getLogger("Thumbnail").info(
            "createThumbnail from children >> $absolutePath"
        )
        val mediaFile = children.findFirstMedia()
        if (mediaFile != null) {
            return@withContext mediaFile.createThumbnail()
        } else { //如果不存在视频或图片则尝试从音频中读取专辑封面
            val audioFile = children.sortedByDescending { it.lastModified() }.find { file ->
                MediaType.isAudio(file)
            } ?: return@withContext null

            return@withContext audioFile.createThumbnail()
        }
    } else {
        if (name.endsWith(".gif", true)) {
            LoggerFactory.getLogger("Thumbnail").info(
                "createThumbnail from gif >> $absolutePath"
            )
            val image = File(Configure.thumbDir, "${absolutePath.toMd5String()}.gif")
            val succeed = Encoder(Configure.ffmpeg).input(this@createThumbnail).resize(320).execute(image)
            return@withContext if (succeed) image else this@createThumbnail
        } else {
            if (MediaType.isVideo(this@createThumbnail)) {
                LoggerFactory.getLogger("Thumbnail").info(
                    "createThumbnail from video >> $absolutePath"
                )
                val image = File(Configure.thumbDir, "${absolutePath.toMd5String()}.jpg")
                val succeed = Encoder(Configure.ffmpeg).input(this@createThumbnail).param("-frames 1").resize(320)
                    .format("mjpeg").startAtHalfDuration(true).execute(image)
                return@withContext if (succeed) image else null
            } else if (MediaType.isImage(this@createThumbnail)) {
                LoggerFactory.getLogger("Thumbnail").info(
                    "createThumbnail from image >> $absolutePath"
                )
                val image = File(Configure.thumbDir, "${absolutePath.toMd5String()}.jpg")
                val succeed = Encoder(Configure.ffmpeg).input(this@createThumbnail).resize(320).execute(image)
                return@withContext if (succeed) image else this@createThumbnail
            } else if (MediaType.isAudio(this@createThumbnail)) {
                LoggerFactory.getLogger("Thumbnail").info(
                    "createThumbnail from audio >> $absolutePath"
                )
                val image = File(Configure.thumbDir, "${absolutePath.toMd5String()}.jpg")
                val succeed = Encoder(Configure.ffmpeg).input(this@createThumbnail).resize(320).execute(image)
                return@withContext if (succeed) image else this@createThumbnail
            } else {
                LoggerFactory.getLogger("Thumbnail").info(
                    "unsupported media type >> $absolutePath"
                )
                return@withContext null
            }
        }
    }
}