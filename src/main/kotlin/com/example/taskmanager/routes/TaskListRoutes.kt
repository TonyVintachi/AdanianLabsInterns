package com.example.taskmanager.routes

import com.example.taskmanager.db.TaskListService
import com.example.taskmanager.routes.dto.TaskListCreateRequest
import com.example.taskmanager.routes.dto.TaskListResponse
import com.example.taskmanager.routes.dto.TaskListUpdateRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.taskListRoutes(taskListService: TaskListService) {

    route("/tasklists") {
        post {
            val request = call.receive<TaskListCreateRequest>()
            // Auth: Check if user has access to boardId and permission to create task list
            val taskList = taskListService.createTaskList(request.name, request.boardId)
            if (taskList != null) {
                call.respond(HttpStatusCode.Created, TaskListResponse(taskList.id, taskList.name, taskList.boardId, taskList.position))
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Failed to create task list")
            }
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid task list ID")
                return@get
            }
            // Auth: Check if user has access to this task list
            val taskList = taskListService.getTaskListById(id)
            if (taskList != null) {
                call.respond(TaskListResponse(taskList.id, taskList.name, taskList.boardId, taskList.position))
            } else {
                call.respond(HttpStatusCode.NotFound, "Task list not found")
            }
        }

        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid task list ID")
                return@put
            }
            // Auth: Check if user has permission to update this task list
            val request = call.receive<TaskListUpdateRequest>()
            var updated = false
            if (request.name != null) {
                updated = updated or taskListService.updateTaskListName(id, request.name)
            }
            if (request.position != null) {
                // This might require more complex logic if reordering affects other lists
                updated = updated or taskListService.updateTaskListPosition(id, request.position)
            }

            if (updated) {
                // Fetch the updated task list to respond with the latest data
                val updatedTaskList = taskListService.getTaskListById(id)
                if (updatedTaskList != null) {
                    call.respond(HttpStatusCode.OK, TaskListResponse(updatedTaskList.id, updatedTaskList.name, updatedTaskList.boardId, updatedTaskList.position))
                } else {
                     call.respond(HttpStatusCode.NotFound, "Task list not found after update")
                }
            } else {
                // This could also mean no actual changes were requested if only nulls were sent
                call.respond(HttpStatusCode.NotModified, "No changes applied or failed to update task list")
            }
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid task list ID")
                return@delete
            }
            // Auth: Check if user has permission to delete
            // Consider implications for tasks within the list (handled by service or cascade)
            val deleted = taskListService.deleteTaskList(id)
            if (deleted) {
                call.respond(HttpStatusCode.OK, "Task list deleted successfully")
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Failed to delete task list")
            }
        }
    }

    // Get all task lists for a board
    route("/boards/{boardId}/tasklists") {
        get {
            val boardId = call.parameters["boardId"]?.toIntOrNull()
            if (boardId == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid board ID")
                return@get
            }
            // Auth: Check if user has access to boardId
            val taskLists = taskListService.getTaskListsByBoard(boardId)
            val taskListResponses = taskLists.map { TaskListResponse(it.id, it.name, it.boardId, it.position) }
            call.respond(taskListResponses)
        }
    }
}
