package com.jason.database.table

import org.jetbrains.exposed.sql.Table

object FileHashTable : Table() {
    val path = text("path")
    val root = text("root")
    val parent = text("parent")
    val hash = varchar("hash", 32)
    val timestamp = long("timestamp")
    val type = text("type")

    override val primaryKey: PrimaryKey = PrimaryKey(path)
}