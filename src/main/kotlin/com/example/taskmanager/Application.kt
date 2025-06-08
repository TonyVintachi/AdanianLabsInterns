package com.example.taskmanager

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.gson.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.* // Import WebSockets plugin
import java.time.Duration // For WebSocket configuration, if needed
import com.example.taskmanager.db.UserService
import com.example.taskmanager.routes.userRoutes
import com.example.taskmanager.db.TeamService
import com.example.taskmanager.routes.teamRoutes
import com.example.taskmanager.db.BoardService
import com.example.taskmanager.routes.boardRoutes
import com.example.taskmanager.db.TaskListService
import com.example.taskmanager.routes.taskListRoutes
import com.example.taskmanager.db.TaskService
import com.example.taskmanager.routes.taskRoutes
import com.example.taskmanager.routes.chatRoutes
import com.example.taskmanager.db.EventService
import com.example.taskmanager.db.UserCalendarSyncService
import com.example.taskmanager.routes.oAuthRoutes
import com.example.taskmanager.calendar.GoogleCalendarService // Import GoogleCalendarService
import com.example.taskmanager.routes.eventRoutes // Import eventRoutes
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() {
    val userService = UserService()
    val teamService = TeamService()
    val boardService = BoardService()
    val taskListService = TaskListService()
    val taskService = TaskService()
    val eventService = EventService()
    val userCalendarSyncService = UserCalendarSyncService()
    val googleCalendarService = GoogleCalendarService(userCalendarSyncService) // Instantiate GoogleCalendarService

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
        launch {
            eventService.init() // Initialize EventService
        }
        launch {
            userCalendarSyncService.init() // Initialize UserCalendarSyncService
        }
    }

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(ContentNegotiation) {
            gson {
                setPrettyPrinting()
                // disableHtmlEscaping()
            }
        }
        install(WebSockets) {
            pingPeriod = Duration.ofSeconds(15)
            timeout = Duration.ofSeconds(15)
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }
        routing {
            get("/") {
                call.respondText("Hello, Task Manager!")
            }
            userRoutes(userService)
            teamRoutes(teamService, userService)
            boardRoutes(boardService)
            taskListRoutes(taskListService)
            taskRoutes(taskService)
            chatRoutes()
            oAuthRoutes(userCalendarSyncService)
            eventRoutes(eventService, googleCalendarService) // Add event routes
        }
    }.start(wait = true)
}
