package com.example.taskmanager.routes

import com.example.taskmanager.db.TaskService
import com.example.taskmanager.routes.dto.TaskCreateRequest
import com.example.taskmanager.routes.dto.TaskResponse
import com.example.taskmanager.routes.dto.TaskUpdateRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

fun Route.taskRoutes(taskService: TaskService) {

    val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun String.toLocalDateTimeSafe(): LocalDateTime? {
        return try {
            LocalDateTime.parse(this, dateTimeFormatter)
        } catch (e: DateTimeParseException) {
            null
        }
    }

    route("/tasks") {
        post {
            val request = call.receive<TaskCreateRequest>()
            // Auth: Check user permissions for taskListId, creatorId
            val dueDate = request.dueDate?.toLocalDateTimeSafe()
            if (request.dueDate != null && dueDate == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid due date format. Use ISO_LOCAL_DATE_TIME.")
                return@post
            }

            val task = taskService.createTask(
                title = request.title,
                description = request.description,
                taskListId = request.taskListId,
                creatorId = request.creatorId, // Should come from auth context
                assigneeId = request.assigneeId,
                dueDate = dueDate
            )
            if (task != null) {
                call.respond(
                    HttpStatusCode.Created,
                    TaskResponse(
                        id = task.id,
                        title = task.title,
                        description = task.description,
                        taskListId = task.taskListId,
                        creatorId = task.creatorId,
                        assigneeId = task.assigneeId,
                        createdAt = task.createdAt.format(dateTimeFormatter),
                        updatedAt = task.updatedAt.format(dateTimeFormatter),
                        dueDate = task.dueDate?.format(dateTimeFormatter),
                        position = task.position
                    )
                )
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Failed to create task")
            }
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid task ID")
                return@get
            }
            // Auth: Check if user has access to this task
            val task = taskService.getTaskById(id)
            if (task != null) {
                call.respond(
                    TaskResponse(
                        id = task.id,
                        title = task.title,
                        description = task.description,
                        taskListId = task.taskListId,
                        creatorId = task.creatorId,
                        assigneeId = task.assigneeId,
                        createdAt = task.createdAt.format(dateTimeFormatter),
                        updatedAt = task.updatedAt.format(dateTimeFormatter),
                        dueDate = task.dueDate?.format(dateTimeFormatter),
                        position = task.position
                    )
                )
            } else {
                call.respond(HttpStatusCode.NotFound, "Task not found")
            }
        }

        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid task ID")
                return@put
            }
            // Auth: Check user permissions
            val request = call.receive<TaskUpdateRequest>()
            val dueDate = request.dueDate?.toLocalDateTimeSafe()
            if (request.dueDate != null && dueDate == null && request.clearDueDate != true) {
                 call.respond(HttpStatusCode.BadRequest, "Invalid due date format. Use ISO_LOCAL_DATE_TIME.")
                 return@put
            }

            val updated = taskService.updateTask(
                id = id,
                title = request.title,
                description = request.description,
                taskListId = request.taskListId,
                assigneeId = request.assigneeId,
                dueDate = dueDate,
                position = request.position,
                clearDescription = request.clearDescription ?: false,
                clearAssigneeId = request.clearAssigneeId ?: false,
                clearDueDate = request.clearDueDate ?: false
            )

            if (updated) {
                val updatedTask = taskService.getTaskById(id) // Fetch updated task
                if (updatedTask != null) {
                     call.respond(
                        TaskResponse(
                            id = updatedTask.id,
                            title = updatedTask.title,
                            description = updatedTask.description,
                            taskListId = updatedTask.taskListId,
                            creatorId = updatedTask.creatorId,
                            assigneeId = updatedTask.assigneeId,
                            createdAt = updatedTask.createdAt.format(dateTimeFormatter),
                            updatedAt = updatedTask.updatedAt.format(dateTimeFormatter),
                            dueDate = updatedTask.dueDate?.format(dateTimeFormatter),
                            position = updatedTask.position
                        )
                    )
                } else {
                    call.respond(HttpStatusCode.NotFound, "Task not found after update")
                }
            } else {
                call.respond(HttpStatusCode.NotModified, "No changes applied or failed to update task")
            }
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid task ID")
                return@delete
            }
            // Auth: Check user permissions
            val deleted = taskService.deleteTask(id)
            if (deleted) {
                call.respond(HttpStatusCode.OK, "Task deleted successfully")
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Failed to delete task")
            }
        }
    }

    // Get all tasks for a task list
    route("/tasklists/{taskListId}/tasks") {
        get {
            val taskListId = call.parameters["taskListId"]?.toIntOrNull()
            if (taskListId == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid task list ID")
                return@get
            }
            // Auth: Check if user has access to taskListId
            val tasks = taskService.getTasksByTaskList(taskListId)
            val taskResponses = tasks.map { task ->
                TaskResponse(
                    id = task.id,
                    title = task.title,
                    description = task.description,
                    taskListId = task.taskListId,
                    creatorId = task.creatorId,
                    assigneeId = task.assigneeId,
                    createdAt = task.createdAt.format(dateTimeFormatter),
                    updatedAt = task.updatedAt.format(dateTimeFormatter),
                    dueDate = task.dueDate?.format(dateTimeFormatter),
                    position = task.position
                )
            }
            call.respond(taskResponses)
        }
    }
}
