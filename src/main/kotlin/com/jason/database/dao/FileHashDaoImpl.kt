package com.jason.database.dao

import com.jason.database.DatabaseFactory
import com.jason.database.table.FileHashTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class FileHashDaoImpl : FileHashDao {
    override suspend fun isExist(path: String): Boolean = DatabaseFactory.dbQuery {
        FileHashTable.select {
            FileHashTable.path eq path
        }.count() > 0
    }

    override suspend fun isExistHash(hash: String): Boolean = DatabaseFactory.dbQuery {
        FileHashTable.select {
            FileHashTable.hash eq hash
        }.count() > 0
    }

    override suspend fun put(path: String, hash: String): Boolean = DatabaseFactory.dbQuery {
        FileHashTable.replace {
            it[FileHashTable.path] = path
            it[FileHashTable.hash] = hash
            it[FileHashTable.timestamp] = System.currentTimeMillis()
        }.insertedCount > 0
    }

    override suspend fun getPath(hash: String): String = DatabaseFactory.dbQuery {
        FileHashTable.select {
            FileHashTable.hash eq hash
        }.firstOrNull()?.getOrNull(FileHashTable.path) ?: ""
    }

    override suspend fun getHash(path: String): String = DatabaseFactory.dbQuery {
        FileHashTable.select {
            FileHashTable.path eq path
        }.firstOrNull()?.getOrNull(FileHashTable.hash) ?: ""
    }

    override suspend fun delete(path: String): Boolean = DatabaseFactory.dbQuery {
        FileHashTable.deleteWhere {
            FileHashTable.path eq path
        } > 0
    }

    override suspend fun clear(): Boolean = DatabaseFactory.dbQuery {
        FileHashTable.deleteAll()
        true
    }

    override suspend fun count(): Long = DatabaseFactory.dbQuery {
        FileHashTable.selectAll().count()
    }

    override suspend fun queryAll(): Query  = DatabaseFactory.dbQuery {
        FileHashTable.selectAll()
    }
}