package com.example.taskmanager.db

import org.jetbrains.exposed.sql.Table

object BoardTable : Table("boards") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val teamId = integer("team_id").references(TeamTable.id)

    override val primaryKey = PrimaryKey(id)
}
