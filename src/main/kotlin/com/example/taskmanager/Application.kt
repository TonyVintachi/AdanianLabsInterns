package com.example.taskmanager

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.gson.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.example.taskmanager.db.UserService
import com.example.taskmanager.routes.userRoutes
import com.example.taskmanager.db.TeamService
import com.example.taskmanager.routes.teamRoutes // Import teamRoutes
import com.example.taskmanager.db.BoardService
import com.example.taskmanager.db.TaskListService
import com.example.taskmanager.db.TaskService
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() {
    val userService = UserService()
    val teamService = TeamService()
    val boardService = BoardService()
    val taskListService = TaskListService()
    val taskService = TaskService()

    runBlocking {
        launch {
            userService.init()
        }
        launch {
            teamService.init()
        }
        launch {
            boardService.init()
        }
        launch {
            taskListService.init()
        }
        launch {
            taskService.init()
        }
    }

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(ContentNegotiation) {
            gson {
                setPrettyPrinting() // Optional: For readable JSON output
                // disableHtmlEscaping() // Optional: Depending on your needs
            }
        }
        routing {
            get("/") {
                call.respondText("Hello, Task Manager!")
            }
            userRoutes(userService)
            teamRoutes(teamService, userService) // Add team routes
        }
    }.start(wait = true)
}
