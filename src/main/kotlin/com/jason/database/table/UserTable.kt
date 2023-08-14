package com.jason.database.table

import org.jetbrains.exposed.sql.Table

object UserTable : Table() {
    val account = varchar("account", 12)
    val password = varchar("password", 40)
    val token = varchar("token", 40)

    override val primaryKey: PrimaryKey = PrimaryKey(account)
}