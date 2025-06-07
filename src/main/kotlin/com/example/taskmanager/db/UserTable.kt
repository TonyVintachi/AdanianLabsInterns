package com.example.taskmanager.db

import org.jetbrains.exposed.sql.Table

object UserTable : Table("users") {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 255).uniqueIndex()
    val email = varchar("email", 255)
    val passwordHash = varchar("password_hash", 255) // Will store plain text for now

    override val primaryKey = PrimaryKey(id)
}
