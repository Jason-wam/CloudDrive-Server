package com.jason.model

import com.jason.database.DatabaseFactory
import com.jason.utils.MediaType
import com.jason.utils.children
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
    val hasImage: Boolean
)

suspend fun List<File>.toFileEntities(): List<FileEntity> {
    return ArrayList<FileEntity>().apply {
        this@toFileEntities.forEach {
            val children = it.children
            add(
                FileEntity(
                    it.name,
                    it.absolutePath,
                    DatabaseFactory.fileHashDao.getHash(it.absolutePath),
                    it.length(),
                    it.lastModified(),
                    it.isFile,
                    it.isDirectory,
                    children.size,
                    if (it.isDirectory.not()) {
                        MediaType.isVideo(it) || MediaType.isImage(it)
                    } else {
                        children.find { file ->
                            MediaType.isVideo(file)
                        } != null || children.find { file ->
                            MediaType.isImage(
                                file
                            )
                        } != null
                    }
                )
            )
        }
    }.sortedByDescending {
        it.isDirectory
    }
}