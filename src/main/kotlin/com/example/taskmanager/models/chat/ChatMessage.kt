package com.example.taskmanager.models.chat

import java.time.Instant

// This class will be serialized to JSON for WebSocket communication.
// Gson will handle Long to number and String to string.
data class ChatMessage(
    val type: String = "USER_MESSAGE", // e.g., USER_MESSAGE, USER_JOINED, USER_LEFT
    val teamId: Int,
    val userId: Int,
    val username: String,
    val message: String,
    val timestamp: Long = Instant.now().toEpochMilli() // Server sets the final timestamp
)
