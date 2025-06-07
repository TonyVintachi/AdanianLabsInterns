package com.example.taskmanager.db

import org.jetbrains.exposed.sql.Table

object TaskListTable : Table("task_lists") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val boardId = integer("board_id").references(BoardTable.id)
    val position = integer("position")

    override val primaryKey = PrimaryKey(id)
}
