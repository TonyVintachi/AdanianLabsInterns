package com.example.taskmanager.db

import com.example.taskmanager.models.TaskList
import org.jetbrains.exposed.sql.*
import com.example.taskmanager.db.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class TaskListService {

    suspend fun init() {
        dbQuery {
            SchemaUtils.create(TaskListTable)
        }
    }

    suspend fun createTaskList(name: String, boardId: Int): TaskList? = dbQuery {
        val currentMaxPosition = TaskListTable
            .select { TaskListTable.boardId eq boardId }
            .maxOfOrNull { it[TaskListTable.position] } ?: 0

        val insertStatement = TaskListTable.insert {
            it[TaskListTable.name] = name
            it[TaskListTable.boardId] = boardId
            it[TaskListTable.position] = currentMaxPosition + 1
        }
        insertStatement.resultedValues?.singleOrNull()?.let {
            rowToTaskList(it)
        }
    }

    suspend fun getTaskListById(id: Int): TaskList? = dbQuery {
        TaskListTable.select { TaskListTable.id eq id }
            .map { rowToTaskList(it) }
            .singleOrNull()
    }

    suspend fun getTaskListsByBoard(boardId: Int): List<TaskList> = dbQuery {
        TaskListTable.select { TaskListTable.boardId eq boardId }
            .orderBy(TaskListTable.position)
            .map { rowToTaskList(it) }
    }

    suspend fun updateTaskListName(id: Int, newName: String): Boolean = dbQuery {
        TaskListTable.update({ TaskListTable.id eq id }) {
            it[name] = newName
        } > 0
    }

    suspend fun updateTaskListPosition(id: Int, newPosition: Int): Boolean = dbQuery {
        // This is a simplified version. A robust implementation would handle
        // shifting other task lists' positions accordingly.
        // For now, we just update the position directly.
        // It's assumed newPosition is valid and managed by the client or a higher-level service.
        TaskListTable.update({ TaskListTable.id eq id }) {
            it[position] = newPosition
        } > 0
    }

    suspend fun deleteTaskList(id: Int): Boolean = dbQuery {
        // For now, just deletes the task list. Cascading deletes for tasks
        // will be handled in the TaskService.
        TaskListTable.deleteWhere { TaskListTable.id eq id } > 0
    }

    private fun rowToTaskList(row: ResultRow): TaskList = TaskList(
        id = row[TaskListTable.id],
        name = row[TaskListTable.name],
        boardId = row[TaskListTable.boardId],
        position = row[TaskListTable.position]
    )
}
