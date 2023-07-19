package com.jason.database.dao

import com.jason.database.DatabaseFactory
import com.jason.database.table.FileHashTable
import com.jason.utils.Configure
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq

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

    override suspend fun put(path: String, hash: String, parent: String, type: String, root: String): Boolean =
        DatabaseFactory.dbQuery {
            FileHashTable.replace {
                it[FileHashTable.path] = path
                it[FileHashTable.hash] = hash
                it[FileHashTable.root] = root
                it[FileHashTable.parent] = parent
                it[FileHashTable.timestamp] = System.currentTimeMillis()
                it[FileHashTable.type] = type
            }.insertedCount > 0
        }

    override suspend fun getPath(hash: String): List<String> = DatabaseFactory.dbQuery {
        FileHashTable.select {
            FileHashTable.hash eq hash
        }.map {
            it[FileHashTable.path]
        }
    }

    override suspend fun getPath(hash: String, parent: String): String = DatabaseFactory.dbQuery {
        FileHashTable.select {
            (FileHashTable.hash eq hash) and (FileHashTable.parent eq parent)
        }.firstOrNull()?.getOrNull(FileHashTable.path).orEmpty()
    }

    override suspend fun getHash(path: String): String = DatabaseFactory.dbQuery {
        FileHashTable.select {
            FileHashTable.path eq path
        }.firstOrNull()?.getOrNull(FileHashTable.hash).orEmpty()
    }

    override suspend fun delete(path: String): Boolean = DatabaseFactory.dbQuery {
        FileHashTable.deleteWhere {
            FileHashTable.path eq path
        } > 0
    }

    override suspend fun deleteByRoot(path: String): Int = DatabaseFactory.dbQuery {
        FileHashTable.deleteWhere {
            FileHashTable.root eq path
        }
    }

    override suspend fun deleteByNotRoot(path: String): Int = DatabaseFactory.dbQuery {
        FileHashTable.deleteWhere {
            FileHashTable.root neq path
        }
    }

    override suspend fun deleteByParent(path: String): Int = DatabaseFactory.dbQuery {
        FileHashTable.deleteWhere {
            FileHashTable.parent eq path
        }
    }

    override suspend fun getHashByParent(path: String): List<String> = DatabaseFactory.dbQuery {
        FileHashTable.select {
            FileHashTable.parent eq path
        }.map {
            it[FileHashTable.parent]
        }
    }

    override suspend fun getPathByParent(path: String): List<String> = DatabaseFactory.dbQuery {
        FileHashTable.select {
            FileHashTable.parent eq path
        }.map {
            it[FileHashTable.path]
        }
    }

    override suspend fun clear(): Boolean = DatabaseFactory.dbQuery {
        FileHashTable.deleteAll()
        true
    }

    override suspend fun count(): Long = DatabaseFactory.dbQuery {
        FileHashTable.selectAll().count()
    }

    override suspend fun queryAll(): Query = DatabaseFactory.dbQuery {
        FileHashTable.selectAll()
    }
}