package com.example.taskmanager.db

import org.jetbrains.exposed.sql.Table

object TeamTable : Table("teams") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)

    override val primaryKey = PrimaryKey(id)
}
