package com.jason.database.dao

import org.jetbrains.exposed.sql.Query

interface FileHashDao {
    suspend fun isExist(path: String): Boolean

    suspend fun isExistHash(hash: String): Boolean

    suspend fun put(path: String, hash: String,parent:String): Boolean

    suspend fun getPath(hash: String): String

    suspend fun getHash(path: String): String

    suspend fun delete(path: String): Boolean

    suspend fun deleteByRoot(path: String): Int

    suspend fun deleteByNotRoot(path: String): Int

    suspend fun deleteByParent(path: String): Int

    suspend fun getByParent(path: String): List<String>

    suspend fun clear(): Boolean

    suspend fun count(): Long

    suspend fun queryAll(): Query
}