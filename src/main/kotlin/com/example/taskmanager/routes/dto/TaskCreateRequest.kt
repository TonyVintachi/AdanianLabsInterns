package com.example.taskmanager.routes.dto

// dueDate will be an ISO Local Date Time String e.g., "2023-10-26T10:00:00"
data class TaskCreateRequest(
    val title: String,
    val description: String?,
    val taskListId: Int,
    val creatorId: Int, // Will be from auth context later
    val assigneeId: Int?,
    val dueDate: String?
)
