package com.example.taskmanager.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object TaskTable : Table("tasks") {
    val id = integer("id").autoIncrement()
    val title = varchar("title", 255)
    val description = text("description").nullable()
    val taskListId = integer("task_list_id").references(TaskListTable.id)
    val creatorId = integer("creator_id").references(UserTable.id)
    val assigneeId = integer("assignee_id").references(UserTable.id).nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
    val dueDate = datetime("due_date").nullable()
    val position = integer("position")

    override val primaryKey = PrimaryKey(id)
}
