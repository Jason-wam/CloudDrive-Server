package com.jason.database

import com.jason.database.dao.FileIndexDao
import com.jason.database.dao.FileIndexDaoImpl
import com.jason.database.table.FileIndexTable
import com.jason.utils.Configure
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object DatabaseFactory {
    private val databaseDir = File(Configure.userDir, "database").also { it.mkdirs() }

    val fileIndexDao: FileIndexDao by lazy {
        FileIndexDaoImpl()
    }

    fun init() {
        val jdbcURL = "jdbc:sqlite:file:${databaseDir.absolutePath}" + File.separator + "indexes.db"
        val database = Database.connect(jdbcURL, "org.sqlite.JDBC")
        transaction(database) {
            SchemaUtils.create(FileIndexTable)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO) {
        block()
    }
}