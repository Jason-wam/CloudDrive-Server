package com.jason.utils.extension

import com.jason.model.FileNavigationEntity
import com.jason.model.findFirstMedia
import com.jason.utils.Configure
import com.jason.utils.MediaType
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
        return listFiles()?.toList().orEmpty()
    }

/**
 * 递归枚举目录下全部文件
 */
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

/**
 * 校验整个文件的MD5
 */
fun File.createMD5String(block: ((bytesRead: Long) -> Unit)? = null): String {
    return inputStream().use { stream ->
        stream.createMD5String(block)
    }
}

fun InputStream.createMD5String(block: ((bytesRead: Long) -> Unit)? = null): String {
    val buffer = ByteArray(8 * 1024)
    var bytesRead: Int
    var totalBytesRead: Long = 0
    val messageDigest = MessageDigest.getInstance("MD5")

    while (read(buffer).also { bytesRead = it } != -1) {
        messageDigest.update(buffer)
        totalBytesRead += bytesRead
        block?.invoke(totalBytesRead)
    }

    val digest = messageDigest.digest()
    val checksum = BigInteger(1, digest).toString(16)
    return checksum.padStart(32, '0')
}

/**
 * 因为大文件校验过慢，所以可以选择读取文件开头和结尾
 */
fun File.createSketchedMD5String(blockSize: Long = 2.MB): String {
    return inputStream().use {
        it.createSketchedMD5String(length(), blockSize)
    }
}

fun InputStream.createSketchedMD5String(fileLength: Long, blockSize: Long = 2.MB): String {
    if (blockSize >= fileLength) return createMD5String()
    val messageDigest = MessageDigest.getInstance("MD5")
    var readPoint = readBlock(blockSize) { buffer ->
        messageDigest.update(buffer)
    }

    var nextStart = fileLength / 2 - blockSize / 2
    if (nextStart > 0) {
        val skipOffset = nextStart - readPoint
        skip(skipOffset)
        readPoint += skipOffset + readBlock(blockSize) { buffer ->
            messageDigest.update(buffer)
        }
    }

    nextStart = fileLength - blockSize
    if (nextStart > 0) {
        val skipOffset = nextStart - readPoint
        skip(skipOffset)
        readBlock(blockSize) { buffer ->
            messageDigest.update(buffer)
        }
    }
    val digest = messageDigest.digest()
    val checksum = BigInteger(1, digest).toString(16)
    return checksum.padStart(32, '0')
}

inline fun InputStream.readBlock(blockSize: Long, block: (buffer: ByteArray) -> Unit): Long {
    var bytesRead: Int
    var totalBytesRead: Long = 0
    val buffer = ByteArray(4096)
    while (read(buffer).also { bytesRead = it } != -1) {
        totalBytesRead += bytesRead
        block.invoke(buffer)
        if (totalBytesRead >= blockSize) {
            break
        }
    }
    return totalBytesRead
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

fun File.toNavigation(): ArrayList<FileNavigationEntity> {
    val rootDir = Configure.rootDir
    fun File.listNavigation2(): ArrayList<FileNavigationEntity> {
        return ArrayList<FileNavigationEntity>().apply {
            this@listNavigation2.parentFile?.let {
                if (it.absolutePath.startsWith(rootDir.absolutePath)) {
                    if (it.absolutePath != rootDir.absolutePath) {
                        addAll(it.listNavigation2())
                        add(FileNavigationEntity(it.name, it.absolutePath.toMd5String()))
                    } else {
                        addAll(it.listNavigation2())
                        add(FileNavigationEntity(it.absolutePath, it.absolutePath.toMd5String()))
                    }
                }
            }
        }
    }

    return ArrayList<FileNavigationEntity>().apply {
        addAll(listNavigation2())
        val current = this@toNavigation
        if (current.absolutePath != rootDir.absolutePath) {
            add(FileNavigationEntity(current.name, current.absolutePath.toMd5String()))
        } else {
            add(FileNavigationEntity(current.absolutePath, current.absolutePath.toMd5String()))
        }
    }
}

suspend fun File.createGif(size: Int = -1): File? = withContext(Dispatchers.IO) {
    if (isFile.not()) {
        LoggerFactory.getLogger("Thumbnail").error("create Gif failed, not a valid file >> $absolutePath")
        return@withContext null
    }
    if (exists().not()) {
        LoggerFactory.getLogger("Thumbnail").error("createThumbnail failed, file not exist >> $absolutePath")
        return@withContext null
    }

    val imageSize = if (size > 0) size else 720
    if (name.endsWith(".gif", true)) {
        LoggerFactory.getLogger("Thumbnail").info("return original gif >> $absolutePath")
        return@withContext this@createGif
    } else {
        if (MediaType.isVideo(this@createGif).not()) {
            return@withContext null
        } else {
            val image = File(Configure.thumbDir, "${path.toMd5String()}_x$imageSize.gif")
            return@withContext if (image.exists()) image else {
                LoggerFactory.getLogger("Thumbnail").info("create gif from video >> $absolutePath")
                val succeed = Encoder(Configure.ffmpeg).input(this@createGif).fps(10).t(10f)
                    .resize(imageSize)
                    .threads(3)
                    .startAtHalfDuration(true).execute(image)
                if (succeed) image else null
            }
        }
    }
}

suspend fun File.createThumbnail(size: Int = -1): File? = withContext(Dispatchers.IO) {
    val imageSize = if (size > 0) size else 320
    if (isDirectory) {
        LoggerFactory.getLogger("Thumbnail").info("createThumbnail from children >> $absolutePath")

        val mediaFile = children.findFirstMedia()
        return@withContext if (mediaFile != null) {
            mediaFile.createThumbnail(imageSize)
        } else { //如果不存在视频或图片则尝试从音频中读取专辑封面
            children.sortedByDescending<File, Long> {
                it.lastModified()
            }.find<File> { file ->
                MediaType.isAudio(file)
            }?.createThumbnail(imageSize)
        }
    }

    val input = this@createThumbnail
    if (name.endsWith(".gif", true)) {
        if (length() < 200.KB) {
            return@withContext input
        }
        val image = File(Configure.thumbDir, "${absolutePath.toMd5String()}.gif")
        return@withContext if (image.exists()) image else {
            LoggerFactory.getLogger("Thumbnail").info("createThumbnail from gif >> $absolutePath")
            val succeed = Encoder(Configure.ffmpeg).input(input).resize(imageSize)
                .execute(image)
            if (succeed) image else input
        }
    } else {
        if (name.endsWith(".svg", true)) {
            return@withContext input
        }
        if (name.endsWith(".webp", true)) {
            return@withContext input
        }

        if (MediaType.isVideo(input)) {
            val image = File(Configure.thumbDir, "${absolutePath.toMd5String()}.jpg")
            return@withContext if (image.exists()) image else {
                LoggerFactory.getLogger("Thumbnail").info("createThumbnail from video >> $absolutePath")
                val succeed = Encoder(Configure.ffmpeg).input(input).param("-frames 1").resize(imageSize)
                    .format("mjpeg").startAtHalfDuration(true).execute(image)
                if (succeed) image else null
            }
        } else if (MediaType.isImage(input)) {
            if (length() < 200.KB) {
                return@withContext input
            }
            val image = File(Configure.thumbDir, "${absolutePath.toMd5String()}.jpg")
            return@withContext if (image.exists()) image else {
                LoggerFactory.getLogger("Thumbnail").info("createThumbnail from image >> $absolutePath")
                val succeed = Encoder(Configure.ffmpeg).input(input).resize(imageSize).execute(image)
                return@withContext if (succeed) image else input
            }
        } else if (MediaType.isAudio(input)) {
            val image = File(Configure.thumbDir, "${absolutePath.toMd5String()}.jpg")
            return@withContext if (image.exists()) image else {
                LoggerFactory.getLogger("Thumbnail").info("createThumbnail from audio >> $absolutePath")
                val succeed = Encoder(Configure.ffmpeg).input(input).resize(imageSize).execute(image)
                if (succeed) image else null
            }
        } else {
            LoggerFactory.getLogger("Thumbnail").error("unsupported media type >> $absolutePath")
            return@withContext null
        }
    }
}