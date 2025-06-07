package com.example.taskmanager.db

import com.example.taskmanager.models.Task
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import com.example.taskmanager.db.DatabaseFactory.dbQuery
import java.time.LocalDateTime

class TaskService {

    suspend fun init() {
        dbQuery {
            SchemaUtils.create(TaskTable)
        }
    }

    suspend fun createTask(
        title: String, description: String?, taskListId: Int,
        creatorId: Int, assigneeId: Int?, dueDate: LocalDateTime?
    ): Task? = dbQuery {
        val currentMaxPosition = TaskTable
            .select { TaskTable.taskListId eq taskListId }
            .maxOfOrNull { it[TaskTable.position] } ?: 0

        val currentTime = LocalDateTime.now()
        val insertStatement = TaskTable.insert {
            it[TaskTable.title] = title
            it[TaskTable.description] = description
            it[TaskTable.taskListId] = taskListId
            it[TaskTable.creatorId] = creatorId
            it[TaskTable.assigneeId] = assigneeId
            it[TaskTable.createdAt] = currentTime
            it[TaskTable.updatedAt] = currentTime
            it[TaskTable.dueDate] = dueDate
            it[TaskTable.position] = currentMaxPosition + 1
        }
        insertStatement.resultedValues?.singleOrNull()?.let { rowToTask(it) }
    }

    suspend fun getTaskById(id: Int): Task? = dbQuery {
        TaskTable.select { TaskTable.id eq id }
            .map { rowToTask(it) }
            .singleOrNull()
    }

    suspend fun getTasksByTaskList(taskListId: Int): List<Task> = dbQuery {
        TaskTable.select { TaskTable.taskListId eq taskListId }
            .orderBy(TaskTable.position)
            .map { rowToTask(it) }
    }

    suspend fun updateTask(
        id: Int, title: String?, description: String?, taskListId: Int?,
        assigneeId: Int?, dueDate: LocalDateTime?, position: Int?,
        clearDescription: Boolean = false,
        clearAssigneeId: Boolean = false,
        clearDueDate: Boolean = false
    ): Boolean = dbQuery {
        TaskTable.update({ TaskTable.id eq id }) {
            title?.let { nonNullTitle -> it[TaskTable.title] = nonNullTitle }

            if (clearDescription) {
                it[TaskTable.description] = null
            } else {
                description?.let { nonNullDescription -> it[TaskTable.description] = nonNullDescription }
            }

            taskListId?.let { nonNullTaskListId -> it[TaskTable.taskListId] = nonNullTaskListId }

            if (clearAssigneeId) {
                it[TaskTable.assigneeId] = null
            } else {
                assigneeId?.let { nonNullAssigneeId -> it[TaskTable.assigneeId] = nonNullAssigneeId }
            }

            if (clearDueDate) {
                it[TaskTable.dueDate] = null
            } else {
                dueDate?.let { nonNullDueDate -> it[TaskTable.dueDate] = nonNullDueDate }
            }

            position?.let { nonNullPosition -> it[TaskTable.position] = nonNullPosition }
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun deleteTask(id: Int): Boolean = dbQuery {
        TaskTable.deleteWhere { TaskTable.id eq id } > 0
    }

    private fun rowToTask(row: ResultRow): Task = Task(
        id = row[TaskTable.id],
        title = row[TaskTable.title],
        description = row[TaskTable.description],
        taskListId = row[TaskTable.taskListId],
        creatorId = row[TaskTable.creatorId],
        assigneeId = row[TaskTable.assigneeId],
        createdAt = row[TaskTable.createdAt],
        updatedAt = row[TaskTable.updatedAt],
        dueDate = row[TaskTable.dueDate],
        position = row[TaskTable.position]
    )
}
