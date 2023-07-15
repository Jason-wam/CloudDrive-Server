package com.jason.database

import com.jason.database.dao.FileHashDao
import com.jason.database.dao.FileHashDaoImpl
import com.jason.database.table.FileHashTable
import com.jason.utils.Configure
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object DatabaseFactory {
    private val databaseDir = File(Configure.userDir, "database")

    val fileHashDao: FileHashDao by lazy {
        FileHashDaoImpl()
    }

    fun init() {
        val jdbcURL = "jdbc:sqlite:file:${databaseDir.absolutePath}" + File.separator + "indexes.db"
        val database = Database.connect(jdbcURL, "org.sqlite.JDBC", user = "root", password = "123456")
        transaction(database) {
            SchemaUtils.create(FileHashTable)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO) {
        block()
    }
}