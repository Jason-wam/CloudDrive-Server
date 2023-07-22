package com.jason.model

import com.jason.database.DatabaseFactory
import com.jason.utils.FileType
import com.jason.utils.ListSort
import com.jason.utils.extension.children
import com.jason.utils.extension.isSymlink
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
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
    val firstFileType: FileType.Media,
    val isVirtual: Boolean
)

suspend fun List<File>.toFileEntities(parentPath: String, sort: ListSort = ListSort.DATE): List<FileEntity> {
    val hashMap = DatabaseFactory.fileHashDao.getHashMapByParent(parentPath)
    return ArrayList<FileEntity>().apply {
        this@toFileEntities.sort(sort).forEach { file ->
//            val hash = DatabaseFactory.fileHashDao.getHash(file.absolutePath) //耗时操作，有待优化
            val hash = hashMap[file.absolutePath].orEmpty()
            if (hash.isNotBlank()) { //如果没有文件索引则忽略该文件
                val children = if (file.isDirectory) file.children else null
                val first: File? = children?.findFirstMedia()
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
                        firstFileHash = if (first == null) "" else DatabaseFactory.fileHashDao.getHash(first.absolutePath),
                        firstFileType = if (first == null) FileType.Media.UNKNOWN else FileType.getMediaType(first.name),
                        false
                    )
                )
            }
        }
    }
}

fun List<File>?.findFirstMedia(): File? {
    this ?: return null
    return if (isEmpty()) null else sortedByName().run {
        find { file ->
            FileType.isVideo(file)
        } ?: find { file ->
            FileType.isImage(file)
        } ?: find { file ->
            FileType.isAudio(file)
        } ?: first()
    }
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
        .thenBy { it.name }
        .thenBy { if (it.isDirectory) 0 else it.name }
    )
}

fun List<File>.sortedByNameDESC(): List<File> {
    return sortedWith(compareByDescending<File> { it.isDirectory } // 文件夹在前
        .thenByDescending { it.name }
        .thenByDescending { if (it.isDirectory) 0 else it.name }
    )
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