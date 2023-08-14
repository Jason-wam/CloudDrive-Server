package com.jason.database.dao

import com.jason.database.DatabaseFactory
import com.jason.database.table.FileIndexTable
import com.jason.model.DuplicationFileEntity
import com.jason.utils.FileType
import com.jason.utils.ListSort
import com.jason.utils.ListSort.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.io.File

class FileIndexDaoImpl : FileIndexDao {
    override suspend fun put(file: File, hash: String, type: String): Unit = DatabaseFactory.dbQuery {
        FileIndexTable.replace {
            it[FileIndexTable.name] = file.name
            it[FileIndexTable.path] = file.path

            it[FileIndexTable.hash] = hash
            it[FileIndexTable.size] = file.length()
            it[FileIndexTable.date] = file.lastModified()
            it[FileIndexTable.type] = type

            it[FileIndexTable.parent] = file.parent.orEmpty()

            it[FileIndexTable.isFile] = file.isFile
            it[FileIndexTable.isDirectory] = file.isDirectory

            it[FileIndexTable.timestamp] = System.currentTimeMillis()
        }
    }

    override suspend fun isExist(path: String): Boolean = DatabaseFactory.dbQuery {
        FileIndexTable.select {
            FileIndexTable.path eq path
        }.count() > 0
    }

    override suspend fun isExistHash(hash: String): Boolean = DatabaseFactory.dbQuery {
        FileIndexTable.select {
            FileIndexTable.hash eq hash
        }.count() > 0
    }

    override suspend fun search(kw: String, page: Int, size: Int, sort: ListSort): HashMap<File, String> =
        DatabaseFactory.dbQuery {
            val start = (page - 1) * size
            var query = FileIndexTable.select { FileIndexTable.name like "%$kw%" }
            query = query.limit(size, start.toLong())
            query = query.orderBy(FileIndexTable.isDirectory, SortOrder.DESC)

            query = when (sort) {
                NAME -> query.orderBy(FileIndexTable.name)
                SIZE -> query.orderBy(FileIndexTable.size)
                DATE -> query.orderBy(FileIndexTable.date)
                NAME_DESC -> query.orderBy(FileIndexTable.name, SortOrder.DESC)
                SIZE_DESC -> query.orderBy(FileIndexTable.size, SortOrder.DESC)
                DATE_DESC -> query.orderBy(FileIndexTable.date, SortOrder.DESC)
            }

            HashMap<File, String>().apply {
                query.map {
                    this[File(it[FileIndexTable.path])] = it[FileIndexTable.hash]
                }
            }
        }

    override suspend fun searchType(
        type: String,
        page: Int,
        size: Int,
        sort: ListSort
    ): HashMap<File, String> = DatabaseFactory.dbQuery {
        val start = (page - 1) * size
        var query = if (type != FileType.Media.DOCUMENTS.name) {
            FileIndexTable.select { FileIndexTable.type eq type }
        } else {
            FileIndexTable.select {
                (FileIndexTable.type eq FileType.Media.TEXT.name) or
                        (FileIndexTable.type eq FileType.Media.EXCEL.name) or
                        (FileIndexTable.type eq FileType.Media.WORD.name) or
                        (FileIndexTable.type eq FileType.Media.PPT.name)
            }
        }

        query = when (sort) {
            NAME -> query.orderBy(FileIndexTable.name)
            SIZE -> query.orderBy(FileIndexTable.size)
            DATE -> query.orderBy(FileIndexTable.date)
            NAME_DESC -> query.orderBy(FileIndexTable.name, SortOrder.DESC)
            SIZE_DESC -> query.orderBy(FileIndexTable.size, SortOrder.DESC)
            DATE_DESC -> query.orderBy(FileIndexTable.date, SortOrder.DESC)
        }

        query = query.orderBy(FileIndexTable.isDirectory, SortOrder.DESC)
        query = query.limit(size, start.toLong())
        HashMap<File, String>().apply {
            query.map {
                this[File(it[FileIndexTable.path])] = it[FileIndexTable.hash]
            }
        }
    }

    override suspend fun recentFiles(size: Int): HashMap<File, String> = DatabaseFactory.dbQuery {
        HashMap<File, String>().apply {
            FileIndexTable.select { FileIndexTable.type neq FileType.Media.FOLDER.name }
                .orderBy(FileIndexTable.date, SortOrder.DESC)
                .limit(size)
                .map {
                    this[File(it[FileIndexTable.path])] = it[FileIndexTable.hash]
                }
        }
    }

    override suspend fun getPath(hash: String): List<String> = DatabaseFactory.dbQuery {
        FileIndexTable.select {
            FileIndexTable.hash eq hash
        }.map {
            it[FileIndexTable.path]
        }
    }

    override suspend fun getPath(hash: String, parent: String): String = DatabaseFactory.dbQuery {
        FileIndexTable.select {
            (FileIndexTable.hash eq hash) and (FileIndexTable.parent eq parent)
        }.firstOrNull()?.getOrNull(FileIndexTable.path).orEmpty()
    }

    override suspend fun getHash(path: String): String = DatabaseFactory.dbQuery {
        FileIndexTable.select {
            FileIndexTable.path eq path
        }.firstOrNull()?.getOrNull(FileIndexTable.hash).orEmpty()
    }

    override suspend fun delete(path: String): Boolean = DatabaseFactory.dbQuery {
        FileIndexTable.deleteWhere {
            FileIndexTable.path eq path
        } > 0
    }

    override suspend fun deleteByParent(path: String): Int = DatabaseFactory.dbQuery {
        FileIndexTable.deleteWhere {
            FileIndexTable.parent eq path
        }
    }

    override suspend fun getHashByParent(path: String): List<String> = DatabaseFactory.dbQuery {
        FileIndexTable.select {
            FileIndexTable.parent eq path
        }.map {
            it[FileIndexTable.parent]
        }
    }

    override suspend fun getHashMapByParent(path: String): HashMap<String, String> = DatabaseFactory.dbQuery {
        HashMap<String, String>().apply {
            FileIndexTable.select {
                FileIndexTable.parent eq path
            }.map {
                this[it[FileIndexTable.path]] = it[FileIndexTable.hash]
            }
        }
    }

    override suspend fun getPathByParent(path: String): List<String> = DatabaseFactory.dbQuery {
        FileIndexTable.select {
            FileIndexTable.parent eq path
        }.map {
            it[FileIndexTable.path]
        }
    }

    override suspend fun clear(): Boolean = DatabaseFactory.dbQuery {
        FileIndexTable.deleteAll()
        true
    }

    override suspend fun count(): Long = DatabaseFactory.dbQuery {
        FileIndexTable.selectAll().count()
    }

    override suspend fun queryAll(): Query = DatabaseFactory.dbQuery {
        FileIndexTable.selectAll()
    }

    override suspend fun findDuplications(): List<DuplicationFileEntity> = DatabaseFactory.dbQuery {
        val list = ArrayList<DuplicationFileEntity>()
        FileIndexTable.selectAll().map {
            DuplicationFileEntity(
                name = it[FileIndexTable.name],
                path = it[FileIndexTable.path],
                hash = it[FileIndexTable.hash],
                size = it[FileIndexTable.size],
                date = it[FileIndexTable.date],
                type = FileType.getMediaType(it[FileIndexTable.name])
            )
        }.groupBy {
            it.hash
        }.filter {
            it.value.size > 1
        }.map {
            it.value
        }.forEach {
            list.addAll(it)
        }
        list
    }
}