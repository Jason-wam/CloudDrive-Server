package com.jason.utils

import com.jason.database.DatabaseFactory
import com.jason.database.table.FileHashTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files

object FileIndexer {
    private const val name = "Indexer"

    suspend fun indexFiles() {
        LoggerFactory.getLogger(name).info("正在建立文件索引...")
        var index = 0
        listFiles(Configure.rootDir) {
            index += 1
            addFileIndex(it)
            if (index % 1000 == 0) {
                LoggerFactory.getLogger(name).info("正在建立文件索引: $index ..")
            }
        }
        LoggerFactory.getLogger(name).info("建立文件索引完毕: $index！")
    }

    suspend fun indexFile(file: File) {
        addFileIndex(file, true)
    }

    suspend fun indexDirectory(file: File) {
        addFileIndex(file, true)
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                addFileIndex(child, true)
            }
        }
    }

    suspend fun clearUnExistSymlinks() {
        listFiles(Configure.rootDir) {
            if (it.isSymlink()) {
                val path = withContext(Dispatchers.IO) {
                    Files.readSymbolicLink(it.toPath())
                }
                val exist = Files.exists(path)
                if (!exist) {
                    it.delete()
                    LoggerFactory.getLogger(name).info("清理不存在的符号链接: $it ..")
                }
            }
        }
    }

    suspend fun scanDatabaseRows() {
        LoggerFactory.getLogger(name).info("正在整理数据库 ..")

        DatabaseFactory.dbQuery {
            val deleted = DatabaseFactory.fileHashDao.deleteByNotRoot(
                Configure.rootDir.absolutePath
            )
            if (deleted > 0) {
                LoggerFactory.getLogger(name).info("移除 $deleted 条旧索引 ..")
            }
        }

        DatabaseFactory.dbQuery {
            DatabaseFactory.fileHashDao.queryAll().map {
                it[FileHashTable.path]
            }
        }.forEachIndexed { index, s ->
            val file = File(s)
            if (file.exists().not()) {
                DatabaseFactory.fileHashDao.delete(s)
                LoggerFactory.getLogger(name).info("清理不存在的索引: $s")
            } else if (file.isSymlink()) {
                val path = Files.readSymbolicLink(file.toPath())
                val exist = Files.exists(path)
                if (exist.not()) {
                    file.delete()
                    DatabaseFactory.fileHashDao.delete(s)
                    LoggerFactory.getLogger(name).info("清理不存在的符号链接: $s")
                }
            } else {
                if (index % 100 == 0) {
                    LoggerFactory.getLogger(name).info("正在整理数据库: $index ..")
                }
            }
        }


        LoggerFactory.getLogger(name).info("整理数据库完毕！")
    }

    private suspend fun listFiles(file: File, block: suspend (file: File) -> Unit) {
        block.invoke(file)
        file.listFiles()?.forEach {
            if (it.isDirectory) {
                listFiles(it, block)
            } else {
                addFileIndex(it)
                block.invoke(it)
            }
        }
    }

    suspend fun reindex() {
        LoggerFactory.getLogger(name).info("正在重建文件索引...")
        DatabaseFactory.fileHashDao.clear()
        indexFiles()
        LoggerFactory.getLogger(name).info("重建文件索引完毕！")
    }

    private suspend fun addFileIndex(file: File, overwrite: Boolean = false) {
        try {
            if (file.exists().not()) {
                DatabaseFactory.fileHashDao.delete(file.absolutePath)
            } else {
                if (overwrite) {
                    if (file.isDirectory) {
                        DatabaseFactory.fileHashDao.deleteByParent(file.absolutePath)
                    } else {
                        DatabaseFactory.fileHashDao.delete(file.absolutePath)
                    }
                }
                if (DatabaseFactory.fileHashDao.isExist(file.absolutePath).not()) {
                    if (file.isDirectory) {
                        DatabaseFactory.fileHashDao.put(
                            file.absolutePath,
                            file.absolutePath.toMd5String(),
                            file.parent ?: ""
                        )
                    } else {
                        DatabaseFactory.fileHashDao.put(
                            file.absolutePath,
                            file.createSketchedMD5String(),
                            file.parent ?: ""
                        )
                    }
                }
            }
        } catch (ignore: Exception) {
        }
    }
}