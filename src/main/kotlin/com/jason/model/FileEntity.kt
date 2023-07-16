package com.jason.model

import com.jason.database.DatabaseFactory
import com.jason.utils.ListSort
import com.jason.utils.MediaType
import com.jason.utils.children
import kotlinx.serialization.Serializable
import java.io.File
import java.util.*

@Serializable
data class FileEntity(
    val name: String,
    val path: String,
    val hash: String,
    val size: Long,
    val date: Long,
    val isFile: Boolean,
    val isDirectory: Boolean,
    val childCount: Int,
    val firstFileHash: String,
    val firstFileType: MediaType.Media
)

suspend fun List<File>.toFileEntities(sort: ListSort = ListSort.DATE): List<FileEntity> {
    return ArrayList<FileEntity>().apply {
        this@toFileEntities.sort(sort).forEach {
            val hash = DatabaseFactory.fileHashDao.getHash(it.absolutePath)
            val children = it.children
            val first: File? = children.findFirstMedia()
            add(
                FileEntity(
                    name = it.name,
                    path = it.absolutePath,
                    hash = hash,
                    size = it.length(),
                    date = it.lastModified(),
                    isFile = it.isFile,
                    isDirectory = it.isDirectory,
                    childCount = children.size,
                    firstFileHash = if (first == null) "" else DatabaseFactory.fileHashDao.getHash(first.absolutePath),
                    firstFileType = if (first == null) MediaType.Media.UNKNOWN else MediaType.getMediaType(first.name)
                )
            )
        }
    }
}

fun List<File>.findFirstMedia(): File? {
    return if (isEmpty()) null else sortedByName().run {
        find { file ->
            MediaType.isVideo(file)
        } ?: find { file ->
            MediaType.isImage(file)
        } ?: find { file ->
            MediaType.isAudio(file)
        } ?: first()
    }
}

fun List<File>.sort(sort: ListSort): List<File> {
    return when (sort) {
        ListSort.NAME -> sortedByName()
        ListSort.SIZE -> sortedBySize()
        ListSort.DATE -> sortedByDate()
    }
}

fun List<File>.sortedByName(): List<File> {
    val array = toTypedArray()
    Arrays.sort(array, Comparator<File> { file1, file2 ->
        if (file1.isDirectory && file2.isDirectory) {  // 目录文件排在前面
            return@Comparator file1.name.compareTo(file2.name)
        } else if (file1.isDirectory) {
            return@Comparator -1
        } else if (file2.isDirectory) {
            return@Comparator 1
        } else {
            val ext1 = file1.extension
            val ext2 = file2.extension
            if (ext1 == ext2) {  // 相同类型的文件按名称排序
                return@Comparator file1.name.compareTo(file2.name)
            } else {  // 不同类型的文件按文件类型排序
                return@Comparator ext1.compareTo(ext2)
            }
        }
    })
    return array.toList()
}

fun List<File>.sortedBySize(): List<File> {
    val array = toTypedArray()
    Arrays.sort(array, Comparator<File> { file1, file2 ->
        if (file1.isDirectory && file2.isDirectory) {  // 目录文件排在前面
            return@Comparator (file2.listFiles()?.size ?: 0).compareTo(file1.listFiles()?.size ?: 0)
        } else if (file1.isDirectory) {
            return@Comparator -1
        } else if (file2.isDirectory) {
            return@Comparator 1
        } else {
            val ext1 = file1.extension
            val ext2 = file2.extension
            if (ext1 == ext2) {  // 相同类型的文件按名称排序
                return@Comparator file1.length().compareTo(file2.length())
            } else {  // 不同类型的文件按文件类型排序
                return@Comparator ext1.compareTo(ext2)
            }
        }
    })
    return array.toList()
}

fun List<File>.sortedByDate(): List<File> {
    val array = toTypedArray()
    Arrays.sort(array, Comparator<File> { file1, file2 ->
        if (file1.isDirectory && file2.isDirectory) {  // 目录文件排在前面
            return@Comparator (file2.lastModified()).compareTo(file1.lastModified())
        } else if (file1.isDirectory) {
            return@Comparator -1
        } else if (file2.isDirectory) {
            return@Comparator 1
        } else {
            val ext1 = file1.extension
            val ext2 = file2.extension
            if (ext1 == ext2) {  // 相同类型的文件按名称排序
                return@Comparator file1.lastModified().compareTo(file2.lastModified())
            } else {  // 不同类型的文件按文件类型排序
                return@Comparator ext1.compareTo(ext2)
            }
        }
    })
    return array.toList()
}