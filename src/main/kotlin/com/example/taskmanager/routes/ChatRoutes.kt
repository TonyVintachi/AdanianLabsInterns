package com.example.taskmanager.routes

import com.example.taskmanager.models.chat.ChatMessage
import com.google.gson.Gson
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import java.time.Instant

fun Route.chatRoutes() {

    // Thread-safe structure to store active connections
    // Maps teamId to a set of WebSocketSession objects
    val connections = ConcurrentHashMap<Int, MutableSet<WebSocketSession>>()
    val gson = Gson() // For serializing/deserializing ChatMessage

    webSocket("/chat/{teamId}") {
        val teamId = call.parameters["teamId"]?.toIntOrNull()
        val userId = call.request.queryParameters["userId"]?.toIntOrNull()
        val username = call.request.queryParameters["username"]

        if (teamId == null || userId == null || username == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "teamId, userId, and username are required."))
            return@webSocket
        }

        // Add current session to the connections map
        val teamConnections = connections.computeIfAbsent(teamId) { Collections.synchronizedSet(LinkedHashSet()) }
        teamConnections.add(this)

        try {
            // Announce user joined
            val joinMessage = ChatMessage(
                type = "USER_JOINED",
                teamId = teamId,
                userId = userId,
                username = username,
                message = "$username joined the chat."
            )
            val joinJson = gson.toJson(joinMessage)
            teamConnections.forEach { session ->
                if (session != this) { // Don't send to self
                    session.send(Frame.Text(joinJson))
                }
            }

            // Listen for incoming messages
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    // For simplicity, we assume incoming text is the raw message content.
                    // A more robust solution would expect a JSON object and deserialize it.
                    // Or, client sends partial ChatMessage JSON, server completes it.

                    val chatMessage = ChatMessage(
                        teamId = teamId,
                        userId = userId,
                        username = username,
                        message = text, // Assuming the raw text is the message
                        timestamp = Instant.now().toEpochMilli() // Server sets/overwrites timestamp
                    )
                    val messageJson = gson.toJson(chatMessage)

                    // Broadcast the message to all users in the same team
                    teamConnections.forEach { session ->
                        session.send(Frame.Text(messageJson))
                    }
                }
            }
        } catch (e: ClosedReceiveChannelException) {
            // Handle client closing the connection
            println("Connection closed for $userId in team $teamId: ${closeReason.await()}")
        } catch (e: Exception) {
            println("Error for $userId in team $teamId: ${e.localizedMessage}")
            e.printStackTrace()
            close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "An error occurred: ${e.message}"))
        } finally {
            // Remove session on disconnect
            teamConnections.remove(this)
            // Announce user left
            val leaveMessage = ChatMessage(
                type = "USER_LEFT",
                teamId = teamId,
                userId = userId,
                username = username,
                message = "$username left the chat."
            )
            val leaveJson = gson.toJson(leaveMessage)
            teamConnections.forEach { session ->
                session.send(Frame.Text(leaveJson))
            }
            // If the set for a teamId becomes empty, remove the teamId entry
            if (teamConnections.isEmpty()) {
                connections.remove(teamId)
            }
        }
    }
}
