package com.jason.database.dao

import org.jetbrains.exposed.sql.Query

interface FileHashDao {
    suspend fun isExist(path: String): Boolean

    suspend fun isExistHash(hash: String): Boolean

    suspend fun put(path: String, hash: String, parent: String, type: String, root: String, date: Long): Boolean

    suspend fun getPath(hash: String): List<String>

    suspend fun getPath(hash: String, parent: String): String

    suspend fun getHash(path: String): String

    suspend fun delete(path: String): Boolean

    suspend fun deleteByRoot(path: String): Int

    suspend fun deleteByNotRoot(path: String): Int

    suspend fun deleteByParent(path: String): Int

    suspend fun getHashByParent(path: String): List<String>

    suspend fun getHashMapByParent(path: String): HashMap<String, String>

    suspend fun getPathByParent(path: String): List<String>

    suspend fun clear(): Boolean

    suspend fun count(): Long

    suspend fun queryAll(): Query
}