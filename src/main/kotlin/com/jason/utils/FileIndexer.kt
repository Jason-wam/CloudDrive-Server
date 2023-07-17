package com.jason.utils

import com.jason.database.DatabaseFactory
import com.jason.database.table.FileHashTable
import com.jason.utils.extension.createSketchedMD5String
import com.jason.utils.extension.isSymlink
import com.jason.utils.extension.toMd5String
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files

object FileIndexer {
    private const val name = "Indexer"

    suspend fun indexFiles() {
        LoggerFactory.getLogger(name).info("正在建立文件索引，可能占用较长时间...")
        var index = 0
        listFiles(Configure.rootDir) {
            index += 1
            addFileIndex(it)
            if (index % 100 == 0) {
                LoggerFactory.getLogger(name).info("正在建立文件索引: $index ..")
            }
        }
        LoggerFactory.getLogger(name).info("建立文件索引完毕: $index！")
    }

    /**
     * 重新扫描指定目录以更新文件列表索引
     */
    suspend fun indexDirectory(file: File) {
        if (file.isDirectory) {
            addFileIndex(file)

            //读取当前目录已索引的文件列表
            val indexedList = DatabaseFactory.fileHashDao.getPathByParent(file.absolutePath)
            //遍历已索引的文件列表
            indexedList.forEach {
                val indexed = File(it)
                if (indexed.exists().not()) {
                    //如果已索引的文件不存在了则从数据库中删除
                    DatabaseFactory.fileHashDao.delete(indexed.absolutePath)
                    LoggerFactory.getLogger(name).info("删除不存在的文件索引: ${indexed.absolutePath}！")
                } else {
                    if (indexed.isSymlink()) { //如果文件是符号链接
                        val path = Files.readSymbolicLink(indexed.toPath())
                        //判断原始文件是否存在
                        val exist = Files.exists(path)
                        if (exist.not()) { //如果原始文件不存在
                            indexed.delete()
                            //删除符号链接文件和索引
                            DatabaseFactory.fileHashDao.delete(indexed.absolutePath)
                            LoggerFactory.getLogger(name).info("清理不存在的符号链接: ${indexed.absolutePath}")
                        }
                    }
                }
            }

            //重新遍历当前目录存在的文件
            file.listFiles()?.filter {
                //筛选出数据库索引中不存在的文件
                indexedList.contains(it.absolutePath).not()
            }?.forEach {
                //遍历不存在的文件重新添加文件索引
                addFileIndex(it)
                LoggerFactory.getLogger(name).info("添加文件索引: ${it.absolutePath}！")
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
        LoggerFactory.getLogger(name).info("正在整理数据库，可能占用较长时间...")

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
        LoggerFactory.getLogger(name).info("正在重建文件索引，可能占用较长时间...")
        DatabaseFactory.fileHashDao.clear()
        indexFiles()
        LoggerFactory.getLogger(name).info("重建文件索引完毕！")
    }

    suspend fun addFileIndex(file: File) {
        try {
            if (file.exists().not()) {
                DatabaseFactory.fileHashDao.delete(file.absolutePath)
            } else {
                if (DatabaseFactory.fileHashDao.isExist(file.absolutePath).not()) {
                    if (file.isDirectory) {
                        DatabaseFactory.fileHashDao.put(
                            file.absolutePath,
                            file.absolutePath.toMd5String(),
                            file.parent.orEmpty()
                        )
                    } else {
                        DatabaseFactory.fileHashDao.put(
                            file.absolutePath,
                            file.createSketchedMD5String(),
                            file.parent.orEmpty()
                        )
                    }
                }
            }
        } catch (ignore: Exception) {
        }
    }
}