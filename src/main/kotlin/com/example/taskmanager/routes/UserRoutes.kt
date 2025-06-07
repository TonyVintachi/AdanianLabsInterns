package com.example.taskmanager.routes

import com.example.taskmanager.db.UserService
import com.example.taskmanager.models.User
import com.example.taskmanager.routes.dto.UserCreateRequest
import com.example.taskmanager.routes.dto.UserLoginRequest
import com.example.taskmanager.routes.dto.UserResponse
import com.example.taskmanager.routes.dto.UserUpdateRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRoutes(userService: UserService) {

    route("/users") {
        post("/register") {
            val request = call.receive<UserCreateRequest>()
            // In a real app, hash the password here before sending to service,
            // or the service does it. For now, passwordHash is treated as the actual hash.
            val userModel = User(id = 0, username = request.username, email = request.email) // id is placeholder
            val createdUser = userService.createUser(userModel, request.passwordHash)
            if (createdUser != null) {
                call.respond(HttpStatusCode.Created, UserResponse(createdUser.id, createdUser.username, createdUser.email))
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Failed to create user")
            }
        }

        post("/login") {
            val request = call.receive<UserLoginRequest>()
            val user = userService.getUserByUsername(request.username)
            if (user != null) {
                // This is a placeholder for actual password hash comparison
                // The UserTable.passwordHash currently stores the plain text passwordHash from UserCreateRequest
                val storedPasswordHash = userService.getUserPasswordHash(user.id) // Need to implement this in UserService
                if (storedPasswordHash == request.passwordHash) {
                    call.respond(UserResponse(user.id, user.username, user.email))
                } else {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid credentials")
                }
            } else {
                call.respond(HttpStatusCode.NotFound, "User not found")
            }
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid user ID")
                return@get
            }
            val user = userService.getUserById(id)
            if (user != null) {
                call.respond(UserResponse(user.id, user.username, user.email))
            } else {
                call.respond(HttpStatusCode.NotFound, "User not found")
            }
        }

        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid user ID")
                return@put
            }
            val request = call.receive<UserUpdateRequest>()
            // For now, any authenticated user can update. Auth to be added later.
            // Password comparison should be done securely if currentPasswordHash is provided.
            val updated = userService.updateUser(
                id,
                request.email,
                request.currentPasswordHash,
                request.newPasswordHash
            )
            if (updated) {
                call.respond(HttpStatusCode.OK, "User updated successfully")
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Failed to update user or invalid current password")
            }
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid user ID")
                return@delete
            }
            // Auth to be added later.
            val deleted = userService.deleteUser(id)
            if (deleted) {
                call.respond(HttpStatusCode.OK, "User deleted successfully")
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Failed to delete user")
            }
        }
    }
}
