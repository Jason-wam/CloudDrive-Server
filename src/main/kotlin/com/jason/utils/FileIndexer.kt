package com.jason.utils

import com.jason.database.DatabaseFactory
import com.jason.database.table.FileHashTable
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption

object FileIndexer {
    suspend fun index() {
        println("正在整理数据库...")
        clearUnExistRows()
        println("正在枚举待校验文件...")
        addFileIndex(Configure.rootDir)

        Configure.rootDir.allChildren().run {
            forEachIndexed { index, file ->
                addFileIndex(file)
                print("\r正在建立文件索引: $index/${size - 1} ..")
            }
        }
    }

    suspend fun index2() {
        println("正在整理数据库...")
        clearUnExistRows()
        println("正在枚举待校验文件...")
        var index = 0
        listFiles(Configure.rootDir) {
            index += 1
            addFileIndex(it)
            print("\r正在建立文件索引: $index ..")
        }
    }

    suspend fun clearUnExistSymlinks() {
        listFiles(Configure.rootDir){
            if (it.isSymlink()) {
                val path = Files.readSymbolicLink(it.toPath())
                val exist = Files.exists(path)
                if (!exist) {
                    it.delete()
                    print("\r清理不存在的符号链接: $it ..")
                }
            }
        }
    }

    suspend fun clearUnExistRows() {
        DatabaseFactory.dbQuery {
            DatabaseFactory.fileHashDao.queryAll().map {
                it[FileHashTable.path]
            }
        }.forEach {
            val file = File(it)
            if (file.exists().not()) {
                DatabaseFactory.fileHashDao.delete(it)
                println("清理不存在的索引: $it ..")
            }

            if (file.isSymlink()) {
                val path = Files.readSymbolicLink(file.toPath())
                val exist = Files.exists(path)
                if (exist.not()) {
                    file.delete()
                    DatabaseFactory.fileHashDao.delete(it)
                    print("\r清理不存在的符号链接: $it ..")
                }
            }
        }
    }

    suspend fun listFiles(file: File, block: suspend (file: File) -> Unit) {
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
        DatabaseFactory.fileHashDao.clear()
        index()
    }

    private suspend fun addFileIndex(file: File) {
        try {
            if (file.exists().not()) {
                DatabaseFactory.fileHashDao.delete(file.absolutePath)
            } else {
                if (DatabaseFactory.fileHashDao.isExist(file.absolutePath).not()) {
                    if (file.isFile) {
                        DatabaseFactory.fileHashDao.put(file.absolutePath, file.createSketchedMD5String())
                    } else {
                        DatabaseFactory.fileHashDao.put(file.absolutePath, file.absolutePath.toMd5String())
                    }
                }
            }
        } catch (ignore: Exception) {
        }
    }
}