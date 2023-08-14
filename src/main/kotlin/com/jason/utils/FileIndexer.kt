package com.jason.utils

import com.jason.database.DatabaseFactory
import com.jason.database.table.FileIndexTable
import com.jason.utils.extension.createSketchedMD5String
import com.jason.utils.extension.isSymlink
import com.jason.utils.extension.toMd5String
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files

object FileIndexer {
    private const val NAME = "Indexer"
    suspend fun indexFiles() {
        LoggerFactory.getLogger(NAME).info("正在建立文件索引，可能占用较长时间...")
        var index = 0
        Configure.mountedDirs.forEach { dir ->
            LoggerFactory.getLogger(NAME).info("正在建立文件索引: ${dir.absolutePath} ...")
            dir.forEachFiles { file ->
                index += 1
                addFileIndex(file)
                if (index % 100 == 0) {
                    LoggerFactory.getLogger(NAME).info("正在建立文件索引: $index ..")
                }
            }
        }
        LoggerFactory.getLogger(NAME).info("建立文件索引完毕: $index！")
    }

    /**
     * 重新扫描指定目录以更新文件列表索引
     */
    suspend fun indexDirectory(file: File) {
        if (file.isDirectory) {
            addFileIndex(file)

            //读取当前目录已索引的文件列表
            val indexedList = DatabaseFactory.fileIndexDao.getPathByParent(file.absolutePath)
            //遍历已索引的文件列表
            indexedList.forEach {
                val indexed = File(it)
                if (indexed.exists().not()) {
                    //如果已索引的文件不存在了则从数据库中删除
                    DatabaseFactory.fileIndexDao.delete(indexed.absolutePath)
                    LoggerFactory.getLogger(NAME).info("删除不存在的文件索引: ${indexed.absolutePath}")
                } else {
                    if (indexed.isSymlink()) { //如果文件是符号链接
                        val path = Files.readSymbolicLink(indexed.toPath())
                        //判断原始文件是否存在
                        val exist = Files.exists(path)
                        if (exist.not()) { //如果原始文件不存在
                            indexed.delete()
                            //删除符号链接文件和索引
                            DatabaseFactory.fileIndexDao.delete(indexed.absolutePath)
                            LoggerFactory.getLogger(NAME).info("清理不存在的符号链接: ${indexed.absolutePath}")
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
                LoggerFactory.getLogger(NAME).info("添加文件索引: ${it.absolutePath}")
            }
        }
    }

    suspend fun scanDatabaseRows() {
        LoggerFactory.getLogger(NAME).info("正在整理数据库，可能占用较长时间...")

        DatabaseFactory.dbQuery {
            DatabaseFactory.fileIndexDao.queryAll().map {
                it[FileIndexTable.path]
            }
        }.forEachIndexed { index, s ->
            val file = File(s)
            if (file.exists().not()) {
                DatabaseFactory.fileIndexDao.delete(s)
                LoggerFactory.getLogger(NAME).info("清理不存在的索引: $s")
            } else if (file.isSymlink()) {
                val path = Files.readSymbolicLink(file.toPath())
                val exist = Files.exists(path)
                if (exist.not()) {
                    file.delete()
                    DatabaseFactory.fileIndexDao.delete(s)
                    LoggerFactory.getLogger(NAME).info("清理不存在的符号链接: $s")
                }
            } else {
                if (index % 100 == 0) {
                    LoggerFactory.getLogger(NAME).info("正在整理数据库: $index ..")
                }
            }
        }

        LoggerFactory.getLogger(NAME).info("数据库整理完毕！")
    }

    private suspend fun File.forEachFiles(block: suspend (file: File) -> Unit) {
        block.invoke(this)
        listFiles()?.forEach {
            block.invoke(it)
            if (it.isDirectory) {
                it.forEachFiles(block)
            }
        }
    }

    suspend fun reindex() {
        LoggerFactory.getLogger(NAME).info("正在重建文件索引，可能占用较长时间...")
        DatabaseFactory.fileIndexDao.clear()
        indexFiles()
        LoggerFactory.getLogger(NAME).info("重建文件索引完毕！")
    }

    suspend fun addFileIndex(file: File) {
        try {
            if (file.exists().not()) {
                DatabaseFactory.fileIndexDao.delete(file.absolutePath)
            } else {
                if (DatabaseFactory.fileIndexDao.isExist(file.absolutePath).not()) {
                    if (file.isDirectory) {
                        DatabaseFactory.fileIndexDao.put(
                            file,
                            file.absolutePath.toMd5String(),
                            FileType.Media.FOLDER.name
                        )
                    } else {
                        DatabaseFactory.fileIndexDao.put(
                            file,
                            file.createSketchedMD5String(),
                            FileType.getMediaType(file.name).name
                        )
                    }
                }
            }
        } catch (ignore: Exception) {
        }
    }

    suspend fun addFileIndex(file: File, hash: String) {
        DatabaseFactory.fileIndexDao.put(file, hash, FileType.getMediaType(file.name).name)
    }
}