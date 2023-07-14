package com.jason.database.dao

interface FileHashDao {
    suspend fun isExist(path: String): Boolean

    suspend fun isExistHash(hash: String): Boolean

    suspend fun put(path: String, hash: String): Boolean

    suspend fun getPath(hash: String): String

    suspend fun getHash(path: String): String

    suspend fun delete(path: String): Boolean

    suspend fun clear(): Boolean

    suspend fun count(): Long
}