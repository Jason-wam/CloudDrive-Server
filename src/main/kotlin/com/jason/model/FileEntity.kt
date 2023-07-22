package com.jason.model

import com.jason.database.DatabaseFactory
import com.jason.utils.FileType
import com.jason.utils.ListSort
import com.jason.utils.extension.children
import kotlinx.serialization.Serializable
import java.io.File

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
    val firstFileType: FileType.Media,
    val isVirtual: Boolean
)

suspend fun List<File>.toFileEntities(parentPath: String, sort: ListSort = ListSort.DATE): List<FileEntity> {
    val hashMap = DatabaseFactory.fileHashDao.getHashMapByParent(parentPath)
    return ArrayList<FileEntity>().apply {
        this@toFileEntities.sort(sort).forEach { file ->
            val hash = hashMap[file.absolutePath].orEmpty()
            if (hash.isNotBlank()) { //如果没有文件索引则忽略该文件
                val children = if (file.isDirectory) file.children else null
                val first: File? = children?.findFirstMedia()
                val firstFileHash = if (first == null) "" else DatabaseFactory.fileHashDao.getHash(first.absolutePath)
                val firstFileType = if (first == null) FileType.Media.UNKNOWN else FileType.getMediaType(first.name)

                add(
                    FileEntity(
                        name = file.name,
                        path = file.absolutePath,
                        hash = hash,
                        size = file.length(),
                        date = file.lastModified(),
                        isFile = file.isFile,
                        isDirectory = file.isDirectory,
                        childCount = children?.size ?: 0,
                        firstFileHash = firstFileHash,
                        firstFileType = firstFileType,
                        false
                    )
                )
            }
        }
    }
}

fun List<File>?.findFirstMedia(): File? {
    this ?: return null
    return if (isEmpty()) null else find { file ->
        FileType.isVideo(file)
    } ?: find { file ->
        FileType.isImage(file)
    } ?: find { file ->
        FileType.isAudio(file)
    } ?: first()
}

fun List<File>.sort(sort: ListSort): List<File> {
    return when (sort) {
        ListSort.NAME -> sortedByName()
        ListSort.SIZE -> sortedBySize()
        ListSort.DATE -> sortedByDate()
        ListSort.NAME_DESC -> sortedByNameDESC()
        ListSort.SIZE_DESC -> sortedBySizeDESC()
        ListSort.DATE_DESC -> sortedByDateDESC()
    }
}

fun List<File>.sortedByName(): List<File> {
    return sortedWith(compareByDescending<File> { it.isDirectory } // 文件夹在前
        .thenBy { it.name.findInt() + it.name.findChineseInt() }
        .thenBy { it.name }
    )
}

fun List<File>.sortedByNameDESC(): List<File> {
    return sortedWith(compareByDescending<File> { it.isDirectory } // 文件夹在前
        .thenByDescending { it.name.findInt() + it.name.findChineseInt() }
        .thenByDescending { it.name }
    )
}

fun String.findInt(): Int {
    val values = "(\\d+)".toRegex().find(this)?.groupValues
    return if ((values?.size ?: 0) > 0) values?.get(1)?.toInt() ?: 0 else 0
}

fun String.findChineseInt(): Int {
    val regex = Regex("[一二三四五六七八九十]+")
    var intNumber = 0
    val matchResult = regex.find(this) ?: return 0
    when (matchResult.value) {
        "一" -> intNumber += 1
        "二" -> intNumber += 2
        "三" -> intNumber += 3
        "四" -> intNumber += 4
        "五" -> intNumber += 5
        "六" -> intNumber += 6
        "七" -> intNumber += 7
        "八" -> intNumber += 8
        "九" -> intNumber += 9
        "十" -> intNumber += 10
    }
    return intNumber
}

fun List<File>.sortedBySize(): List<File> {
    return sortedWith(compareByDescending<File> { it.isDirectory } // 文件夹在前
        .thenBy { it.listFiles()?.size ?: 0 } // 子项数量排序
        .thenBy { if (it.isDirectory) 0 else it.length() } // 文件大小排序
        .thenBy { it.name } // 文件名排序
    )
}

fun List<File>.sortedBySizeDESC(): List<File> {
    return sortedWith(compareByDescending<File> { it.isDirectory } // 文件夹在前
        .thenByDescending { it.listFiles()?.size ?: 0 } // 子项数量排序
        .thenByDescending { if (it.isDirectory) 0 else it.length() } // 文件大小排序
        .thenBy { it.name } // 文件名排序
    )
}

fun List<File>.sortedByDate(): List<File> {
    return sortedWith(compareByDescending<File> { it.isDirectory } // 文件夹在前
        .thenByDescending { it.lastModified() } // 日期排序
        .thenByDescending { if (it.isDirectory) 0 else it.lastModified() } // 日期排序
        .thenBy { it.name } // 文件名排序
    )
}

fun List<File>.sortedByDateDESC(): List<File> {
    return sortedWith(compareByDescending<File> { it.isDirectory } // 文件夹在前
        .thenBy { it.lastModified() } // 日期排序
        .thenBy { if (it.isDirectory) 0 else it.lastModified() } // 日期排序
        .thenBy { it.name } // 文件名排序
    )
}