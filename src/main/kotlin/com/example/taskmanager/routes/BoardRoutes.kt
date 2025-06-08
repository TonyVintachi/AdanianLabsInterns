package com.example.taskmanager.routes

import com.example.taskmanager.db.BoardService
import com.example.taskmanager.routes.dto.BoardCreateRequest
import com.example.taskmanager.routes.dto.BoardResponse
import com.example.taskmanager.routes.dto.BoardUpdateRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.boardRoutes(boardService: BoardService) {

    route("/boards") {
        post {
            val request = call.receive<BoardCreateRequest>()
            // Auth: Check if user is member of teamId and has permission
            val board = boardService.createBoard(request.name, request.teamId)
            if (board != null) {
                call.respond(HttpStatusCode.Created, BoardResponse(board.id, board.name, board.teamId))
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Failed to create board")
            }
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid board ID")
                return@get
            }
            // Auth: Check if user has access to this board
            val board = boardService.getBoardById(id)
            if (board != null) {
                call.respond(BoardResponse(board.id, board.name, board.teamId))
            } else {
                call.respond(HttpStatusCode.NotFound, "Board not found")
            }
        }

        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid board ID")
                return@put
            }
            // Auth: Check if user has permission to update this board
            val request = call.receive<BoardUpdateRequest>()
            val updated = boardService.updateBoardName(id, request.name)
            if (updated) {
                call.respond(HttpStatusCode.OK, "Board updated successfully")
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Failed to update board")
            }
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid board ID")
                return@delete
            }
            // Auth: Check if user has permission to delete this board
            // Consider implications: what happens to task lists and tasks? Handled by service or cascade.
            val deleted = boardService.deleteBoard(id)
            if (deleted) {
                call.respond(HttpStatusCode.OK, "Board deleted successfully")
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Failed to delete board")
            }
        }
    }

    // Get all boards for a team
    route("/teams/{teamId}/boards") {
        get {
            val teamId = call.parameters["teamId"]?.toIntOrNull()
            if (teamId == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid team ID")
                return@get
            }
            // Auth: Check if user is member of teamId
            val boards = boardService.getBoardsByTeam(teamId)
            val boardResponses = boards.map { BoardResponse(it.id, it.name, it.teamId) }
            call.respond(boardResponses)
        }
    }
}
