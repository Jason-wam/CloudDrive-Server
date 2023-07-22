package com.jason.database

import com.jason.database.dao.FileHashDao
import com.jason.database.dao.FileHashDaoImpl
import com.jason.database.table.FileHashTable
import com.jason.utils.Configure
import com.jason.utils.extension.toMd5String
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object DatabaseFactory {
    private val databaseDir = File(Configure.userDir, "database").also { it.mkdirs() }

    val fileHashDao: FileHashDao by lazy {
        FileHashDaoImpl()
    }

    fun init() {
        val hash = Configure.rootDir.absolutePath.toMd5String()
        val jdbcURL = "jdbc:sqlite:file:${databaseDir.absolutePath}" + File.separator + "indexes_$hash.db"
        val database = Database.connect(jdbcURL, "org.sqlite.JDBC")
        transaction(database) {
            SchemaUtils.create(FileHashTable)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO) {
        block()
    }
}