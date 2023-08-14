package com.jason.model

import com.jason.database.DatabaseFactory
import com.jason.utils.Configure
import com.jason.utils.FileType
import com.jason.utils.ListSort
import com.jason.utils.extension.children
import com.jason.utils.extension.isSymlink
import com.jason.utils.extension.size
import com.jason.utils.extension.toMd5String
import kotlinx.serialization.Serializable
import java.io.File
import java.text.Collator

@Serializable
data class FileEntity(
    val name: String,
    val path: String,
    val hash: String,
    val parentHash: String,
    val size: Long,
    val date: Long,
    val isFile: Boolean,
    val isDirectory: Boolean,
    val childCount: Int,
    val firstFileHash: String,
    val firstFileType: FileType.Media,
    val isVirtual: Boolean
)

suspend fun List<File>.toFileEntities(parentPath: String, sort: ListSort? = null): List<FileEntity> {
    val hashMap = DatabaseFactory.fileIndexDao.getHashMapByParent(parentPath)
    return ArrayList<FileEntity>().apply {
        //根据文件夹和文件进行分组
        val group = this@toFileEntities.groupedByIsDirectory()

        //先展示文件夹
        group[true]?.sort(sort)?.forEach { file ->
            val hash = hashMap[file.absolutePath].orEmpty()
            if (hash.isNotBlank()) { //如果没有文件索引则忽略该文件
                val children = file.children
                val first: File? = children.findFirstMedia()
                val firstFileHash = if (first == null) "" else DatabaseFactory.fileIndexDao.getHash(first.absolutePath)
                val firstFileType = if (first == null) FileType.Media.UNKNOWN else FileType.getMediaType(first.name)
                add(
                    FileEntity(
                        name = file.name,
                        path = file.absolutePath,
                        hash = hash,
                        parentHash = parentPath.toMd5String(),
                        size = if (Configure.countDirSize) file.size else 0,
                        date = file.lastModified(),
                        isFile = file.isFile,
                        isDirectory = file.isDirectory,
                        childCount = children.size,
                        firstFileHash = firstFileHash,
                        firstFileType = firstFileType,
                        file.isSymlink()
                    )
                )
            }
        }

        //然后展示文件
        group[false]?.sort(sort)?.forEach { file ->
            val hash = hashMap[file.absolutePath].orEmpty()
            if (hash.isNotBlank()) { //如果没有文件索引则忽略该文件
                add(
                    FileEntity(
                        name = file.name,
                        path = file.absolutePath,
                        hash = hash,
                        parentHash = parentPath.toMd5String(),
                        size = file.length(),
                        date = file.lastModified(),
                        isFile = file.isFile,
                        isDirectory = file.isDirectory,
                        childCount = 0,
                        firstFileHash = "",
                        firstFileType = FileType.Media.UNKNOWN,
                        file.isSymlink()
                    )
                )
            }
        }
    }
}


suspend fun Map<File, String>.toFileEntitiesNoneSort(): List<FileEntity> {
    return ArrayList<FileEntity>().apply {
        this@toFileEntitiesNoneSort.forEach {
            val file = it.key
            if (file.isDirectory) {
                val children = file.children
                val first: File? = children.findFirstMedia()
                val firstFileHash = if (first == null) "" else DatabaseFactory.fileIndexDao.getHash(first.absolutePath)
                val firstFileType = if (first == null) FileType.Media.UNKNOWN else FileType.getMediaType(first.name)
                add(
                    FileEntity(
                        name = file.name,
                        path = file.absolutePath,
                        hash = it.value,
                        parentHash = file.parentFile.absolutePath.toMd5String(),
                        size = if (Configure.countDirSize) file.size else 0,
                        date = file.lastModified(),
                        isFile = false,
                        isDirectory = true,
                        childCount = children.size,
                        firstFileHash = firstFileHash,
                        firstFileType = firstFileType,
                        file.isSymlink()
                    )
                )
            } else {
                add(
                    FileEntity(
                        name = file.name,
                        path = file.absolutePath,
                        hash = it.value,
                        parentHash = file.parentFile.absolutePath.toMd5String(),
                        size = file.length(),
                        date = file.lastModified(),
                        isFile = true,
                        isDirectory = false,
                        childCount = 0,
                        firstFileHash = "",
                        firstFileType = FileType.Media.UNKNOWN,
                        file.isSymlink()
                    )
                )
            }
        }
    }
}

suspend fun Map<File, String>.toFileEntities(sort: ListSort? = null): List<FileEntity> {
    val map = this
    return ArrayList<FileEntity>().apply {
        //根据文件夹和文件进行分组
        val group = map.groupedByIsDirectory()

        //先展示文件夹
        group[true]?.sort(sort)?.forEach { file ->
            val hash = map[file].orEmpty()
            if (hash.isNotBlank()) { //如果没有文件索引则忽略该文件
                val children = file.children
                val first: File? = children.findFirstMedia()
                val firstFileHash = if (first == null) "" else DatabaseFactory.fileIndexDao.getHash(first.absolutePath)
                val firstFileType = if (first == null) FileType.Media.UNKNOWN else FileType.getMediaType(first.name)
                add(
                    FileEntity(
                        name = file.name,
                        path = file.absolutePath,
                        hash = hash,
                        parentHash = file.parentFile.absolutePath.toMd5String(),
                        size = if (Configure.countDirSize) file.size else 0,
                        date = file.lastModified(),
                        isFile = file.isFile,
                        isDirectory = file.isDirectory,
                        childCount = children.size,
                        firstFileHash = firstFileHash,
                        firstFileType = firstFileType,
                        file.isSymlink()
                    )
                )
            }
        }

        //然后展示文件[文件已经在查询时排序过，所以不用再次排序]
        group[false]?.sort(sort)?.forEach { file ->
            val hash = map[file].orEmpty()
            if (hash.isNotBlank()) { //如果没有文件索引则忽略该文件
                add(
                    FileEntity(
                        name = file.name,
                        path = file.absolutePath,
                        hash = hash,
                        parentHash = file.parentFile.absolutePath.toMd5String(),
                        size = file.length(),
                        date = file.lastModified(),
                        isFile = file.isFile,
                        isDirectory = file.isDirectory,
                        childCount = 0,
                        firstFileHash = "",
                        firstFileType = FileType.Media.UNKNOWN,
                        file.isSymlink()
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

fun List<File>.sort(sort: ListSort?): List<File> {
    sort ?: return this
    return when (sort) {
        ListSort.NAME -> sortedByName()
        ListSort.SIZE -> sortedBySize()
        ListSort.DATE -> sortedByDate()
        ListSort.NAME_DESC -> sortedByNameDESC()
        ListSort.SIZE_DESC -> sortedBySizeDESC()
        ListSort.DATE_DESC -> sortedByDateDESC()
    }
}

fun Map<File, String>.groupedByIsDirectory(): Map<Boolean, List<File>> {
    return keys.groupBy {
        it.isDirectory
    }
}

fun List<File>.groupedByIsDirectory(): Map<Boolean, List<File>> {
    return groupBy {
        it.isDirectory
    }
}

fun List<File>.sortedByName(): List<File> {
    val collator = Collator.getInstance()
    return sortedWith(compareBy<File> {
        it.nameWithoutExtension.findStartNumber()
    }.thenBy {
        it.name.findChineseNumber()
    }.thenComparator { a, b ->
        collator.compare(a.name, b.name)
    }.thenBy {
        it.nameWithoutExtension.firstNumber()
    })
}

fun List<File>.sortedByNameDESC(): List<File> {
    return sortedByName().reversed()
}

fun List<File>.sortedBySize(): List<File> {
    return sortedBy { child ->
        if (child.isDirectory) {
            if (Configure.countDirSize) {
                child.size
            } else {
                child.listFiles()?.sumOf { mChild -> mChild.length() } ?: 0
            }
        } else {
            child.length()
        }
    }
}

fun List<File>.sortedBySizeDESC(): List<File> {
    return sortedBySize().reversed()
}

fun List<File>.sortedByDate(): List<File> {
    return sortedBy { it.lastModified() }
}

fun List<File>.sortedByDateDESC(): List<File> {
    return sortedByDate().reversed()
}


fun String.hasNumber(): Boolean {
    return "(\\d+)".toRegex().find(this)?.groupValues?.isNotEmpty() == true
}

fun String.startWithNumber(): Boolean {
    return "^\\d+".toRegex().find(this)?.groupValues?.isNotEmpty() == true
}

fun String.findStartNumber(): Long {
    return "^\\d+".toRegex().find(this)?.groupValues?.first()?.toLong() ?: 0
}

fun String.firstNumber(): Long {
    val values = "(\\d+)".toRegex().find(this)?.groupValues?.map {
        it.toLong()
    }
    return values?.firstOrNull() ?: 0
}

fun String.findChineseNumber(): Int {
    val regex = Regex("[一二三四五六七八九十百千壹贰叁肆伍陆柒捌玖拾佰仟]+")
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
        "百" -> intNumber += 100
        "千" -> intNumber += 1000
        "壹" -> intNumber += 1
        "贰" -> intNumber += 2
        "叁" -> intNumber += 3
        "肆" -> intNumber += 4
        "伍" -> intNumber += 5
        "陆" -> intNumber += 6
        "柒" -> intNumber += 7
        "捌" -> intNumber += 8
        "玖" -> intNumber += 9
        "拾" -> intNumber += 10
        "佰" -> intNumber += 100
        "仟" -> intNumber += 1000
    }
    return intNumber
}