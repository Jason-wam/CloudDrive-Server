package com.jason.database.table

import org.jetbrains.exposed.sql.Table

object FileHashTable : Table() {
    val path = text("path")
    val hash = varchar("hash", 32)
    val timestamp = long("timestamp")

    override val primaryKey: PrimaryKey = PrimaryKey(path)
}