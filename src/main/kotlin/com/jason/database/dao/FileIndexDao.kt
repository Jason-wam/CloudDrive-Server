package com.jason.database.dao

import com.jason.model.DuplicationFileEntity
import com.jason.utils.ListSort
import org.jetbrains.exposed.sql.Query
import java.io.File

interface FileIndexDao {
    suspend fun put(file: File, hash: String, type: String)

    suspend fun isExist(path: String): Boolean

    suspend fun isExistHash(hash: String): Boolean

    suspend fun search(kw: String, page: Int, size: Int, sort: ListSort): HashMap<File, String>

    suspend fun searchType(type: String, page: Int, size: Int, sort: ListSort): HashMap<File, String>

    suspend fun recentFiles(size: Int): HashMap<File, String>

    suspend fun getPath(hash: String): List<String>

    suspend fun getPath(hash: String, parent: String): String

    suspend fun getHash(path: String): String

    suspend fun delete(path: String): Boolean

    suspend fun deleteByParent(path: String): Int

    suspend fun getHashByParent(path: String): List<String>

    suspend fun getHashMapByParent(path: String): HashMap<String, String>

    suspend fun getPathByParent(path: String): List<String>

    suspend fun clear(): Boolean

    suspend fun count(): Long

    suspend fun queryAll(): Query

    suspend fun findDuplications(): List<DuplicationFileEntity>
}