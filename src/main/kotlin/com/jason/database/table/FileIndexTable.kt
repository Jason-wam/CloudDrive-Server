package com.jason.database.table

import org.jetbrains.exposed.sql.Table

object FileIndexTable : Table() {
    val name = text("name")
    val path = text("path")
    val hash = text("hash")
    val size = long("size")
    val date = long("date")
    val type = text("type")
    val root = text("root")
    val parent = text("parent")

    val isFile = bool("isFile")
    val isDirectory = bool("isDirectory")
    val timestamp = long("timestamp")

    override val primaryKey: PrimaryKey = PrimaryKey(path)
}