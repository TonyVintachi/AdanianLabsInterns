package com.example.taskmanager.routes

import com.example.taskmanager.db.TeamService
import com.example.taskmanager.db.UserService
import com.example.taskmanager.routes.dto.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.teamRoutes(teamService: TeamService, userService: UserService) {

    route("/teams") {
        post {
            val request = call.receive<TeamCreateRequest>()
            // Authentication/Authorization to be added: verify creatorId matches authenticated user
            val team = teamService.createTeam(request.name, request.creatorId)
            if (team != null) {
                call.respond(HttpStatusCode.Created, TeamResponse(team.id, team.name))
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Failed to create team")
            }
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid team ID")
                return@get
            }
            val team = teamService.getTeamById(id)
            if (team != null) {
                call.respond(TeamResponse(team.id, team.name))
            } else {
                call.respond(HttpStatusCode.NotFound, "Team not found")
            }
        }

        get {
            val teams = teamService.getAllTeams()
            val teamResponses = teams.map { TeamResponse(it.id, it.name) }
            call.respond(teamResponses)
        }

        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid team ID")
                return@put
            }
            // Auth: Check if user is admin/owner of the team
            val request = call.receive<TeamUpdateRequest>()
            val updated = teamService.updateTeamName(id, request.name)
            if (updated) {
                call.respond(HttpStatusCode.OK, "Team updated successfully")
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Failed to update team")
            }
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid team ID")
                return@delete
            }
            // Auth: Check if user is admin/owner of the team
            val deleted = teamService.deleteTeam(id)
            if (deleted) {
                call.respond(HttpStatusCode.OK, "Team deleted successfully")
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Failed to delete team")
            }
        }

        post("/{id}/members") {
            val teamId = call.parameters["id"]?.toIntOrNull()
            if (teamId == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid team ID")
                return@post
            }
            // Auth: Check if user can add members to this team
            val request = call.receive<TeamMemberRequest>()
            // Check if user exists (optional, depends on desired strictness)
            if (userService.getUserById(request.userId) == null) {
                 call.respond(HttpStatusCode.NotFound, "User to be added not found")
                 return@post
            }
            val added = teamService.addUserToTeam(request.userId, teamId)
            if (added) {
                call.respond(HttpStatusCode.Created, "User added to team")
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Failed to add user to team or user already in team")
            }
        }

        delete("/{teamId}/members/{userId}") {
            val teamId = call.parameters["teamId"]?.toIntOrNull()
            val userId = call.parameters["userId"]?.toIntOrNull()
            if (teamId == null || userId == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid team ID or user ID")
                return@delete
            }
            // Auth: Check if user can remove members from this team
            val removed = teamService.removeUserFromTeam(userId, teamId)
            if (removed) {
                call.respond(HttpStatusCode.OK, "User removed from team")
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Failed to remove user from team")
            }
        }

        get("/{id}/members") {
            val teamId = call.parameters["id"]?.toIntOrNull()
            if (teamId == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid team ID")
                return@get
            }
            val team = teamService.getTeamById(teamId)
            if (team == null) {
                call.respond(HttpStatusCode.NotFound, "Team not found")
                return@get
            }
            val members = teamService.getTeamMembers(teamId)
            val memberResponses = members.map { UserResponse(it.id, it.username, it.email) }
            call.respond(TeamWithMembersResponse(team.id, team.name, memberResponses))
        }
    }

    // This route is typically under /users/{userId}/teams as per REST conventions
    // but placing it here for grouping by TeamService usage for now.
    route("/users/{userId}/teams") {
        get {
            val userId = call.parameters["userId"]?.toIntOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid user ID")
                return@get
            }
            // Auth: Check if authenticated user matches userId or is an admin
             if (userService.getUserById(userId) == null) {
                 call.respond(HttpStatusCode.NotFound, "User not found")
                 return@get
            }
            val teams = teamService.getUserTeams(userId)
            val teamResponses = teams.map { TeamResponse(it.id, it.name) }
            call.respond(teamResponses)
        }
    }
}
